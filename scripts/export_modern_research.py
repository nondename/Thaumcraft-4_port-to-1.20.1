"""Export the six core TC4 research tabs from the maintained legacy source.

No research completion is granted by this data. Gameplay unlocks remain separate.
"""
import json
import re
import shutil
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEGACY = ROOT / 'mod/src/main/resources/assets/thaumcraft'
OUTPUT = ROOT / 'mod/src/modern/resources/assets/thaumcraft'
SOURCE = ROOT / 'mod/src/main/java/thaumcraft/common/config/research'

ICON_OVERRIDES = {
    'PLANTS': 'blocks/shimmerleaf', 'ETHEREALBLOOM': 'blocks/purifier_seed',
    'NODEJAR': 'blocks/jar_side', 'JARLABEL': 'blocks/jar_side',
    'JARVOID': 'blocks/jar_side_void', 'JARBRAIN': 'items/brain',
    'BANNERS': 'models/banner_blank', 'FOCUSPOUCH': 'items/focuspouch',
    'ELEMENTALPICK': 'items/elementalpick', 'PRIMALARROW': 'items/el_arrow_air',
    'MIRRORHAND': 'items/mirrorhand', 'ARMORFORTRESS': 'items/thaumiumfortresshelm',
    'BOOTSTRAVELLER': 'items/bootstraveler', 'HOVERHARNESS': 'items/hoverharness',
    'GOLEMBELL': 'items/ironbell', 'TRAVELTRUNK': 'models/trunk',
    'ADVANCEDGOLEM': 'items/golem_thaumium', 'ESSENTIARESERVOIR': 'blocks/essentiareservoir',
    'ARMORVOIDFORTRESS': 'items/voidrobehelm',
}
for key, suffix in zip(['TINYHAT','TINYGLASSES','TINYBOWTIE','TINYFEZ','TINYDART','TINYVISOR','TINYARMOR','TINYHAMMER'],
                       ['tophat','glasses','bowtie','fez','dart','visor','armor','mace']):
    ICON_OVERRIDES[key] = 'items/golemdeco' + suffix
for suffix in ['straw','wood','clay','stone','iron','thaumium','flesh','tallow']:
    ICON_OVERRIDES['GOLEM' + suffix.upper()] = 'items/golem_' + suffix
for suffix in ['air','earth','fire','water','order','entropy']:
    ICON_OVERRIDES['UPGRADE' + suffix.upper()] = 'items/golem_upgrade_' + suffix
for key, suffix in [('GATHER','gather'),('FILL','fill'),('EMPTY','empty'),('SORTING','sorting'),('USE','use'),('HARVEST','harvest'),('FISHING','fish'),('LUMBER','lumber'),('GUARD','guard'),('BUTCHER','butcher'),('LIQUID','liquid'),('ALCHEMY','essentia')]:
    ICON_OVERRIDES['CORE' + key] = 'items/golem_core_' + suffix


def arguments(text):
    depth, quoted, escaped, start = 0, False, False, 0
    result = []
    for i, char in enumerate(text):
        if quoted:
            if char == '"' and not escaped:
                quoted = False
            escaped = char == '\\' and not escaped
        elif char == '"':
            quoted = True
        elif char == '(':
            depth += 1
        elif char == ')':
            if depth == 0:
                result.append(text[start:i].strip())
                return result
            depth -= 1
        elif char == ',' and depth == 0:
            result.append(text[start:i].strip())
            start = i + 1
    raise ValueError('Unterminated constructor')


def copy(resource):
    path = resource.removeprefix('thaumcraft:')
    source = LEGACY / path
    if not source.exists():
        raise ValueError(f'Missing reference texture: {resource}')
    target = OUTPUT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
    return resource


def model_texture(model, depth=0):
    if depth > 5:
        return None
    path = LEGACY / 'models' / (model.removeprefix('thaumcraft:') + '.json')
    if not path.exists():
        return None
    data = json.loads(path.read_text(encoding='utf-8'))
    for key in ['layer0', 'all', 'side', 'top', 'particle']:
        value = data.get('textures', {}).get(key, '')
        if value.startswith('thaumcraft:'):
            return copy('thaumcraft:textures/' + value.split(':')[1] + '.png')
    parent = data.get('parent', '')
    return model_texture(parent, depth + 1) if parent.startswith('thaumcraft:') else None


def main():
    entries = []
    missing = []
    for tab in ['Basics', 'Thaumaturgy', 'Alchemy', 'Artifice', 'Golemancy', 'Eldritch']:
        text = (SOURCE / f'ConfigResearch{tab}.java').read_text(encoding='utf-8')
        for match in re.finditer(r'new ResearchItem\(', text):
            end = text.index('.registerResearchItem()', match.end())
            chain = text[match.end():end]
            args = arguments(chain)
            if len(args) < 7:
                # Virtual triggers have no node coordinates or icon in the original browser.
                continue
            key, category = args[0].strip('"'), args[1].strip('"')
            icon = args[6]
            entry = dict(key=key, category=category, x=int(args[3]), y=int(args[4]),
                         parents=[], pages=re.findall(r'new ResearchPage\("([^"]+)"\)', chain),
                         auto='.setAutoUnlock()' in chain, round='.setRound()' in chain,
                         concealed='.setConcealed()' in chain or '.setHidden()' in chain)
            parents = re.search(r'\.setParents\(([^)]+)\)', chain)
            if parents:
                entry['parents'] = re.findall(r'"([^"]+)"', parents[1])
            resource = re.search(r'new ResourceLocation\("thaumcraft",\s*"([^"]+)"\)', icon)
            if resource:
                entry['icon'] = copy('thaumcraft:' + resource[1])
            else:
                if key in ['BASICTHAUMATURGY', 'SCEPTRE']:
                    entry['item'] = 'thaumcraft:wand'
                elif key == 'TRANSGOLD':
                    entry['item'] = 'minecraft:gold_nugget'
                elif key.startswith('CAP_'):
                    entry['icon'] = copy('thaumcraft:textures/items/wand_cap_' + key[4:] + '.png')
                elif key.startswith('ROD_'):
                    suffix = key[4:].removesuffix('_staff')
                    prefix = 'staff_rod_' if key.endswith('_staff') else 'wand_rod_'
                    entry['icon'] = copy('thaumcraft:textures/items/' + prefix + suffix + '.png')
                elif key in ICON_OVERRIDES:
                    entry['icon'] = copy('thaumcraft:textures/' + ICON_OVERRIDES[key] + '.png')
                field = re.search(r'(ConfigItems|ConfigBlocks)\.(\w+)', icon)
                if field and 'icon' not in entry and 'item' not in entry:
                    model = field[2].lower()
                    if field[2] == 'itemResource':
                        meta = re.search(r',\s*1,\s*(\d+)', icon)
                        names = ['alumentum','nitor','thaumiumingot','quicksilver','tallow','brain','amber','cloth','filter','knowledgefragment','mirrorglass','taint_slime','taint_tendril','label','dust','charm','voidingot','voidseed','coin','silveringot']
                        if meta:
                            model += '_' + names[int(meta[1])]
                    texture = model_texture('item/' + model)
                    if texture:
                        entry['icon'] = texture
                if 'icon' not in entry and 'item' not in entry:
                    missing.append((key, icon))
            if 'icon' in entry:
                w, h = struct.unpack('>II', (OUTPUT / entry['icon'].split(':')[1]).read_bytes()[16:24])
                entry['iconWidth'], entry['iconHeight'] = w, h
            entries.append(entry)
    target = OUTPUT / 'research/tree.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(entries, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    for lang in ['en_us', 'ru_ru']:
        path = OUTPUT / 'lang' / (lang + '.json')
        translations = json.loads(path.read_text(encoding='utf-8'))
        for line in (LEGACY / 'lang' / (lang + '.lang')).read_text(encoding='utf-8').splitlines():
            if line.startswith(('tc.research_name.', 'tc.research_text.', 'tc.research_page.', 'tc.research_category.')) and '=' in line:
                key, value = line.split('=', 1)
                translations[key] = value
        path.write_text(json.dumps(translations, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    for name in ['gui_research', 'gui_researchback', 'gui_researchbackeldritch', 'hud']:
        copy(f'thaumcraft:textures/gui/{name}.png')
    for name in ['r_thaumaturgy', 'r_crucible', 'r_artifice', 'r_golemancy', 'r_eldritch']:
        copy(f'thaumcraft:textures/misc/{name}.png')
    copy('thaumcraft:textures/items/thaumonomiconcheat.png')
    print(f'Exported {len(entries)} research entries. Icons requiring explicit mapping:')
    for item in missing:
        print(item)


if __name__ == '__main__':
    main()


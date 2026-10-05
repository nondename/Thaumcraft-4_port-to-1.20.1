param([string]$Repository = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
$resourceRoot = Join-Path $Repository 'mod/src/modern/resources'
$originalRoot = Join-Path $Repository 'mod/thaumcraft_src/assets/thaumcraft/textures'
$assetIds = @('arcane_stone','arcane_pedestal','runic_matrix','alchemy_furnace','alembic','warded_jar','greatwood_planks','silverwood_planks','nitor')
$itemIds = @('alumentum','balanced_shard','salis_mundus','essentia_filter','research_notes','zombie_brain','phial','phial_filled')
$checked = [Collections.Generic.HashSet[string]]::new()
function Test-Model([string]$Id) {
    if (-not $checked.Add($Id)) { return }
    $modelPath = Join-Path $resourceRoot "assets/thaumcraft/models/$Id.json"
    if (-not (Test-Path -LiteralPath $modelPath)) { throw "Missing model: $Id" }
    $model = Get-Content -LiteralPath $modelPath -Raw | ConvertFrom-Json -AsHashtable
    if ($model.parent -like 'thaumcraft:*') { Test-Model $model.parent.Substring(11) }
    foreach ($texture in $model.textures.Values) {
        if ($texture -like 'thaumcraft:*') {
            $texturePath = Join-Path $resourceRoot ('assets/thaumcraft/textures/' + $texture.Substring(11) + '.png')
            if (-not (Test-Path -LiteralPath $texturePath)) { throw "Missing texture in $Id : $texture" }
            $bytes = [IO.File]::ReadAllBytes($texturePath)
            if ($bytes.Length -lt 24 -or $bytes[0] -ne 137 -or $bytes[1] -ne 80 -or $bytes[2] -ne 78 -or $bytes[3] -ne 71) { throw "Invalid PNG: $texture" }
        }
    }
    foreach ($variant in $model.overrides) {
        if ($variant.model -like 'thaumcraft:*') { Test-Model $variant.model.Substring(11) }
    }
    if ($model.loader -eq 'forge:obj') {
        $objPath = Join-Path $resourceRoot ('assets/thaumcraft/' + $model.model.Substring(11))
        if (-not (Test-Path -LiteralPath $objPath)) { throw 'Missing OBJ' }
        $obj = Get-Content -LiteralPath $objPath
        $material = ($obj | Where-Object { $_ -like 'mtllib *' }) -replace '^mtllib ', ''
        $mtlPath = Join-Path (Split-Path $objPath -Parent) $material
        if (-not (Test-Path -LiteralPath $mtlPath)) { throw 'Missing MTL' }
        if (-not ((Get-Content $mtlPath) -match '^map_Kd #texture0$')) { throw 'MTL texture mapping is missing' }
    }
}
foreach ($id in $assetIds) {
    $blockstate = Get-Content (Join-Path $resourceRoot "assets/thaumcraft/blockstates/$id.json") -Raw | ConvertFrom-Json -AsHashtable
    foreach ($variant in $blockstate.variants.Values) { Test-Model $variant.model.Substring(11) }
    Test-Model "item/$id"
    $null = Get-Content (Join-Path $resourceRoot "data/thaumcraft/loot_tables/blocks/$id.json") -Raw | ConvertFrom-Json
}
foreach ($id in $itemIds) { Test-Model "item/$id" }
foreach ($id in @('alumentum','nitor','brain','filter')) {
    $targetId = switch ($id) { 'brain' {'zombie_brain'} 'filter' {'essentia_filter'} default {$id} }
    $original = Join-Path $originalRoot "items/$id.png"
    $target = Join-Path $resourceRoot "assets/thaumcraft/textures/item/$targetId.png"
    if ((Get-FileHash $original).Hash -ne (Get-FileHash $target).Hash) { throw "Texture differs from TC4: $targetId" }
}
$null = Get-Content (Join-Path $resourceRoot 'data/thaumcraft/research/progression.json') -Raw | ConvertFrom-Json
foreach ($lang in @('en_us','ru_ru')) { $null = Get-Content (Join-Path $resourceRoot "assets/thaumcraft/lang/$lang.json") -Raw | ConvertFrom-Json }
& (Join-Path $PSScriptRoot 'verify-pedestal-resources.ps1') -Repository $Repository
Write-Output "Validated $($checked.Count) survival models, texture paths, OBJ material, blockstates, loot, language and original PNG hashes."

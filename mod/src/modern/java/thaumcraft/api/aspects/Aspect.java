package thaumcraft.api.aspects;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Thaumcraft 4.2 aspect definition and registry for the 1.20.1 port.
 *
 * Gameplay values and compositions intentionally mirror the maintained 1.12.2
 * port. The registry remains strictly TC4.2: 48 gameplay aspects. TC6 names
 * exposed below are aliases only and are not iterated registry entries.
 */
public class Aspect {
    private String tag;
    private Aspect[] components;
    private final int color;
    private String chatcolor;
    private final ResourceLocation image;
    private int blend;

    public static final LinkedHashMap<String, Aspect> aspects = new AspectRegistryMap();

    public static final Aspect AIR = new Aspect("aer", 0xFFFF7E, "e", 1);
    public static final Aspect EARTH = new Aspect("terra", 5685248, "2", 1);
    public static final Aspect FIRE = new Aspect("ignis", 16734721, "c", 1);
    public static final Aspect WATER = new Aspect("aqua", 3986684, "3", 1);
    public static final Aspect ORDER = new Aspect("ordo", 14013676, "7", 1);
    public static final Aspect ENTROPY = new Aspect("perditio", 0x404040, "8", 771);
    public static final Aspect VOID = new Aspect("vacuos", 0x888888, new Aspect[]{AIR, ENTROPY}, 771);
    public static final Aspect LIGHT = new Aspect("lux", 0xFFF663, new Aspect[]{AIR, FIRE});
    public static final Aspect WEATHER = new Aspect("tempestas", 0xFFFFFF, new Aspect[]{AIR, WATER});
    public static final Aspect MOTION = new Aspect("motus", 13487348, new Aspect[]{AIR, ORDER});
    public static final Aspect COLD = new Aspect("gelum", 0xE1FFFF, new Aspect[]{FIRE, ENTROPY});
    public static final Aspect CRYSTAL = new Aspect("vitreus", 0x80FFFF, new Aspect[]{EARTH, ORDER});
    public static final Aspect LIFE = new Aspect("victus", 14548997, new Aspect[]{WATER, EARTH});
    public static final Aspect POISON = new Aspect("venenum", 9039872, new Aspect[]{WATER, ENTROPY});
    public static final Aspect ENERGY = new Aspect("potentia", 0xC0FFFF, new Aspect[]{ORDER, FIRE});
    public static final Aspect EXCHANGE = new Aspect("permutatio", 5735255, new Aspect[]{ENTROPY, ORDER});
    public static final Aspect METAL = new Aspect("metallum", 11908557, new Aspect[]{EARTH, CRYSTAL});
    public static final Aspect DEATH = new Aspect("mortuus", 0x887788, new Aspect[]{LIFE, ENTROPY});
    public static final Aspect FLIGHT = new Aspect("volatus", 0xE7E7D7, new Aspect[]{AIR, MOTION});
    public static final Aspect DARKNESS = new Aspect("tenebrae", 0x222222, new Aspect[]{VOID, LIGHT});
    public static final Aspect SOUL = new Aspect("spiritus", 0xEBEBFB, new Aspect[]{LIFE, DEATH});
    public static final Aspect HEAL = new Aspect("sano", 16723764, new Aspect[]{LIFE, ORDER});
    public static final Aspect TRAVEL = new Aspect("iter", 14702683, new Aspect[]{MOTION, EARTH});
    public static final Aspect ELDRITCH = new Aspect("alienis", 0x805080, new Aspect[]{VOID, DARKNESS});
    public static final Aspect MAGIC = new Aspect("praecantatio", 9896128, new Aspect[]{VOID, ENERGY});
    public static final Aspect AURA = new Aspect("auram", 0xFFC0FF, new Aspect[]{MAGIC, AIR});
    public static final Aspect TAINT = new Aspect("vitium", 0x800080, new Aspect[]{MAGIC, ENTROPY});
    public static final Aspect FLUX = TAINT;
    public static final Aspect SLIME = new Aspect("limus", 129024, new Aspect[]{LIFE, WATER});
    public static final Aspect PLANT = new Aspect("herba", 109568, new Aspect[]{LIFE, EARTH});
    public static final Aspect TREE = new Aspect("arbor", 8873265, new Aspect[]{AIR, PLANT});
    public static final Aspect BEAST = new Aspect("bestia", 10445833, new Aspect[]{MOTION, LIFE});
    public static final Aspect FLESH = new Aspect("corpus", 15615885, new Aspect[]{DEATH, BEAST});
    public static final Aspect UNDEAD = new Aspect("exanimis", 3817472, new Aspect[]{MOTION, DEATH});
    public static final Aspect MIND = new Aspect("cognitio", 16761523, new Aspect[]{FIRE, SOUL});
    public static final Aspect SENSES = new Aspect("sensus", 1038847, new Aspect[]{AIR, SOUL});
    public static final Aspect MAN = new Aspect("humanus", 16766912, new Aspect[]{BEAST, MIND});
    public static final Aspect CROP = new Aspect("messis", 14791537, new Aspect[]{PLANT, MAN});
    public static final Aspect MINE = new Aspect("perfodio", 14471896, new Aspect[]{MAN, EARTH});
    public static final Aspect TOOL = new Aspect("instrumentum", 0x4040EE, new Aspect[]{MAN, ORDER});
    public static final Aspect HARVEST = new Aspect("meto", 15641986, new Aspect[]{CROP, TOOL});
    public static final Aspect WEAPON = new Aspect("telum", 0xC05050, new Aspect[]{TOOL, FIRE});
    public static final Aspect ARMOR = new Aspect("tutamen", 49344, new Aspect[]{TOOL, EARTH});
    public static final Aspect HUNGER = new Aspect("fames", 10093317, new Aspect[]{LIFE, VOID});
    public static final Aspect GREED = new Aspect("lucrum", 15121988, new Aspect[]{MAN, HUNGER});
    public static final Aspect CRAFT = new Aspect("fabrico", 8428928, new Aspect[]{MAN, TOOL});
    public static final Aspect CLOTH = new Aspect("pannus", 15395522, new Aspect[]{TOOL, BEAST});
    public static final Aspect MECHANISM = new Aspect("machina", 0x8080A0, new Aspect[]{MOTION, TOOL});
    public static final Aspect TRAP = new Aspect("vinculum", 10125440, new Aspect[]{MOTION, ENTROPY});

    // TC6 compatibility aliases retained by the maintained 1.12.2 port.
    @Deprecated public static final Aspect ALCHEMY = MAGIC;
    @Deprecated public static final Aspect AVERSION = WEAPON;
    @Deprecated public static final Aspect PROTECT = ARMOR;
    @Deprecated public static final Aspect DESIRE = GREED;

    public Aspect(String tag, int color, Aspect[] components, ResourceLocation image, int blend) {
        if (aspects.containsKey(tag)) {
            throw new IllegalArgumentException(tag + " already registered!");
        }
        this.tag = tag;
        this.components = components;
        this.color = color;
        this.image = image;
        this.blend = blend;
        aspects.put(tag, this);
    }

    public Aspect(String tag, int color, Aspect[] components) {
        this(tag, color, components, new ResourceLocation("thaumcraft", "textures/aspects/" + tag.toLowerCase() + ".png"), 1);
    }

    public Aspect(String tag, int color, Aspect[] components, int blend) {
        this(tag, color, components, new ResourceLocation("thaumcraft", "textures/aspects/" + tag.toLowerCase() + ".png"), blend);
    }

    public Aspect(String tag, int color, String chatcolor, int blend) {
        this(tag, color, (Aspect[]) null, blend);
        this.chatcolor = chatcolor;
    }

    public int getColor() {
        return color;
    }

    public String getName() {
        return tag.isEmpty() ? tag : Character.toUpperCase(tag.charAt(0)) + tag.substring(1);
    }

    public String getDescriptionKey() {
        return "tc.aspect.help." + tag;
    }

    public String getLocalizedDescription() {
        return Component.translatable(getDescriptionKey()).getString();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Aspect[] getComponents() {
        return components;
    }

    public void setComponents(Aspect[] components) {
        this.components = components;
    }

    public ResourceLocation getImage() {
        return image;
    }

    public static Aspect getAspect(String tag) {
        Aspect aspect = aspects.get(tag);
        return aspect != null ? aspect : getLegacyAspect(tag);
    }

    private static Aspect getLegacyAspect(Object tag) {
        if (!(tag instanceof String value)) {
            return null;
        }
        return switch (value) {
            case "alkimia" -> MAGIC;
            case "aversio" -> WEAPON;
            case "praemunio" -> ARMOR;
            case "desiderium" -> GREED;
            default -> null;
        };
    }

    private static final class AspectRegistryMap extends LinkedHashMap<String, Aspect> {
        @Override
        public Aspect get(Object key) {
            Aspect aspect = super.get(key);
            return aspect != null ? aspect : getLegacyAspect(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return super.containsKey(key) || getLegacyAspect(key) != null;
        }
    }

    public int getBlend() {
        return blend;
    }

    public void setBlend(int blend) {
        this.blend = blend;
    }

    public boolean isPrimal() {
        return components == null || components.length != 2;
    }

    public static ArrayList<Aspect> getPrimalAspects() {
        ArrayList<Aspect> primals = new ArrayList<>();
        Collection<Aspect> values = aspects.values();
        for (Aspect aspect : values) {
            if (aspect.isPrimal()) {
                primals.add(aspect);
            }
        }
        return primals;
    }

    public static ArrayList<Aspect> getCompoundAspects() {
        ArrayList<Aspect> compounds = new ArrayList<>();
        Collection<Aspect> values = aspects.values();
        for (Aspect aspect : values) {
            if (!aspect.isPrimal()) {
                compounds.add(aspect);
            }
        }
        return compounds;
    }

    public String getChatcolor() {
        return chatcolor;
    }

    public void setChatcolor(String chatcolor) {
        this.chatcolor = chatcolor;
    }
}

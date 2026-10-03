package thaumcraft.common.world;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;
import thaumcraft.api.aspects.Aspect;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern replacement for TC4's BiomeDictionary-backed BiomeHandler.
 *
 * <p>The original node generator did not derive aura from raw temperature/downfall. It averaged
 * the configured values of every BiomeDictionary type attached to the biome, then selected one of
 * those types at random for the biome aspect. Forge 1.20.1 expresses the same idea through biome
 * tags, so this class translates modern tags back into the TC4 type table.</p>
 *
 * <p>Unknown or poorly tagged modded biomes deliberately fall back to aura 100 and no biome
 * aspect, matching BiomeHandler's old exception fallback. New 1.20.1 cave biomes can participate
 * naturally when Forge tags them as lush, wet, sparse, spooky, and so on.</p>
 */
public final class BiomeAuraResolver {
    private static final TagKey<Biome> IS_OCEAN = biomeTag("minecraft", "is_ocean");
    private static final TagKey<Biome> IS_RIVER = biomeTag("minecraft", "is_river");
    private static final TagKey<Biome> IS_FOREST = biomeTag("minecraft", "is_forest");
    private static final TagKey<Biome> IS_JUNGLE = biomeTag("minecraft", "is_jungle");
    private static final TagKey<Biome> IS_BADLANDS = biomeTag("minecraft", "is_badlands");
    private static final TagKey<Biome> IS_BEACH = biomeTag("minecraft", "is_beach");
    private static final TagKey<Biome> IS_NETHER = biomeTag("minecraft", "is_nether");
    private static final TagKey<Biome> IS_END = biomeTag("minecraft", "is_end");

    // Optional compatibility tags. They are harmless when no datapack defines them.
    private static final TagKey<Biome> IS_SAVANNA = biomeTag("forge", "is_savanna");
    private static final TagKey<Biome> IS_FROZEN = biomeTag("forge", "is_frozen");
    private static final TagKey<Biome> IS_TAINTED = biomeTag("thaumcraft", "is_tainted");

    public static Profile resolve(Holder<Biome> biome) {
        List<Rule> rules = new ArrayList<>();

        boolean ocean = biome.is(IS_OCEAN);
        boolean river = biome.is(IS_RIVER);
        boolean forest = biome.is(IS_FOREST);
        boolean jungle = biome.is(IS_JUNGLE);
        boolean badlands = biome.is(IS_BADLANDS);
        boolean beach = biome.is(IS_BEACH);
        boolean nether = biome.is(IS_NETHER);
        boolean end = biome.is(IS_END);

        boolean water = biome.is(Tags.Biomes.IS_WATER);
        boolean wet = biome.is(Tags.Biomes.IS_WET);
        boolean hot = biome.is(Tags.Biomes.IS_HOT);
        boolean desert = biome.is(Tags.Biomes.IS_DESERT);
        boolean dense = biome.is(Tags.Biomes.IS_DENSE);
        boolean snowy = biome.is(Tags.Biomes.IS_SNOWY);
        boolean cold = biome.is(Tags.Biomes.IS_COLD);
        boolean mushroom = biome.is(Tags.Biomes.IS_MUSHROOM);
        boolean coniferous = biome.is(Tags.Biomes.IS_CONIFEROUS);
        boolean sandy = biome.is(Tags.Biomes.IS_SANDY);
        boolean sparse = biome.is(Tags.Biomes.IS_SPARSE);
        boolean plains = biome.is(Tags.Biomes.IS_PLAINS);
        boolean dry = biome.is(Tags.Biomes.IS_DRY);
        boolean swamp = biome.is(Tags.Biomes.IS_SWAMP);
        boolean wasteland = biome.is(Tags.Biomes.IS_WASTELAND);
        boolean lush = biome.is(Tags.Biomes.IS_LUSH);
        boolean magical = biome.is(Tags.Biomes.IS_MAGICAL);
        boolean spooky = biome.is(Tags.Biomes.IS_SPOOKY);
        boolean dead = biome.is(Tags.Biomes.IS_DEAD);

        if (water) add(rules, 100, Aspect.WATER);          // WATER
        if (ocean) add(rules, 120, Aspect.WATER);         // OCEAN
        if (river) add(rules, 100, Aspect.WATER);         // RIVER
        if (wet) add(rules, 80, Aspect.WATER);            // WET

        if (hot) add(rules, 100, Aspect.FIRE);            // HOT
        if (desert) add(rules, 100, Aspect.FIRE);         // DESERT
        if (nether) add(rules, 120, Aspect.FIRE);         // NETHER
        if (badlands) add(rules, 80, Aspect.FIRE);        // MESA

        if (dense) add(rules, 100, Aspect.ORDER);         // DENSE
        if (snowy) add(rules, 80, Aspect.ORDER);          // SNOWY
        if (cold) add(rules, 80, Aspect.ORDER);           // COLD
        // 1.20.1 has no direct Forge equivalent of the old FROZEN type. Frozen ocean/river is the
        // closest semantic match; an optional forge:is_frozen tag lets modded biomes opt in.
        if (biome.is(IS_FROZEN) || (snowy && (ocean || river))) {
            add(rules, 100, Aspect.ORDER);                 // FROZEN
        }
        if (mushroom) add(rules, 140, Aspect.ORDER);      // MUSHROOM

        if (coniferous) add(rules, 100, Aspect.EARTH);    // CONIFEROUS
        if (forest) add(rules, 120, Aspect.EARTH);        // FOREST
        if (sandy) add(rules, 80, Aspect.EARTH);          // SANDY
        if (beach) add(rules, 80, Aspect.EARTH);          // BEACH

        // Forge 1.20.1 has no built-in SAVANNA tag. Vanilla savannas are consistently hot+sparse,
        // unlike deserts/badlands, so retain the TC4 category without hard-coding biome ids.
        boolean savanna = biome.is(IS_SAVANNA)
                || (hot && sparse && !desert && !sandy && !nether);
        if (savanna) add(rules, 80, Aspect.AIR);           // SAVANNA
        if (biome.is(Tags.Biomes.IS_PEAK)) add(rules, 100, Aspect.AIR);   // MOUNTAIN
        if (biome.is(Tags.Biomes.IS_SLOPE)) add(rules, 120, Aspect.AIR); // HILLS
        if (plains) add(rules, 80, Aspect.AIR);            // PLAINS

        if (dry) add(rules, 80, Aspect.ENTROPY);          // DRY
        if (sparse) add(rules, 80, Aspect.ENTROPY);       // SPARSE
        if (swamp) add(rules, 120, Aspect.ENTROPY);       // SWAMP
        if (wasteland) add(rules, 80, Aspect.ENTROPY);    // WASTELAND

        if (jungle) add(rules, 100, Aspect.PLANT);        // JUNGLE
        if (lush) add(rules, 100, Aspect.PLANT);          // LUSH
        if (magical) add(rules, 100, null);               // MAGICAL
        if (end) add(rules, 80, Aspect.VOID);             // END
        if (spooky) add(rules, 80, Aspect.SOUL);          // SPOOKY
        if (dead) add(rules, 50, Aspect.DEATH);           // DEAD

        if (rules.isEmpty()) {
            return new Profile(100, List.of(), biome.is(IS_TAINTED));
        }

        int aura = 0;
        for (Rule rule : rules) {
            aura += rule.auraLevel();
        }
        return new Profile(aura / rules.size(), List.copyOf(rules), biome.is(IS_TAINTED));
    }

    private static void add(List<Rule> rules, int auraLevel, Aspect aspect) {
        rules.add(new Rule(auraLevel, aspect));
    }

    private static TagKey<Biome> biomeTag(String namespace, String path) {
        return TagKey.create(Registries.BIOME, new ResourceLocation(namespace, path));
    }

    private record Rule(int auraLevel, Aspect aspect) {
    }

    public static final class Profile {
        private final int auraLevel;
        private final List<Rule> rules;
        private final boolean tainted;

        private Profile(int auraLevel, List<Rule> rules, boolean tainted) {
            this.auraLevel = auraLevel;
            this.rules = rules;
            this.tainted = tainted;
        }

        public int auraLevel() {
            return auraLevel;
        }

        public boolean tainted() {
            return tainted;
        }

        /** Mirrors BiomeHandler.getRandomBiomeTag: choose one matched biome type, null included. */
        public Aspect randomAspect(RandomSource random) {
            if (rules.isEmpty()) {
                return null;
            }
            return rules.get(random.nextInt(rules.size())).aspect();
        }
    }

    private BiomeAuraResolver() {
    }
}

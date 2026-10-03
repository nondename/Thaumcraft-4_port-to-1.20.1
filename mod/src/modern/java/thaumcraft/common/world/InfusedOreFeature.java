package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.ModOres;

/**
 * TC4 4.2.3.5 ore pass adapted to 1.20.1's -64..320 world and deepslate.
 *
 * <p>Counts and distributions intentionally mirror the original generator: 18 single cinnabar
 * attempts in the bottom fifth of the world, 20 single amber attempts within 24 blocks of the
 * surface, and eight six-block infused-stone veins. One third of infused veins are biased toward
 * the biome's primal aspect just like BiomeHandler.getRandomBiomeTag in TC4.</p>
 */
public final class InfusedOreFeature extends Feature<NoneFeatureConfiguration> {
    private static final int CINNABAR_ATTEMPTS = 18;
    private static final int AMBER_ATTEMPTS = 20;
    private static final int INFUSED_ATTEMPTS = 8;
    private static final int INFUSED_VEIN_SIZE = 6;

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);

    static {
        FEATURES.register("infused_ores", InfusedOreFeature::new);
    }

    public InfusedOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        boolean placed = false;

        placed |= placeCinnabar(context, level, random);
        placed |= placeAmber(context, level, random);
        placed |= placeInfused(context, level, random);
        return placed;
    }

    private static boolean placeCinnabar(FeaturePlaceContext<NoneFeatureConfiguration> context,
                                         WorldGenLevel level, RandomSource random) {
        boolean placed = false;
        int minY = level.getMinBuildHeight();
        int worldHeight = level.getMaxBuildHeight() - minY;
        int deepBand = Math.max(1, worldHeight / 5);

        for (int i = 0; i < CINNABAR_ATTEMPTS; i++) {
            int x = context.origin().getX() + random.nextInt(16);
            int z = context.origin().getZ() + random.nextInt(16);
            int y = minY + random.nextInt(deepBand);
            placed |= placeSingleOre(level, new BlockPos(x, y, z), ModOres.CINNABAR_ORE.get());
        }
        return placed;
    }

    private static boolean placeAmber(FeaturePlaceContext<NoneFeatureConfiguration> context,
                                      WorldGenLevel level, RandomSource random) {
        boolean placed = false;
        for (int i = 0; i < AMBER_ATTEMPTS; i++) {
            int x = context.origin().getX() + random.nextInt(16);
            int z = context.origin().getZ() + random.nextInt(16);
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            int y = surface - random.nextInt(25);
            if (y >= level.getMinBuildHeight() && y < level.getMaxBuildHeight()) {
                placed |= placeSingleOre(level, new BlockPos(x, y, z), ModOres.AMBER_ORE.get());
            }
        }
        return placed;
    }

    private static boolean placeInfused(FeaturePlaceContext<NoneFeatureConfiguration> context,
                                        WorldGenLevel level, RandomSource random) {
        boolean placed = false;
        int minY = level.getMinBuildHeight();

        for (int i = 0; i < INFUSED_ATTEMPTS; i++) {
            int x = context.origin().getX() + random.nextInt(16);
            int z = context.origin().getZ() + random.nextInt(16);
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            int ceiling = Math.max(minY + 5, surface - 5);
            int span = Math.max(1, ceiling - minY);
            int y = minY + random.nextInt(span);
            BlockPos position = new BlockPos(x, y, z);

            String kind = ModOres.NAMES[random.nextInt(ModOres.NAMES.length)];
            if (random.nextInt(3) == 0) {
                Aspect preferred = BiomeAuraResolver.resolve(level.getBiome(position)).randomAspect(random);
                String preferredKind = kindForPrimal(preferred);
                if (preferredKind != null) {
                    kind = preferredKind;
                }
            }

            OreConfiguration config = new OreConfiguration(
                    new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD),
                    ModOres.ORES.get(kind).get().defaultBlockState(),
                    INFUSED_VEIN_SIZE);
            placed |= Feature.ORE.place(config, level, context.chunkGenerator(), random, position);
        }
        return placed;
    }

    private static boolean placeSingleOre(WorldGenLevel level, BlockPos pos, Block ore) {
        if (!level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) {
            return false;
        }
        return level.setBlock(pos, ore.defaultBlockState(), 2);
    }

    private static String kindForPrimal(Aspect aspect) {
        if (aspect == Aspect.AIR) return "air";
        if (aspect == Aspect.FIRE) return "fire";
        if (aspect == Aspect.WATER) return "water";
        if (aspect == Aspect.EARTH) return "earth";
        if (aspect == Aspect.ORDER) return "order";
        if (aspect == Aspect.ENTROPY) return "entropy";
        return null;
    }

    private InfusedOreFeature() {
    }
}

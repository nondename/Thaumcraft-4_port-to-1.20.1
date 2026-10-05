package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.ManaPodBlock;
import thaumcraft.common.blocks.ManaPodBlockEntity;
import thaumcraft.common.blocks.ModMagicalForestContent;

/** TC4 BiomeGenMagicalForest decoration/tree pass adapted to the 1.20.1 feature pipeline. */
public final class MagicalForestDecoratorFeature extends Feature<NoneFeatureConfiguration> {
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String namespace, String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(namespace, path));
    }

    private static final ResourceKey<ConfiguredFeature<?, ?>> FOREST_ROCK = key("minecraft", "forest_rock");
    private static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_BROWN = key("minecraft", "huge_brown_mushroom");
    private static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_RED = key("minecraft", "huge_red_mushroom");
    private static final ResourceKey<ConfiguredFeature<?, ?>> FANCY_OAK = key("minecraft", "fancy_oak");
    private static final ResourceKey<ConfiguredFeature<?, ?>> GREATWOOD = key(Thaumcraft.MODID, "greatwood_tree");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SILVERWOOD = key(Thaumcraft.MODID, "silverwood_tree");

    static {
        FEATURES.register("magical_forest_decorator", MagicalForestDecoratorFeature::new);
    }

    private MagicalForestDecoratorFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int baseX = context.origin().getX() & ~15;
        int baseZ = context.origin().getZ() & ~15;

        // TC4 decorate(): 0..2 mossy-cobblestone blobs before normal biome decoration.
        int blobs = random.nextInt(3);
        for (int i = 0; i < blobs; i++) {
            int x = baseX + random.nextInt(16) + 8;
            int z = baseZ + random.nextInt(16) + 8;
            placeConfigured(level, context, FOREST_ROCK, new BlockPos(x, surface(level, x, z), z));
        }

        // TC4 decorate(): sixteen grid points, each with a 1/40 huge-mushroom chance.
        for (int gx = 0; gx < 4; gx++) {
            for (int gz = 0; gz < 4; gz++) {
                int x = baseX + gx * 4 + 1 + 8 + random.nextInt(3);
                int z = baseZ + gz * 4 + 1 + 8 + random.nextInt(3);
                if (random.nextInt(40) == 0) {
                    placeConfigured(level, context, random.nextBoolean() ? HUGE_BROWN : HUGE_RED,
                            new BlockPos(x, surface(level, x, z), z));
                }
            }
        }

        // TC4 BiomeDecorator.treesPerChunk=2 and func_150567_a():
        // 1/14 Silverwood, otherwise 1/10 Greatwood, otherwise WorldGenBigMagicTree.
        for (int i = 0; i < 2; i++) {
            int x = baseX + random.nextInt(16) + 8;
            int z = baseZ + random.nextInt(16) + 8;
            BlockPos pos = new BlockPos(x, surface(level, x, z), z);
            ResourceKey<ConfiguredFeature<?, ?>> tree = random.nextInt(14) == 0
                    ? SILVERWOOD
                    : (random.nextInt(10) == 0 ? GREATWOOD : FANCY_OAK);
            placeConfigured(level, context, tree, pos);
        }

        // TC4 runs WorldGenManaPods ten times from y=64.
        for (int i = 0; i < 10; i++) {
            generateManaPod(level, random, baseX + random.nextInt(16) + 8, baseZ + random.nextInt(16) + 8);
        }

        // TC4 makes eight Manashroom attempts near surface wood.
        for (int i = 0; i < 8; i++) {
            int x = baseX + random.nextInt(16);
            int z = baseZ + random.nextInt(16);
            int y = surface(level, x, z);
            while (y > 50 && !level.getBlockState(new BlockPos(x, y, z)).is(Blocks.GRASS_BLOCK)) y--;
            BlockPos ground = new BlockPos(x, y, z);
            BlockPos plant = ground.above();
            if (level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                    && level.getBlockState(plant).canBeReplaced()
                    && adjacentToLog(level, plant)) {
                level.setBlock(plant, ModMagicalForestContent.MANASHROOM.get().defaultBlockState(), 2);
            }
        }
        return true;
    }

    private static int surface(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
    }

    private static void generateManaPod(WorldGenLevel level, RandomSource random, int startX, int startZ) {
        int x = startX;
        int z = startZ;
        int top = Math.min(128, surface(level, startX, startZ));
        for (int y = 64; y < top; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.below())) {
                if (level.getBlockState(pos.above()).is(BlockTags.LOGS)) {
                    int age = Math.min(7, 3 + random.nextInt(5));
                    level.setBlock(pos, ModMagicalForestContent.MANA_POD.get().defaultBlockState()
                            .setValue(ManaPodBlock.AGE, age), 2);
                    if (level.getBlockEntity(pos) instanceof ManaPodBlockEntity pod) {
                        if (random.nextInt(8) == 0) pod.setAspect(Aspect.PLANT);
                        else {
                            var primals = Aspect.getPrimalAspects();
                            pod.setAspect(primals.get(random.nextInt(primals.size())));
                        }
                    }
                    return;
                }
            } else {
                x = startX + random.nextInt(4) - random.nextInt(4);
                z = startZ + random.nextInt(4) - random.nextInt(4);
            }
        }
    }

    private static boolean adjacentToLog(WorldGenLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            if ((dx != 0 || dy != 0 || dz != 0) && level.getBlockState(pos.offset(dx, dy, dz)).is(BlockTags.LOGS)) return true;
        }
        return false;
    }

    private static void placeConfigured(WorldGenLevel level, FeaturePlaceContext<NoneFeatureConfiguration> context,
                                        ResourceKey<ConfiguredFeature<?, ?>> key, BlockPos pos) {
        level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(key).ifPresent(holder ->
                holder.value().place(level, context.chunkGenerator(), context.random(), pos));
    }
}

package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ModMagicalTrees;

/**
 * Procedural ports of TC4 WorldGenGreatwoodTrees and WorldGenSilverwoodTrees.
 * The generators intentionally keep the old scale and silhouettes instead of mapping them onto
 * vanilla oak/birch configured trees.
 */
public final class MagicalTreeFeature extends Feature<NoneFeatureConfiguration> {
    private enum Kind { GREATWOOD, SILVERWOOD }

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);

    static {
        FEATURES.register("greatwood_tree", () -> new MagicalTreeFeature(Kind.GREATWOOD));
        FEATURES.register("silverwood_tree", () -> new MagicalTreeFeature(Kind.SILVERWOOD));
    }

    private final Kind kind;

    private MagicalTreeFeature(Kind kind) {
        super(NoneFeatureConfiguration.CODEC);
        this.kind = kind;
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return kind == Kind.GREATWOOD
                ? generateGreatwood(context.level(), context.origin(), context.random())
                : generateSilverwood(context.level(), context.origin(), context.random());
    }

    private static boolean generateGreatwood(WorldGenLevel level, BlockPos origin, RandomSource random) {
        if (!canRoot(level, origin) || origin.getY() + 30 >= level.getMaxBuildHeight()) {
            return false;
        }

        // TC4 chooses 11..21 for each greatwood section, with trunk height ~= 61.8% of that.
        int firstLimit = 11 + random.nextInt(11);
        int firstTrunk = Math.max(6, (int) (firstLimit * 0.618D));
        int secondLimit = 11 + random.nextInt(11);
        int secondTrunk = Math.max(6, (int) (secondLimit * 0.618D));

        if (!hasVerticalRoom(level, origin, firstTrunk + secondTrunk + 8, 3)) {
            return false;
        }

        growGreatwoodSection(level, origin, firstLimit, firstTrunk, random);
        BlockPos upper = origin.above(firstTrunk);
        growGreatwoodSection(level, upper, secondLimit, secondTrunk, random);
        return true;
    }

    private static void growGreatwoodSection(WorldGenLevel level, BlockPos base, int heightLimit, int trunkHeight,
                                             RandomSource random) {
        BlockState log = ModMagicalTrees.GREATWOOD_LOG.get().defaultBlockState();
        BlockState leaves = ModMagicalTrees.GREATWOOD_LEAVES.get().defaultBlockState();

        // The legacy generator used a 2x2 trunk and then connected multiple leaf nodes back into it.
        for (int y = 0; y <= trunkHeight; y++) {
            setLog(level, base.offset(0, y, 0), log, Direction.Axis.Y);
            setLog(level, base.offset(1, y, 0), log, Direction.Axis.Y);
            setLog(level, base.offset(0, y, 1), log, Direction.Axis.Y);
            setLog(level, base.offset(1, y, 1), log, Direction.Axis.Y);
        }

        int start = Math.max(4, (int) (heightLimit * 0.30F));
        int nodeCount = Math.max(5, heightLimit / 2);
        BlockPos trunkCenter = base.offset(0, trunkHeight, 0);

        for (int i = 0; i < nodeCount; i++) {
            int y = start + random.nextInt(Math.max(1, heightLimit - start));
            float half = heightLimit / 2.0F;
            float dy = half - y;
            float layer = Math.abs(dy) >= half ? 0.0F : (float) Math.sqrt(half * half - dy * dy) * 0.5F;
            double radius = 1.2D * layer * (random.nextFloat() + 0.328D);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int dx = Mth.floor(radius * Math.sin(angle) + 0.5D);
            int dz = Mth.floor(radius * Math.cos(angle) + 0.5D);
            BlockPos node = base.offset(dx, y, dz);

            leafBlob(level, node, leaves, 3, 4, random);
            connect(level, trunkCenter.offset(0, Math.min(0, y - trunkHeight), 0), node, log);
        }

        leafBlob(level, base.above(heightLimit - 4), leaves, 3, 4, random);
    }

    private static boolean generateSilverwood(WorldGenLevel level, BlockPos origin, RandomSource random) {
        if (!canRoot(level, origin)) {
            return false;
        }

        // Saplings in TC4 used min=7/random=5 -> 7..11 blocks.
        int height = 7 + random.nextInt(5);
        if (!hasVerticalRoom(level, origin, height + 5, 5)) {
            return false;
        }

        BlockState log = ModMagicalTrees.SILVERWOOD_LOG.get().defaultBlockState();
        BlockState leaves = ModMagicalTrees.SILVERWOOD_LEAVES.get().defaultBlockState();

        int start = height - 5;
        int end = height + 3 + random.nextInt(3);
        for (int y = start; y <= end; y++) {
            int centerY = Mth.clamp(y, height - 3, height);
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    double dist = dx * dx + (y - centerY) * (double) (y - centerY) + dz * dz;
                    if (dist < 10 + random.nextInt(8)) {
                        setLeaf(level, origin.offset(dx, y, dz), leaves);
                    }
                }
            }
        }

        // Cross-shaped TC4 trunk.
        for (int y = 0; y < height; y++) {
            setLog(level, origin.offset(0, y, 0), log, Direction.Axis.Y);
            setLog(level, origin.offset(-1, y, 0), log, Direction.Axis.Y);
            setLog(level, origin.offset(1, y, 0), log, Direction.Axis.Y);
            setLog(level, origin.offset(0, y, -1), log, Direction.Axis.Y);
            setLog(level, origin.offset(0, y, 1), log, Direction.Axis.Y);
        }
        setLog(level, origin.above(height), log, Direction.Axis.Y);

        // Buttress roots from the legacy generator.
        int[][] diagonals = {{-1, -1}, {1, 1}, {-1, 1}, {1, -1}};
        for (int[] d : diagonals) {
            setLog(level, origin.offset(d[0], 0, d[1]), log, Direction.Axis.Y);
            if (random.nextInt(3) != 0) {
                setLog(level, origin.offset(d[0], 1, d[1]), log, Direction.Axis.Y);
            }
            setLog(level, origin.offset(d[0], height - 4, d[1]), log, Direction.Axis.Y);
            if (random.nextInt(3) == 0) {
                setLog(level, origin.offset(d[0], height - 5, d[1]), log, Direction.Axis.Y);
            }
        }
        setLog(level, origin.offset(-2, 0, 0), log, Direction.Axis.X);
        setLog(level, origin.offset(2, 0, 0), log, Direction.Axis.X);
        setLog(level, origin.offset(0, 0, -2), log, Direction.Axis.Z);
        setLog(level, origin.offset(0, 0, 2), log, Direction.Axis.Z);
        setLog(level, origin.offset(-2, -1, 0), log, Direction.Axis.Y);
        setLog(level, origin.offset(2, -1, 0), log, Direction.Axis.Y);
        setLog(level, origin.offset(0, -1, -2), log, Direction.Axis.Y);
        setLog(level, origin.offset(0, -1, 2), log, Direction.Axis.Y);

        setLog(level, origin.offset(-2, height - 4, 0), log, Direction.Axis.X);
        setLog(level, origin.offset(2, height - 4, 0), log, Direction.Axis.X);
        setLog(level, origin.offset(0, height - 4, -2), log, Direction.Axis.Z);
        setLog(level, origin.offset(0, height - 4, 2), log, Direction.Axis.Z);

        // The original sometimes embeds a PURE aura node inside a special silverwood-knot log.
        // The modern port deliberately leaves that for SilverwoodNodeLog, so we do not replace a
        // visible trunk block with the current invisible airy-node carrier.
        return true;
    }

    private static boolean canRoot(WorldGenLevel level, BlockPos origin) {
        BlockState soil = level.getBlockState(origin.below());
        return soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.COARSE_DIRT)
                || soil.is(Blocks.PODZOL) || soil.is(Blocks.ROOTED_DIRT) || soil.is(Blocks.MOSS_BLOCK);
    }

    private static boolean hasVerticalRoom(WorldGenLevel level, BlockPos origin, int height, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= height; y++) {
            int r = y < 2 ? 1 : radius;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir() && !state.canBeReplaced() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void leafBlob(WorldGenLevel level, BlockPos center, BlockState leaves, int radius, int height,
                                 RandomSource random) {
        for (int y = 0; y < height; y++) {
            int r = (y == 0 || y == height - 1) ? radius - 1 : radius;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if ((dx * dx + dz * dz) <= r * r + random.nextInt(3)) {
                        setLeaf(level, center.offset(dx, y, dz), leaves);
                    }
                }
            }
        }
    }

    private static void connect(WorldGenLevel level, BlockPos from, BlockPos to, BlockState log) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) return;
        Direction.Axis axis = Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            BlockPos p = new BlockPos(
                    Mth.floor(from.getX() + dx * t + 0.5D),
                    Mth.floor(from.getY() + dy * t + 0.5D),
                    Mth.floor(from.getZ() + dz * t + 0.5D));
            setLog(level, p, log, Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz)) ? Direction.Axis.Y : axis);
        }
    }

    private static void setLeaf(WorldGenLevel level, BlockPos pos, BlockState leaves) {
        BlockState old = level.getBlockState(pos);
        if (old.isAir() || old.canBeReplaced() || old.is(net.minecraft.tags.BlockTags.LEAVES)) {
            level.setBlock(pos, leaves, 2);
        }
    }

    private static void setLog(WorldGenLevel level, BlockPos pos, BlockState log, Direction.Axis axis) {
        BlockState old = level.getBlockState(pos);
        if (old.isAir() || old.canBeReplaced() || old.is(net.minecraft.tags.BlockTags.LEAVES)
                || old.is(ModMagicalTrees.GREATWOOD_LOG.get()) || old.is(ModMagicalTrees.SILVERWOOD_LOG.get())) {
            level.setBlock(pos, log.setValue(RotatedPillarBlock.AXIS, axis), 2);
        }
    }
}

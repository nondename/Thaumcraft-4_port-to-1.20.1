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
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.nodes.ModNodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedural ports of TC4 WorldGenGreatwoodTrees and WorldGenSilverwoodTrees.
 * Keeps the legacy silhouettes instead of mapping them onto vanilla tree features.
 */
public final class MagicalTreeFeature extends Feature<NoneFeatureConfiguration> {
    private enum Kind { GREATWOOD, SILVERWOOD }

    private record GreatwoodLeafNode(BlockPos pos, int branchBaseY) {}

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
        if (!canRoot(level, origin)) {
            return false;
        }

        // TC4: heightLimitLimit=11 and heightLimit = 11 + random.nextInt(11).
        int heightLimit = 11 + random.nextInt(11);
        int trunkHeight = Math.max(1, (int) (heightLimit * 0.618D));

        // TC4 performs a second, wider pass starting at the top of the first trunk.
        int maxY = origin.getY() + trunkHeight + heightLimit + 4;
        if (maxY >= level.getMaxBuildHeight() || !hasTrunkRoom(level, origin, trunkHeight * 2 + 2)) {
            return false;
        }

        growGreatwoodPass(level, origin, heightLimit, 1.20D, random);
        growGreatwoodPass(level, origin.above(trunkHeight), heightLimit, 1.66D, random);
        return true;
    }

    /**
     * Faithful 1.20.1 adaptation of TC4's generateLeafNodeList/generateLeaves/
     * generateLeafNodeBases/generateTrunk sequence.
     */
    private static void growGreatwoodPass(WorldGenLevel level, BlockPos base, int heightLimit,
                                          double scaleWidth, RandomSource random) {
        BlockState log = ModMagicalTrees.GREATWOOD_LOG.get().defaultBlockState();
        BlockState leaves = ModMagicalTrees.GREATWOOD_LEAVES.get().defaultBlockState();

        final int leafDistanceLimit = 4;
        final int trunkHeight = Math.max(1, (int) (heightLimit * 0.618D));
        final int nodesPerLayer = Math.max(1,
                (int) (1.382D + Math.pow(0.9D * heightLimit / 13.0D, 2.0D)));

        List<GreatwoodLeafNode> nodes = new ArrayList<>();

        // Original generator always seeds one central leaf node at heightLimit - 4.
        int topNodeY = heightLimit - leafDistanceLimit;
        nodes.add(new GreatwoodLeafNode(base.above(topNodeY), trunkHeight));

        for (int relY = topNodeY - 1; relY >= 0; relY--) {
            float layer = greatwoodLayerSize(heightLimit, relY);
            if (layer < 0.0F) {
                continue;
            }

            for (int i = 0; i < nodesPerLayer; i++) {
                double radialDistance = scaleWidth * layer * (random.nextFloat() + 0.328D);
                double angle = random.nextFloat() * Math.PI * 2.0D;
                int dx = Mth.floor(radialDistance * Math.sin(angle) + 0.5D);
                int dz = Mth.floor(radialDistance * Math.cos(angle) + 0.5D);

                BlockPos nodePos = base.offset(dx, relY, dz);
                BlockPos nodeTop = nodePos.above(leafDistanceLimit);
                if (!lineClearForTree(level, nodePos, nodeTop)) {
                    continue;
                }

                double horizontalDistance = Math.sqrt((double) dx * dx + (double) dz * dz);
                int branchBaseY = (int) (relY - horizontalDistance * 0.38D);
                branchBaseY = Math.min(branchBaseY, trunkHeight);

                BlockPos branchBase = base.above(branchBaseY);
                if (!lineClearForTree(level, branchBase, nodePos)) {
                    continue;
                }

                nodes.add(new GreatwoodLeafNode(nodePos, branchBaseY));
            }
        }

        // TC4 leaf nodes are four circular layers with radii 2,3,3,2.
        for (GreatwoodLeafNode node : nodes) {
            generateGreatwoodLeafNode(level, node.pos(), leaves);
        }

        // Branches only exist for nodes whose base begins above 20% of heightLimit.
        for (GreatwoodLeafNode node : nodes) {
            if (node.branchBaseY() >= heightLimit * 0.20D) {
                connect(level, base.above(node.branchBaseY()), node.pos(), log);
            }
        }

        // 2x2 trunk, exactly as TC4 trunkSize=2.
        for (int y = 0; y <= trunkHeight; y++) {
            setLog(level, base.offset(0, y, 0), log, Direction.Axis.Y);
            setLog(level, base.offset(1, y, 0), log, Direction.Axis.Y);
            setLog(level, base.offset(0, y, 1), log, Direction.Axis.Y);
            setLog(level, base.offset(1, y, 1), log, Direction.Axis.Y);
        }
    }

    private static float greatwoodLayerSize(int heightLimit, int relY) {
        if (relY < heightLimit * 0.30D) {
            return -1.618F;
        }

        float half = heightLimit / 2.0F;
        float delta = half - relY;
        float radius;
        if (delta == 0.0F) {
            radius = half;
        } else if (Math.abs(delta) >= half) {
            radius = 0.0F;
        } else {
            radius = (float) Math.sqrt(half * half - delta * delta);
        }
        return radius * 0.5F;
    }

    private static void generateGreatwoodLeafNode(WorldGenLevel level, BlockPos base, BlockState leaves) {
        for (int dy = 0; dy < 4; dy++) {
            float radius = (dy == 0 || dy == 3) ? 2.0F : 3.0F;
            int extent = (int) (radius + 0.618D);
            for (int dx = -extent; dx <= extent; dx++) {
                for (int dz = -extent; dz <= extent; dz++) {
                    double dist = Math.pow(Math.abs(dx) + 0.5D, 2.0D)
                            + Math.pow(Math.abs(dz) + 0.5D, 2.0D);
                    if (dist <= radius * radius) {
                        setLeaf(level, base.offset(dx, dy, dz), leaves);
                    }
                }
            }
        }
    }

    private static boolean generateSilverwood(WorldGenLevel level, BlockPos origin, RandomSource random) {
        if (!canRoot(level, origin)) {
            return false;
        }

        // TC4 default constructor: minHeight=8, extraHeight=5 -> 8..12.
        int height = 8 + random.nextInt(5);
        if (!hasVerticalRoom(level, origin, height + 5, 5)) {
            return false;
        }

        BlockState log = ModMagicalTrees.SILVERWOOD_LOG.get().defaultBlockState();
        BlockState leaves = ModMagicalTrees.SILVERWOOD_LEAVES.get().defaultBlockState();

        // Exact legacy canopy volume.
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

        // Cross-shaped TC4 trunk. The centre can become the metadata-2 silverwood knot,
        // represented here by a real AuraNodeBlockEntity.
        int nodeChance = Math.max(1, (int) (height * 1.5F));
        boolean lastWasNode = false;
        for (int y = 0; y < height; y++) {
            BlockPos center = origin.offset(0, y, 0);
            boolean makeNode = y > 0 && !lastWasNode && random.nextInt(nodeChance) == 0;
            if (makeNode) {
                BlockState knot = ModNodes.SILVERWOOD_NODE_LOG.get().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                level.setBlock(center, knot, 2);
                if (level.getBlockEntity(center) instanceof AuraNodeBlockEntity node) {
                    var generated = AuraNodeGenerator.generate(level, center, random, true, false, false);
                    node.initialize(generated.type(), generated.modifier(), generated.aspects());
                }
                nodeChance += height;
                lastWasNode = true;
            } else {
                setLog(level, center, log, Direction.Axis.Y);
                lastWasNode = false;
            }

            setLog(level, origin.offset(-1, y, 0), log, Direction.Axis.Y);
            setLog(level, origin.offset(1, y, 0), log, Direction.Axis.Y);
            setLog(level, origin.offset(0, y, -1), log, Direction.Axis.Y);
            setLog(level, origin.offset(0, y, 1), log, Direction.Axis.Y);
        }
        setLog(level, origin.above(height), log, Direction.Axis.Y);

        // TC4 buttress roots.
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
        return true;
    }

    private static boolean canRoot(WorldGenLevel level, BlockPos origin) {
        BlockState soil = level.getBlockState(origin.below());
        return soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.COARSE_DIRT)
                || soil.is(Blocks.PODZOL) || soil.is(Blocks.ROOTED_DIRT) || soil.is(Blocks.MOSS_BLOCK);
    }

    private static boolean hasTrunkRoom(WorldGenLevel level, BlockPos origin, int height) {
        for (int y = 0; y <= height; y++) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    BlockState state = level.getBlockState(origin.offset(dx, y, dz));
                    if (!replaceableForTree(state)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean hasVerticalRoom(WorldGenLevel level, BlockPos origin, int height, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= height; y++) {
            int r = y < 2 ? 1 : radius;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    if (!replaceableForTree(level.getBlockState(cursor))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean lineClearForTree(WorldGenLevel level, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) {
            return replaceableForTree(level.getBlockState(from));
        }

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            BlockPos p = new BlockPos(
                    Mth.floor(from.getX() + dx * t + 0.5D),
                    Mth.floor(from.getY() + dy * t + 0.5D),
                    Mth.floor(from.getZ() + dz * t + 0.5D));
            if (!replaceableForTree(level.getBlockState(p))) {
                return false;
            }
        }
        return true;
    }

    private static boolean replaceableForTree(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(net.minecraft.tags.BlockTags.LEAVES)
                || state.is(ModMagicalTrees.GREATWOOD_LOG.get()) || state.is(ModMagicalTrees.SILVERWOOD_LOG.get());
    }

    private static void connect(WorldGenLevel level, BlockPos from, BlockPos to, BlockState log) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) {
            return;
        }

        Direction.Axis horizontalAxis = Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            BlockPos p = new BlockPos(
                    Mth.floor(from.getX() + dx * t + 0.5D),
                    Mth.floor(from.getY() + dy * t + 0.5D),
                    Mth.floor(from.getZ() + dz * t + 0.5D));
            Direction.Axis axis = Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz))
                    ? Direction.Axis.Y : horizontalAxis;
            setLog(level, p, log, axis);
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
        if (replaceableForTree(old)) {
            level.setBlock(pos, log.setValue(RotatedPillarBlock.AXIS, axis), 2);
        }
    }
}

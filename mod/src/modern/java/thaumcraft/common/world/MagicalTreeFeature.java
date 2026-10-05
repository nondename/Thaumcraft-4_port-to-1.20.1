package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ModMagicalTrees;
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.nodes.ModNodes;

import java.util.ArrayList;
import java.util.List;

/** TC4 Greatwood/Silverwood worldgen adapted to Forge 1.20.1 without changing legacy math. */
public final class MagicalTreeFeature extends Feature<NoneFeatureConfiguration> {
    private enum Kind { GREATWOOD, SILVERWOOD_WORLDGEN, SILVERWOOD_SAPLING }
    private record GreatwoodLeafNode(BlockPos pos, int branchBaseY) {}
    private record GreatwoodBase(BlockPos pos, int heightLimit) {}

    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);

    static {
        FEATURES.register("greatwood_tree", () -> new MagicalTreeFeature(Kind.GREATWOOD));
        FEATURES.register("silverwood_tree", () -> new MagicalTreeFeature(Kind.SILVERWOOD_WORLDGEN));
        FEATURES.register("silverwood_tree_sapling", () -> new MagicalTreeFeature(Kind.SILVERWOOD_SAPLING));
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
        return switch (kind) {
            case GREATWOOD -> generateGreatwood(context.level(), context.origin(), context.random());
            case SILVERWOOD_WORLDGEN -> generateSilverwood(context.level(), context.origin(), context.random(), 8, true);
            case SILVERWOOD_SAPLING -> generateSilverwood(context.level(), context.origin(), context.random(), 7, false);
        };
    }

    private static boolean generateGreatwood(WorldGenLevel level, BlockPos origin, RandomSource worldRandom) {
        boolean spiders = worldRandom.nextInt(8) == 0;
        RandomSource random = RandomSource.create(worldRandom.nextLong());

        int requestedHeightLimit = 11 + random.nextInt(11);
        GreatwoodBase chosen = chooseGreatwoodBase(level, origin, requestedHeightLimit);
        if (chosen == null) return false;

        BlockPos base = chosen.pos();
        int heightLimit = chosen.heightLimit();
        int trunkHeight = Math.max(1, (int) (heightLimit * 0.618D));
        if (base.getY() + trunkHeight + heightLimit + 4 >= level.getMaxBuildHeight()) return false;

        growGreatwoodPass(level, base, heightLimit, 1.20D, random);
        growGreatwoodPass(level, base.above(trunkHeight), heightLimit, 1.66D, random);
        if (spiders) decorateSpiderGreatwood(level, origin, worldRandom);
        return true;
    }

    private static GreatwoodBase chooseGreatwoodBase(WorldGenLevel level, BlockPos origin, int requestedHeight) {
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                int candidateHeight = requestedHeight;
                boolean valid = true;
                for (int tx = 0; tx < 2 && valid; tx++) {
                    for (int tz = 0; tz < 2; tz++) {
                        BlockPos columnBase = origin.offset(a + tx, 0, b + tz);
                        if (!canRoot(level, columnBase)) {
                            valid = false;
                            break;
                        }
                        int obstruction = firstBlockedDistance(level, columnBase, candidateHeight - 1);
                        if (obstruction >= 0) {
                            if (obstruction < 6) {
                                valid = false;
                                break;
                            }
                            candidateHeight = Math.min(candidateHeight, obstruction);
                        }
                    }
                }
                if (valid) return new GreatwoodBase(origin.offset(a, 0, b), candidateHeight);
            }
        }
        return null;
    }

    private static int firstBlockedDistance(WorldGenLevel level, BlockPos from, int distance) {
        for (int y = 0; y <= distance; y++) {
            if (!replaceableForTree(level.getBlockState(from.above(y)))) return y;
        }
        return -1;
    }

    private static void growGreatwoodPass(WorldGenLevel level, BlockPos base, int heightLimit,
                                          double scaleWidth, RandomSource random) {
        BlockState log = ModMagicalTrees.GREATWOOD_LOG.get().defaultBlockState();
        BlockState leaves = ModMagicalTrees.GREATWOOD_LEAVES.get().defaultBlockState();
        final int leafDistanceLimit = 4;
        final int trunkHeight = Math.max(1, (int) (heightLimit * 0.618D));
        final int nodesPerLayer = Math.max(1,
                (int) (1.382D + Math.pow(0.9D * heightLimit / 13.0D, 2.0D)));

        List<GreatwoodLeafNode> nodes = new ArrayList<>();
        int topNodeY = heightLimit - leafDistanceLimit;
        nodes.add(new GreatwoodLeafNode(base.above(topNodeY), trunkHeight));

        int relY = topNodeY - 1;
        while (relY >= 0) {
            float layer = greatwoodLayerSize(heightLimit, relY);
            if (layer >= 0.0F) {
                for (int i = 0; i < nodesPerLayer; i++) {
                    double radialDistance = scaleWidth * layer * (random.nextFloat() + 0.328D);
                    double angle = random.nextFloat() * Math.PI * 2.0D;
                    int dx = Mth.floor(radialDistance * Math.sin(angle) + 0.5D);
                    int dz = Mth.floor(radialDistance * Math.cos(angle) + 0.5D);
                    BlockPos nodePos = base.offset(dx, relY, dz);
                    if (!lineClearForTree(level, nodePos, nodePos.above(leafDistanceLimit))) continue;

                    double horizontalDistance = Math.sqrt((double) dx * dx + (double) dz * dz);
                    int branchBaseY = Math.min((int) (relY - horizontalDistance * 0.38D), trunkHeight);
                    if (!lineClearForTree(level, base.above(branchBaseY), nodePos)) continue;
                    nodes.add(new GreatwoodLeafNode(nodePos, branchBaseY));
                }
            }
            relY--;
        }

        for (GreatwoodLeafNode node : nodes) generateGreatwoodLeafNode(level, node.pos(), leaves);
        for (GreatwoodLeafNode node : nodes) {
            if (node.branchBaseY() >= heightLimit * 0.20D) {
                connect(level, base.above(node.branchBaseY()), node.pos(), log);
            }
        }
        for (int y = 0; y <= trunkHeight; y++) {
            setLog(level, base.offset(0, y, 0), log, Direction.Axis.Y);
            setLog(level, base.offset(1, y, 0), log, Direction.Axis.Y);
            setLog(level, base.offset(0, y, 1), log, Direction.Axis.Y);
            setLog(level, base.offset(1, y, 1), log, Direction.Axis.Y);
        }
    }

    private static float greatwoodLayerSize(int heightLimit, int relY) {
        if (relY < heightLimit * 0.30D) return -1.618F;
        float half = heightLimit / 2.0F;
        float delta = half - relY;
        float radius;
        if (delta == 0.0F) radius = half;
        else if (Math.abs(delta) >= half) radius = 0.0F;
        else radius = (float) Math.sqrt(half * half - delta * delta);
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
                    if (dist <= radius * radius) setLeaf(level, base.offset(dx, dy, dz), leaves);
                }
            }
        }
    }

    private static void decorateSpiderGreatwood(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos spawnerPos = origin.below();
        level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 3);
        if (level.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(EntityType.CAVE_SPIDER, random);
            spawner.setChanged();

            for (int i = 0; i < 50; i++) {
                BlockPos webPos = origin.offset(
                        -7 + random.nextInt(14),
                        random.nextInt(10),
                        -7 + random.nextInt(14));
                if (level.getBlockState(webPos).isAir() && isTouchingGreatwood(level, webPos)) {
                    level.setBlock(webPos, Blocks.COBWEB.defaultBlockState(), 3);
                }
            }

            BlockPos chestPos = origin.below(2);
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                chest.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, random.nextLong());
                chest.setChanged();
            }
        }
    }

    private static boolean isTouchingGreatwood(WorldGenLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.is(ModMagicalTrees.GREATWOOD_LEAVES.get()) || state.is(ModMagicalTrees.GREATWOOD_LOG.get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean generateSilverwood(WorldGenLevel level, BlockPos origin, RandomSource random,
                                              int minHeight, boolean naturalWorldgen) {
        if (!canRoot(level, origin)) return false;
        int height = minHeight + random.nextInt(5);
        if (!hasVerticalRoom(level, origin, height + 5, 5)) return false;

        BlockState log = ModMagicalTrees.SILVERWOOD_LOG.get().defaultBlockState();
        BlockState leaves = ModMagicalTrees.SILVERWOOD_LEAVES.get().defaultBlockState();

        int start = height - 5;
        int end = height + 3 + random.nextInt(3);
        for (int y = start; y <= end; y++) {
            int centerY = Mth.clamp(y, height - 3, height);
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    double dist = dx * dx + (y - centerY) * (double) (y - centerY) + dz * dz;
                    if (dist < 10 + random.nextInt(8)) setLeaf(level, origin.offset(dx, y, dz), leaves);
                }
            }
        }

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

        int[][] diagonals = {{-1, -1}, {1, 1}, {-1, 1}, {1, -1}};
        for (int[] d : diagonals) {
            setLog(level, origin.offset(d[0], 0, d[1]), log, Direction.Axis.Y);
            if (random.nextInt(3) != 0) setLog(level, origin.offset(d[0], 1, d[1]), log, Direction.Axis.Y);
            setLog(level, origin.offset(d[0], height - 4, d[1]), log, Direction.Axis.Y);
            if (random.nextInt(3) == 0) setLog(level, origin.offset(d[0], height - 5, d[1]), log, Direction.Axis.Y);
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

        if (naturalWorldgen) generateShimmerleafPatch(level, origin, random);
        return true;
    }

    /** Exact TC4 WorldGenCustomFlowers spread used by natural Silverwood. */
    private static void generateShimmerleafPatch(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockState shimmerleaf = ModMagicalTrees.SHIMMERLEAF.get().defaultBlockState();
        for (int i = 0; i < 18; i++) {
            BlockPos p = origin.offset(
                    random.nextInt(8) - random.nextInt(8),
                    random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
            if (p.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(p).isAir()) continue;
            BlockState below = level.getBlockState(p.below());
            if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.SAND)) {
                level.setBlock(p, shimmerleaf, 3);
            }
        }
    }

    private static boolean canRoot(WorldGenLevel level, BlockPos origin) {
        BlockState soil = level.getBlockState(origin.below());
        return soil.is(BlockTags.DIRT) || soil.is(Blocks.FARMLAND) || soil.is(Blocks.MOSS_BLOCK);
    }

    private static boolean hasVerticalRoom(WorldGenLevel level, BlockPos origin, int height, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= height; y++) {
            int r = y < 2 ? 1 : radius;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    if (!replaceableForTree(level.getBlockState(cursor))) return false;
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
        if (steps == 0) return replaceableForTree(level.getBlockState(from));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            BlockPos p = new BlockPos(
                    Mth.floor(from.getX() + dx * t + 0.5D),
                    Mth.floor(from.getY() + dy * t + 0.5D),
                    Mth.floor(from.getZ() + dz * t + 0.5D));
            if (!replaceableForTree(level.getBlockState(p))) return false;
        }
        return true;
    }

    private static boolean replaceableForTree(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)
                || state.is(ModMagicalTrees.GREATWOOD_LOG.get()) || state.is(ModMagicalTrees.SILVERWOOD_LOG.get());
    }

    private static void connect(WorldGenLevel level, BlockPos from, BlockPos to, BlockState log) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) return;
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
        if (old.isAir() || old.canBeReplaced() || old.is(BlockTags.LEAVES)) level.setBlock(pos, leaves, 2);
    }

    private static void setLog(WorldGenLevel level, BlockPos pos, BlockState log, Direction.Axis axis) {
        if (replaceableForTree(level.getBlockState(pos))) {
            level.setBlock(pos, log.setValue(RotatedPillarBlock.AXIS, axis), 2);
        }
    }
}

package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.nodes.ModNodes;

/** Places one TC4-style wild aura node when the 1/36 placed-feature rarity check succeeds. */
public final class AuraNodeFeature extends Feature<NoneFeatureConfiguration> {
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);

    static {
        FEATURES.register("aura_node", AuraNodeFeature::new);
        FEATURES.register("structure_aura_node", StructureAuraNodeFeature::new);
    }

    public AuraNodeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        ChunkPos chunkPos = new ChunkPos(context.origin());

        // TC4 checks scattered structures before the normal node-rarity roll. A structure node
        // therefore consumes the chunk's aura-node slot and the wild pass must not add a second one.
        if (StructureAuraNodeFeature.hasScatteredStructureStart(level, chunkPos)) {
            return false;
        }

        int x = context.origin().getX() + random.nextInt(16);
        int z = context.origin().getZ() + random.nextInt(16);

        // TC4's Utils.getFirstUncoveredY did NOT use the world surface. It scanned upward from
        // near the bottom until the first air block appeared above the scan position, which is why
        // wild nodes could occur in caves. 1.20.1 extends the world below y=0, so use minY+5 as the
        // modern equivalent of the old hard-coded y=5 start.
        int q = getFirstUncoveredY(level, x, z);
        if (q == Integer.MIN_VALUE) {
            return false;
        }

        BlockPos above = new BlockPos(x, q + 1, z);
        if (level.isEmptyBlock(above)) {
            q++;
        }

        int p = random.nextInt(4);
        BlockPos candidate = new BlockPos(x, q + p, z);
        if (level.isEmptyBlock(candidate) || level.getBlockState(candidate).canBeReplaced()) {
            q += p;
        }

        if (q >= level.getMaxBuildHeight()) {
            return false;
        }

        return placeRandomNode(level, new BlockPos(x, q, z), random, false, false, false);
    }

    /** Shared modern equivalent of TC4 createRandomNodeAt + createNodeAt for worldgen callers. */
    static boolean placeRandomNode(WorldGenLevel level, BlockPos pos, RandomSource random,
                                   boolean silverwood, boolean eerie, boolean small) {
        // TC4 createNodeAt accepted both air and replaceable blocks. This matters for wild nodes
        // landing on vegetation/snow and also keeps future structure/tree node callers faithful.
        if (!level.isEmptyBlock(pos) && !level.getBlockState(pos).canBeReplaced()) {
            return false;
        }

        var generated = AuraNodeGenerator.generate(level, pos, random, silverwood, eerie, small);
        if (!level.setBlock(pos, ModNodes.AURA_NODE.get().defaultBlockState(), 2)) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
            node.initialize(generated.type(), generated.modifier(), generated.aspects());
            return true;
        }

        // Never leave an inert invisible carrier behind if BE creation failed.
        level.removeBlock(pos, false);
        return false;
    }

    private static int getFirstUncoveredY(WorldGenLevel level, int x, int z) {
        int start = level.getMinBuildHeight() + 5;
        int limit = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = start; y < limit; y++) {
            cursor.set(x, y + 1, z);
            if (level.isEmptyBlock(cursor)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}

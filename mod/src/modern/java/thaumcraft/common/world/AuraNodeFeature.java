package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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

        BlockPos pos = new BlockPos(x, q, z);
        // createNodeAt in TC4 only installed BlockAiry when the final position was actually air.
        // Preserve that behaviour rather than silently eating grass, flowers, snow layers, etc.
        if (!level.isEmptyBlock(pos)) {
            return false;
        }

        var generated = AuraNodeGenerator.generate(level, pos, random);
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

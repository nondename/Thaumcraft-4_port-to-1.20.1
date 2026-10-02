package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.nodes.ModNodes;

/** Places one wild aura node when the 1/36 placed-feature rarity check succeeds. */
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
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        if (surface <= level.getMinBuildHeight() + 1 || surface >= level.getMaxBuildHeight() - 5) {
            return false;
        }

        // TC4 nudges a wild node a few blocks into open air above the first uncovered surface.
        BlockPos pos = new BlockPos(x, surface + 1 + random.nextInt(4), z);
        for (int i = 0; i < 4 && !level.getBlockState(pos).canBeReplaced(); i++) {
            pos = pos.above();
        }
        if (pos.getY() >= level.getMaxBuildHeight() || !level.getBlockState(pos).canBeReplaced()) {
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
}

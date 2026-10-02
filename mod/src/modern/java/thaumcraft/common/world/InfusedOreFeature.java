package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ModOres;

/** Legacy density: eight attempts per chunk, veins of six, above Y=0 and below the surface. */
public final class InfusedOreFeature extends Feature<NoneFeatureConfiguration> {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);
    static { FEATURES.register("infused_ores", InfusedOreFeature::new); }

    public InfusedOreFeature() { super(NoneFeatureConfiguration.CODEC); }

    public static void register(IEventBus bus) { FEATURES.register(bus); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level();
        var random = context.random();
        boolean placed = false;
        for (int i = 0; i < 8; i++) {
            int x = context.origin().getX() + random.nextInt(16);
            int z = context.origin().getZ() + random.nextInt(16);
            int top = Math.max(5, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 5);
            BlockPos position = new BlockPos(x, random.nextInt(top), z);
            String kind = ModOres.NAMES[random.nextInt(ModOres.NAMES.length)];
            var config = new OreConfiguration(new BlockMatchTest(Blocks.STONE),
                    ModOres.ORES.get(kind).get().defaultBlockState(), 6);
            placed |= Feature.ORE.place(config, level, context.chunkGenerator(), random, position);
        }
        return placed;
    }
}

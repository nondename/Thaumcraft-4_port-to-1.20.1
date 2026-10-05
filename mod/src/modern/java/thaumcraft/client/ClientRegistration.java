package thaumcraft.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.FoliageColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ModMagicalTrees;
import thaumcraft.common.blocks.ModOres;
import thaumcraft.common.nodes.ModNodes;

@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent
    public static void entityRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(thaumcraft.common.alchemy.ModAlchemyEntities.ALUMENTUM.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                    thaumcraft.common.research.ModResearch.MENU.get(), ResearchScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    thaumcraft.common.blocks.ArcaneWorkbenchMenu.MENU_TYPE.get(), ArcaneWorkbenchScreen::new);
            BlockEntityRenderers.register(ModNodes.AURA_NODE_ENTITY.get(), AuraNodeRenderer::new);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    thaumcraft.common.infusion.ModInfusion.MENU.get(), AlchemyScreen::new);
            BlockEntityRenderers.register(
                    thaumcraft.common.infusion.ModInfusion.PEDESTAL_ENTITY.get(), PedestalRenderer::new);
            BlockEntityRenderers.register(
                    thaumcraft.common.infusion.ModInfusion.STORE_ENTITY.get(), EssentiaRenderer::new);
            BlockEntityRenderers.register(
                    thaumcraft.common.infusion.ModInfusion.MATRIX_ENTITY.get(), MatrixRenderer::new);
            ItemBlockRenderTypes.setRenderLayer(
                    thaumcraft.common.infusion.ModInfusion.JAR.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    thaumcraft.common.alchemy.ModAlchemy.NITOR.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModMagicalTrees.GREATWOOD_LEAVES.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModMagicalTrees.SILVERWOOD_LEAVES.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModMagicalTrees.GREATWOOD_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModMagicalTrees.SILVERWOOD_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModMagicalTrees.SHIMMERLEAF.get(), RenderType.cutout());
            net.minecraft.client.renderer.item.ItemProperties.register(
                    thaumcraft.common.research.ModResearch.PHIAL.get(),
                    new net.minecraft.resources.ResourceLocation("thaumcraft", "filled"),
                    (stack, level, entity, seed) -> {
                        var contents = new thaumcraft.api.aspects.AspectList();
                        if (stack.hasTag()) contents.readFromNBT(stack.getTag());
                        return contents.visSize() == 8 ? 1 : 0;
                    });
            for (String name : ModOres.NAMES) {
                ItemBlockRenderTypes.setRenderLayer(ModOres.ORES.get(name).get(), RenderType.cutout());
            }
        });
    }

    @SubscribeEvent
    public static void models(ModelEvent.RegisterAdditional event) {
        event.register(ThaumometerItemRenderer.MODEL);
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        // TC4 parity: Greatwood uses the biome foliage tint. Silverwood is deliberately untinted.
        event.register((state, level, pos, tint) -> {
                    if (tint != 0) return 0xFFFFFF;
                    return level != null && pos != null
                            ? BiomeColors.getAverageFoliageColor(level, pos)
                            : FoliageColor.getDefaultColor();
                },
                ModMagicalTrees.GREATWOOD_LEAVES.get());

        event.register((state, level, pos, tint) -> 0xFFFFFF,
                ModMagicalTrees.SILVERWOOD_LEAVES.get());

        for (int i = 0; i < ModOres.NAMES.length; i++) {
            int color = ModOres.COLORS[i];
            event.register((state, level, pos, tint) -> tint == 0 ? color : 0xFFFFFF,
                    ModOres.ORES.get(ModOres.NAMES[i]).get());
        }
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? FoliageColor.getDefaultColor() : 0xFFFFFF,
                ModMagicalTrees.GREATWOOD_LEAVES_ITEM.get());
        event.register((stack, tint) -> 0xFFFFFF,
                ModMagicalTrees.SILVERWOOD_LEAVES_ITEM.get());

        event.register((stack, tint) -> {
                    var contents = new thaumcraft.api.aspects.AspectList();
                    if (stack.hasTag()) contents.readFromNBT(stack.getTag());
                    return tint == 1 && contents.size() == 1 ? contents.getAspects()[0].getColor() : 0xFFFFFF;
                },
                thaumcraft.common.research.ModResearch.PHIAL.get());

        for (int i = 0; i < ModOres.NAMES.length; i++) {
            int color = ModOres.COLORS[i];
            event.register((stack, tint) -> tint == 0 ? color : 0xFFFFFF,
                    ModOres.SHARDS.get(ModOres.NAMES[i]).get(),
                    ModOres.ORES.get(ModOres.NAMES[i]).get().asItem());
        }
    }
}

package thaumcraft.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ModOres;
import thaumcraft.common.nodes.ModNodes;

@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(
                    thaumcraft.common.research.ModResearch.MENU.get(), ResearchScreen::new);
            BlockEntityRenderers.register(ModNodes.AURA_NODE_ENTITY.get(), AuraNodeRenderer::new);
        });
    }

    @SubscribeEvent
    public static void models(ModelEvent.RegisterAdditional event) {
        event.register(ThaumometerItemRenderer.MODEL);
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item event) {
        for (int i = 0; i < ModOres.NAMES.length; i++) {
            int color = ModOres.COLORS[i];
            event.register((stack, tint) -> tint == 0 ? color : 0xFFFFFF,
                    ModOres.SHARDS.get(ModOres.NAMES[i]).get());
        }
    }
}

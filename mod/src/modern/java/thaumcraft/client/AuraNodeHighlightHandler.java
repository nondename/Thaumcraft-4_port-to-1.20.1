package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.nodes.ModNodes;

/** Keeps the invisible BlockAiry-style aura node from showing a vanilla black selection cube. */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class AuraNodeHighlightHandler {
    private AuraNodeHighlightHandler() {
    }

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (minecraft.level.getBlockState(event.getTarget().getBlockPos()).is(ModNodes.AURA_NODE.get())) {
            event.setCanceled(true);
        }
    }
}

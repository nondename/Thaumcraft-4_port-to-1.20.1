package thaumcraft.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.tools.ItemWand;

/** Original TC4 dial geometry and atlas, driven by the held stack's stored vis. */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WandHud {
    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/hud.png");
    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("wand_vis", (gui, g, partialTick, width, height) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui || mc.screen != null) return;
            var stack = mc.player.getMainHandItem();
            if (!(stack.getItem() instanceof ItemWand)) stack = mc.player.getOffhandItem();
            if (!(stack.getItem() instanceof ItemWand)) return;
            g.pose().pushPose();
            g.pose().translate(2, 2, 0);
            g.pose().pushPose();
            g.pose().scale(0.5F, 0.5F, 1);
            g.blit(HUD, 0, 0, 0, 0, 64, 64);
            g.pose().popPose();
            g.pose().translate(16, 16, 0);
            var primals = Aspect.getPrimalAspects();
            for (int i = 0; i < primals.size(); i++) {
                Aspect a = primals.get(i);
                int amount = ItemWand.getVis(stack, a), fill = 30 * amount / ItemWand.getMaxVis(stack);
                g.pose().pushPose();
                g.pose().mulPose(Axis.ZP.rotationDegrees(75 + i * 24));
                g.pose().translate(0, -32, 0);
                g.pose().scale(0.5F, 0.5F, 1);
                int color = a.getColor();
                g.setColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 0.8F);
                if (fill > 0) g.blit(HUD, -4, 35 - fill, 104, 0, 8, fill);
                g.setColor(1, 1, 1, 1);
                g.blit(HUD, -8, -3, 72, 0, 16, 42);
                g.pose().popPose();
            }
            g.pose().popPose();
            if (mc.player.isShiftKeyDown()) {
                for (int i = 0; i < primals.size(); i++) {
                    var a = primals.get(i);
                    g.drawString(mc.font, a.getName() + " " + String.format(java.util.Locale.ROOT, "%.2f / 25", ItemWand.getVis(stack, a) / 100F),
                            4, 72 + i * 10, a.getColor(), true);
                }
            }
        });
    }
}

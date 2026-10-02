package thaumcraft.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ModItems;

/** Two-handed grip when the other hand is free; vanilla item rendering otherwise. */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerHands {
    private static ThaumometerItemRenderer renderer;
    @SubscribeEvent
    public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
        thaumcraft.common.config.RecipeAspects.clear();
    }
    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        InteractionHand scannerHand;
        if (player.getMainHandItem().is(ModItems.THAUMOMETER.get()) && player.getOffhandItem().isEmpty())
            scannerHand = InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem().is(ModItems.THAUMOMETER.get()) && player.getMainHandItem().isEmpty())
            scannerHand = InteractionHand.OFF_HAND;
        else return;
        event.setCanceled(true);
        if (event.getHand() != scannerHand) return;
        var pose = event.getPoseStack();
        pose.pushPose();
        float swing = net.minecraft.util.Mth.sin((float)Math.sqrt(event.getSwingProgress()) * (float)Math.PI);
        pose.translate(0, -0.23 - event.getEquipProgress() * 0.6 - swing * 0.08, -0.8);
        if (!player.isInvisible()) {
            var arms = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
            for (int side : new int[]{-1, 1}) {
                pose.pushPose();
                // Vanilla map-hand transforms keep skin sleeves and slim arms aligned.
                pose.mulPose(Axis.YP.rotationDegrees(92));
                pose.mulPose(Axis.XP.rotationDegrees(45));
                pose.mulPose(Axis.ZP.rotationDegrees(side * -41));
                pose.translate(side * 0.30, -1.1, 0.45);
                if (side == 1) arms.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
                else arms.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
                pose.popPose();
            }
        }
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.28F, 0.28F, 0.28F);
        // Direct BEWLR invocation: compensate its normal ItemRenderer origin offset.
        pose.translate(-0.5, -0.5, -0.5);
        if (renderer == null) renderer = new ThaumometerItemRenderer();
        renderer.renderByItem(event.getItemStack(), ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                pose, event.getMultiBufferSource(), event.getPackedLight(), 0);
        pose.popPose();
    }
}

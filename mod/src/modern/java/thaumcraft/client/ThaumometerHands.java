package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ModItems;

/** First-person TC4 thaumometer pose and its two real player arms. */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerHands {
    private ThaumometerHands() {
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        InteractionHand thaumometerHand = getThaumometerHand(player);
        if (thaumometerHand == null) {
            return;
        }

        // TC4 predates the offhand. While the scanner is raised it owns the complete first-person
        // hand render, so suppress both vanilla hand passes and draw the original two-handed pose once.
        event.setCanceled(true);
        if (event.getHand() != thaumometerHand) {
            return;
        }

        ItemStack stack = player.getItemInHand(thaumometerHand);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // Exact first-person foundation from TC4 4.2.3.5 ItemThaumometerRenderer:
        // glTranslatef(1, .75, -1), then glRotatef(135, 0, -1, 0).
        poseStack.translate(1.0D, 0.75D, -1.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F));

        // TC4 used the smoothed render-arm pitch/yaw here. LocalPlayer's xBob/yBob fields are
        // their modern equivalents, so preserve the small 10% camera-follow correction.
        float armPitch = Mth.lerp(event.getPartialTick(), player.xBobO, player.xBob);
        float armYaw = Mth.lerp(event.getPartialTick(), player.yBobO, player.yBob);
        poseStack.mulPose(Axis.XP.rotationDegrees((player.getXRot() - armPitch) * 0.1F));
        poseStack.mulPose(Axis.YP.rotationDegrees((player.getYRot() - armYaw) * 0.1F));

        // Original f9 was 0.8. Modern RenderHandEvent exposes the unequip motion directly as
        // equipProgress (0 when fully raised), so this is the same vertical draw animation.
        poseStack.translate(-0.56D, 0.52D + event.getEquipProgress() * 1.5D, 0.72D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0D, 0.0D, -0.72D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        renderTc4Arms(minecraft, player, poseStack, event);

        // Exact scanner transform applied after the two arms in TC4.
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.translate(0.4D, -0.4D, 0.0D);
        poseStack.scale(2.0F, 2.0F, 2.0F);
        ThaumometerItemRenderer.renderTc4FirstPerson(
                stack, poseStack, event.getMultiBufferSource(), event.getPackedLight());

        poseStack.popPose();
    }

    private static void renderTc4Arms(Minecraft minecraft, LocalPlayer player, PoseStack poseStack,
                                      RenderHandEvent event) {
        if (player.isInvisible()) {
            return;
        }

        EntityRenderer<?> entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (!(entityRenderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        poseStack.pushPose();
        // TC4 scales the player arm model by five before placing each arm around the scanner.
        poseStack.scale(5.0F, 5.0F, 5.0F);

        for (int index = 0; index < 2; index++) {
            int side = index * 2 - 1; // -1, +1 exactly as the original renderer.
            poseStack.pushPose();
            poseStack.translate(0.0D, -0.6D, 1.1D * side);
            poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F * side));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(59.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-65.0F * side));

            if (side < 0) {
                playerRenderer.renderRightHand(
                        poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
            } else {
                playerRenderer.renderLeftHand(
                        poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static InteractionHand getThaumometerHand(LocalPlayer player) {
        if (player.getMainHandItem().is(ModItems.THAUMOMETER.get())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(ModItems.THAUMOMETER.get())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    @SubscribeEvent
    public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
        thaumcraft.common.config.RecipeAspects.clear();
    }
}

package thaumcraft.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ModItems;

/**
 * First-person thaumometer presentation built on Minecraft 1.20.1's vanilla
 * two-handed filled-map pose.
 *
 * <p>The previous version manually positioned and scaled the BEWLR mesh. That bypassed the
 * item's normal FIRST_PERSON display transform and made the scanner enormous at common FOVs.
 * Keeping the vanilla map transform as the outer pose gives us a stable, FOV-friendly two-hand
 * anchor, while ItemRenderer applies thaumometer.json exactly like it does for normal 1.20.1
 * items.</p>
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerHands {
    private ThaumometerHands() {
    }

    @SubscribeEvent
    public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
        thaumcraft.common.config.RecipeAspects.clear();
    }

    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }

        InteractionHand scannerHand;
        if (player.getMainHandItem().is(ModItems.THAUMOMETER.get()) && player.getOffhandItem().isEmpty()) {
            scannerHand = InteractionHand.MAIN_HAND;
        } else if (player.getOffhandItem().is(ModItems.THAUMOMETER.get()) && player.getMainHandItem().isEmpty()) {
            scannerHand = InteractionHand.OFF_HAND;
        } else {
            return;
        }

        // Exactly like a two-handed map, the scanner owns both first-person hand renders.
        event.setCanceled(true);
        if (event.getHand() != scannerHand) {
            return;
        }

        var poseStack = event.getPoseStack();
        poseStack.pushPose();

        // Vanilla 1.20.1 ItemInHandRenderer#renderTwoHandedMap movement.
        float swingRoot = Mth.sqrt(event.getSwingProgress());
        float swingY = -0.2F * Mth.sin(event.getSwingProgress() * (float) Math.PI);
        float swingZ = -0.4F * Mth.sin(swingRoot * (float) Math.PI);
        poseStack.translate(0.0F, -swingY / 2.0F, swingZ);

        float tilt = calculateMapTilt(player.getXRot());
        poseStack.translate(
                0.0F,
                0.04F + event.getEquipProgress() * -1.2F + tilt * -0.5F,
                -0.72F
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt * -85.0F));

        if (!player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            PlayerRenderer arms = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
            renderMapHand(poseStack, event, arms, player, HumanoidArm.RIGHT);
            renderMapHand(poseStack, event, arms, player, HumanoidArm.LEFT);
            poseStack.popPose();
        }

        // Vanilla map's final swing rotation, but render our actual item instead of map geometry.
        float finalSwing = Mth.sin(swingRoot * (float) Math.PI);
        poseStack.mulPose(Axis.XP.rotationDegrees(finalSwing * 20.0F));

        ItemDisplayContext context = scannerHand == InteractionHand.OFF_HAND
                ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        // Important: go through ItemRenderer. This applies thaumometer.json's normal first-person
        // scale/rotation before invoking ThaumometerItemRenderer, instead of drawing the raw mesh
        // directly at camera scale.
        minecraft.getItemRenderer().renderStatic(
                event.getItemStack(),
                context,
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                poseStack,
                event.getMultiBufferSource(),
                player.level(),
                player.getId()
        );

        poseStack.popPose();
    }

    /** Vanilla 1.20.1 ItemInHandRenderer#calculateMapTilt. */
    private static float calculateMapTilt(float pitch) {
        float value = 1.0F - pitch / 45.0F + 0.1F;
        value = Mth.clamp(value, 0.0F, 1.0F);
        return -Mth.cos(value * (float) Math.PI) * 0.5F + 0.5F;
    }

    /** Vanilla 1.20.1 ItemInHandRenderer#renderMapHand. */
    private static void renderMapHand(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                      RenderHandEvent event,
                                      PlayerRenderer arms,
                                      net.minecraft.client.player.LocalPlayer player,
                                      HumanoidArm arm) {
        poseStack.pushPose();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -41.0F));
        poseStack.translate(side * 0.3F, -1.1F, 0.45F);
        if (arm == HumanoidArm.RIGHT) {
            arms.renderRightHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            arms.renderLeftHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        poseStack.popPose();
    }
}

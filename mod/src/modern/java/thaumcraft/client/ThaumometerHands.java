package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ModItems;

/**
 * First-person TC4 thaumometer pose adapted to the 1.20.1 hand pipeline.
 *
 * <p>RenderHandEvent already receives the camera-space pitch/yaw smoothing applied by
 * ItemInHandRenderer. This class therefore owns only the per-hand/item transform. Re-applying
 * xBob/yBob here would move the scanner lens away from the actual camera ray.</p>
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerHands {
    /** Neutral TC4 pose offset measured from the scanner lens centre. */
    private static final float FIRST_PERSON_CENTER_OFFSET_X = 0.272F;
    private static final float FIRST_PERSON_CENTER_OFFSET_Y = 0.148F;
    private static final float LEGACY_HELD_SCALE = 0.8F;

    private ThaumometerHands() {
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack mainStack = player.getMainHandItem();
        ItemStack offStack = player.getOffhandItem();
        boolean mainThaumometer = mainStack.is(ModItems.THAUMOMETER.get());
        boolean offThaumometer = offStack.is(ModItems.THAUMOMETER.get());

        // No scanner, or the deliberately unsupported edge case of one scanner in each hand:
        // leave the complete modern pipeline alone rather than rendering two centred viewports.
        if (mainThaumometer == offThaumometer) {
            return;
        }

        InteractionHand thaumometerHand = mainThaumometer
                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;
        InteractionHand supportHand = opposite(thaumometerHand);
        boolean twoHanded = player.getItemInHand(supportHand).isEmpty();

        if (event.getHand() == thaumometerHand) {
            if (!event.getItemStack().is(ModItems.THAUMOMETER.get())) {
                return;
            }
            event.setCanceled(true);
            renderThaumometer(minecraft, player, event, thaumometerHand, twoHanded);
            return;
        }

        if (twoHanded) {
            // TC4 had no offhand. When the support hand is empty the scanner owns both modern
            // first-person passes and draws both arms itself, so suppress the redundant vanilla arm.
            event.setCanceled(true);
        }
    }

    private static void renderThaumometer(Minecraft minecraft, LocalPlayer player, RenderHandEvent event,
                                           InteractionHand thaumometerHand, boolean twoHanded) {
        ItemStack stack = event.getItemStack();
        HumanoidArm holdingArm = physicalArm(player, thaumometerHand);
        float handedness = holdingArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        /*
         * The scan target is resolved from eyePosition + lookAngle, i.e. the centre camera ray.
         * Correct the neutral TC4 mesh offset in view space before applying swing/equip animation,
         * so the lens and its readout share that exact axis in either physical hand.
         */
        poseStack.translate(
                -FIRST_PERSON_CENTER_OFFSET_X * handedness,
                -FIRST_PERSON_CENTER_OFFSET_Y,
                0.0D
        );

        applyLegacyHeldItemBasis(poseStack, event, handedness);

        /*
         * TC4 ItemThaumometerRenderer first-person basis. Unlike the previous implementation every
         * horizontal transform is mirrored from the physical arm, not from MAIN_HAND/OFF_HAND.
         */
        poseStack.translate(handedness, 0.75D, -1.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F * handedness));
        poseStack.translate(
                -0.7D * LEGACY_HELD_SCALE * handedness,
                0.65D * LEGACY_HELD_SCALE + event.getEquipProgress() * 1.5D,
                0.9D * LEGACY_HELD_SCALE
        );
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F * handedness));
        poseStack.translate(0.0D, 0.0D, -0.9D * LEGACY_HELD_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F * handedness));

        if (twoHanded) {
            renderTc4Arms(minecraft, player, poseStack, event, holdingArm, handedness);
        } else {
            renderOneHandedArm(minecraft, player, poseStack, event, holdingArm, handedness);
        }

        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F * handedness));
        poseStack.translate(0.4D * handedness, -0.4D, 0.0D);
        poseStack.scale(2.0F, 2.0F, 2.0F);
        ThaumometerItemRenderer.renderTc4FirstPerson(
                stack, poseStack, event.getMultiBufferSource(), event.getPackedLight());

        poseStack.popPose();
    }

    /**
     * Replays only the per-item part of the old 1.7.10 EQUIPPED_FIRST_PERSON transform.
     * Camera pitch/yaw smoothing is intentionally absent: 1.20.1 applied it before RenderHandEvent.
     */
    private static void applyLegacyHeldItemBasis(PoseStack poseStack, RenderHandEvent event, float handedness) {
        float swing = event.getSwingProgress();
        float swingRoot = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        float swingLinear = Mth.sin(swing * Mth.PI);
        float swingSquared = Mth.sin(swing * swing * Mth.PI);

        poseStack.translate(
                -swingRoot * 0.4F * handedness,
                Mth.sin(Mth.sqrt(swing) * Mth.PI * 2.0F) * 0.2F,
                -swingLinear * 0.2F
        );
        poseStack.translate(
                0.7D * LEGACY_HELD_SCALE * handedness,
                -0.65D * LEGACY_HELD_SCALE - event.getEquipProgress() * 0.6D,
                -0.9D * LEGACY_HELD_SCALE
        );
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * handedness));
        poseStack.mulPose(Axis.YP.rotationDegrees(-swingSquared * 20.0F * handedness));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-swingRoot * 20.0F * handedness));
        poseStack.mulPose(Axis.XP.rotationDegrees(-swingRoot * 80.0F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
    }

    private static void renderTc4Arms(Minecraft minecraft, LocalPlayer player, PoseStack poseStack,
                                      RenderHandEvent event, HumanoidArm holdingArm, float handedness) {
        if (player.isInvisible()) {
            return;
        }

        EntityRenderer<?> entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (!(entityRenderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        HumanoidArm supportArm = holdingArm.getOpposite();
        int holdingDirection = holdingArm == HumanoidArm.RIGHT ? 1 : -1;

        poseStack.pushPose();
        poseStack.scale(5.0F, 5.0F, 5.0F);
        renderTc4ArmPose(playerRenderer, player, poseStack, event,
                -holdingDirection, supportArm, handedness);
        renderTc4ArmPose(playerRenderer, player, poseStack, event,
                holdingDirection, holdingArm, handedness);
        poseStack.popPose();
    }

    private static void renderOneHandedArm(Minecraft minecraft, LocalPlayer player, PoseStack poseStack,
                                           RenderHandEvent event, HumanoidArm holdingArm, float handedness) {
        if (player.isInvisible()) {
            return;
        }

        EntityRenderer<?> entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (!(entityRenderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        int holdingDirection = holdingArm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.pushPose();
        poseStack.scale(5.0F, 5.0F, 5.0F);
        renderTc4ArmPose(playerRenderer, player, poseStack, event,
                holdingDirection, holdingArm, handedness);
        poseStack.popPose();
    }

    private static void renderTc4ArmPose(PlayerRenderer playerRenderer, LocalPlayer player, PoseStack poseStack,
                                         RenderHandEvent event, int direction, HumanoidArm renderedArm,
                                         float handedness) {
        poseStack.pushPose();
        poseStack.translate(0.0D, -0.6D, 1.1D * direction);
        poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F * direction));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F * handedness));
        poseStack.mulPose(Axis.ZP.rotationDegrees(59.0F * handedness));
        poseStack.mulPose(Axis.YP.rotationDegrees(-65.0F * direction * handedness));

        // 1.20.1 left/right ModelPart pivots differ by ten model pixels. TC4 rendered the same
        // first-person arm basis twice, so move the modern left-arm +5px pivot back onto the
        // legacy/right-arm -5px origin: (-5 - +5) / 16 = -0.625 model units.
        if (renderedArm == HumanoidArm.LEFT) {
            poseStack.translate(-0.625D, 0.0D, 0.0D);
        }

        if (renderedArm == HumanoidArm.RIGHT) {
            playerRenderer.renderRightHand(
                    poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            playerRenderer.renderLeftHand(
                    poseStack, event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        poseStack.popPose();
    }

    private static HumanoidArm physicalArm(LocalPlayer player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    private static InteractionHand opposite(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @SubscribeEvent
    public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
        thaumcraft.common.config.RecipeAspects.clear();
    }
}

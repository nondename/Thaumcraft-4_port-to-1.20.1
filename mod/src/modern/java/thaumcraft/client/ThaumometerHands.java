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

/** First-person TC4 thaumometer pose, with a modern one-handed fallback when the other hand is occupied. */
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

        InteractionHand supportHand = opposite(thaumometerHand);
        boolean supportHandFree = player.getItemInHand(supportHand).isEmpty();

        if (supportHandFree) {
            // TC4 predates the offhand. With a free support hand the scanner owns both first-person
            // hand passes and reproduces the original two-handed pose.
            event.setCanceled(true);
            if (event.getHand() != thaumometerHand) {
                return;
            }
            renderThaumometer(minecraft, player, event, thaumometerHand, true);
            return;
        }

        // Modern compatibility rule: when the other hand contains an item, leave that hand entirely
        // to vanilla and replace only the thaumometer hand. This prevents shields, totems, torches,
        // etc. from being swallowed by the TC4 two-handed renderer.
        if (event.getHand() != thaumometerHand) {
            return;
        }
        event.setCanceled(true);
        renderThaumometer(minecraft, player, event, thaumometerHand, false);
    }

    private static void renderThaumometer(Minecraft minecraft, LocalPlayer player, RenderHandEvent event,
                                           InteractionHand thaumometerHand, boolean twoHanded) {
        ItemStack stack = player.getItemInHand(thaumometerHand);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        HumanoidArm holdingArm = physicalArm(player, thaumometerHand);
        int screenSide = holdingArm == HumanoidArm.RIGHT ? 1 : -1;

        // A one-handed scanner is deliberately less comfortable than the proper TC4 grip: it sits
        // a little toward the holding hand and slightly farther from the camera, like a one-handed map.
        // The scanner/arm relationship itself still comes from the original TC4 renderer.
        if (!twoHanded) {
            poseStack.translate(0.24D * screenSide, 0.04D, -0.12D);
            poseStack.scale(0.88F, 0.88F, 0.88F);
        }

        float partialTick = event.getPartialTick();
        float armPitch = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float armYaw = Mth.lerp(partialTick, player.yBobO, player.yBob);
        float pitchCorrection = (player.getXRot() - armPitch) * 0.1F;
        float yawCorrection = (player.getYRot() - armYaw) * 0.1F;

        /*
         * The original IItemRenderer was NOT called from a clean camera matrix. Minecraft 1.7.10's
         * ItemRenderer.renderItemInFirstPerson() first applied this ordinary held-item transform and
         * only then called Forge's custom item renderer.
         */
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchCorrection));
        poseStack.mulPose(Axis.YP.rotationDegrees(yawCorrection));
        applyVanilla1710HeldItemBasis(player, poseStack, event);

        // Exact first-person foundation from TC4 4.2.3.5 ItemThaumometerRenderer:
        // glTranslatef(1, .75, -1), then glRotatef(135, 0, -1, 0).
        poseStack.translate(1.0D, 0.75D, -1.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F));

        // TC4's renderer applies the render-arm smoothing correction a second time internally.
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchCorrection));
        poseStack.mulPose(Axis.YP.rotationDegrees(yawCorrection));

        // Original f9 was 0.8. RenderHandEvent equipProgress is the modern unequip amount
        // (0 when fully raised), matching the old (1 - equippedProgress) terms below.
        poseStack.translate(-0.56D, 0.52D + event.getEquipProgress() * 1.5D, 0.72D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0D, 0.0D, -0.72D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        if (twoHanded) {
            renderTc4Arms(minecraft, player, poseStack, event);
        } else {
            renderTc4Arm(minecraft, player, poseStack, event, holdingArm);
        }

        // Exact scanner transform applied after the arm(s) in TC4.
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.translate(0.4D, -0.4D, 0.0D);
        poseStack.scale(2.0F, 2.0F, 2.0F);
        ThaumometerItemRenderer.renderTc4FirstPerson(
                stack, poseStack, event.getMultiBufferSource(), event.getPackedLight());

        poseStack.popPose();
    }

    /**
     * Reproduces the vanilla 1.7.10 matrix that surrounded Forge's EQUIPPED_FIRST_PERSON
     * IItemRenderer call. These transforms are part of the original thaumometer pose even though
     * they lived in Minecraft rather than Thaumcraft itself.
     */
    private static void applyVanilla1710HeldItemBasis(LocalPlayer player, PoseStack poseStack,
                                                       RenderHandEvent event) {
        float swing = player.getAttackAnim(event.getPartialTick());
        float sinSwing = Mth.sin(swing * Mth.PI);
        float sinSqrtSwing = Mth.sin(Mth.sqrt(swing) * Mth.PI);

        // In 1.7.10 this initial swing translation was skipped while an item with EnumAction.none
        // was being used. That is exactly the state of a thaumometer during its 25-tick scan.
        if (!player.isUsingItem()) {
            poseStack.translate(
                    -sinSqrtSwing * 0.4F,
                    Mth.sin(Mth.sqrt(swing) * Mth.PI * 2.0F) * 0.2F,
                    -sinSwing * 0.2F
            );
        }

        poseStack.translate(
                0.56D,
                -0.52D - event.getEquipProgress() * 0.6D,
                -0.72D
        );
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));

        float sinSwingSquared = Mth.sin(swing * swing * Mth.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(-sinSwingSquared * 20.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-sinSqrtSwing * 20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-sinSqrtSwing * 80.0F));
        poseStack.scale(0.4F, 0.4F, 0.4F);
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
        renderTc4Arm(playerRenderer, player, poseStack, event, HumanoidArm.RIGHT);
        renderTc4Arm(playerRenderer, player, poseStack, event, HumanoidArm.LEFT);
        poseStack.popPose();
    }

    private static void renderTc4Arm(Minecraft minecraft, LocalPlayer player, PoseStack poseStack,
                                     RenderHandEvent event, HumanoidArm arm) {
        if (player.isInvisible()) {
            return;
        }
        EntityRenderer<?> entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (!(entityRenderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(5.0F, 5.0F, 5.0F);
        renderTc4Arm(playerRenderer, player, poseStack, event, arm);
        poseStack.popPose();
    }

    private static void renderTc4Arm(PlayerRenderer playerRenderer, LocalPlayer player, PoseStack poseStack,
                                     RenderHandEvent event, HumanoidArm arm) {
        int side = arm == HumanoidArm.RIGHT ? -1 : 1; // original TC4 arm-space convention
        poseStack.pushPose();
        poseStack.translate(0.0D, -0.6D, 1.1D * side);
        poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F * side));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(59.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-65.0F * side));

        if (arm == HumanoidArm.RIGHT) {
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

package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** TC4's no-focus WAVE animation, adapted to the modern baked wand model. */
public final class WandChargeAnimation implements IClientItemExtensions {
    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
                                            ItemStack stack, float partialTick,
                                            float equipProgress, float swingProgress) {
        InteractionHand hand = arm == player.getMainArm()
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (!player.isUsingItem() || player.getUsedItemHand() != hand
                || player.getUseItem().getItem() != stack.getItem()) {
            return false;
        }

        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        // Use elapsed ticks: converting the MAX_VALUE remaining duration to float loses
        // individual ticks and would freeze the sine wave for several seconds at a time.
        float ticks = player.getTicksUsingItem() + partialTick;
        float raise = Mth.clamp(ticks / 3.0F, 0.0F, 1.0F);

        // Vanilla's resting hand basis, followed by the TC4 first-person use rotations.
        // Mirror Y/Z for the physical left arm; the model retains its own display transform.
        pose.translate(side * 0.56F, -0.52F - equipProgress * 0.6F, -0.72F);
        pose.mulPose(Axis.XP.rotationDegrees(10.0F * raise));
        pose.mulPose(Axis.ZP.rotationDegrees(side * 10.0F * raise));
        pose.mulPose(Axis.XP.rotationDegrees(-60.0F * raise));
        // ItemWandRenderer.applyUseAnimation: no focus uses WAVE, ten-degree sine
        // rotations with periods expressed by ticks/10 and ticks/15.
        pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(ticks / 10.0F) * 10.0F * raise));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(ticks / 15.0F) * 10.0F * raise));
        return true; // Replace the vanilla BOW pull pose, which becomes static after charging.
    }
}

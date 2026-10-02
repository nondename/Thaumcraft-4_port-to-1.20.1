package thaumcraft.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ModItems;

/**
 * TC4-style first-person thaumometer pose.
 *
 * <p>The original scanner is not a normal one-handed item: it is held in front of the camera
 * with both hands and the hexagonal face is noticeably pitched away from the viewer.  Keeping
 * that pose in a RenderHandEvent also avoids trying to express a two-handed view through item
 * JSON transforms.</p>
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerHands {
    /*
     * scanner.obj is taller than it is wide (roughly 2.8 x 2.42). TC4 foreshortens the vertical
     * axis in first person; a ~40 degree pitch reproduces the broad hexagon seen in 1.7.10.
     * These values are deliberately kept here instead of thaumometer.json because only the
     * first-person, two-handed presentation uses them.
     */
    private static final float SCANNER_PITCH = 130.0F;
    private static final float SCANNER_SCALE = 0.44F;
    private static final double SCANNER_Y = -0.035D;
    private static final double SCANNER_Z = -0.92D;

    private static ThaumometerItemRenderer renderer;

    private ThaumometerHands() {
    }

    @SubscribeEvent
    public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
        thaumcraft.common.config.RecipeAspects.clear();
    }

    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        InteractionHand scannerHand;
        if (player.getMainHandItem().is(ModItems.THAUMOMETER.get()) && player.getOffhandItem().isEmpty()) {
            scannerHand = InteractionHand.MAIN_HAND;
        } else if (player.getOffhandItem().is(ModItems.THAUMOMETER.get()) && player.getMainHandItem().isEmpty()) {
            scannerHand = InteractionHand.OFF_HAND;
        } else {
            return;
        }

        // The scanner owns the whole first-person pose. Suppress the empty second hand as well.
        event.setCanceled(true);
        if (event.getHand() != scannerHand) return;

        renderHands(event, mc, player);
        renderScanner(event);
    }

    private static void renderHands(RenderHandEvent event, Minecraft mc, net.minecraft.client.player.LocalPlayer player) {
        if (player.isInvisible()) return;

        var pose = event.getPoseStack();
        pose.pushPose();

        float swing = Mth.sin(Mth.sqrt(event.getSwingProgress()) * (float) Math.PI);
        // Keep the hands below the frame while retaining the vanilla equip/swing motion.
        pose.translate(0.0D,
                -0.19D - event.getEquipProgress() * 0.58D - swing * 0.045D,
                -0.78D + swing * 0.025D);

        PlayerRenderer arms = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        renderMapStyleHand(pose, event, arms, player, -1);
        renderMapStyleHand(pose, event, arms, player, 1);
        pose.popPose();
    }

    private static void renderMapStyleHand(com.mojang.blaze3d.vertex.PoseStack pose,
                                           RenderHandEvent event,
                                           PlayerRenderer arms,
                                           net.minecraft.client.player.LocalPlayer player,
                                           int side) {
        pose.pushPose();
        // Vanilla 1.20.1 two-handed map grip: 92 / 45 / +/-41 and +/-0.3, -1.1, 0.45.
        // It gives us modern skin/slim-arm handling while matching TC4's two-handed silhouette.
        pose.mulPose(Axis.YP.rotationDegrees(92.0F));
        pose.mulPose(Axis.XP.rotationDegrees(45.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(side * -41.0F));
        pose.translate(side * 0.30D, -1.10D, 0.45D);
        if (side > 0) {
            arms.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        } else {
            arms.renderLeftHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        }
        pose.popPose();
    }

    private static void renderScanner(RenderHandEvent event) {
        var pose = event.getPoseStack();
        pose.pushPose();

        float swing = Mth.sin(Mth.sqrt(event.getSwingProgress()) * (float) Math.PI);
        double equipDrop = event.getEquipProgress() * 0.82D;

        // Screen-centred like TC4: equip animation comes from below, normal swing is intentionally
        // very small so the crosshair remains inside the lens instead of the whole scanner wobbling.
        pose.translate(swing * 0.018D,
                SCANNER_Y - equipDrop - swing * 0.025D,
                SCANNER_Z + swing * 0.025D);
        pose.mulPose(Axis.XP.rotationDegrees(SCANNER_PITCH));
        pose.scale(SCANNER_SCALE, SCANNER_SCALE, SCANNER_SCALE);

        // Direct BEWLR invocation bypasses ItemRenderer's usual 0.5 model-origin compensation.
        pose.translate(-0.5D, -0.5D, -0.5D);
        if (renderer == null) renderer = new ThaumometerItemRenderer();
        renderer.renderByItem(event.getItemStack(), ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                pose, event.getMultiBufferSource(), event.getPackedLight(), 0);
        pose.popPose();
    }
}

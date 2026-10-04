package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.nodes.AuraNodeBlockEntity;

/** Billboard renderer ported from TC4 TileNodeRenderer's 32-frame node atlas. */
public final class AuraNodeRenderer implements BlockEntityRenderer<AuraNodeBlockEntity> {
    private static final int FRAMES = 32;
    private static final int ATLAS_ROWS = 32;
    private static final double THAUMOMETER_VIEW_DISTANCE = 48.0D;
    private static final double FAINT_VIEW_DISTANCE = 64.0D;

    /**
     * Physical glass opening of our current first-person Thaumometer model, measured from the
     * full 1920x1020 gameplay capture. Coordinates are normalized screen coordinates, not a
     * generic FOV cone.
     */
    private static final double[][] THAUMOMETER_LENS = {
            {0.471D, 0.184D},
            {0.643D, 0.184D},
            {0.720D, 0.447D},
            {0.638D, 0.717D},
            {0.472D, 0.717D},
            {0.393D, 0.447D}
    };

    public AuraNodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AuraNodeBlockEntity node, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        var viewer = minecraft.player;
        if (viewer == null || node.getLevel() == null) return;

        // Discharge bolts are rendered in the same block-entity pipeline as the node itself.
        NodeZapClientHandler.renderForNode(node, partialTick, poseStack, bufferSource);

        if (node.getAspects().size() == 0) return;

        Vec3 center = Vec3.atCenterOf(node.getBlockPos());
        double distance = viewer.getEyePosition(partialTick).distanceTo(center);

        boolean depthIgnore = false;
        boolean revealed = false;
        double viewDistance = FAINT_VIEW_DISTANCE;
        var helmet = viewer.getItemBySlot(EquipmentSlot.HEAD);
        boolean holdingThaumometer = viewer.getMainHandItem().is(ModItems.THAUMOMETER.get());
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();

        // TC4 goggles reveal nodes independently of where the player is aiming. The Thaumometer
        // is different: on our 1.20.1 first-person model it is a physical viewport, so full node
        // rendering is clipped to the actual glass opening. Outside the glass the same node falls
        // back to the ordinary alpha-0.1 ghost core instead of remaining fully revealed.
        if (helmet.getItem() instanceof IRevealer revealer && revealer.showNodes(helmet, viewer)) {
            revealed = true;
            depthIgnore = true;
        } else if (holdingThaumometer && firstPerson && isInsideThaumometerLens(minecraft, node)) {
            viewDistance = THAUMOMETER_VIEW_DISTANCE;
            revealed = true;
            depthIgnore = true;
        }
        if (distance > viewDistance) return;

        int frame = (int) ((System.nanoTime() / 40_000_000L + node.getBlockPos().getX()) % FRAMES);
        if (!revealed) {
            // TC4 TileNodeRenderer leaves a faint central core visible even with no revealing gear.
            drawLayer(poseStack, bufferSource, minecraft, 0.50F, 0.0F, 0.10F,
                    1, frame, 0xFFFFFF, true, false);
            return;
        }

        float alpha = (float) Mth.clamp((viewDistance - distance) / viewDistance, 0.0D, 1.0D);
        NodeModifier modifier = node.getNodeModifier();
        if (modifier == NodeModifier.BRIGHT) alpha *= 1.5F;
        else if (modifier == NodeModifier.PALE) alpha *= 0.66F;
        else if (modifier == NodeModifier.FADING) {
            alpha *= Mth.sin((viewer.tickCount + partialTick) / 3.0F) * 0.25F + 0.33F;
        }
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        int count = 0;
        float average = 0.0F;
        float lastAngle = 0.0F;
        int aspectCount = Math.max(1, node.getAspects().size());
        for (Aspect aspect : node.getAspects().getAspects()) {
            if (aspect == null) continue;
            int amount = node.getAspects().getAmount(aspect);
            average += amount;
            float pulse = Mth.sin((viewer.tickCount + partialTick) / (14.0F - Math.min(count, 12))) * 0.25F + 0.50F;
            float scale = 0.20F + pulse * ((float) amount / 50.0F);
            lastAngle = (float) ((System.nanoTime() / 5_000_000L) % (5000L + 500L * count))
                    / (5000.0F + 500.0F * count) * Mth.TWO_PI;

            // TC4 feeds Aspect#getBlend directly to glBlendFunc(GL_SRC_ALPHA, blend):
            // 1 = additive, 771 = ordinary alpha.
            boolean additive = aspect.getBlend() != 771;
            float aspectAlpha = alpha * (additive ? 1.0F : 1.5F);
            drawLayer(poseStack, bufferSource, minecraft, scale, lastAngle,
                    aspectAlpha / Math.max(1.0F, aspectCount / 2.0F), 0, frame, aspect.getColor(),
                    additive, depthIgnore);
            count++;
        }

        average /= aspectCount;
        float coreScale = 0.10F + average / 150.0F;
        NodeType nodeType = node.getNodeType();
        int strip = stripFor(nodeType);
        boolean additiveCore = true;
        float coreAngle = lastAngle;

        switch (nodeType) {
            case UNSTABLE -> coreAngle = 0.0F;
            case DARK, TAINTED -> additiveCore = false;
            case HUNGRY -> coreScale *= 0.75F;
            default -> {
            }
        }

        drawLayer(poseStack, bufferSource, minecraft, coreScale, coreAngle, alpha,
                strip, frame, 0xFFFFFF, additiveCore, depthIgnore);
    }

    private static boolean isInsideThaumometerLens(Minecraft minecraft, AuraNodeBlockEntity node) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 relative = Vec3.atCenterOf(node.getBlockPos()).subtract(camera.getPosition());
        Vec3 forward = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot()).normalize();
        double depth = relative.dot(forward);
        if (depth <= 0.05D) return false;

        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();

        double horizontal = relative.dot(right);
        double vertical = relative.dot(up);
        double tanHalfFov = Math.tan(Math.toRadians(minecraft.options.fov().get() * 0.5D));
        if (tanHalfFov <= 0.0D) return false;

        double screenAspect = (double) minecraft.getWindow().getWidth()
                / Math.max(1.0D, (double) minecraft.getWindow().getHeight());
        double ndcX = horizontal / (depth * tanHalfFov * screenAspect);
        double ndcY = vertical / (depth * tanHalfFov);
        double screenX = 0.5D + ndcX * 0.5D;
        double screenY = 0.5D - ndcY * 0.5D;
        return pointInPolygon(screenX, screenY, THAUMOMETER_LENS);
    }

    private static boolean pointInPolygon(double x, double y, double[][] polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            double xi = polygon[i][0];
            double yi = polygon[i][1];
            double xj = polygon[j][0];
            double yj = polygon[j][1];
            boolean crosses = ((yi > y) != (yj > y))
                    && x < (xj - xi) * (y - yi) / ((yj - yi) + 1.0E-12D) + xi;
            if (crosses) inside = !inside;
        }
        return inside;
    }

    private static int stripFor(NodeType type) {
        return switch (type) {
            case NORMAL -> 1;
            case UNSTABLE -> 6;
            case DARK -> 2;
            case TAINTED -> 5;
            case PURE -> 4;
            case HUNGRY -> 3;
        };
    }

    private static void drawLayer(PoseStack poseStack, MultiBufferSource bufferSource, Minecraft minecraft,
                                  float scale, float angle, float alpha, int strip, int frame, int color,
                                  boolean additive, boolean throughWalls) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotation(angle));
        poseStack.scale(scale, scale, scale);

        // UtilsFX.renderFacingStrip in TC4 uses the same 32 divisor on both atlas axes.
        float u0 = (float) frame / (float) FRAMES;
        float u1 = (float) (frame + 1) / (float) FRAMES;
        float v0 = (float) strip / (float) ATLAS_ROWS;
        float v1 = (float) (strip + 1) / (float) ATLAS_ROWS;
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);

        // Keep TC4's original quad winding / UV order. Scale is the billboard half-extent.
        VertexConsumer vertices = bufferSource.getBuffer(ThaumcraftRenderTypes.node(additive, throughWalls));
        PoseStack.Pose pose = poseStack.last();
        vertex(vertices, pose, -1.0F, 1.0F, u0, v0, red, green, blue, a);
        vertex(vertices, pose, 1.0F, 1.0F, u1, v0, red, green, blue, a);
        vertex(vertices, pose, 1.0F, -1.0F, u1, v1, red, green, blue, a);
        vertex(vertices, pose, -1.0F, -1.0F, u0, v1, red, green, blue, a);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float u, float v,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(pose.pose(), x, y, 0.0F)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                // 220 was an old lightmap coordinate, not a valid packed 1.20.1 light value.
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(AuraNodeBlockEntity node) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}

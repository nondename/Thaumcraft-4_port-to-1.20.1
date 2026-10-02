package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.nodes.AuraNodeBlockEntity;

/** Billboard renderer based directly on TC4 TileNodeRenderer's 32-frame node atlas. */
public final class AuraNodeRenderer implements BlockEntityRenderer<AuraNodeBlockEntity> {
    private static final ResourceLocation NODE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/nodes.png");
    private static final int FRAMES = 32;
    private static final int STRIPS = 8;
    private static final double THAUMOMETER_VIEW_DISTANCE = 48.0D;
    private static final double FAINT_VIEW_DISTANCE = 64.0D;

    public AuraNodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AuraNodeBlockEntity node, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        var viewer = minecraft.player;
        if (viewer == null || node.getLevel() == null || node.getAspects().size() == 0) return;

        Vec3 center = Vec3.atCenterOf(node.getBlockPos());
        double distance = viewer.getEyePosition(partialTick).distanceTo(center);
        if (distance > FAINT_VIEW_DISTANCE) return;

        boolean holdingThaumometer = viewer.getMainHandItem().is(ModItems.THAUMOMETER.get())
                || viewer.getOffhandItem().is(ModItems.THAUMOMETER.get());
        Vec3 towardNode = center.subtract(viewer.getEyePosition(partialTick)).normalize();
        double alignment = viewer.getViewVector(partialTick).dot(towardNode);
        boolean revealed = holdingThaumometer
                && distance <= THAUMOMETER_VIEW_DISTANCE
                && alignment > 0.86D;

        int frame = (int) ((System.nanoTime() / 40_000_000L + node.getBlockPos().getX()) % FRAMES);
        if (!revealed) {
            // TC4 still leaves a nearly imperceptible generic aura when no revealer is active.
            drawLayer(poseStack, bufferSource, minecraft, 0.50F, 0.0F, 0.10F,
                    1, frame, 0xFFFFFF);
            return;
        }

        float alpha = (float) Mth.clamp((THAUMOMETER_VIEW_DISTANCE - distance) / THAUMOMETER_VIEW_DISTANCE,
                0.0D, 1.0D);
        NodeModifier modifier = node.getNodeModifier();
        if (modifier == NodeModifier.BRIGHT) alpha *= 1.5F;
        else if (modifier == NodeModifier.PALE) alpha *= 0.66F;
        else if (modifier == NodeModifier.FADING) {
            alpha *= Mth.sin((viewer.tickCount + partialTick) / 3.0F) * 0.25F + 0.33F;
        }
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        int count = 0;
        float average = 0.0F;
        int aspectCount = Math.max(1, node.getAspects().size());
        for (Aspect aspect : node.getAspects().getAspects()) {
            if (aspect == null) continue;
            int amount = node.getAspects().getAmount(aspect);
            average += amount;
            float pulse = Mth.sin((viewer.tickCount + partialTick) / (14.0F - Math.min(count, 12))) * 0.25F + 0.50F;
            float scale = 0.20F + pulse * ((float) amount / 50.0F);
            float angle = (float) ((System.nanoTime() / 5_000_000L) % (5000L + 500L * count))
                    / (5000.0F + 500.0F * count) * Mth.TWO_PI;
            drawLayer(poseStack, bufferSource, minecraft, scale, angle,
                    alpha / Math.max(1.0F, aspectCount / 2.0F), 0, frame, aspect.getColor());
            count++;
        }

        average /= aspectCount;
        float coreScale = 0.10F + average / 150.0F;
        int strip = stripFor(node.getNodeType());
        if (node.getNodeType() == NodeType.HUNGRY) coreScale *= 0.75F;
        drawLayer(poseStack, bufferSource, minecraft, coreScale, 0.0F, alpha,
                strip, frame, 0xFFFFFF);
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
                                  float scale, float angle, float alpha, int strip, int frame, int color) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotation(angle));
        poseStack.scale(scale, scale, scale);

        float u0 = (float) frame / (float) FRAMES;
        float u1 = (float) (frame + 1) / (float) FRAMES;
        float v0 = (float) strip / (float) STRIPS;
        float v1 = (float) (strip + 1) / (float) STRIPS;
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);

        VertexConsumer vertices = bufferSource.getBuffer(RenderType.entityTranslucent(NODE_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        vertex(vertices, pose, -0.5F, 0.5F, u0, v0, red, green, blue, a);
        vertex(vertices, pose, 0.5F, 0.5F, u1, v0, red, green, blue, a);
        vertex(vertices, pose, 0.5F, -0.5F, u1, v1, red, green, blue, a);
        vertex(vertices, pose, -0.5F, -0.5F, u0, v1, red, green, blue, a);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float u, float v,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(pose.pose(), x, y, 0.0F)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
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

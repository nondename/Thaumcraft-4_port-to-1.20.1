package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;

/** Modern equivalent of TC4 ItemNodeRenderer for the single creative Aura Node item. */
@OnlyIn(Dist.CLIENT)
public final class AuraNodeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation NODE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/nodes.png");
    private static final Aspect[] PREVIEW_ASPECTS = {Aspect.AIR, Aspect.FIRE, Aspect.EARTH, Aspect.WATER};

    public AuraNodeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // ItemRenderer shifts BEWLR models by -0.5; restore a local 0..1 item volume like TC4.
        poseStack.translate(0.5D, 0.5D, 0.5D);
        float scale = context == ItemDisplayContext.GUI ? 1.35F : 1.0F;
        poseStack.scale(scale, scale, scale);

        renderCrossedNode(poseStack, buffers);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderCrossedNode(poseStack, buffers);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        renderCrossedNode(poseStack, buffers);
        poseStack.popPose();
    }

    private static void renderCrossedNode(PoseStack poseStack, MultiBufferSource buffers) {
        long now = System.nanoTime();
        int frame = (int) ((now / 40_000_000L + 1L) % 32L);
        for (int i = 0; i < PREVIEW_ASPECTS.length; i++) {
            float pulse = Mth.sin((float) (now / 50_000_000L) / (14.0F - i)) * 0.12F + 0.38F;
            float angle = (float) ((now / 5_000_000L) % (5000L + 500L * i))
                    / (5000.0F + 500.0F * i) * Mth.TWO_PI;
            drawLayer(poseStack, buffers, 0.34F + pulse * 0.12F, angle, 0.23F,
                    0, frame, PREVIEW_ASPECTS[i].getColor());
        }
        drawLayer(poseStack, buffers, 0.42F, 0.0F, 0.78F, 1, frame, 0xFFFFFF);
    }

    private static void drawLayer(PoseStack poseStack, MultiBufferSource buffers,
                                  float scale, float angle, float alpha,
                                  int strip, int frame, int color) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(angle));
        poseStack.scale(scale, scale, scale);

        float u0 = frame / 32.0F;
        float u1 = (frame + 1) / 32.0F;
        float v0 = strip / 32.0F;
        float v1 = (strip + 1) / 32.0F;
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);

        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(NODE_TEXTURE));
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
}

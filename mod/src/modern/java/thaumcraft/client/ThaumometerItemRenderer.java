package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.capabilities.IThaumometerKnowledge;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;

/** Renders the original scanner mesh and its live readout on the lens. */
@OnlyIn(Dist.CLIENT)
public final class ThaumometerItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "item/thaumometer_mesh");
    private static final ResourceLocation SCREEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/scanscreen.png");
    private static final int MAX_READOUT_ASPECTS = 15;

    public ThaumometerItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(MODEL);
        for (BakedModel pass : model.getRenderPasses(stack, false)) {
            for (RenderType renderType : pass.getRenderTypes(stack, false)) {
                VertexConsumer vertices = bufferSource.getBuffer(renderType);
                minecraft.getItemRenderer().renderModelLists(
                        pass, stack, packedLight, packedOverlay, poseStack, vertices);
            }
        }

        renderScannerLens(poseStack, bufferSource, LightTexture.FULL_BRIGHT);
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            renderReadout(minecraft, poseStack, bufferSource);
        }
        poseStack.popPose();
    }

    private static void renderScannerLens(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.11D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = bufferSource.getBuffer(RenderType.entityTranslucent(SCREEN_TEXTURE));
        float half = 1.25F;
        putVertex(vertices, pose, -half, half, 0.0F, 0.0F, 1.0F, packedLight);
        putVertex(vertices, pose, half, half, 0.0F, 1.0F, 1.0F, packedLight);
        putVertex(vertices, pose, half, -half, 0.0F, 1.0F, 0.0F, packedLight);
        putVertex(vertices, pose, -half, -half, 0.0F, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
    }

    private static void renderReadout(Minecraft minecraft, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        var scan = thaumcraft.common.items.tools.ThaumometerTargets.find(minecraft.player);
        if (scan == null) return;
        IThaumometerKnowledge knowledge = minecraft.player
                .getCapability(ThaumometerKnowledgeProvider.CAPABILITY)
                .orElse(null);
        AspectList aspects = knowledge != null && scan.scanned(knowledge)
                ? scan.aspects(minecraft.level)
                : null;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.12D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        renderAspectIcons(minecraft, poseStack, bufferSource, aspects);
        renderTargetName(minecraft.font, poseStack, bufferSource, scan.readoutName(knowledge).getString());
        poseStack.popPose();
    }

    private static void renderAspectIcons(Minecraft minecraft, PoseStack poseStack,
                                          MultiBufferSource bufferSource, AspectList aspects) {
        if (aspects == null || aspects.size() == 0) {
            return;
        }

        Aspect[] sorted = aspects.getAspectsSorted();
        int count = Math.min(sorted.length, MAX_READOUT_ASPECTS);
        int posX = 0;
        int posY = 0;
        int remaining = count;
        int baseX = Math.min(5, remaining) * 8;
        for (int index = 0; index < count; index++) {
            Aspect aspect = sorted[index];
            if (aspect != null) {
                poseStack.pushPose();
                poseStack.scale(0.023F, 0.023F, 0.023F);
                drawTexture(bufferSource, poseStack, aspect.getImage(),
                        -baseX + posX * 16, -8 + posY * 16, 16, 16, LightTexture.FULL_BRIGHT, aspect.getColor());
                String amount = Integer.toString(aspects.getAmount(aspect));
                drawText(minecraft.font, poseStack, bufferSource, amount,
                        -baseX + posX * 16 + 9, -8 + posY * 16 + 9, 0xFFFFFFFF, 0.55F);
                poseStack.popPose();
            }
            posX++;
            if (posX >= 5 - posY) {
                posX = 0;
                remaining -= 5 - posY;
                posY++;
                baseX = Math.min(5 - posY, remaining) * 8;
            }
        }
    }

    private static void renderTargetName(Font font, PoseStack poseStack, MultiBufferSource bufferSource, String name) {
        if (name.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0D, -0.5D, 0.0D);
        float scale = Math.min(0.020F, 1.9F / Math.max(1, font.width(name)));
        int width = font.width(name);
        if (width > 90) {
            scale -= 0.000025F * (width - 90);
        }
        scale = Math.max(0.0015F, scale);
        poseStack.scale(scale, scale, scale);
        drawText(font, poseStack, bufferSource, name, -width / 2.0F, 0.0F, 0xFFFFFFFF, 1.0F);
        poseStack.popPose();
    }

    private static void drawText(Font font, PoseStack poseStack, MultiBufferSource bufferSource,
                                 String value, float x, float y, int color, float scale) {
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        font.drawInBatch(value, x, y, color, false, poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, 0xF000F0);
        poseStack.popPose();
    }

    private static void drawTexture(MultiBufferSource bufferSource, PoseStack poseStack,
                                    ResourceLocation texture, float x, float y, float width, float height, int light, int color) {
        VertexConsumer vertices = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        putVertex(vertices, pose, x, y + height, 0.0F, 0.0F, 1.0F, light, color);
        putVertex(vertices, pose, x + width, y + height, 0.0F, 1.0F, 1.0F, light, color);
        putVertex(vertices, pose, x + width, y, 0.0F, 1.0F, 0.0F, light, color);
        putVertex(vertices, pose, x, y, 0.0F, 0.0F, 0.0F, light, color);
    }

    private static void putVertex(VertexConsumer vertices, PoseStack.Pose pose,
                                  float x, float y, float z, float u, float v, int packedLight) {
        putVertex(vertices, pose, x, y, z, u, v, packedLight, 0xFFFFFF);
    }

    private static void putVertex(VertexConsumer vertices, PoseStack.Pose pose,
                                  float x, float y, float z, float u, float v, int packedLight, int color) {
        vertices.vertex(pose.pose(), x, y, z)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}

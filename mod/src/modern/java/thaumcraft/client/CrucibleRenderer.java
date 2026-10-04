package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.alchemy.CrucibleEntity;

/** The iron is baked separately; only the liquid uses blending. */
public final class CrucibleRenderer implements BlockEntityRenderer<CrucibleEntity> {
    private static final ResourceLocation WATER = new ResourceLocation("minecraft", "block/water_still");
    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(CrucibleEntity crucible, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (crucible.getWater() <= 0 || crucible.getLevel() == null) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WATER);
        int color = BiomeColors.getAverageWaterColor(crucible.getLevel(), crucible.getBlockPos());
        float amount = Math.min(1F, crucible.getAspects().visSize() / 100F);
        float recolor = amount > 0 ? .5F + amount / 2F : 0;
        // TC4 changes the tint and opacity as aspects accumulate.
        float r = ((color >> 16) & 255) / 255F;
        float g = ((color >> 8) & 255) / 255F * (1 - recolor / 3F);
        float b = (color & 255) / 255F * (1 - recolor);
        r += (1 - r) * recolor;
        float alpha = 1 - recolor / 2F;
        float height = (float) crucible.getFluidHeight();
        var vertices = buffers.getBuffer(RenderType.translucent());
        var transform = pose.last();
        vertex(vertices,transform,.125F,height,.125F,sprite.getU(2),sprite.getV(2),r,g,b,alpha,light);
        vertex(vertices,transform,.125F,height,.875F,sprite.getU(2),sprite.getV(14),r,g,b,alpha,light);
        vertex(vertices,transform,.875F,height,.875F,sprite.getU(14),sprite.getV(14),r,g,b,alpha,light);
        vertex(vertices,transform,.875F,height,.125F,sprite.getU(14),sprite.getV(2),r,g,b,alpha,light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float r, float g, float b, float alpha, int light) {
        consumer.vertex(pose.pose(),x,y,z).color(r,g,b,alpha).uv(u,v).uv2(light)
                .normal(pose.normal(),0,1,0).endVertex();
    }
}

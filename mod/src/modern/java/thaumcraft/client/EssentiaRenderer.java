package thaumcraft.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.infusion.*;

public final class EssentiaRenderer implements BlockEntityRenderer<EssentiaStoreEntity> {
    public EssentiaRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(EssentiaStoreEntity jar,float tick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(jar.aspect()==null || !jar.getBlockState().is(ModInfusion.JAR.get()))return;
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(new ResourceLocation("thaumcraft","block/animatedglow"));
        float lo=4/16F,hi=12/16F,bottom=1/16F,top=bottom+10/16F*jar.amount()/64F;
        float[][][] faces={{{lo,top,lo},{lo,top,hi},{hi,top,hi},{hi,top,lo}},{{lo,bottom,lo},{lo,top,lo},{hi,top,lo},{hi,bottom,lo}},{{hi,bottom,hi},{hi,top,hi},{lo,top,hi},{lo,bottom,hi}},{{lo,bottom,hi},{lo,top,hi},{lo,top,lo},{lo,bottom,lo}},{{hi,bottom,lo},{hi,top,lo},{hi,top,hi},{hi,bottom,hi}}};
        var vertices=buffers.getBuffer(RenderType.translucent());int color=jar.aspect().getColor();
        for(var face:faces)for(int i=0;i<4;i++) {
            var p=face[i];vertices.vertex(pose.last().pose(),p[0],p[1],p[2]).color((color>>16)&255,(color>>8)&255,color&255,220)
                .uv(i<2?sprite.getU0():sprite.getU1(),i==0 || i==3?sprite.getV1():sprite.getV0()).overlayCoords(overlay).uv2(light).normal(pose.last().normal(),0,1,0).endVertex();
        }
    }
}

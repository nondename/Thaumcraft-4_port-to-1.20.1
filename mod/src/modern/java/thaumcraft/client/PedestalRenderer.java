package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.common.infusion.PedestalEntity;

public final class PedestalRenderer implements BlockEntityRenderer<PedestalEntity> {
    public PedestalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(PedestalEntity pedestal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(pedestal.item().isEmpty() || pedestal.getLevel()==null)return;
        float time=pedestal.getLevel().getGameTime()+partial;
        pose.pushPose();pose.translate(.5,1.2+Math.sin(time/20)*.03,.5);pose.mulPose(Axis.YP.rotationDegrees(time*2));pose.scale(.5F,.5F,.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(pedestal.item(),ItemDisplayContext.GROUND,light,overlay,pose,buffers,pedestal.getLevel(),0);pose.popPose();
    }
}

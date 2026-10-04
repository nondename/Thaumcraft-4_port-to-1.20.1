package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import thaumcraft.common.infusion.*;

public final class MatrixRenderer implements BlockEntityRenderer<InfusionMatrixEntity> {
    public MatrixRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(InfusionMatrixEntity matrix,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!matrix.getBlockState().getValue(InfusionDeviceBlock.ACTIVE) || matrix.getLevel()==null)return;
        pose.pushPose();pose.translate(.5,.5,.5);
        pose.mulPose(Axis.YP.rotationDegrees((matrix.getLevel().getGameTime()+partial)%360));pose.mulPose(Axis.XP.rotationDegrees(35));pose.mulPose(Axis.ZP.rotationDegrees(45));pose.translate(-.5,-.5,-.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(matrix.getBlockState().setValue(InfusionDeviceBlock.ACTIVE,false),pose,buffers,light,overlay);pose.popPose();
    }
}

package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.common.blocks.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.items.tools.ItemWand;

/** The table itself is baked; the parked wand follows TileArcaneWorkbenchRenderer. */
public final class ArcaneWorkbenchRenderer implements BlockEntityRenderer<ArcaneWorkbenchBlockEntity> {
    public ArcaneWorkbenchRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ArcaneWorkbenchBlockEntity table, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var wand = table.getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND);
        if (!(wand.getItem() instanceof ItemWand)) return;
        pose.pushPose();
        try {
            // Original table placement: x+.65, y+1.0625, z+.25; X=90, Z=20.
            // FIXED uses the baked model's centre instead of the old Y-down ModelWand.
            pose.translate(0.65, 1.0625, 0.25);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(20));
            pose.scale(0.5F, 0.5F, 0.5F);
            Minecraft.getInstance().getItemRenderer().renderStatic(wand, ItemDisplayContext.FIXED,
                    light, overlay, pose, buffers, table.getLevel(), 0);
        } finally {
            pose.popPose();
        }
    }
}

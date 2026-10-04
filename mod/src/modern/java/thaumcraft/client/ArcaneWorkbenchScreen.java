package thaumcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.text.DecimalFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.blocks.ArcaneWorkbenchMenu;
import thaumcraft.common.crafting.ModRecipes;
import thaumcraft.common.items.tools.ItemWand;

/**
 * Port of TC4 {@code GuiArcaneWorkbench} (1.7.10, full source read): 190x234 texture,
 * the six primal cost tags around the grid with the pulsing alpha, and the greyed result
 * ghost plus "Insufficient vis" label when the wand cannot pay. Like the original, the
 * foreground layer is empty — the GUI has no title and no inventory labels
 * ({@code drawGuiContainerForegroundLayer} lines 33-34).
 *
 * <p>Documented deviations:
 * <ul>
 *   <li>aspect tags: {@code UtilsFX.drawTag} (lines 815-863) is reimplemented inline —
 *       16x16 glyph tinted by the aspect colour and alpha via shader colour + flush,
 *       half-scale amount text with a black cross outline, {@code #######.##} format;</li>
 *   <li>the grey ghost uses the same 0.33/0.66 shader tint instead of fixed-function
 *       {@code glColor}; the legacy count overlay is skipped because recipe outputs here
 *       are stack size 1 (vanilla draws no count for 1 anyway).</li>
 * </ul>
 */
public final class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Thaumcraft.MODID, "textures/gui/gui_arcaneworkbench.png");
    /** Original GuiArcaneWorkbench line 22: tag centres in GUI coordinates. */
    private static final int[][] ASPECT_LOCS = {
            {72, 21}, {24, 43}, {24, 102}, {72, 124}, {120, 102}, {120, 43}};
    /** Original UtilsFX line 311. */
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#######.##");
    /** Original line 107. */
    private static final int INSUFFICIENT_COLOR = 15625838;

    public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 190;
        imageHeight = 234;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, 0, 0, 0, 0, imageWidth, imageHeight);
        if (this.minecraft == null || this.minecraft.level == null || this.minecraft.player == null) {
            return;
        }
        var level = this.minecraft.level;
        var player = this.minecraft.player;
        var grid = menu.getGrid();
        ItemStack wandStack = menu.getTable().getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND);
        boolean hasWand = wandStack.getItem() instanceof ItemWand;

        // Original lines 49-74: cost only exists when an arcane recipe matches.
        AspectList cost = null;
        if (ModRecipes.arcaneMatch(level, grid).isPresent()) {
            cost = ModRecipes.arcaneAspects(level, grid);
        }
        if (cost != null) {
            int count = 0;
            for (Aspect primal : Aspect.getPrimalAspects()) {
                float amt = cost.getAmount(primal);
                if (amt > 0) {
                    float alpha = 0.5F + ((float) Math.sin((player.tickCount + count * 10) / 2.0F) * 0.2F - 0.2F);
                    if (hasWand) {
                        amt *= ItemWand.getConsumptionModifier(wandStack, player, primal, true);
                        if (amt * 100.0F <= ItemWand.getVis(wandStack, primal)) {
                            alpha = 1.0F;
                        }
                    }
                    drawTag(g, ASPECT_LOCS[count][0] - 8, ASPECT_LOCS[count][1] - 8, primal, amt, alpha);
                }
                if (++count > 5) {
                    break;
                }
            }
        }

        // Original lines 76-110: darkened arcane-result ghost in the output slot plus
        // the half-scale "Insufficient vis" label under the wand slot.
        if (hasWand && cost != null
                && !ItemWand.consumeAllVisCrafting(wandStack, player, cost, false)) {
            ItemStack ghost = ModRecipes.arcaneResult(level, grid);
            if (!ghost.isEmpty()) {
                RenderSystem.setShaderColor(0.33F, 0.33F, 0.33F, 0.66F);
                g.renderItem(ghost, 160, 64);
                g.flush();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            g.pose().pushPose();
            g.pose().translate(168, 46, 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            String text = "Insufficient vis";
            g.drawString(this.font, text, -this.font.width(text) / 2, 0, INSUFFICIENT_COLOR, false);
            g.pose().popPose();
        }
    }

    /** Original UtilsFX#drawTag lines 815-863 (blend 771 → outlined amount). */
    private void drawTag(GuiGraphics g, int x, int y, Aspect aspect, float amount, float alpha) {
        int color = aspect.getColor();
        RenderSystem.setShaderColor(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F,
                (color & 255) / 255.0F, alpha);
        g.blit(aspect.getImage(), x, y, 16, 16, 0, 0, 32, 32, 32, 32);
        g.flush(); // the tint must apply to this batch while the shader colour is set
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (amount > 0.0F) {
            String am = AMOUNT_FORMAT.format(amount);
            int width = this.font.width(am);
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            // Legacy: drawString at (32 - width + 2x, 32 - FONT_HEIGHT + 2y) under 0.5
            // scale — the same screen position after this translate/scale pair.
            int tx = 32 - width;
            int ty = 32 - this.font.lineHeight;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if ((dx == 0 || dy == 0) && (dx != 0 || dy != 0)) {
                        g.drawString(this.font, am, tx + dx, ty + dy, 0, false);
                    }
                }
            }
            g.drawString(this.font, am, tx, ty, 0xFFFFFF, false);
            g.pose().popPose();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Original foreground layer is empty: no title, no inventory captions.
    }
}

package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.blocks.ModOres;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import java.util.List;

/** Readable starting chapters and the player's discovered aspect catalogue. */
public final class ThaumonomiconScreen extends Screen {
    private int page;
    private Screen parent;
    private int left, top;
    private float uiScale = 1;
    private Button previous, next;
    public ThaumonomiconScreen() { super(Component.translatable("item.thaumcraft.thaumonomicon")); }
    public ThaumonomiconScreen(Screen parent, int page) { this(); this.parent = parent; this.page = page; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    public static void open() { Minecraft.getInstance().setScreen(new ResearchTreeScreen()); }
    private List<Aspect> known() {
        var knowledge = minecraft.player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
        return Aspect.aspects.values().stream().filter(a -> knowledge != null && knowledge.hasDiscoveredAspect(a)).toList();
    }
    private int lastPage() { return 3 + Math.max(0, (known().size() - 1) / 12); }
    @Override protected void init() {
        uiScale = Math.min(1, Math.min(width / 410F, height / 310F));
        left = ((int)(width / uiScale) - 360) / 2;
        top = ((int)(height / uiScale) - 252) / 2;
        previous = addRenderableWidget(BookSkin.turn(left+22,top+225,false,b -> page--));
        next = addRenderableWidget(BookSkin.turn(left+314,top+225,true,b -> page++));
        addRenderableWidget(BookSkin.back(left+204,top+224,106,Component.translatable("tc.tree.back"),b -> onClose()));
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1);
        BookSkin.draw(g,left,top,360,252);
        previous.active = page > 0;
        next.active = page < lastPage();
        if (page < 3) {
            paragraph(g, Component.translatable("tc.book.chapter." + page), left + 23, top + 20, 142);
            paragraph(g, Component.translatable("tc.book.body." + page), left + 23, top + 43, 142);
            paragraph(g, Component.translatable("tc.book.recipe." + page), left + 196, top + 20, 140);
            if (page == 0) {
                grid(g, new ItemStack[]{stack(Items.IRON_NUGGET),stack(Items.IRON_NUGGET),stack(Items.IRON_NUGGET),stack(Items.IRON_NUGGET),ItemStack.EMPTY,stack(Items.IRON_NUGGET)}, top + 90);
                g.renderItem(new ItemStack(thaumcraft.common.items.wands.ModWandParts.CAP_IRON.get()), left + 295, top + 112);
                grid(g, new ItemStack[]{ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(thaumcraft.common.items.wands.ModWandParts.CAP_IRON.get()),ItemStack.EMPTY,stack(Items.STICK),ItemStack.EMPTY,new ItemStack(thaumcraft.common.items.wands.ModWandParts.CAP_IRON.get())}, top + 151);
                g.renderItem(new ItemStack(ModItems.WAND.get()), left + 295, top + 173);
            } else if (page == 1) {
                grid(g, new ItemStack[]{ItemStack.EMPTY,new ItemStack(ModOres.SHARDS.get("air").get()),ItemStack.EMPTY,stack(Items.IRON_INGOT),stack(Items.GLASS),stack(Items.IRON_INGOT),ItemStack.EMPTY,new ItemStack(ModOres.SHARDS.get("water").get())}, top + 132);
                g.renderItem(new ItemStack(ModItems.THAUMOMETER.get()), left + 295, top + 154);
            } else {
                grid(g, new ItemStack[]{stack(Items.OAK_SLAB),stack(Items.OAK_SLAB),stack(Items.OAK_SLAB),stack(Items.OAK_PLANKS),ItemStack.EMPTY,stack(Items.OAK_PLANKS)}, top + 151);
                g.renderItem(new ItemStack(thaumcraft.common.research.ModResearch.TABLE_ITEM.get()), left + 295, top + 173);
            }
        } else {
            g.drawString(font, Component.translatable("tc.book.aspects"), left + 24, top + 17, 0x423024, false);
            var known = known();
            var knowledge = minecraft.player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
            for (int i = 0; i < 12; i++) {
                int index = (page - 3) * 12 + i;
                if (index >= known.size()) break;
                Aspect a = known.get(index);
                int x = left + (i < 6 ? 23 : 196), y = top + 38 + (i % 6) * 29;
                int c = a.getColor();
                g.setColor(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F, 1);
                g.blit(a.getImage(), x, y, 20, 20, 0, 0, 32, 32, 32, 32);
                g.setColor(1, 1, 1, 1);
                g.drawString(font, a.getName() + " (" + knowledge.getAspectPool(a) + ")", x + 23, y, 0x423024, false);
                String parents = a.isPrimal() ? Component.translatable("tc.book.primal").getString()
                        : a.getComponents()[0].getName() + " + " + a.getComponents()[1].getName();
                g.pose().pushPose();
                g.pose().translate(x + 23, y + 12, 0);
                g.pose().scale(0.7F, 0.7F, 1);
                g.drawString(font, parents, 0, 0, 0x554435, false);
                g.pose().popPose();
            }
        }
        g.drawString(font, (page + 1) + " / " + (lastPage() + 1), left + 90, top + 232, 0x776B55,false);
        super.render(g, (int)(mouseX / uiScale), (int)(mouseY / uiScale), partialTick);
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        return super.mouseClicked(x / uiScale, y / uiScale, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        return super.mouseReleased(x / uiScale, y / uiScale, button);
    }
    private static ItemStack stack(net.minecraft.world.level.ItemLike item) { return new ItemStack(item); }
    private void grid(GuiGraphics g, ItemStack[] items, int y) {
        for (int i = 0; i < 9; i++) {
            int x = left + 202 + (i % 3) * 20, row = y + (i / 3) * 20;
            g.fill(x, row, x + 18, row + 18, 0x30543A22);
            if (i < items.length) g.renderItem(items[i], x + 1, row + 1);
        }
    }
    private void paragraph(GuiGraphics g, Component text, int x, int y, int maxWidth) {
        for (var line : font.split(text, maxWidth)) {
            g.drawString(font, line, x, y, 0x423024, false);
            y += 10;
        }
    }
    @Override public boolean isPauseScreen() { return false; }
}

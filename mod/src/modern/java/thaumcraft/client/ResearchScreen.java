package thaumcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.IThaumometerKnowledge;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import thaumcraft.common.research.ResearchMenu;

/** The functional aspect-combination section of the research table. */
public final class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {
    private final Button[] aspects = new Button[ResearchMenu.ASPECTS.length];
    private int page;

    public ResearchScreen(ResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 255;
        imageHeight = 255;
    }

    private IThaumometerKnowledge knowledge() {
        return minecraft.player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < aspects.length; i++) {
            int index = i;
            Aspect aspect = ResearchMenu.ASPECTS[i];
            aspects[i] = addRenderableWidget(new Button(leftPos + 10 + (i % 5) * 16, topPos + 38 + ((i % 25) / 5) * 16,
                    16, 16, Component.literal(aspect.getName()), button -> send(index), supplier -> supplier.get()) {
                @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                    int x = getX(), y = getY();
                    boolean selected = menu.first() == index || menu.second() == index;
                    if (selected || isHoveredOrFocused()) g.fill(x, y, x + 16, y + 16, selected ? 0xFFB28C43 : 0xFF655342);
                    int color = aspect.getColor();
                    g.setColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1);
                    g.blit(aspect.getImage(), x, y, 16, 16, 0, 0, 32, 32, 32, 32);
                    g.setColor(1, 1, 1, 1);
                    var data = knowledge();
                    String amount = Integer.toString(data == null ? 0 : data.getAspectPool(aspect));
                    g.drawString(font, amount, x + 16 - font.width(amount), y + 9, 0xFFFFFF, true);
                }
            });
            aspects[i].setTooltip(Tooltip.create(Component.literal(aspect.getName())));
        }
        addRenderableWidget(Button.builder(Component.translatable("tc.research.combine"), button -> send(101))
                .bounds(leftPos + 9, topPos + 137, 80, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("tc.research.clear"), button -> send(100))
                .bounds(leftPos + 105, topPos + 137, 80, 18).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> page = Math.max(0, page - 1))
                .bounds(leftPos + 10, topPos + 119, 20, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> page = Math.min((aspects.length - 1) / 25, page + 1))
                .bounds(leftPos + 69, topPos + 119, 20, 14).build());
    }

    private void send(int id) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        var data = knowledge();
        for (int i = 0; i < aspects.length; i++) aspects[i].visible = i / 25 == page
                && data != null && data.hasDiscoveredAspect(ResearchMenu.ASPECTS[i]);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        var texture = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/guiresearchtable2.png");
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(texture, leftPos, topPos, 0, 0, 255, 167, 256, 256);
        g.blit(texture, leftPos + 40, topPos + 167, 0, 166, 184, 88, 256, 256);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 105, 9, 0xEED9AA, false);
        String pair = name(menu.first()) + " + " + name(menu.second());
        g.drawString(font, pair, 105, 35, 0xFFFFFF, false);
        Component result = menu.result() >= 0 ? Component.literal("= " + name(menu.result()))
                : Component.translatable(menu.result() == -2 ? "tc.research.failed" : "tc.research.choose");
        g.drawWordWrap(font, result, 105, 50, 137, 0xDDCC99);
    }

    private static String name(int index) { return index < 0 ? "?" : ResearchMenu.ASPECTS[index].getName(); }
}

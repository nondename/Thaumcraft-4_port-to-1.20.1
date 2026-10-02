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

    public ResearchScreen(ResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 240;
        imageHeight = 246;
    }

    private IThaumometerKnowledge knowledge() {
        return minecraft.player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
    }

    @Override protected void init() {
        super.init();
        for (int i = 0; i < aspects.length; i++) {
            int index = i;
            Aspect aspect = ResearchMenu.ASPECTS[i];
            aspects[i] = addRenderableWidget(new Button(leftPos + 16 + (i % 8) * 26, topPos + 51 + (i / 8) * 26,
                    24, 24, Component.literal(aspect.getName()), button -> send(index), supplier -> supplier.get()) {
                @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                    int x = getX(), y = getY();
                    boolean selected = menu.first() == index || menu.second() == index;
                    g.fill(x, y, x + 24, y + 24, selected ? 0xFFB28C43 : isHoveredOrFocused() ? 0xFF655342 : 0xFF302B2A);
                    int color = aspect.getColor();
                    g.setColor(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, 1);
                    g.blit(aspect.getImage(), x + 4, y + 2, 16, 16, 0, 0, 32, 32, 32, 32);
                    g.setColor(1, 1, 1, 1);
                    var data = knowledge();
                    String amount = Integer.toString(data == null ? 0 : data.getAspectPool(aspect));
                    g.drawString(font, amount, x + 23 - font.width(amount), y + 15, 0xFFFFFF, true);
                }
            });
            aspects[i].setTooltip(Tooltip.create(Component.literal(aspect.getName())));
        }
        addRenderableWidget(Button.builder(Component.translatable("tc.research.combine"), button -> send(101))
                .bounds(leftPos + 16, topPos + 214, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tc.research.clear"), button -> send(100))
                .bounds(leftPos + 124, topPos + 214, 100, 20).build());
    }

    private void send(int id) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        var data = knowledge();
        for (int i = 0; i < aspects.length; i++) aspects[i].visible = data != null && data.hasDiscoveredAspect(ResearchMenu.ASPECTS[i]);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF9A773B);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF211E20);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 12, 9, 0xEED9AA, false);
        String pair = name(menu.first()) + " + " + name(menu.second());
        g.drawString(font, pair, 12, 24, 0xFFFFFF, false);
        Component result = menu.result() >= 0 ? Component.literal("= " + name(menu.result()))
                : Component.translatable(menu.result() == -2 ? "tc.research.failed" : "tc.research.choose");
        g.drawString(font, result, 12, 37, 0xDDCC99, false);
    }

    private static String name(int index) { return index < 0 ? "?" : ResearchMenu.ASPECTS[index].getName(); }
}

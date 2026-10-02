package thaumcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** Original reference text with page wrapping; this screen does not unlock recipes. */
public final class ResearchEntryScreen extends Screen {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbook.png");
    private final Screen parent;
    private final ResearchTreeScreen.Node node;
    private final List<List<FormattedCharSequence>> pages = new ArrayList<>();
    private int spread, left, top;
    private float scale;
    private Button back, next;
    public ResearchEntryScreen(Screen parent, ResearchTreeScreen.Node node) {
        super(Component.translatable("tc.research_name." + node.key()));
        this.parent = parent; this.node = node;
    }
    @Override protected void init() {
        scale = Math.min(1, Math.min(width / 380F, height / 290F));
        left = ((int)(width / scale) - 360) / 2;
        top = ((int)(height / scale) - 280) / 2;
        pages.clear();
        for (String key : node.pages()) {
            if (!I18n.exists(key)) continue;
            String text = I18n.get(key).replace("<LINE>", "\n\n").replace("<BR>", "\n")
                    .replace("<PAGE>", "\n\n").replaceAll("<[^>]*>", "");
            var lines = font.split(Component.literal(text), 140);
            for (int i = 0; i < lines.size(); i += 17) pages.add(new ArrayList<>(lines.subList(i, Math.min(lines.size(), i + 17))));
        }
        if (pages.isEmpty()) pages.add(font.split(Component.translatable("tc.research_text." + node.key()), 140));
        spread = Math.min(spread, (pages.size() - 1) / 2);
        back = addRenderableWidget(Button.builder(Component.literal("<"), b -> spread--).bounds(left + 22, top + 226, 24, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> spread++).bounds(left + 314, top + 226, 24, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("tc.tree.back"), b -> onClose()).bounds(left + 130, top + 256, 100, 20).build());
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        g.pose().pushPose(); g.pose().scale(scale, scale, 1);
        g.blit(BOOK, left, top, 360, 252, 0, 0, 512, 360, 512, 512);
        g.drawString(font, title, left + 23, top + 18, 0x423024, false);
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if (index >= pages.size()) continue;
            int y = top + 43;
            for (var line : pages.get(index)) { g.drawString(font, line, left + (side == 0 ? 23 : 196), y, 0x423024, false); y += 10; }
        }
        back.active = spread > 0; next.active = spread * 2 + 2 < pages.size();
        g.drawCenteredString(font, (spread + 1) + " / " + ((pages.size() + 1) / 2), left + 180, top + 231, 0xC3A576);
        super.render(g, (int)(mx / scale), (int)(my / scale), partialTick);
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x, double y, int b) { return super.mouseClicked(x / scale, y / scale, b); }
    @Override public boolean mouseReleased(double x, double y, int b) { return super.mouseReleased(x / scale, y / scale, b); }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

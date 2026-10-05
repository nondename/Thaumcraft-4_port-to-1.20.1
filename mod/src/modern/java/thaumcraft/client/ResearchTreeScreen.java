package thaumcraft.client;

import com.google.gson.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;

import java.util.*;

/** Original tab layout and research graph backed by synchronized player research state. */
public final class ResearchTreeScreen extends Screen {
    private static final String[] TABS = {"BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH"};
    private static final String[] ICONS = {"items/thaumonomiconcheat", "misc/r_thaumaturgy", "misc/r_crucible", "misc/r_artifice", "misc/r_golemancy", "misc/r_eldritch"};

    public record Node(String key, String category, int x, int y, List<String> parents, List<String> pages,
                       boolean auto, boolean round, boolean concealed, ResourceLocation icon, String item,
                       int iconWidth, int iconHeight) {
    }

    private final List<Node> nodes = new ArrayList<>();
    private final Map<String,List<String>> hiddenParents = new HashMap<>();
    private final Set<String> secondary = new HashSet<>();
    private final Map<String, Node> byKey = new HashMap<>();
    private static final double[] panX = new double[6], panY = new double[6];
    private static int tab;
    private int left, top;
    private float scale = 1;
    private static float zoom = 1;
    private boolean dragging;
    private double dragDistance;
    private Node hovered;
    private boolean loadFailed;

    public ResearchTreeScreen() {
        super(Component.translatable("item.thaumcraft.thaumonomicon"));
        var mc = net.minecraft.client.Minecraft.getInstance();
        try (var reader = mc.getResourceManager().openAsReader(id("research/tree.json"))) {
            for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
                var o = element.getAsJsonObject();
                Node n = new Node(
                        o.get("key").getAsString(),
                        o.get("category").getAsString(),
                        o.get("x").getAsInt(),
                        o.get("y").getAsInt(),
                        strings(o, "parents"),
                        strings(o, "pages"),
                        o.get("auto").getAsBoolean(),
                        o.get("round").getAsBoolean(),
                        o.get("concealed").getAsBoolean(),
                        o.has("icon") ? ResourceLocation.tryParse(o.get("icon").getAsString()) : null,
                        o.has("item") ? o.get("item").getAsString() : null,
                        o.has("iconWidth") ? o.get("iconWidth").getAsInt() : 16,
                        o.has("iconHeight") ? o.get("iconHeight").getAsInt() : 16
                );
                nodes.add(n);
                if(o.has("hiddenParents"))hiddenParents.put(n.key(),strings(o,"hiddenParents"));
                if(o.has("secondary") && o.get("secondary").getAsBoolean())secondary.add(n.key());
                byKey.put(n.key(), n);
            }
        } catch (Exception e) {
            Thaumcraft.LOGGER.error("Cannot load research tree", e);
            loadFailed = true;
        }
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> result = new ArrayList<>();
        o.getAsJsonArray(key).forEach(e -> result.add(e.getAsString()));
        return result;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }

    @Override
    protected void init() {
        scale = Math.min(1, Math.min(width / 410F, height / 285F));
        left = ((int) (width / scale) - 360) / 2 + 10;
        top = ((int) (height / scale) - 252) / 2;
        dragging = false;
    }

    private double nx(Node n) {
        return left + 180 + (n.x() * 24 - panX[tab]) * zoom;
    }

    private double ny(Node n) {
        return top + 124 + (n.y() * 24 - panY[tab]) * zoom;
    }

    private boolean inside(double x, double y) {
        return x >= left + 18 && x < left + 342 && y >= top + 36 && y < top + 224;
    }

    private boolean isUnlocked(Node node) {
        if (node.auto()) {
            return true;
        }
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        return minecraft.player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY)
                .map(knowledge -> knowledge.hasResearch(node.key()))
                .orElse(false);
    }

    private boolean parentsUnlocked(Node node) {
        var required=new ArrayList<>(node.parents());required.addAll(hiddenParents.getOrDefault(node.key(),List.of()));
        for (String key : required) {
            Node parent = byKey.get(key);
            boolean complete=parent!=null?isUnlocked(parent):minecraft!=null && minecraft.player!=null && minecraft.player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).map(k -> k.hasResearch(key)).orElse(false);
            if (!complete) {
                return false;
            }
        }
        return true;
    }

    private boolean isVisible(Node node) {
        return !node.concealed() || isUnlocked(node) || parentsUnlocked(node);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        double mx = mouseX / scale, my = mouseY / scale;
        hovered = null;
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1);
        BookSkin.draw(g,left,top,360,252);
        g.drawCenteredString(font,Component.translatable("tc.research_category."+TABS[tab]),left+180,top+18,0x514535);
        g.enableScissor((int) ((left + 18) * scale), (int) ((top + 36) * scale),
                (int) ((left + 342) * scale), (int) ((top + 224) * scale));

        for (Node n : nodes) {
            if (!n.category().equals(TABS[tab]) || !isVisible(n)) {
                continue;
            }
            for (String key : n.parents()) {
                Node parent = byKey.get(key);
                if (parent != null && parent.category().equals(n.category()) && isVisible(parent)) {
                    int color = isUnlocked(n) ? 0xFF69806A : parentsUnlocked(n) ? 0xFF9C8763 : 0xFFCEC4AE;
                    line(g, (int) nx(parent), (int) ny(parent), (int) nx(n), (int) ny(n), color);
                }
            }
        }

        for (Node n : nodes) {
            if (!n.category().equals(TABS[tab]) || !isVisible(n)) {
                continue;
            }
            double x = nx(n), y = ny(n);
            if (x < left || x > left + 360 || y < top || y > top + 252) {
                continue;
            }
            boolean over = inside(mx, my) && Math.abs(mx - x) < 13 * zoom && Math.abs(my - y) < 13 * zoom;
            if (over) {
                hovered = n;
            }

            boolean unlocked = isUnlocked(n);
            boolean available = !unlocked && parentsUnlocked(n);
            float brightness = unlocked ? 1.0F : available ? (over ? 0.72F : 0.58F) : (over ? 0.42F : 0.25F);

            g.pose().pushPose();
            g.pose().translate(x - 13 * zoom, y - 13 * zoom, 0);
            g.pose().scale(zoom, zoom, 1);
            BookSkin.node(g,unlocked,available,secondary.contains(n.key()),over);
            g.setColor(brightness, brightness, brightness, 1);
            if (n.icon() != null) {
                g.blit(n.icon(), 5, 5, 16, 16, 0, 0, n.iconWidth(), Math.min(n.iconWidth(), n.iconHeight()), n.iconWidth(), n.iconHeight());
            } else if (n.item() != null) {
                ResourceLocation itemId = ResourceLocation.tryParse(n.item());
                if (itemId != null) {
                    g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(itemId)), 5, 5);
                }
            }
            g.pose().popPose();
            g.setColor(1, 1, 1, 1);
        }

        g.disableScissor();
        for (int i = 0; i < TABS.length; i++) {
            int x = left - 22, y = top + 12 + i * 32;
            g.fill(x+2,y+2,left+6,y+28,0x50000000);
            g.fill(x,y,left+6,y+26,i==tab?0xFF65735F:0xFF92816B);
            g.fill(x+2,y+2,left+4,y+23,i==tab?0xFF809078:0xFFAF9C7F);
            g.blit(id("textures/" + ICONS[i] + ".png"), x + 4, y + 4, 16, 16, 0, 0, 16, 16, 16, 16);
        }
        g.drawCenteredString(font,Component.translatable("tc.book.map_hint"),left+180,top+232,0x776B55);
        if (loadFailed) {
            g.drawCenteredString(font, Component.translatable("tc.tree.load_failed"), left + 180, top + 110, 0x704A3A);
        }
        g.pose().popPose();

        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tc.research_name." + thaumcraft.common.research.ResearchNotes.displayKey(hovered.key())));
            String description = "tc.research_text." + hovered.key();
            if (net.minecraft.client.resources.language.I18n.exists(description)) {
                lines.add(Component.translatable(description));
            }
            if (!isUnlocked(hovered)) {
                lines.add(Component.translatable(parentsUnlocked(hovered)?"tc.book.available":"tc.tree.locked").withStyle(net.minecraft.ChatFormatting.GRAY));
                if(parentsUnlocked(hovered))lines.add(Component.translatable(secondary.contains(hovered.key())?"tc.progress.secondary_hint":"tc.progress.start_hint"));
            }
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        } else {
            for (int i = 0; i < TABS.length; i++) {
                if (mx >= left - 22 && mx < left + 6 && my >= top + 12 + i * 32 && my < top + 12 + i * 32 + 26) {
                    g.renderTooltip(font, Component.translatable("tc.research_category." + TABS[i]), mouseX, mouseY);
                }
            }
        }
    }

    private void line(GuiGraphics g, int x, int y, int endX, int endY, int color) {
        int steps = Math.max(Math.abs(endX - x), Math.abs(endY - y));
        for (int i = 0; i <= steps; i++) {
            int px = x + (endX - x) * i / Math.max(1, steps);
            int py = y + (endY - y) * i / Math.max(1, steps);
            if (inside(px, py)) {
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) {
            return super.mouseClicked(x, y, button);
        }
        x /= scale;
        y /= scale;
        for (int i = 0; i < TABS.length; i++) {
            if (x >= left - 22 && x < left + 6 && y >= top + 12 + i * 32 && y < top + 12 + i * 32 + 26) {
                tab = i;
                return true;
            }
        }
        if (inside(x, y)) {
            dragging = true;
            dragDistance = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (!dragging || button != 0) {
            return false;
        }
        dragDistance += Math.abs(dx) + Math.abs(dy);
        panX[tab] -= dx / scale / zoom;
        panY[tab] -= dy / scale / zoom;
        clampPan();
        return true;
    }

    private void clampPan() {
        var list = nodes.stream().filter(n -> n.category().equals(TABS[tab])).toList();
        panX[tab] = net.minecraft.util.Mth.clamp(
                panX[tab],
                list.stream().mapToInt(Node::x).min().orElse(0) * 24 - 48,
                list.stream().mapToInt(Node::x).max().orElse(0) * 24 + 48
        );
        panY[tab] = net.minecraft.util.Mth.clamp(
                panY[tab],
                list.stream().mapToInt(Node::y).min().orElse(0) * 24 - 48,
                list.stream().mapToInt(Node::y).max().orElse(0) * 24 + 48
        );
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (dragging && button == 0) {
            dragging = false;
            if (dragDistance < 4 && hovered != null && !isUnlocked(hovered) && parentsUnlocked(hovered)) {
                thaumcraft.common.lib.network.ModNetwork.requestResearch(hovered.key());
            }
            if (dragDistance < 4 && hovered != null && isUnlocked(hovered)) {
                int chapter = switch (hovered.key()) {
                    case "BASICTHAUMATURGY", "THAUMONOMICON" -> 0;
                    case "ASPECTS" -> 3;
                    default -> -1;
                };
                minecraft.setScreen(chapter >= 0
                        ? new ThaumonomiconScreen(this, chapter)
                        : new ResearchEntryScreen(this, hovered));
            }
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (!inside(x / scale, y / scale)) {
            return false;
        }
        double localX = x / scale - left - 180, localY = y / scale - top - 124;
        float next = net.minecraft.util.Mth.clamp(zoom + (float) delta * 0.1F, 0.6F, 1.6F);
        panX[tab] += localX / zoom - localX / next;
        panY[tab] += localY / zoom - localY / next;
        zoom = next;
        clampPan();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

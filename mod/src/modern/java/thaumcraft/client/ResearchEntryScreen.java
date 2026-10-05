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
    private final List<net.minecraft.world.item.crafting.Recipe<?>> recipes=new ArrayList<>();
    private int pageCount() {return pages.size()+recipes.size();}
    private int spread, left, top;
    private float scale;
    private Button back, next;
    public ResearchEntryScreen(Screen parent, ResearchTreeScreen.Node node) {
        super(Component.translatable("tc.research_name." + thaumcraft.common.research.ResearchNotes.displayKey(node.key())));
        this.parent = parent; this.node = node;
    }
    @Override protected void init() {
        scale = Math.min(1, Math.min(width / 380F, height / 290F));
        left = ((int)(width / scale) - 360) / 2;
        top = ((int)(height / scale) - 280) / 2;
        pages.clear();
        for (String key : node.pages()) {
            if (!I18n.exists(key)) continue;
            // Legacy image tags contain asset paths, not readable book text.
            String text = I18n.get(key).replaceAll("(?s)<IMG>.*?</IMG>", "")
                    .replace("<LINE>", "\n\n").replace("<BR>", "\n")
                    .replace("<PAGE>", "\n\n").replaceAll("<[^>]*>", "");
            var lines = font.split(Component.literal(text), 140);
            for (int i = 0; i < lines.size(); i += 17) pages.add(new ArrayList<>(lines.subList(i, Math.min(lines.size(), i + 17))));
        }
        if (pages.isEmpty()) pages.add(font.split(Component.translatable("tc.research_text." + node.key()), 140));
        recipes.clear();
        if(minecraft.level!=null)minecraft.level.getRecipeManager().getRecipes().stream().filter(recipe -> {
            String key=recipe instanceof thaumcraft.common.crafting.ArcaneRecipe r?r.getResearch():
                recipe instanceof thaumcraft.common.alchemy.CrucibleRecipe r?r.research():
                recipe instanceof thaumcraft.common.infusion.InfusionRecipe r?r.research():"";
            if(!key.isEmpty())return key.equalsIgnoreCase(node.key());
            if(!recipe.getId().getNamespace().equals("thaumcraft"))return false;
            return switch(node.key()) {
                case "RESEARCH" -> java.util.Set.of("scribing_tools","scribing_refill","phial").contains(recipe.getId().getPath());
                case "TABLE" -> recipe.getId().getPath().equals("table");
                default -> false;
            };
        }).sorted(Comparator.comparing(r -> r.getId().toString())).forEach(recipes::add);
        spread = Math.min(spread, (pageCount() - 1) / 2);
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
            if(index>=pages.size()) {if(index<pageCount())drawRecipe(g,recipes.get(index-pages.size()),left+(side==0?23:196),top+43,(int)(mx/scale),(int)(my/scale));continue;}
            int y = top + 43;
            for (var line : pages.get(index)) { g.drawString(font, line, left + (side == 0 ? 23 : 196), y, 0x423024, false); y += 10; }
        }
        back.active = spread > 0; next.active = spread * 2 + 2 < pageCount();
        g.drawCenteredString(font, (spread + 1) + " / " + ((pageCount() + 1) / 2), left + 180, top + 231, 0xC3A576);
        super.render(g, (int)(mx / scale), (int)(my / scale), partialTick);
        g.pose().popPose();
    }
    @Override public boolean mouseClicked(double x, double y, int b) { return super.mouseClicked(x / scale, y / scale, b); }
    private void drawRecipe(GuiGraphics g,net.minecraft.world.item.crafting.Recipe<?> recipe,int x,int y,int mx,int my) {
        if(recipe instanceof thaumcraft.common.crafting.ScribingRefillRecipe) {
            g.drawWordWrap(font,Component.translatable("tc.progress.refill_help"),x,y,137,0x423024);
            g.renderItem(new net.minecraft.world.item.ItemStack(thaumcraft.common.research.ModResearch.SCRIBING_TOOLS.get()),x,y+48);
            g.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.INK_SAC),x+25,y+48);
            return;
        }
        var ingredients=new ArrayList<net.minecraft.world.item.crafting.Ingredient>();var cost=new thaumcraft.api.aspects.AspectList();int columns=3;
        if(recipe instanceof thaumcraft.common.alchemy.CrucibleRecipe r) {ingredients.add(r.catalyst());cost=r.cost();}
        else if(recipe instanceof thaumcraft.common.infusion.InfusionRecipe r) {ingredients.add(r.input());ingredients.addAll(r.components());cost=r.aspects();}
        else {ingredients.addAll(recipe.getIngredients());if(recipe instanceof thaumcraft.common.crafting.ArcaneRecipe r) {cost=r.getAspects();columns=r.width();}}
        g.drawString(font,Component.translatable("tc.progress.recipe"),x,y,0x423024,false);
        var output=recipe.getResultItem(minecraft.level.registryAccess());g.renderItem(output,x+110,y+26);g.renderItemDecorations(font,output,x+110,y+26);
        if(mx>=x+110 && mx<x+126 && my>=y+26 && my<y+42)g.renderTooltip(font,output,mx,my);
        for(int i=0;i<ingredients.size();i++) {
            var choices=ingredients.get(i).getItems();int px=x+(i%columns)*20,py=y+20+(i/columns)*20;
            g.fill(px-1,py-1,px+17,py+17,0x33805C37);if(choices.length==0)continue;
            var stack=choices[(int)(minecraft.level.getGameTime()/20%choices.length)];g.renderItem(stack,px,py);
            if(mx>=px && mx<px+16 && my>=py && my<py+16)g.renderTooltip(font,stack,mx,my);
        }
        if(recipe instanceof thaumcraft.common.infusion.InfusionRecipe r)g.drawString(font,"⚠ "+r.instability(),x,y+90,0x663344,false);
        int i=0;
        for(var aspect:cost.getAspects()) {int px=x+i%4*34,py=y+115+i/4*27;int c=aspect.getColor();g.setColor((c>>16&255)/255F,(c>>8&255)/255F,(c&255)/255F,1);
            g.blit(aspect.getImage(),px,py,16,16,0,0,32,32,32,32);g.setColor(1,1,1,1);g.drawString(font,Integer.toString(cost.getAmount(aspect)),px+17,py+4,0x423024,false);
            if(mx>=px && mx<px+16 && my>=py && my<py+16)g.renderTooltip(font,Component.literal(aspect.getName()),mx,my);i++;}
    }
    @Override public boolean mouseReleased(double x, double y, int b) { return super.mouseReleased(x / scale, y / scale, b); }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

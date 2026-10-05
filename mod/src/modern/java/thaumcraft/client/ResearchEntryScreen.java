package thaumcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Original reference text with page wrapping; this screen does not unlock recipes. */
public final class ResearchEntryScreen extends Screen {
    private final Screen parent;
    private final ResearchTreeScreen.Node node;
    private final List<List<BookPageLayout.Block>> pages = new ArrayList<>();
    private final BookRecipeRenderer recipeRenderer=new BookRecipeRenderer();
    private final List<net.minecraft.world.item.crafting.Recipe<?>> recipes=new ArrayList<>();
    private final List<List<net.minecraft.world.item.crafting.Recipe<?>>> recipePages=new ArrayList<>();
    private int pageCount() {return pages.size()+recipePages.size();}
    private int spread, left, top;
    private float scale;
    private Button back, next;
    public ResearchEntryScreen(Screen parent, ResearchTreeScreen.Node node) {
        super(Component.translatable("tc.research_name." + thaumcraft.common.research.ResearchNotes.displayKey(node.key())));
        this.parent = parent; this.node = node;
    }
    @Override protected void init() {
        scale = Math.min(1, Math.min(width / 410F, height / 310F));
        left = ((int)(width / scale) - 360) / 2;
        top = ((int)(height / scale) - 252) / 2;
        pages.clear();
        var originalText=new ArrayList<String>();
        for(String key:node.pages())if(I18n.exists(key))originalText.add(I18n.get(key));
        if(originalText.isEmpty())originalText.add(I18n.get("tc.research_text."+node.key()));
        pages.addAll(BookPageLayout.layout(font,minecraft.getResourceManager(),originalText));
        recipes.clear();
        if(minecraft.level!=null)minecraft.level.getRecipeManager().getRecipes().stream().filter(recipe -> {
            String key=recipe instanceof thaumcraft.common.crafting.ArcaneRecipe r?r.getResearch():
                recipe instanceof thaumcraft.common.alchemy.CrucibleRecipe r?r.research():
                recipe instanceof thaumcraft.common.infusion.InfusionRecipe r?r.research():"";
            if(!key.isEmpty())return key.equalsIgnoreCase(node.key());
            if(!recipe.getId().getNamespace().equals("thaumcraft"))return false;
            return switch(node.key()) {
                case "BASICTHAUMATURGY" -> java.util.Set.of("wand_cap_iron","wand","thaumometer").contains(recipe.getId().getPath());
                case "RESEARCH" -> java.util.Set.of("scribing_tools","scribing_refill","phial").contains(recipe.getId().getPath());
                case "TABLE" -> recipe.getId().getPath().equals("table");
                default -> false;
            };
        }).sorted(Comparator.comparing(r -> r.getId().toString())).forEach(recipes::add);
        recipePages.clear();
        var variants=new LinkedHashMap<String,List<net.minecraft.world.item.crafting.Recipe<?>>>();
        for(var recipe:recipes){
            var result=recipe.getResultItem(minecraft.level.registryAccess());
            String key=recipe.getType()+"/"+result.getItem()+"/"+result.getCount()+"/"+result.getTag();
            variants.computeIfAbsent(key,k -> new ArrayList<>()).add(recipe);
        }
        recipePages.addAll(variants.values());
        spread = Math.min(spread, (pageCount() - 1) / 2);
        back = addRenderableWidget(BookSkin.turn(left+22,top+225,false,b -> spread--));
        next = addRenderableWidget(BookSkin.turn(left+314,top+225,true,b -> spread++));
        addRenderableWidget(BookSkin.back(left+204,top+224,106,Component.translatable("tc.tree.back"),b -> onClose()));
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        recipeRenderer.reset();
        g.pose().pushPose(); g.pose().scale(scale, scale, 1);
        BookSkin.draw(g,left,top,360,252);
        g.drawWordWrap(font, title, left + 23, top + 18, 140, 0x423024);
        for (int side = 0; side < 2; side++) {
            int index = spread * 2 + side;
            if(index>=pages.size()) {
                if(index<pageCount())drawRecipe(g,recipePages.get(index-pages.size()).get((int)(minecraft.level.getGameTime()/60%recipePages.get(index-pages.size()).size())),left+(side==0?23:196),top+43,(int)(mx/scale),(int)(my/scale));
                else endPage(g);
                continue;
            }
            int y = top + 43;
            for (var block : pages.get(index)) { block.draw(g,font,left + (side == 0 ? 23 : 196),y); y+=block.height(); }
        }
        back.active = spread > 0; next.active = spread * 2 + 2 < pageCount();
        g.drawString(font, (spread + 1) + " / " + ((pageCount() + 1) / 2), left + 90, top + 231, 0x776B55,false);
        super.render(g, (int)(mx / scale), (int)(my / scale), partialTick);
        g.pose().popPose();
        recipeRenderer.tooltip(g,mx,my);
    }
    @Override public boolean mouseClicked(double x, double y, int b) { return super.mouseClicked(x / scale, y / scale, b); }
    private void endPage(GuiGraphics g) {
        BookSkin.caption(g,font,Component.translatable("tc.research_category."+node.category()),left+200,top+20,130,0.75F);
        g.pose().pushPose();g.pose().translate(left+246,top+80,0);g.pose().scale(3,3,1);
        if(node.icon()!=null)g.blit(node.icon(),0,0,16,16,0,0,node.iconWidth(),Math.min(node.iconWidth(),node.iconHeight()),node.iconWidth(),node.iconHeight());
        else if(node.item()!=null) {
            var id=ResourceLocation.tryParse(node.item());
            if(id!=null)g.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)),0,0);
        }
        g.pose().popPose();
        g.fill(left+232,top+146,left+308,top+147,0xFFCEC4AC);
        g.drawWordWrap(font,title,left+211,top+155,120,0x5D5547);
        BookSkin.caption(g,font,Component.translatable("tc.book.end_entry"),left+232,top+193,95,0.75F);
    }
    private void drawRecipe(GuiGraphics g,net.minecraft.world.item.crafting.Recipe<?> recipe,int x,int y,int mx,int my) {
        recipeRenderer.render(g,recipe,x,y,mx,my);
    }
    @Override public boolean mouseReleased(double x, double y, int b) { return super.mouseReleased(x / scale, y / scale, b); }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

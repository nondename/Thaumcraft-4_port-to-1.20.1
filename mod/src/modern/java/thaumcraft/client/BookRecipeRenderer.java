package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.alchemy.CrucibleRecipe;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.infusion.InfusionRecipe;

/** Separate TC4 recipe-page arrangements, backed by the actual server recipes. */
final class BookRecipeRenderer {
    private final Minecraft mc=Minecraft.getInstance();
    private static final ResourceLocation OVERLAY=ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/gui/gui_researchbook_overlay.png");
    private ItemStack hover=ItemStack.EMPTY;
    private Component aspectHover;
    void render(GuiGraphics g,Recipe<?> recipe,int x,int y,int mx,int my){
        String type=recipe instanceof CrucibleRecipe?"crucible":recipe instanceof InfusionRecipe?"infusion":recipe instanceof ArcaneRecipe?"arcane":recipe instanceof ShapedRecipe?"workbench":"workbenchshapeless";
        var label=Component.translatable("recipe.type."+type);
        BookSkin.caption(g,mc.font,label,x,y,140,0.95F);
        boolean refill=recipe instanceof thaumcraft.common.crafting.ScribingRefillRecipe;
        ItemStack result=refill?new ItemStack(thaumcraft.common.research.ModResearch.SCRIBING_TOOLS.get()):recipe.getResultItem(mc.level.registryAccess());
        slot(g,result,x+62,y+21,mx,my);
        if(recipe instanceof CrucibleRecipe r){
            ingredient(g,r.catalyst(),x+31,y+61,mx,my);
            arrow(g,x+38,y+83,true);
            var crucible=new ItemStack(thaumcraft.common.alchemy.ModAlchemy.CRUCIBLE_ITEM.get());
            g.pose().pushPose();g.pose().translate(x+60,y+65,0);g.pose().scale(2,2,1);g.renderItem(crucible,0,0);g.pose().popPose();
            arrow(g,x+70,y+48,false);
            aspects(g,r.cost(),x+26,y+115,3,mx,my);
        }else if(recipe instanceof InfusionRecipe r){
            // Original altar diagram and components distributed around the central input.
            g.blit(OVERLAY,x+10,y+50,120,88,400,154,120,88,512,512);
            ingredient(g,r.input(),x+62,y+86,mx,my);
            var components=r.components();
            for(int i=0;i<components.size();i++){
                double a=-Math.PI/2+i*Math.PI*2/components.size();
                ingredient(g,components.get(i),x+62+(int)Math.round(Math.cos(a)*43),y+86+(int)Math.round(Math.sin(a)*31),mx,my);
            }
            aspects(g,r.aspects(),x+6,y+138,5,mx,my);
            BookSkin.caption(g,mc.font,Component.translatable("tc.book.instability",r.instability()),x,y+11,140,0.8F);
        }else{
            java.util.List<Ingredient> ingredients=refill?java.util.List.of(Ingredient.of(thaumcraft.common.research.ModResearch.SCRIBING_TOOLS.get()),Ingredient.of(net.minecraft.world.item.Items.INK_SAC)):recipe.getIngredients();
            int columns=recipe instanceof ArcaneRecipe r?r.width():recipe instanceof ShapedRecipe r?r.getWidth():3;
            int rows=recipe instanceof ArcaneRecipe r?r.height():recipe instanceof ShapedRecipe r?r.getHeight():(ingredients.size()+2)/3;
            int gridY=recipe instanceof ArcaneRecipe?y+54:y+62;
            // Exact TC4 overlay UVs were in a 256-space; this PNG is actually 512 square.
            g.blit(OVERLAY,x+30,gridY,80,80,recipe instanceof ArcaneRecipe?224:120,30,104,104,512,512);
            for(int i=0;i<9;i++){
                int col=i%3,row=i/3;
                int index=row*columns+col;
                if(col<columns && row<rows && index<ingredients.size())
                    ingredient(g,ingredients.get(index),x+35+col*27,gridY+5+row*27,mx,my);
            }
            arrow(g,x+70,y+44,true);
            if(recipe instanceof ArcaneRecipe r)aspects(g,r.getAspects(),x+26,y+138,3,mx,my);
        }
    }
    private void slot(GuiGraphics g,ItemStack stack,int x,int y,int mx,int my){
        if(stack.isEmpty())return;
        g.renderItem(stack,x,y);g.renderItemDecorations(mc.font,stack,x,y);
        if(mx>=x && mx<x+16 && my>=y && my<y+16)hover=stack;
    }
    private void ingredient(GuiGraphics g,Ingredient ingredient,int x,int y,int mx,int my){
        var choices=ingredient.getItems();if(choices.length>0)slot(g,choices[(int)(mc.level.getGameTime()/20%choices.length)],x,y,mx,my);
    }
    private static void arrow(GuiGraphics g,int x,int y,boolean down){
        int c=0xFF47311E;g.fill(x-1,y,x+1,y+8,c);
        for(int i=0;i<4;i++){int py=down?y+7-i:y+i;g.fill(x-i,py,x+i+1,py+1,c);}
    }
    private void aspects(GuiGraphics g,AspectList list,int x,int y,int columns,int mx,int my){
        int i=0;
        for(var aspect:list.getAspects()){
            int px=x+(i%columns)*27,py=y+(i/columns)*21,c=aspect.getColor();
            g.setColor((c>>16&255)/255F,(c>>8&255)/255F,(c&255)/255F,1);
            g.blit(aspect.getImage(),px,py,16,16,0,0,32,32,32,32);g.setColor(1,1,1,1);
            g.drawString(mc.font,Integer.toString(list.getAmount(aspect)),px+17,py+5,0x352316,false);
            if(mx>=px && mx<px+16 && my>=py && my<py+16)aspectHover=Component.literal(aspect.getName());i++;
        }
    }
    void reset(){hover=ItemStack.EMPTY;aspectHover=null;}
    void tooltip(GuiGraphics g,int mouseX,int mouseY){
        if(!hover.isEmpty())g.renderTooltip(mc.font,hover,mouseX,mouseY);
        else if(aspectHover!=null)g.renderTooltip(mc.font,aspectHover,mouseX,mouseY);
    }
}

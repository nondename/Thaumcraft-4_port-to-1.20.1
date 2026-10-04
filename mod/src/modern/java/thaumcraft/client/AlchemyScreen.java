package thaumcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.infusion.AlchemyMenu;

public final class AlchemyScreen extends AbstractContainerScreen<AlchemyMenu> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("thaumcraft","textures/gui/gui_alchemyfurnace.png");
    public AlchemyScreen(AlchemyMenu menu,Inventory inventory,Component title) {super(menu,inventory,title);}
    @Override public void render(GuiGraphics g,int x,int y,float tick) {renderBackground(g);super.render(g,x,y,tick);renderTooltip(g,x,y);}
    @Override protected void renderBg(GuiGraphics g,float tick,int x,int y) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(TEXTURE,leftPos,topPos,0,0,176,166,256,256);
        g.fill(leftPos+79,topPos+46,leftPos+97,topPos+64,0xFF333333);
        if(menu.burning())g.blit(TEXTURE,leftPos+104,topPos+26,176,0,14,20,256,256);
        int progress=menu.progress();g.fill(leftPos+106,topPos+48-progress/5,leftPos+111,topPos+68,0xFFFF7722);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) {g.drawString(font,playerInventoryTitle,8,74,0x404040,false);}
}

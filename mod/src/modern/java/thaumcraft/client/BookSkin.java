package thaumcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Original TC4 book texture and page arrows, without the experimental skin. */
final class BookSkin {
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbook.png");
    private BookSkin() {}
    static void draw(GuiGraphics g,int x,int y,int width,int height) {
        g.setColor(1,1,1,1);
        g.blit(BOOK,x,y,width,height,0,0,512,360,512,512);
    }
    static void caption(GuiGraphics g,net.minecraft.client.gui.Font font,Component text,int x,int y,int width,float size) {
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size,size,1);
        String fitted=font.plainSubstrByWidth(text.getString(),(int)(width/size));
        g.drawString(font,fitted,0,0,0x303030,false);g.pose().popPose();
    }
    static Button back(int x,int y,int width,Component label,Button.OnPress press) {
        return Button.builder(label,press).bounds(x,y,width,20).build();
    }
    static Button turn(int x,int y,boolean next,Button.OnPress press) {
        return new Button(x,y,24,16,Component.translatable(next?"tc.book.next_page":"tc.book.previous_page"),press,supplier -> supplier.get()) {
            @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float tick) {
                if(!active)return;
                float tint=isHoveredOrFocused()?1.0F:0.8F;
                g.setColor(tint,tint,tint,1);
                g.blit(BOOK,getX(),getY(),24,16,next?24:0,368,24,16,512,512);
                g.setColor(1,1,1,1);
            }
        };
    }
}

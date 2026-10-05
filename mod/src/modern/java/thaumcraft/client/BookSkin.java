package thaumcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Pixel-aligned paper and leather; shared by the map and reading screens. */
final class BookSkin {
    private static final net.minecraft.resources.ResourceLocation VANILLA_BOOK =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","textures/gui/book.png");
    private BookSkin() {}
    static void draw(GuiGraphics g,int x,int y,int width,int height) {
        // Offset shadow, leather cover, stacked page edges and two clean paper leaves.
        g.fill(x+4,y+5,x+width+4,y+height+5,0x65000000);
        // A stepped silhouette distinguishes the leaves from a rectangular picture frame.
        g.fill(x,y+8,x+width,y+height-8,0xFF302D2B);
        g.fill(x+4,y+4,x+width-4,y+height-4,0xFF302D2B);
        g.fill(x+8,y+2,x+width/2-6,y+height-2,0xFF52463B);
        g.fill(x+width/2+6,y+2,x+width-8,y+height-2,0xFF52463B);
        g.fill(x+7,y+7,x+width-7,y+height-6,0xFFB7AA8C);
        g.fill(x+8,y+8,x+width-8,y+height-9,0xFFDDD3B5);
        g.fill(x+10,y+9,x+width/2,y+height-12,0xFFF6F0DE);
        g.fill(x+width/2,y+9,x+width-10,y+height-12,0xFFFAF5E7);
        // Reuse Minecraft's paper grain and corner details instead of a flat colour.
        g.blit(VANILLA_BOOK,x+10,y+9,width/2-10,height-21,32,10,122,163,256,256);
        g.pose().pushPose();g.pose().translate(x+width-10,y+9,0);g.pose().scale(-1,1,1);
        g.blit(VANILLA_BOOK,0,0,width/2-10,height-21,32,10,122,163,256,256);
        g.pose().popPose();
        g.fill(x+11,y+10,x+width-11,y+11,0xFFFFFAE8);
        g.fill(x+width/2-4,y+10,x+width/2-2,y+height-12,0xFFEBE3CF);
        g.fill(x+width/2-2,y+10,x+width/2,y+height-12,0xFFD9CFB9);
        g.fill(x+width/2,y+10,x+width/2+1,y+height-12,0xFFBEB39D);
        g.fill(x+width/2+1,y+10,x+width/2+3,y+height-12,0xFFE6DDC9);
        // Small page-edge details, without noise behind the text.
        for(int offset=18;offset<height-18;offset+=23) {
            g.fill(x+8,y+offset,x+10,y+offset+8,0xFFC6BBA2);
            g.fill(x+width-10,y+offset+4,x+width-8,y+offset+12,0xFFD1C7AF);
        }
        g.fill(x+8,y+height-10,x+width-8,y+height-9,0xFFF8F0DA);
        g.fill(x+9,y+height-7,x+width-9,y+height-6,0xFF8D7D64);
    }
    static void caption(GuiGraphics g,net.minecraft.client.gui.Font font,Component text,int x,int y,int width,float size) {
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size,size,1);
        String fitted=font.plainSubstrByWidth(text.getString(),(int)(width/size));
        g.drawString(font,fitted,0,0,0x665E4E,false);g.pose().popPose();
    }
    static Button back(int x,int y,int width,Component label,Button.OnPress press) {
        return new Button(x,y,width,16,label,press,supplier -> supplier.get()) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float tick) {
                var font=net.minecraft.client.Minecraft.getInstance().font;
                int color=isHoveredOrFocused()?0xFF8E6840:0xFF6C6150;
                String fitted=font.plainSubstrByWidth(getMessage().getString(),getWidth()-8);
                int tx=getX()+(getWidth()-font.width(fitted))/2;
                g.drawString(font,fitted,tx,getY()+4,color,false);
                if(isHoveredOrFocused())g.fill(tx,getY()+14,tx+font.width(fitted),getY()+15,color);
            }
        };
    }
    static Button turn(int x,int y,boolean next,Button.OnPress press) {
        return new Button(x,y,24,18,Component.translatable(next?"tc.book.next_page":"tc.book.previous_page"),press,supplier -> supplier.get()) {
            @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float tick) {
                if(!active)return;
                int ink=isHoveredOrFocused()?0xFFAA793B:0xFF70533A;
                int cx=getX()+12,cy=getY()+9;
                g.fill(getX()+5,cy-1,getX()+19,cy+2,ink);
                for(int i=0;i<6;i++) {
                    int px=next?cx+6-i:cx-6+i;
                    g.fill(px,cy-i,px+1,cy+i+1,ink);
                }
            }
        };
    }
    static void node(GuiGraphics g,boolean unlocked,boolean available,boolean secondary,boolean hovered) {
        int edge=unlocked?0xFF57765B:available?0xFF927047:0xFFACA28B;
        int paper=unlocked?0xFFF1F2DB:available?0xFFFFF8DF:0xFFE0D8C1;
        if(hovered)edge=0xFF44382F;
        // Hexagonal secondary research, square primary research.
        if(secondary)for(int y=0;y<26;y++) {
            int inset=Math.max(0,Math.abs(y-13)-7)/2;
            g.fill(inset,y,26-inset,y+1,edge);
            if(y>1 && y<24)g.fill(inset+2,y,24-inset,y+1,paper);
        } else {
            g.fill(1,1,25,25,edge);g.fill(3,3,23,23,paper);
        }
        if(unlocked) {g.fill(20,20,26,26,0xFF57765B);g.fill(21,22,23,24,0xFFFFFFFF);g.fill(23,21,25,23,0xFFFFFFFF);}
        else if(!available) {g.fill(20,21,26,26,0xFF8D8270);g.fill(21,19,25,21,0xFF8D8270);}
    }
}

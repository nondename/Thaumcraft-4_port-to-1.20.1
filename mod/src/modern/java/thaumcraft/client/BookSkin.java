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
        g.fill(x+4,y+5,x+width+4,y+height+5,0x65000000);
        // One vanilla cover around the entire spread; preserve corner pixels with nine slices.
        int[] sourceX={20,32,156}, sourceY={1,13,169}, sourceW={12,124,12}, sourceH={12,156,12};
        int[] destX={x,x+12,x+width-12},destY={y,y+12,y+height-12};
        int[] destW={12,width-24,12},destH={12,height-24,12};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)
            g.blit(VANILLA_BOOK,destX[col],destY[row],destW[col],destH[row],sourceX[col],sourceY[row],sourceW[col],sourceH[row],256,256);
        g.setColor(0.88F,0.83F,0.72F,1);
        g.blit(VANILLA_BOOK,x+12,y+12,width-24,height-24,32,10,122,163,256,256);
        g.setColor(1,1,1,1);
        int fold=x+width/2;
        g.fill(fold-3,y+12,fold-1,y+height-12,0xFFCCBB97);
        g.fill(fold-1,y+12,fold+1,y+height-12,0xFF9B825D);
        g.fill(fold+1,y+12,fold+3,y+height-12,0xFFD5C6A5);
    }
    static void caption(GuiGraphics g,net.minecraft.client.gui.Font font,Component text,int x,int y,int width,float size) {
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(size,size,1);
        String fitted=font.plainSubstrByWidth(text.getString(),(int)(width/size));
        g.drawString(font,fitted,0,0,0x3D291C,false);g.pose().popPose();
    }
    static Button back(int x,int y,int width,Component label,Button.OnPress press) {
        return new Button(x,y,width,16,label,press,supplier -> supplier.get()) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float tick) {
                var font=net.minecraft.client.Minecraft.getInstance().font;
                int color=isHoveredOrFocused()?0xFF713C16:0xFF392516;
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
                int ink=isHoveredOrFocused()?0xFF965015:0xFF392516;
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
        int edge=unlocked?0xFF36583C:available?0xFF77502B:0xFF766653;
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

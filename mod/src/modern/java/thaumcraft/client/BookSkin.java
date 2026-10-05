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
        // Vanilla's complete leather binding, stitching and paper corners on both leaves.
        // Reverse UVs, not geometry: a negative pose scale culls the right-hand leaf.
        g.fill(x+3,y+4,x+width-3,y+height-3,0xFF493022);
        g.blit(VANILLA_BOOK,x,y,width/2,height,20,1,148,180,256,256);
        g.blit(VANILLA_BOOK,x+width/2,y,width-width/2,height,168,1,-148,180,256,256);
        int fold=x+width/2;
        g.fill(fold-4,y+8,fold-2,y+height-10,0xFFE3D4AF);
        g.fill(fold-2,y+8,fold,y+height-10,0xFFCDBA92);
        g.fill(fold,y+8,fold+1,y+height-10,0xFF9A8059);
        g.fill(fold+1,y+8,fold+3,y+height-10,0xFFE9DCBF);
        // Small brass corners on the leather, like a well-used grimoire.
        for(int corner : new int[]{x+3,x+width-7}) {
            g.fill(corner,y+5,corner+4,y+6,0xFFB9904B);
            g.fill(corner,y+5,corner+1,y+11,0xFFB9904B);
            g.fill(corner,y+height-8,corner+4,y+height-7,0xFFB9904B);
        }
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

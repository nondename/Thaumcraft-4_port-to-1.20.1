package thaumcraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Pixel-aligned paper and leather; shared by the map and reading screens. */
final class BookSkin {
    private BookSkin() {}
    static void draw(GuiGraphics g,int x,int y,int width,int height) {
        // Offset shadow, leather cover, stacked page edges and two clean paper leaves.
        g.fill(x+4,y+5,x+width+4,y+height+5,0x65000000);
        g.fill(x,y+2,x+width,y+height-2,0xFF392A24);
        g.fill(x+2,y,x+width-2,y+height,0xFF392A24);
        g.fill(x+4,y+4,x+width-4,y+height-4,0xFF795641);
        g.fill(x+7,y+7,x+width-7,y+height-6,0xFFB7AA8C);
        g.fill(x+8,y+8,x+width-8,y+height-9,0xFFDDD3B5);
        g.fill(x+10,y+9,x+width/2,y+height-12,0xFFF7F0D8);
        g.fill(x+width/2,y+9,x+width-10,y+height-12,0xFFFBF5E1);
        g.fill(x+11,y+10,x+width-11,y+11,0xFFFFFAE8);
        g.fill(x+width/2-7,y+10,x+width/2-3,y+height-12,0xFFEBE1C7);
        g.fill(x+width/2-3,y+10,x+width/2-1,y+height-12,0xFFDACDB0);
        g.fill(x+width/2-1,y+10,x+width/2+1,y+height-12,0xFFBBA98B);
        g.fill(x+width/2+1,y+10,x+width/2+4,y+height-12,0xFFE6D9BE);
        g.fill(x+width/2+4,y+10,x+width/2+7,y+height-12,0xFFF0E6CC);
        g.fill(x+8,y+height-10,x+width-8,y+height-9,0xFFF8F0DA);
        g.fill(x+9,y+height-7,x+width-9,y+height-6,0xFF8D7D64);
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

package thaumcraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;
import java.util.regex.Pattern;

/** TC4 inline markup, laid out by pixel height so illustrations never overlap text. */
final class BookPageLayout {
    interface Block { int height(); void draw(GuiGraphics g,Font font,int x,int y); }
    record Text(FormattedCharSequence line) implements Block {
        public int height(){return 10;}
        public void draw(GuiGraphics g,Font font,int x,int y){g.drawString(font,line,x,y,0x352316,false);}
    }
    record Rule() implements Block {
        public int height(){return 8;}
        public void draw(GuiGraphics g,Font font,int x,int y){g.fill(x+16,y+3,x+124,y+4,0xFF715337);}
    }
    record Picture(ResourceLocation texture,int u,int v,int w,int h,int tw,int th,int dw,int dh) implements Block {
        public int height(){return dh+6;}
        public void draw(GuiGraphics g,Font font,int x,int y){
            g.setColor(1,1,1,1);g.blit(texture,x+(140-dw)/2,y+3,dw,dh,u,v,w,h,tw,th);
        }
    }
    private static final Pattern TOKENS=Pattern.compile("(?s)<IMG>(.*?)</IMG>|<PAGE>|<LINE>");
    static List<List<Block>> layout(Font font,ResourceManager resources,List<String> texts) {
        var result=new ArrayList<List<Block>>();var current=new ArrayList<Block>();
        for(String source:texts) {
            var matcher=TOKENS.matcher(source);int previous=0;
            while(matcher.find()) {
                addText(font,source.substring(previous,matcher.start()),result,current);
                if(matcher.group().equals("<PAGE>"))flush(result,current);
                else if(matcher.group().equals("<LINE>"))add(new Rule(),result,current);
                else {
                    try {
                        String[] p=matcher.group(1).split(":");
                        var texture=ResourceLocation.fromNamespaceAndPath(p[0],p[1]);
                        int u=Integer.parseInt(p[2]),v=Integer.parseInt(p[3]),w=Integer.parseInt(p[4]),h=Integer.parseInt(p[5]);
                        int tw,th;
                        try(var in=new java.io.DataInputStream(resources.open(texture))){in.skipNBytes(16);tw=in.readInt();th=in.readInt();}
                        // Legacy item inserts used 256-space UVs even for a 16px item PNG.
                        if(tw<256 && w>tw){u=u*tw/256;v=v*th/256;w=Math.max(1,Math.round(w*tw/256F));h=Math.max(1,Math.round(h*th/256F));}
                        float factor=Float.parseFloat(p[6]);
                        int dw=Math.max(1,Math.round(Integer.parseInt(p[4])*factor)),dh=Math.max(1,Math.round(Integer.parseInt(p[5])*factor));
                        float fit=Math.min(1,Math.min(140F/dw,164F/dh));
                        add(new Picture(texture,u,v,w,h,tw,th,Math.round(dw*fit),Math.round(dh*fit)),result,current);
                    }catch(Exception e){thaumcraft.Thaumcraft.LOGGER.error("Invalid book illustration {}",matcher.group(1),e);addText(font,"[?]",result,current);}
                }
                previous=matcher.end();
            }
            addText(font,source.substring(previous),result,current);
            // Keep the original ResearchPage boundary, including explicit inline images.
            flush(result,current);
        }
        if(result.isEmpty())result.add(List.of());return result;
    }
    private static void addText(Font font,String text,List<List<Block>> pages,List<Block> current){
        text=text.replace("<BR/>","\n").replace("<BR>","\n").replaceAll("<[^>]*>","");
        if(!text.isEmpty())for(var line:font.split(Component.literal(text),140))add(new Text(line),pages,current);
    }
    private static void add(Block block,List<List<Block>> pages,List<Block> current){
        if(current.stream().mapToInt(Block::height).sum()+block.height()>170)flush(pages,current);
        current.add(block);
    }
    private static void flush(List<List<Block>> pages,List<Block> current){if(!current.isEmpty()){pages.add(List.copyOf(current));current.clear();}}
}

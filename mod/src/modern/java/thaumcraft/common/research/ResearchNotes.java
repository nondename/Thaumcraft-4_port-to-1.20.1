package thaumcraft.common.research;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.network.chat.Component;
import thaumcraft.api.aspects.*;
import thaumcraft.common.lib.network.ModNetwork;
import java.util.*;

/** A finite axial hex board; adjacent aspects connect only to their components. */
public final class ResearchNotes extends Item {
    public static final int[] ANCHORS={index(-3,0),index(3,0),index(0,-3),index(0,3),index(-3,3),index(3,-3)};
    public ResearchNotes(Properties properties) {super(properties);}
    public static int index(int q,int r) {return (r+3)*7+q+3;}
    public static boolean valid(int index) {
        int q=index%7-3,r=index/7-3;
        return index>=0 && index<49 && Math.abs(q+r)<=3;
    }
    public static ItemStack create(String key,ResearchProgression.Definition def) {
        var stack=new ItemStack(ModResearch.NOTES.get()); var tag=stack.getOrCreateTag(); tag.putString("Research",key);
        int i=0; for(Aspect a:def.aspects().getAspects()) {if(i>=6) break; tag.putString("Cell"+ANCHORS[i++],a.getTag());}
        if(i==1) {tag.putString("Cell"+ANCHORS[1],def.aspects().getAspects()[0].getTag());i=2;}
        tag.putIntArray("Anchors",Arrays.copyOf(ANCHORS,i));
        return stack;
    }
    public static Aspect cell(ItemStack stack,int index) {
        return stack.hasTag()?Aspect.getAspect(stack.getTag().getString("Cell"+index)):null;
    }
    public static boolean anchored(ResearchProgression.Definition def,int index) {
        for(int i=0;i<Math.min(6,Math.max(2,def.aspects().size()));i++) if(ANCHORS[i]==index) return true;
        return false;
    }
    public static boolean related(Aspect a,Aspect b) {
        if(a==null || b==null || a==b) return false;
        return contains(a,b) || contains(b,a);
    }
    private static boolean contains(Aspect a,Aspect b) {
        return a.getComponents()!=null && Arrays.asList(a.getComponents()).contains(b);
    }
    public static boolean complete(ItemStack stack,ResearchProgression.Definition def) {
        int count=Math.min(6,Math.max(2,def.aspects().size())); if(count<2) return false;
        int i=0; for(Aspect a:def.aspects().getAspects()) {if(i>=count)break; if(cell(stack,ANCHORS[i++])!=a) return false;}
        if(def.aspects().size()==1 && cell(stack,ANCHORS[1])!=def.aspects().getAspects()[0])return false;
        var visited=new HashSet<Integer>(); var queue=new ArrayDeque<Integer>(); queue.add(ANCHORS[0]);
        int[][] steps={{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};
        while(!queue.isEmpty()) {
            int current=queue.remove(); if(!visited.add(current))continue;
            int q=current%7-3,r=current/7-3;
            for(var step:steps) {int next=index(q+step[0],r+step[1]);
                if(Math.abs(q+step[0])>3 || Math.abs(r+step[1])>3 || !valid(next))continue;
                if(!visited.contains(next) && related(cell(stack,current),cell(stack,next))) queue.add(next);
            }
        }
        for(i=0;i<count;i++)if(!visited.contains(ANCHORS[i]))return false;
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide) {
            String key=stack.getOrCreateTag().getString("Research"); var def=ResearchProgression.definitions(level).get(key);
            var data=ResearchProgression.knowledge(player);
            if(def!=null && data!=null && !data.hasResearch(key) && ResearchProgression.parents(player,def) && complete(stack,def)) {
                ResearchProgression.finish(player,key); stack.shrink(1); ModNetwork.syncThaumometerKnowledge(player);
                player.displayClientMessage(Component.translatable("tc.progress.completed",Component.translatable("tc.research_name."+displayKey(key))),false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable("item.thaumcraft.research_notes").append(": ").append(Component.translatable("tc.research_name."+displayKey(stack.hasTag()?stack.getTag().getString("Research"):"?")));
    }
    public static String displayKey(String key) {return key.startsWith("CAP_") || key.startsWith("ROD_")?key.substring(0,4)+key.substring(4).toLowerCase(Locale.ROOT):key;}
    @Override public boolean isFoil(ItemStack stack) {return stack.hasTag() && stack.getTag().getBoolean("Solved");}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {lines.add(Component.translatable("tc.progress.notes_help"));}
}

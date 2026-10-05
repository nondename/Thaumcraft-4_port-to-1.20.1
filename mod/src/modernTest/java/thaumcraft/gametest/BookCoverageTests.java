package thaumcraft.gametest;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import thaumcraft.common.research.*;
import thaumcraft.api.aspects.*;
import java.util.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class BookCoverageTests {
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void everyShippedRecipeHasAnExistingChapter(GameTestHelper h) {
        var defs=ResearchProgression.definitions(h.getLevel());int count=0;
        for(var recipe:h.getLevel().getRecipeManager().getRecipes()) {
            if(!recipe.getId().getNamespace().equals("thaumcraft"))continue;
            count++;var chapters=BookRecipeCatalog.chapters(recipe);
            h.assertTrue(!chapters.isEmpty(),"Recipe missing from book: "+recipe.getId());
            for(String chapter:chapters)h.assertTrue(defs.containsKey(chapter.toUpperCase(Locale.ROOT)),"Missing chapter "+chapter);
        }
        h.assertTrue(count>=54,"Shipped recipe set was loaded");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void notesKeepEveryRequiredAspectAndProtectNewAnchors(GameTestHelper h) {
        var player=h.makeMockPlayer();var menu=new ResearchMenu(1,player.getInventory());
        menu.getSlot(0).set(new net.minecraft.world.item.ItemStack(ModResearch.SCRIBING_TOOLS.get()));
        for(var entry:ResearchProgression.definitions(h.getLevel()).entrySet()) {
            var def=entry.getValue();if(def.aspects().size()==0)continue;
            var note=ResearchNotes.create(entry.getKey(),def);var anchors=note.getTag().getIntArray("Anchors");
            h.assertTrue(anchors.length==Math.max(2,def.aspects().size()),"Full anchor count for "+entry.getKey());
            int i=0;for(var aspect:def.aspects().getAspects())h.assertTrue(ResearchNotes.cell(note,anchors[i++])==aspect,"Missing aspect "+aspect.getTag());
            if(anchors.length>6) {
                var data=ResearchProgression.knowledge(player);
                for(String parent:def.parents())data.grantResearch(parent);
                for(String parent:def.hiddenParents())data.grantResearch(parent);
                menu.getSlot(1).set(note);
                for(i=6;i<anchors.length;i++)h.assertTrue(!menu.clickMenuButton(player,2000+anchors[i]),"Additional anchor cannot be erased");
            }
        }
        h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void eightAnchorSolutionRequiresLastTwoAndSurvivesSave(GameTestHelper h) {
        var aspects=new AspectList().add(Aspect.VOID,1).add(Aspect.LIGHT,1).add(Aspect.AIR,1).add(Aspect.MOTION,1)
                .add(Aspect.WEATHER,1).add(Aspect.TREE,1).add(Aspect.AURA,1).add(Aspect.SENSES,1);
        var def=new ResearchProgression.Definition(List.of(),aspects,false,false,List.of(),List.of());
        var note=ResearchNotes.create("fixture",def);
        for(int i=0;i<49;i++)if(ResearchNotes.valid(i) && !ResearchNotes.anchored(def,i))
            note.getOrCreateTag().putString("Cell"+i,((i%7-3)%2==0?Aspect.AIR:Aspect.FLIGHT).getTag());
        h.assertTrue(ResearchNotes.complete(note,def),"Eight distinct anchors are connected");
        var restored=net.minecraft.world.item.ItemStack.of(note.save(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(ResearchNotes.complete(restored,def),"Solution persists in NBT");
        for(int i=6;i<8;i++) {
            var broken=note.copy();broken.getTag().remove("Cell"+ResearchNotes.ANCHORS[i]);
            broken.getTag().putBoolean("Solved",true);
            h.assertTrue(!ResearchNotes.complete(broken,def),"Solved flag cannot bypass missing seventh/eighth anchor");
        }
        h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void oldSixAnchorNotesUpgradeOnceWithoutLosingPaidPoints(GameTestHelper h) {
        var def=ResearchProgression.definitions(h.getLevel()).get("ROD_PRIMAL_STAFF");
        var note=ResearchNotes.create("ROD_PRIMAL_STAFF",def);var tag=note.getTag();
        tag.putIntArray("Anchors",Arrays.copyOf(ResearchNotes.ANCHORS,6));
        for(int i=6;i<def.aspects().size();i++)tag.remove("Cell"+ResearchNotes.ANCHORS[i]);
        tag.putString("Cell"+ResearchNotes.ANCHORS[6],Aspect.FIRE.getTag());
        var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);int before=data.getAspectPool(Aspect.FIRE);
        var menu=new ResearchMenu(1,player.getInventory());menu.getSlot(1).set(note);menu.broadcastChanges();
        h.assertTrue(tag.getIntArray("Anchors").length==8 && data.getAspectPool(Aspect.FIRE)==before+1,"Old note gains missing anchors and refunds displaced paid cell");
        h.assertTrue(!ResearchNotes.upgrade(note,def,player) && data.getAspectPool(Aspect.FIRE)==before+1,"Migration cannot refund twice");
        h.assertTrue(!ResearchNotes.complete(note,def),"Migration does not grant free completion");h.succeed();
    }
}

package thaumcraft.gametest;

import net.minecraft.gametest.framework.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumcraft.common.commands.*;
import thaumcraft.common.research.ResearchProgression;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledge;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class CommandsTests {
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void selectorCommandsAffectTargetOnly(GameTestHelper h) throws Exception {
        var server=h.getLevel().getServer();
        var target=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TCCommandTarget"));
        var other=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TCCommandOther"));
        var dispatcher=new com.mojang.brigadier.CommandDispatcher<CommandSourceStack>();AspectCommands.register(dispatcher);
        var source=server.createCommandSourceStack().withLevel(h.getLevel()).withPermission(2).withEntity(target);
        try {
            dispatcher.execute("tc research @s NITOR",source);
            dispatcher.execute("thaum aspect @s aqua 30",source);
            dispatcher.execute("thaumcraft warp @s set 12 TEMP",source);
            var data=ResearchProgression.knowledge(target);
            h.assertTrue(data.hasResearch("NITOR") && data.getAspectPool(Aspect.WATER)==30 && data.getTemporaryWarp()==12,"EntityArgument resolves and synchronizes the selected target");
            var untouched=ResearchProgression.knowledge(other);
            h.assertTrue(!untouched.hasResearch("NITOR") && untouched.getAspectPool(Aspect.WATER)==0 && untouched.getTemporaryWarp()==0,"Other player's knowledge is unchanged");
        } finally {target.discard();other.discard();}
        h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void originalSyntaxAliasesAndOperatorPermissions(GameTestHelper h) throws Exception {
        var dispatcher=new com.mojang.brigadier.CommandDispatcher<CommandSourceStack>();AspectCommands.register(dispatcher);
        var source=h.getLevel().getServer().createCommandSourceStack().withLevel(h.getLevel()).withPermission(2);
        h.assertTrue(dispatcher.execute("tc help",source)==1 && dispatcher.execute("thaum research list",source)>100,"Aliases execute original help/catalogue from console");
        h.assertTrue(dispatcher.getRoot().getChild("thaumcraft").getChild("debug")!=null,"Port diagnostic commands are separate");
        h.assertTrue(dispatcher.getRoot().getChild("thaumcraft").getChild("heldaspects")==null,"No diagnostic verbs in the original root");
        for(String command:new String[]{"thaumcraft help","tc help","thaum research list","thaumcraft debug aspects"}) {
            boolean rejected=false;try{dispatcher.execute(command,source.withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}
            h.assertTrue(rejected,"Permission level 0 cannot run "+command);
        }
        for(String command:new String[]{"thaumcraft research Tester NITOR","tc aspect Tester all 20","thaum warp Tester add -10 PERM","thaumcraft warp Tester set 20"}) {
            var parsed=dispatcher.parse(command,source);h.assertTrue(!parsed.getReader().canRead() && parsed.getExceptions().isEmpty(),"Original argument order parses: "+command);
        }
        boolean rejected=false;try{dispatcher.execute("tc warp Tester add 101",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}
        h.assertTrue(rejected,"Warp add rejects amounts outside original [-100,100]");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void researchPrerequisitesSiblingsAndReset(GameTestHelper h) throws Exception {
        var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);
        data.addAspectPool(Aspect.MAGIC,45);data.setWarp(7,8,9);
        ThaumcraftCommands.changeResearch(player,"distilessentia");
        h.assertTrue(data.hasResearch("DISTILESSENTIA") && data.hasResearch("NITOR") && data.hasResearch("ALUMENTUM") && data.hasResearch("JARLABEL"),"Grant recursively includes original parents and siblings");
        ThaumcraftCommands.changeResearch(player,"ROD_ice_staff");
        h.assertTrue(data.hasResearch("ROD_GREATWOOD_STAFF"),"Grant includes hidden prerequisites");
        int before=data.getResearchKeys().size();boolean rejected=false;
        try{ThaumcraftCommands.changeResearch(player,"DOES_NOT_EXIST");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}
        h.assertTrue(rejected && data.getResearchKeys().size()==before,"Invalid keys do not mutate knowledge");
        ThaumcraftCommands.changeResearch(player,"all");h.assertTrue(data.getResearchKeys().size()==ResearchProgression.definitions(h.getLevel()).size(),"All grants the registered research catalogue");
        int perm=data.getPermanentWarp(),normal=data.getNormalWarp();ThaumcraftCommands.changeResearch(player,"reset");
        for(var e:ResearchProgression.definitions(h.getLevel()).entrySet())h.assertTrue(data.hasResearch(e.getKey())==e.getValue().auto(),"Reset keeps only auto-unlocks");
        h.assertTrue(data.getAspectPool(Aspect.MAGIC)==45 && data.getPermanentWarp()==perm && data.getNormalWarp()==normal && data.getTemporaryWarp()==9,"Reset preserves aspect knowledge and warp");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void aspectAdministrationIsExactAndWarpTypesPersist(GameTestHelper h) throws Exception {
        var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);
        ThaumcraftCommands.giveAspects(player,"Praecantatio",20);h.assertTrue(data.getAspectPool(Aspect.MAGIC)==20,"First admin grant does not add scan discovery bonus");
        ThaumcraftCommands.giveAspects(player,"praecantatio",500);h.assertTrue(data.getAspectPool(Aspect.MAGIC)==520,"Admin grants are not reduced by gameplay diminishing returns");
        ThaumcraftCommands.giveAspects(player,"all",1);h.assertTrue(data.getDiscoveredAspects().size()==48,"All discovers every registered aspect");
        ThaumcraftCommands.changeWarp(player,"set",20000,"PERM");ThaumcraftCommands.changeWarp(player,"set",10,"");ThaumcraftCommands.changeWarp(player,"set",5,"temp");
        ThaumcraftCommands.changeWarp(player,"add",-20,"");ThaumcraftCommands.changeWarp(player,"add",-2,"TEMP");
        var loaded=new ThaumometerKnowledge();loaded.deserializeNBT(data.serializeNBT());
        h.assertTrue(loaded.getPermanentWarp()==20000 && loaded.getNormalWarp()==0 && loaded.getTemporaryWarp()==3,"Typed set/add including subtraction survives save/reload without artificial 10000 cap");
        h.assertTrue(loaded.getAspectPool(Aspect.MAGIC)==521,"Administrative aspect pools persist");h.succeed();
    }
}

package thaumcraft.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class ResearchAuditTests {
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void noteIssueConsumesLastInkButKeepsRefillableTools(GameTestHelper h) {
        var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);
        var def=ResearchProgression.definitions(h.getLevel()).get("NITOR");
        for(var a:def.aspects().getAspects())data.discoverAspect(a);
        player.getInventory().setItem(0,new ItemStack(ModItems.THAUMONOMICON.get()));
        player.getInventory().setItem(1,new ItemStack(Items.PAPER,2));
        var tools=new ItemStack(ModResearch.SCRIBING_TOOLS.get());tools.setDamageValue(99);
        tools.getOrCreateTag().putString("Owner","scribe");player.getInventory().setItem(2,tools);
        ResearchProgression.request(player,"NITOR");
        h.assertTrue(tools.getCount()==1 && tools.getDamageValue()==100 && player.getInventory().getItem(1).getCount()==1,"Last ink leaves an empty bottle and spends one paper");
        ResearchProgression.request(player,"NITOR");
        h.assertTrue(player.getInventory().getItem(1).getCount()==1,"Duplicate request cannot consume paper");
        var menu=new ResearchMenu(1,player.getInventory());var grid=new TransientCraftingContainer(menu,2,2);
        grid.setItem(0,tools.copy());grid.setItem(3,new ItemStack(Items.INK_SAC));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();
        var refilled=recipe.assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(refilled.is(ModResearch.SCRIBING_TOOLS.get()) && refilled.getDamageValue()==0 && refilled.getTag().getString("Owner").equals("scribe") && tools.getDamageValue()==100,"Refill preserves data without mutating the input");
        grid.setItem(1,new ItemStack(Items.DIRT));h.assertTrue(!recipe.matches(grid,h.getLevel()),"Refill rejects extra ingredients");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void emptyInkCannotEditOrSpendAspectPools(GameTestHelper h) {
        var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);data.addAspectPool(Aspect.FIRE,10);
        var def=ResearchProgression.definitions(h.getLevel()).get("NITOR");var notes=ResearchNotes.create("NITOR",def);
        var menu=new ResearchMenu(1,player.getInventory());var tools=new ItemStack(ModResearch.SCRIBING_TOOLS.get());tools.setDamageValue(99);
        menu.getSlot(0).set(tools);menu.getSlot(1).set(notes);
        int fire=java.util.Arrays.asList(ResearchMenu.ASPECTS).indexOf(Aspect.FIRE),cell=ResearchNotes.index(-2,0);
        menu.clickMenuButton(player,fire);int before=data.getAspectPool(Aspect.FIRE);
        h.assertTrue(menu.clickMenuButton(player,1000+cell),"Last ink places one aspect");
        h.assertTrue(tools.getCount()==1 && tools.getDamageValue()==100 && data.getAspectPool(Aspect.FIRE)==before-1,"Placement spends one point and one ink");
        h.assertTrue(!menu.clickMenuButton(player,2000+cell) && ResearchNotes.cell(notes,cell)==Aspect.FIRE,"Empty ink cannot erase");
        h.assertTrue(!menu.clickMenuButton(player,1000+ResearchNotes.index(-1,0)) && data.getAspectPool(Aspect.FIRE)==before-1,"Empty ink cannot spend another point");
        tools.setDamageValue(0);h.assertTrue(!menu.clickMenuButton(player,1000+ResearchNotes.index(-3,0)),"Anchor cannot be overwritten");
        h.assertTrue(menu.clickMenuButton(player,2000+cell) && tools.getDamageValue()==1 && data.getAspectPool(Aspect.FIRE)==before-1,"Erasure spends ink without refunding an aspect");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void hiddenParentsAndSiblingUnlocksApplyToGameplay(GameTestHelper h) {
        var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);var defs=ResearchProgression.definitions(h.getLevel());
        var def=defs.get("ROD_REED_STAFF");for(var parent:def.parents())data.grantResearch(parent);
        h.assertTrue(!ResearchProgression.parents(player,def),"Visible parent alone does not bypass hidden greatwood staff requirement");
        for(var parent:def.hiddenParents())data.grantResearch(parent);
        h.assertTrue(ResearchProgression.parents(player,def),"Both sets of requirements unlock the research");
        var labels=defs.get("JARLABEL");for(var parent:labels.parents())if(!parent.equals("DISTILESSENTIA"))data.grantResearch(parent);
        for(var parent:labels.hiddenParents())data.grantResearch(parent);
        ResearchProgression.finish(player,"DISTILESSENTIA");
        h.assertTrue(data.hasResearch("JARLABEL"),"Distillation also grants its eligible label sibling");
        var copy=new thaumcraft.common.lib.capabilities.ThaumometerKnowledge();copy.deserializeNBT(data.serializeNBT());
        h.assertTrue(copy.hasResearch("JARLABEL") && copy.hasResearch("DISTILESSENTIA"),"Linked discoveries persist together");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void boardPacketsKeepIdsAndRejectWrongOrDistantMenus(GameTestHelper h) {
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));var state=ModResearch.TABLE.get().defaultBlockState()
                .setValue(ResearchTableBlock.PART,1).setValue(ResearchTableBlock.FACING,net.minecraft.core.Direction.EAST);
        h.getLevel().setBlockAndUpdate(pos,state);h.getLevel().setBlockAndUpdate(pos.east(),state.setValue(ResearchTableBlock.PART,2));
        var player=h.makeMockPlayer();player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos));
        var menu=new ResearchMenu(7,player.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(),pos));player.containerMenu=menu;
        var data=ResearchProgression.knowledge(player);data.addAspectPool(Aspect.FIRE,10);
        menu.getSlot(0).set(new ItemStack(ModResearch.SCRIBING_TOOLS.get()));
        menu.getSlot(1).set(ResearchNotes.create("NITOR",ResearchProgression.definitions(h.getLevel()).get("NITOR")));
        menu.clickMenuButton(player,java.util.Arrays.asList(ResearchMenu.ASPECTS).indexOf(Aspect.FIRE));
        int cell=ResearchNotes.index(-2,0);
        for(int action:new int[]{1000+cell,2000+cell}) {
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var message=new thaumcraft.common.lib.network.message.ResearchActionMessage(7,action);
                thaumcraft.common.lib.network.message.ResearchActionMessage.encode(message,buffer);
                var decoded=thaumcraft.common.lib.network.message.ResearchActionMessage.decode(buffer);
                h.assertTrue(decoded.equals(message) && decoded.apply(player),"Real network encoding retains board ID and applies the action");
            } finally {buffer.release();}
        }
        int before=data.getAspectPool(Aspect.FIRE);
        h.assertTrue(!new thaumcraft.common.lib.network.message.ResearchActionMessage(8,1000+cell).apply(player),"Wrong container ID cannot edit the board");
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos.offset(20,0,0)));
        h.assertTrue(!new thaumcraft.common.lib.network.message.ResearchActionMessage(7,1000+cell).apply(player) && data.getAspectPool(Aspect.FIRE)==before,"Distant table cannot spend resources");h.succeed();
    }
}

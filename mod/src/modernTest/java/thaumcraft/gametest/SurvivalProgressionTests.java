package thaumcraft.gametest;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumcraft.common.alchemy.*;
import thaumcraft.common.blocks.*;
import thaumcraft.common.items.*;
import thaumcraft.common.items.tools.ItemWand;
import thaumcraft.common.items.wands.ModWandParts;
import thaumcraft.common.infusion.*;
import thaumcraft.common.research.*;
import thaumcraft.common.config.RecipeAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class SurvivalProgressionTests {
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void researchGateRejectsStaleWorkbenchPreview(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,ArcaneWorkbenchBlock.ARCANE_WORKBENCH.get().defaultBlockState());
        var table=(ArcaneWorkbenchBlockEntity)h.getLevel().getBlockEntity(pos);var player=h.makeMockPlayer();player.setPos(Vec3.atCenterOf(pos));
        var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);var wand=new ItemStack(ModItems.WAND.get());
        ItemWand.setCap(wand,ModWandParts.WAND_CAP_GOLD);for(var a:Aspect.getPrimalAspects())ItemWand.storeVis(wand,a,1000);
        table.setItem(10,wand);for(int i:new int[]{0,1,2,3,5})table.setItem(i,new ItemStack(Items.GOLD_NUGGET));
        h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"An affordable recipe stays locked without research");
        var data=ResearchProgression.knowledge(player);data.grantResearch("CAP_gold");menu.broadcastChanges();
        h.assertTrue(menu.getSlot(0).hasItem(),"Original research key unlocks the recipe");data.revokeResearch("CAP_GOLD");
        h.assertTrue(!menu.getSlot(0).mayPickup(player) && table.getItem(0).getCount()==1 && ItemWand.getVis(wand,Aspect.AIR)==1000,"Revocation invalidates the preview before payment");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void secondaryResearchIsAtomicAndNotRepeatable(GameTestHelper h) {
        var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);var def=ResearchProgression.definitions(h.getLevel()).get("ROD_OBSIDIAN");
        player.getInventory().add(new ItemStack(ModItems.THAUMONOMICON.get()));data.grantResearch("ROD_GREATWOOD");data.grantResearch("INFUSION");
        for(var a:def.aspects().getAspects()) {data.discoverAspect(a);data.awardAspect(a,20);}
        data.spendAspects(new AspectList().add(Aspect.EARTH,20));int before=data.getAspectPool(Aspect.TOOL);
        ResearchProgression.request(player,"ROD_OBSIDIAN");h.assertTrue(!data.hasResearch("ROD_OBSIDIAN") && data.getAspectPool(Aspect.TOOL)==before,"A missing pool must not partly pay a research");
        data.awardAspect(Aspect.EARTH,20);ResearchProgression.request(player,"ROD_OBSIDIAN");int after=data.getAspectPool(Aspect.TOOL);
        ResearchProgression.request(player,"ROD_OBSIDIAN");ResearchProgression.request(player,"made_up_key");
        h.assertTrue(data.hasResearch("ROD_OBSIDIAN") && after==before-3 && data.getAspectPool(Aspect.TOOL)==after,"Completion pays once and unknown keys do nothing");
        var clone=new thaumcraft.common.lib.capabilities.ThaumometerKnowledge();clone.deserializeNBT(data.serializeNBT());
        h.assertTrue(clone.hasResearch("rod_obsidian") && clone.getAspectPool(Aspect.TOOL)==after,"Progress and reduced pools survive a save");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void notesRequireRealAspectConnections(GameTestHelper h) {
        var def=ResearchProgression.definitions(h.getLevel()).get("NITOR");var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);
        var note=ResearchNotes.create("NITOR",def);note.getOrCreateTag().putBoolean("Completed",true);
        h.assertTrue(!ResearchNotes.complete(note,def),"A client completion flag does not solve the board");
        int[][] path={{-2,0},{-1,0},{0,0},{1,0},{2,0},{2,1}};
        for(int i=0;i<path.length;i++)note.getOrCreateTag().putString("Cell"+ResearchNotes.index(path[i][0],path[i][1]),(i%2==0?Aspect.FIRE:Aspect.LIGHT).getTag());
        h.assertTrue(ResearchNotes.complete(note,def),"Direct component links connect both required anchors");
        player.setItemInHand(InteractionHand.MAIN_HAND,note);ModResearch.NOTES.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(data.hasResearch("NITOR") && note.isEmpty(),"Server completion consumes a finished note");
        var dust=RecipeAspects.get(new ItemStack(ModAlchemy.SALIS_MUNDUS.get()),h.getLevel());
        h.assertTrue(dust.getAmount(Aspect.MAGIC)==2 && dust.getAmount(Aspect.CRYSTAL)==0,"Salis keeps the original aspect assignment");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void crucibleResearchAndNitorHeat(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));h.getLevel().setBlockAndUpdate(pos.below(),ModAlchemy.NITOR.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,ModAlchemy.CRUCIBLE.get().defaultBlockState());var c=(CrucibleEntity)h.getLevel().getBlockEntity(pos);c.fillWater();
        for(int i=0;i<151;i++)CrucibleEntity.tick(h.getLevel(),pos,c.getBlockState(),c);h.assertTrue(c.isBoiling(),"Nitor heats the crucible");
        var saved=c.saveWithoutMetadata();new AspectList().add(Aspect.ENERGY,3).add(Aspect.FIRE,3).add(Aspect.LIGHT,3).writeToNBT(saved);c.load(saved);
        var player=h.makeMockPlayer();var item=new ItemEntity(h.getLevel(),pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.GLOWSTONE_DUST));
        c.process(item,player);h.assertTrue(c.getWater()==1000,"Unknown research cannot run the nitor recipe");
        c.load(saved);ResearchProgression.knowledge(player).grantResearch("NITOR");c.process(new ItemEntity(h.getLevel(),pos.getX(),pos.getY(),pos.getZ(),new ItemStack(Items.GLOWSTONE_DUST)),player);
        h.assertTrue(c.getWater()==950 && c.getAspects().visSize()==0,"Known research pays the original 3/3/3 aspects and one water charge");
        h.assertTrue(net.minecraftforge.common.ForgeHooks.getBurnTime(new ItemStack(ModAlchemy.ALUMENTUM.get()),null)==6400,"Alumentum is fuel for 32 smelts");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void distillationPreservesAllInputAspects(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,ModInfusion.FURNACE.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.above(),ModInfusion.ALEMBIC.get().defaultBlockState());var f=(AlchemyFurnaceEntity)h.getLevel().getBlockEntity(pos);
        f.setItem(0,new ItemStack(Items.ENDER_PEARL));f.setItem(1,new ItemStack(ModAlchemy.ALUMENTUM.get()));
        for(int i=0;i<100;i++)AlchemyFurnaceEntity.tick(h.getLevel(),pos,f.getBlockState(),f);
        var a=(EssentiaStoreEntity)h.getLevel().getBlockEntity(pos.above());
        h.assertTrue(f.getItem(0).isEmpty() && f.getItem(1).isEmpty() && f.buffer().visSize()+a.amount()==10,"Distillation spends one input and one fuel, preserves the ten pearl aspects");
        var saved=f.saveWithoutMetadata();f.load(saved);h.assertTrue(f.buffer().visSize()+a.amount()==10 && f.burning(),"Buffer and remaining burn time survive reload");
        var thaumium=RecipeAspects.get(new ItemStack(ModMetals.THAUMIUM_INGOT.get()),h.getLevel());
        h.assertTrue(thaumium!=null && thaumium.getAmount(Aspect.METAL)==4 && thaumium.getAmount(Aspect.MAGIC)==2,"Crucible aspect derivation includes catalyst plus sqrt cost");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void phialsTransferExactlyEightWithoutMixing(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,ModInfusion.JAR.get().defaultBlockState());
        var jar=(EssentiaStoreEntity)h.getLevel().getBlockEntity(pos);jar.add(Aspect.AIR,16);var player=h.makeMockPlayer();player.getAbilities().instabuild=false;
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModResearch.PHIAL.get()));var block=(InfusionDeviceBlock)ModInfusion.JAR.get();
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);block.use(jar.getBlockState(),h.getLevel(),pos,player,InteractionHand.OFF_HAND,hit);
        ItemStack filled=ItemStack.EMPTY;for(var stack:player.getInventory().items)if(stack.is(ModResearch.PHIAL.get()) && stack.hasTag())filled=stack;
        h.assertTrue(jar.amount()==8 && RecipeAspects.get(filled,h.getLevel()).getAmount(Aspect.AIR)==8,"One phial removes and stores exactly eight");
        var bad=new ItemStack(ModResearch.PHIAL.get());new AspectList().add(Aspect.WATER,8).writeToNBT(bad.getOrCreateTag());player.setItemInHand(InteractionHand.OFF_HAND,bad);
        block.use(jar.getBlockState(),h.getLevel(),pos,player,InteractionHand.OFF_HAND,hit);h.assertTrue(jar.amount()==8 && !bad.isEmpty(),"A different aspect cannot mix or consume the phial");
        player.setItemInHand(InteractionHand.OFF_HAND,filled);block.use(jar.getBlockState(),h.getLevel(),pos,player,InteractionHand.OFF_HAND,hit);
        h.assertTrue(jar.amount()==16 && player.getOffhandItem().isEmpty(),"Depositing returns all eight and consumes exactly one full phial");
        var saved=jar.saveWithoutMetadata();jar.take(Aspect.AIR,16);jar.load(saved);h.assertTrue(jar.aspect()==Aspect.AIR && jar.amount()==16,"Jar payload survives NBT");
        h.getLevel().destroyBlock(pos,true);
        var dropped=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),e -> e.getItem().is(ModInfusion.JAR_ITEM.get()));
        h.assertTrue(dropped.size()==1 && dropped.get(0).getItem().getOrCreateTag().getCompound("BlockEntityTag").getInt("Amount")==16,"The real block loot preserves jar contents once");h.succeed();
    }
    private static InfusionMatrixEntity altar(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,3,2));level.setBlockAndUpdate(pos,ModInfusion.MATRIX.get().defaultBlockState());
        level.setBlockAndUpdate(pos.below(2),ModInfusion.PEDESTAL.get().defaultBlockState());
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})for(int y:new int[]{-1,-2})level.setBlockAndUpdate(pos.offset(x,y,z),ModInfusion.ARCANE_STONE.get().defaultBlockState());
        ((PedestalEntity)level.getBlockEntity(pos.below(2))).set(new ItemStack(ModWandParts.CAP_THAUMIUM_INERT.get()));
        for(var p:new BlockPos[]{pos.offset(-2,-2,0),pos.offset(2,-2,0),pos.offset(0,-2,2)}) {
            level.setBlockAndUpdate(p,ModInfusion.PEDESTAL.get().defaultBlockState());((PedestalEntity)level.getBlockEntity(p)).set(new ItemStack(ModAlchemy.SALIS_MUNDUS.get()));
        }
        for(int i=0;i<2;i++) {var p=pos.offset(i==0?-2:2,-2,-2);level.setBlockAndUpdate(p,ModInfusion.JAR.get().defaultBlockState());((EssentiaStoreEntity)level.getBlockEntity(p)).add(i==0?Aspect.ENERGY:Aspect.AURA,i==0?12:6);}
        return (InfusionMatrixEntity)level.getBlockEntity(pos);
    }
    private static Player ritualPlayer(GameTestHelper h) {var player=h.makeMockPlayer();var data=ResearchProgression.knowledge(player);data.grantResearch("INFUSION");data.grantResearch("CAP_THAUMIUM");return player;}
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void infusionPaysOnceAcrossSaveAndOwnerChecks(GameTestHelper h) {
        var matrix=altar(h);var player=ritualPlayer(h);h.assertTrue(matrix.start(player),"A researched, funded altar starts");matrix.advance(player);
        var tag=matrix.saveWithoutMetadata();matrix.load(tag);var stranger=ritualPlayer(h);matrix.advance(stranger);
        h.assertTrue(matrix.saveWithoutMetadata().get("Remaining").equals(tag.get("Remaining")),"A foreign player cannot spend someone else's ritual essentia");
        for(int i=0;i<30;i++)matrix.advance(player);var centre=(PedestalEntity)h.getLevel().getBlockEntity(matrix.getBlockPos().below(2));
        h.assertTrue(!matrix.active() && centre.item().is(ModWandParts.CAP_THAUMIUM.get()) && centre.item().getCount()==1,"Saved ritual produces exactly one activated cap");
        for(int i=0;i<2;i++) {var p=matrix.getBlockPos().offset(i==0?-2:2,-2,-2);h.assertTrue(((EssentiaStoreEntity)h.getLevel().getBlockEntity(p)).amount()==0,"Full original essentia cost was consumed");}
        h.assertTrue(!matrix.start(player),"Output cannot repeat with consumed components");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void interruptedInfusionCannotYieldFreeResult(GameTestHelper h) {
        var matrix=altar(h);var player=ritualPlayer(h);var centre=(PedestalEntity)h.getLevel().getBlockEntity(matrix.getBlockPos().below(2));
        var data=ResearchProgression.knowledge(player);data.revokeResearch("CAP_THAUMIUM");h.assertTrue(!matrix.start(player),"Part research is required in addition to INFUSION");data.grantResearch("CAP_THAUMIUM");
        h.assertTrue(matrix.start(player),"Valid ritual starts");matrix.advance(player);centre.set(ItemStack.EMPTY);matrix.advance(player);
        h.assertTrue(!matrix.active() && centre.item().isEmpty(),"Removing the centre cancels the ritual without conjuring an output");h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void seniorRecipesAndAspectFallbackStayReachable(GameTestHelper h) {
        for(var part:java.util.List.of(ModWandParts.CAP_THAUMIUM_INERT,ModWandParts.CAP_VOID_INERT,ModWandParts.CAP_THAUMIUM,ModWandParts.CAP_VOID,
                ModWandParts.ROD_GREATWOOD,ModWandParts.ROD_SILVERWOOD,ModWandParts.ROD_OBSIDIAN,ModWandParts.ROD_ICE,ModWandParts.ROD_QUARTZ,
                ModWandParts.ROD_REED,ModWandParts.ROD_BLAZE,ModWandParts.ROD_BONE)) {
            var aspects=RecipeAspects.get(new ItemStack(part.get()),h.getLevel());h.assertTrue(aspects!=null && aspects.visSize()>0,"Every native senior part has derived aspects: "+part.getId());
        }
        var shard=RecipeAspects.get(new ItemStack(ModAlchemy.BALANCED_SHARD.get()),h.getLevel());h.assertTrue(shard.visSize()==13,"Balanced shard has six primals x2 and crystal x1");
        var wand=new ItemStack(ModItems.WAND.get());var initial=RecipeAspects.get(wand,h.getLevel());ItemWand.setRod(wand,ModWandParts.WAND_ROD_SILVERWOOD);ItemWand.setCap(wand,ModWandParts.WAND_CAP_VOID);
        var senior=RecipeAspects.get(wand,h.getLevel());h.assertTrue(initial.getAmount(Aspect.MAGIC)==1 && senior.getAmount(Aspect.MAGIC)==9 && senior.getAmount(Aspect.METAL)==4 && senior.getAmount(Aspect.TOOL)==6,"NBT wand aspects retain the base tag and never share cached bonuses");
        var inert=h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("thaumcraft","wand_cap_void_inert")).orElseThrow();
        h.assertTrue(inert.getIngredients().stream().filter(i -> !i.isEmpty()).count()==5 && inert.getIngredients().stream().filter(i -> !i.isEmpty()).allMatch(i -> i.test(new ItemStack(ModMetals.VOID_NUGGET.get()))),"Void cap uses five real void nuggets");
        h.succeed();
    }
}

package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumcraft.common.alchemy.*;
import thaumcraft.common.research.ResearchProgression;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AlchemyMechanicsTests {
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void alumentumThrowsOneAndCreativeKeepsStack(GameTestHelper h) {
        var player=h.makeMockPlayer();var p=h.absolutePos(new BlockPos(2,2,2));player.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);
        player.getAbilities().instabuild=false;var stack=new ItemStack(ModAlchemy.ALUMENTUM.get(),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        ModAlchemy.ALUMENTUM.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(stack.getCount()==2,"A throw consumes exactly one alumentum");
        var area=new AABB(p).inflate(2);var projectiles=h.getLevel().getEntitiesOfClass(AlumentumProjectile.class,area);
        h.assertTrue(projectiles.size()==1 && projectiles.get(0).getOwner()==player,"Server spawns one projectile with the thrower's owner");
        h.assertTrue(projectiles.get(0).getDeltaMovement().length()>.6 && projectiles.get(0).getDeltaMovement().length()<.9,"Original throw speed is 0.75");
        player.getAbilities().instabuild=true;ModAlchemy.ALUMENTUM.get().use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(stack.getCount()==2 && h.getLevel().getEntitiesOfClass(AlumentumProjectile.class,area).size()==2,"Creative throws without consumption");
        h.getLevel().getEntitiesOfClass(AlumentumProjectile.class,area).forEach(AlumentumProjectile::discard);h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void alumentumImpactHonorsMobGriefing(GameTestHelper h) {
        var rule=h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);boolean previous=rule.get();
        var p=h.absolutePos(new BlockPos(3,2,3));var owner=h.makeMockPlayer();owner.setPos(p.getX()-3,p.getY(),p.getZ()-3);
        try {
            for(boolean griefing:new boolean[]{false,true}) {
                rule.set(griefing,h.getLevel().getServer());h.getLevel().setBlockAndUpdate(p,Blocks.WHITE_WOOL.defaultBlockState());
                var projectile=new AlumentumProjectile(h.getLevel(),owner);projectile.setPos(p.getX()+.5,p.getY()+.5,p.getZ()-.5);
                projectile.setDeltaMovement(0,0,1);h.getLevel().addFreshEntity(projectile);projectile.tick();
                h.assertTrue(projectile.isRemoved(),"Impact explodes and removes the projectile");
                h.assertTrue(h.getLevel().getBlockState(p).is(Blocks.WHITE_WOOL)!=griefing,"mobGriefing controls block destruction");
            }
        } finally {rule.set(previous,h.getLevel().getServer());}
        h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void alumentumCoalAndCharcoalNeedResearchAndPayCosts(GameTestHelper h) {
        var p=h.absolutePos(new BlockPos(1,2,1));h.getLevel().setBlockAndUpdate(p.below(),ModAlchemy.NITOR.get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(p,ModAlchemy.CRUCIBLE.get().defaultBlockState());var c=(CrucibleEntity)h.getLevel().getBlockEntity(p);
        c.fillWater();for(int i=0;i<151;i++)CrucibleEntity.tick(h.getLevel(),p,c.getBlockState(),c);
        var saved=c.saveWithoutMetadata();new AspectList().add(Aspect.ENERGY,3).add(Aspect.FIRE,3).add(Aspect.ENTROPY,3).writeToNBT(saved);
        var player=h.makeMockPlayer();c.load(saved);
        c.process(new ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),new ItemStack(Items.COAL)),player);
        h.assertTrue(c.getWater()==1000,"No researched alumentum output before ALUMENTUM knowledge");
        ResearchProgression.knowledge(player).grantResearch("ALUMENTUM");
        for(Item catalyst:new Item[]{Items.COAL,Items.CHARCOAL}) {
            c.load(saved);var input=new ItemEntity(h.getLevel(),p.getX(),p.getY(),p.getZ(),new ItemStack(catalyst));c.process(input,player);
            h.assertTrue(c.getWater()==950 && c.getAspects().visSize()==0 && input.isRemoved(),"One catalyst, 3/3/3 aspects and 50 water per output");
        }
        h.succeed();
    }
}

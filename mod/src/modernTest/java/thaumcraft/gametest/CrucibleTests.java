package thaumcraft.gametest;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import thaumcraft.api.aspects.*;
import thaumcraft.common.alchemy.*;
import thaumcraft.common.items.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CrucibleTests {
    private static CrucibleEntity prepare(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.LAVA.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, ModAlchemy.CRUCIBLE.get().defaultBlockState());
        return (CrucibleEntity) helper.getLevel().getBlockEntity(pos);
    }
    private static ItemEntity input(GameTestHelper h, CrucibleEntity c, ItemStack stack) {
        BlockPos p = c.getBlockPos();
        var item = new ItemEntity(h.getLevel(), p.getX()+.5, p.getY()+.6, p.getZ()+.5, stack);
        h.getLevel().addFreshEntity(item); return item;
    }
    private static void warm(GameTestHelper helper, CrucibleEntity c) {
        for (int i=0;i<151;i++) CrucibleEntity.tick(helper.getLevel(), c.getBlockPos(), c.getBlockState(), c);
    }
    @GameTest(templateNamespace="thaumcraft", template="empty")
    public static void conversionAndSurvivalBucket(GameTestHelper h) {
        var level=h.getLevel(); var player=h.makeMockPlayer(); var pos=h.absolutePos(new BlockPos(1,2,1));
        level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        player.getAbilities().instabuild=false;
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.WAND.get()));
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        ModItems.WAND.get().useOn(new UseOnContext(player,InteractionHand.OFF_HAND,hit));
        h.assertTrue(level.getBlockEntity(pos) instanceof CrucibleEntity,"Wand must convert cauldron");
        var c=(CrucibleEntity)level.getBlockEntity(pos);
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.WATER_BUCKET));
        ((CrucibleBlock)ModAlchemy.CRUCIBLE.get()).use(c.getBlockState(),level,pos,player,InteractionHand.OFF_HAND,hit);
        h.assertTrue(c.getWater()==1000 && player.getOffhandItem().is(Items.BUCKET),"Water fill must return empty bucket in survival");
        var saved=c.saveWithoutMetadata(); c.empty(); c.load(saved);
        h.assertTrue(c.getWater()==1000,"Water survives NBT reload");
        player.setShiftKeyDown(true); player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.WAND.get()));
        ModItems.WAND.get().useOn(new UseOnContext(player,InteractionHand.OFF_HAND,hit));
        h.assertTrue(c.getWater()==0,"Sneaking wand item path must empty crucible even when vanilla skips block use");
        h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft", template="empty")
    public static void heatAndThaumiumStackAccounting(GameTestHelper h) {
        var c=prepare(h); c.fillWater();
        var donor=input(h,c,new ItemStack(Items.ENDER_PEARL,2));
        c.process(donor); h.assertTrue(donor.getItem().getCount()==2,"Cold water must not consume items");
        warm(h,c); h.assertTrue(c.isBoiling(),"Water must boil after 151 heated ticks");
        c.process(donor);
        h.assertTrue(!donor.isAlive() && c.getAspects().getAmount(Aspect.MAGIC)==4,"Two pearls must supply four magic");
        var iron=input(h,c,new ItemStack(Items.IRON_INGOT,2)); c.process(iron);
        h.assertTrue(!iron.isAlive() && c.getWater()==950 && c.getAspects().getAmount(Aspect.MAGIC)==0,
                "Only one iron can craft with four magic, remaining iron dissolves; water cost fifty");
        var outputs=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(c.getBlockPos()).inflate(2),
                i->i.getItem().is(ModMetals.THAUMIUM_INGOT.get()));
        h.assertTrue(outputs.size()==1 && outputs.get(0).getItem().getCount()==1,"Exactly one thaumium output");
        c.process(outputs.get(0)); h.assertTrue(outputs.get(0).isAlive(),"Crafted output must not dissolve again");
        var tag=c.saveWithoutMetadata(); c.empty(); c.load(tag);
        h.assertTrue(c.getWater()==950 && c.getHeat()>150 && c.getAspects().getAmount(Aspect.METAL)>0,
                "Heat, water and excess aspects must survive reload");
        h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft", template="empty")
    public static void dryAndUnknownItemsStayIntact(GameTestHelper h) {
        var c=prepare(h); warm(h,c); h.assertTrue(c.getHeat()==0,"Dry crucible must not heat");
        var iron=input(h,c,new ItemStack(Items.IRON_INGOT)); c.process(iron);
        h.assertTrue(iron.isAlive(),"Dry crucible must not consume catalyst"); iron.discard();
        c.fillWater(); warm(h,c);
        var unknown=input(h,c,new ItemStack(Items.BARRIER)); c.process(unknown);
        h.assertTrue(unknown.isAlive() && unknown.getItem().getCount()==1,"Unrecognized item must be rejected intact");
        c.empty(); h.assertTrue(c.getWater()==0 && c.getAspects().size()==0,"Emptying must clear water and aspects");
        h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft", template="empty")
    public static void voidMetalChainAndEssencePayload(GameTestHelper h) {
        var c=prepare(h); c.fillWater(); warm(h,c);
        var essence=input(h,c,ItemWispEssence.create(Aspect.MAGIC,2)); c.process(essence);
        h.assertTrue(c.getAspects().getAmount(Aspect.MAGIC)==2,"Ethereal essence must supply its NBT aspect payload");
        var data=c.saveWithoutMetadata();
        new AspectList().add(Aspect.DARKNESS,8).add(Aspect.VOID,8).add(Aspect.ELDRITCH,2).writeToNBT(data);
        c.load(data);
        c.process(input(h,c,new ItemStack(Items.ENDER_PEARL)));
        var seeds=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(c.getBlockPos()).inflate(2),
                i->i.getItem().is(ModAlchemy.VOID_SEED.get()));
        h.assertTrue(seeds.size()==1 && c.getWater()==950 && c.getAspects().size()==0,"Void seed must use exact compound cost and fifty water");
        c.process(input(h,c,new ItemStack(Items.IRON_INGOT,3)));
        c.process(input(h,c,seeds.get(0).getItem().copy())); seeds.get(0).discard();
        var ingots=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(c.getBlockPos()).inflate(2),
                i->i.getItem().is(ModMetals.VOID_INGOT.get()));
        h.assertTrue(ingots.size()==1 && c.getWater()==900 && c.getAspects().getAmount(Aspect.METAL)==4,
                "Void seed and eight metal must make one void ingot, keeping surplus metal");
        h.succeed();
    }
}

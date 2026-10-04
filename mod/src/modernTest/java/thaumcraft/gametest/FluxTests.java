package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.alchemy.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class FluxTests {
    private static int units(GameTestHelper h, BlockPos center) {
        int total=0;
        for (BlockPos pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))) {
            var state=h.getLevel().getBlockState(pos);
            if (state.getBlock() instanceof FluxBlock) total+=state.getValue(FluxBlock.AMOUNT);
        }
        return total;
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void cleanupSpillsOnceAndPreservesSolidBlocks(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(1,2,1));
        level.setBlockAndUpdate(pos,ModAlchemy.CRUCIBLE.get().defaultBlockState());
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        var c=(CrucibleEntity)level.getBlockEntity(pos); c.fillWater();
        c.empty(); h.assertTrue(units(h,pos)==0,"Clean water must not generate flux");
        var tag=c.saveWithoutMetadata(); new AspectList().add(Aspect.MAGIC,1).writeToNBT(tag); c.load(tag);
        c.empty(); h.assertTrue(units(h,pos)==0,"One remaining unit gives zero spill attempts");
        tag=c.saveWithoutMetadata(); new AspectList().add(Aspect.MAGIC,128).writeToNBT(tag); c.load(tag);
        level.random.setSeed(1234); c.empty(); int total=units(h,pos);
        h.assertTrue(total>0 && total<=64,"Cleanup must create finite flux from remaining aspects");
        h.assertTrue(c.getWater()==0 && c.getAspects().visSize()==0,"Cleanup clears contents");
        c.empty(); h.assertTrue(units(h,pos)==total,"Repeated emptying must not emit twice");
        h.assertTrue(level.getBlockState(pos.below()).is(Blocks.STONE),"Heat source/terrain must survive spill");
        h.succeed();
    }
    @GameTest(templateNamespace="thaumcraft",template="empty")
    public static void blockedSpillAndFiniteVerticalMovement(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(1,2,1));
        for (BlockPos p:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))
            level.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
        for (int i=0;i<128;i++) FluxSpill.attempt(level,pos);
        h.assertTrue(units(h,pos)==0,"A sealed area must not be replaced by flux");
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        var gas=(FluxBlock)ModAlchemy.FLUX_GAS.get();
        var state=gas.defaultBlockState().setValue(FluxBlock.AMOUNT,5);
        level.setBlockAndUpdate(pos,state); gas.tick(state,level,pos,level.random);
        h.assertTrue(level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).is(gas)
                && level.getBlockState(pos.above()).getValue(FluxBlock.AMOUNT)==5,"Gas rises without multiplying volume");
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
        var goo=(FluxBlock)ModAlchemy.FLUX_GOO.get();
        state=goo.defaultBlockState().setValue(FluxBlock.AMOUNT,6);
        level.setBlockAndUpdate(pos,state); goo.tick(state,level,pos,level.random);
        h.assertTrue(level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(goo)
                && level.getBlockState(pos.below()).getValue(FluxBlock.AMOUNT)==6,"Goo falls without creating infinite sources");
        h.succeed();
    }
}

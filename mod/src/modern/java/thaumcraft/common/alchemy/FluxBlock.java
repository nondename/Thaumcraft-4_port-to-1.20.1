package thaumcraft.common.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Finite pollution: gas rises, goo falls and levels out; transfer conserves units. */
public final class FluxBlock extends Block {
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, 8);
    private final boolean gas;
    public FluxBlock(boolean gas, Properties properties) {
        super(properties); this.gas = gas; registerDefaultState(stateDefinition.any().setValue(AMOUNT, 1));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AMOUNT); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return box(0,0,0,16,gas ? 16 : state.getValue(AMOUNT)*2,16);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos,this,20);
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos).is(this)) return;
        BlockPos vertical = gas ? pos.above() : pos.below();
        transfer(level,pos,vertical,8);
        if (!gas && level.getBlockState(pos).is(this)) {
            Direction[] horizontal = {Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
            for (int i=0;i<4;i++) transfer(level,pos,pos.relative(horizontal[(i+(int)(level.getGameTime()%4))%4]),1);
        }
        if (level.getBlockState(pos).is(this)) level.scheduleTick(pos,this,20);
    }
    private void transfer(ServerLevel level, BlockPos from, BlockPos to, int limit) {
        if (!level.isLoaded(to) || level.isOutsideBuildHeight(to)) return;
        var source=level.getBlockState(from); if (!source.is(this)) return;
        var target=level.getBlockState(to);
        if (!target.isAir() && !target.is(this)) return;
        int a=source.getValue(AMOUNT), b=target.is(this)?target.getValue(AMOUNT):0;
        boolean vertical=from.getY()!=to.getY();
        int move=vertical?Math.min(a,8-b):Math.max(0,(a-b)/2);
        move=Math.min(limit,move); if (move<=0) return;
        level.setBlockAndUpdate(to,defaultBlockState().setValue(AMOUNT,b+move));
        level.setBlockAndUpdate(from,a==move?Blocks.AIR.defaultBlockState():source.setValue(AMOUNT,a-move));
        level.scheduleTick(to,this,20);
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Slow dissipation; never creates infinite fluid source blocks.
        if (random.nextInt(30)==0) {
            int amount=state.getValue(AMOUNT);
            level.setBlockAndUpdate(pos,amount==1?Blocks.AIR.defaultBlockState():state.setValue(AMOUNT,amount-1));
        }
    }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!gas && entity.getY() >= pos.getY()+state.getValue(AMOUNT)/8D) return;
        if (!gas) entity.makeStuckInBlock(state,new Vec3(.5,1,.5));
        if (!level.isClientSide && gas && entity instanceof LivingEntity living && living.tickCount%10==0
                && !living.isInvertedHealAndHarm() && !living.hasEffect(MobEffects.CONFUSION)) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION,80+(state.getValue(AMOUNT)-1)*20));
            int amount=state.getValue(AMOUNT);
            level.setBlockAndUpdate(pos,amount==1?Blocks.AIR.defaultBlockState():state.setValue(AMOUNT,amount-1));
        }
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3)==0) level.addParticle(ParticleTypes.WITCH,
                pos.getX()+random.nextDouble(),pos.getY()+(gas?random.nextDouble():state.getValue(AMOUNT)/8D),
                pos.getZ()+random.nextDouble(),0,.02,0);
    }
}

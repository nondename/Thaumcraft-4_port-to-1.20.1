package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** TC4 Shimmerleaf (BlockCustomPlant metadata 2). */
public final class ShimmerleafBlock extends BushBlock {
    public ShimmerleafBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.DANDELION)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .lightLevel(state -> 8));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SAND) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(3) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                    pos.getY() + 0.2D + random.nextDouble() * 0.6D,
                    pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                    0.0D, 0.01D, 0.0D);
        }
    }
}

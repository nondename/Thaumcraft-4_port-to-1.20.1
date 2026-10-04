package thaumcraft.common.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floating magical flame: no fuel use, no solid collision, constant light/heat. */
public final class NitorBlock extends Block {
    public NitorBlock(Properties properties) { super(properties); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return box(5,5,5,11,11,11);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.FLAME,pos.getX()+.5+(random.nextDouble()-.5)*.15,
                pos.getY()+.5,pos.getZ()+.5+(random.nextDouble()-.5)*.15,0,.005,0);
    }
}

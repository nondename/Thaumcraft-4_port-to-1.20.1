package thaumcraft.common.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Placement/chance follows the saved TileCrucible.spill implementation. */
public final class FluxSpill {
    public static void attempt(ServerLevel level, BlockPos crucible) {
        if (level.random.nextInt(4) != 0) return;
        BlockPos target = crucible.above();
        var state = level.getBlockState(target);
        if (state.getBlock() instanceof FluxBlock && state.getValue(FluxBlock.AMOUNT) < 8) {
            level.setBlockAndUpdate(target, state.setValue(FluxBlock.AMOUNT, state.getValue(FluxBlock.AMOUNT) + 1));
            return;
        }
        if (!state.isAir()) target = crucible.offset(level.random.nextInt(3)-1,
                level.random.nextInt(3)-1, level.random.nextInt(3)-1);
        // Never overwrite a lid, heat source, container, water, or other terrain.
        if (!level.isLoaded(target) || !level.getBlockState(target).isAir()) return;
        var block = level.random.nextBoolean() ? ModAlchemy.FLUX_GAS.get() : ModAlchemy.FLUX_GOO.get();
        level.setBlockAndUpdate(target, block.defaultBlockState());
    }
    private FluxSpill() {}
}

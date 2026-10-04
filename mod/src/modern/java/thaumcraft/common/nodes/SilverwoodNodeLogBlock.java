package thaumcraft.common.nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.ItemWispEssence;

/**
 * TC4 BlockMagicalLog metadata 2 (silverwood knot): visually a log, internally a real aura node.
 */
public final class SilverwoodNodeLogBlock extends RotatedPillarBlock implements EntityBlock {
    public SilverwoodNodeLogBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuraNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModNodes.AURA_NODE_ENTITY.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<AuraNodeBlockEntity>) AuraNodeBlockEntity::serverTick;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && !isMoving && level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
                for (Aspect aspect : node.getAspects().getAspects()) {
                    if (aspect == null) continue;
                    int amount = node.getAspects().getAmount(aspect);
                    if (amount < 5) continue;
                    int drops = amount / 10 + 1;
                    for (int i = 0; i < drops; i++) {
                        Containers.dropItemStack(level,
                                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                                ItemWispEssence.create(aspect, 2));
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}

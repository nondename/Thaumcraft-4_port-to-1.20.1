package thaumcraft.common.nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.world.AuraNodeGenerator;

/**
 * Invisible carrier block for a TC4 aura node.
 *
 * <p>The small outline shape is intentional: nodes have no collision, but the thaumometer must
 * still be able to ray-pick them exactly like the old airy node block.</p>
 */
public final class AuraNodeBlock extends BaseEntityBlock {
    private static final VoxelShape SCAN_SHAPE = box(3.0D, 3.0D, 3.0D, 13.0D, 13.0D, 13.0D);

    public AuraNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuraNodeBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SCAN_SHAPE;
    }

    /**
     * Original TC4 BlockAiry metadata 0 calls ThaumcraftWorldGenerator.createRandomNodeAt when a
     * player places the single creative Aura Node item. Worldgen uses setBlock directly and is
     * therefore unaffected by this placement hook.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)
                || !(placer instanceof Player)
                || !stack.is(ModNodes.AURA_NODE_ITEM.get())) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
            var generated = AuraNodeGenerator.generate(serverLevel, pos, level.random);
            node.initialize(generated.type(), generated.modifier(), generated.aspects());
        }
    }
}

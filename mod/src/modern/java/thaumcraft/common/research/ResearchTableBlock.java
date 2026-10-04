package thaumcraft.common.research;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public final class ResearchTableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Plain table, primary research half, secondary research half.
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    private static final VoxelShape SHAPE = Shapes.or(box(0, 12, 0, 16, 16, 16), box(0, 0, 4, 16, 4, 12),
            box(2, 4, 6, 6, 12, 10), box(10, 4, 6, 14, 12, 10));

    public ResearchTableBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, PART); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ResearchTableEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PART);
        VoxelShape shape = part == 0 ? SHAPE : Shapes.or(box(0, 12, 0, 16, 16, 16),
                part == 1 ? box(4, 2, 6, 16, 6, 10) : box(0, 2, 6, 12, 6, 10),
                part == 1 ? box(2, 0, 2, 6, 12, 6) : box(10, 0, 2, 14, 12, 6),
                part == 1 ? box(2, 0, 10, 6, 12, 14) : box(10, 0, 10, 14, 12, 14));
        int turns = switch (state.getValue(FACING)) { case SOUTH -> 1; case WEST -> 2; case NORTH -> 3; default -> 0; };
        for (int i = 0; i < turns; i++) {
            VoxelShape[] rotated = {Shapes.empty()};
            shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> rotated[0] = Shapes.or(rotated[0],
                    Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
            shape = rotated[0];
        }
        return shape;
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var held = player.getItemInHand(hand);
        if (state.getValue(PART) == 0 && held.is(ModResearch.SCRIBING_TOOLS.get())) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(direction);
                var other = level.getBlockState(neighbor);
                if (!other.is(this) || other.getValue(PART) != 0) continue;
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(PART, 1).setValue(FACING, direction), 3);
                    level.setBlock(neighbor, other.setValue(PART, 2).setValue(FACING, direction), 3);
                    if (level.getBlockEntity(pos) instanceof ResearchTableEntity table) table.setTools(held);
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (state.getValue(PART) == 0) return InteractionResult.PASS;
        BlockPos primary = state.getValue(PART) == 1 ? pos : pos.relative(state.getValue(FACING).getOpposite());
        if (player instanceof ServerPlayer serverPlayer && isComplete(level, primary)) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inventory, ignored) ->
                    new ResearchMenu(id, inventory, ContainerLevelAccess.create(level, primary)), Component.translatable("container.thaumcraft.research")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean isComplete(Level level, BlockPos primary) {
        var state = level.getBlockState(primary);
        if (!state.is(ModResearch.TABLE.get()) || state.getValue(PART) != 1) return false;
        var other = level.getBlockState(primary.relative(state.getValue(FACING)));
        return other.is(ModResearch.TABLE.get()) && other.getValue(PART) == 2 && other.getValue(FACING) == state.getValue(FACING)
                && level.getBlockEntity(primary) instanceof ResearchTableEntity;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide && state.getValue(PART) != 0) {
            Direction facing = state.getValue(FACING);
            BlockPos primary = state.getValue(PART) == 1 ? pos : pos.relative(facing.getOpposite());
            if (level.getBlockEntity(primary) instanceof ResearchTableEntity table) popResource(level, pos, table.takeTools());
            BlockPos partner = state.getValue(PART) == 1 ? pos.relative(facing) : primary;
            var other = level.getBlockState(partner);
            if (other.is(this) && other.getValue(FACING) == facing && other.getValue(PART) == (state.getValue(PART) == 1 ? 2 : 1)) {
                level.setBlock(partner, other.setValue(PART, 0), 3);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}

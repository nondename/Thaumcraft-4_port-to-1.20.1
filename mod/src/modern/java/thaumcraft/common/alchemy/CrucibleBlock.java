package thaumcraft.common.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.common.items.ModItems;

public final class CrucibleBlock extends BaseEntityBlock {
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 3);
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 5, 16),
            box(0, 5, 0, 2, 16, 16), box(14, 5, 0, 16, 16, 16),
            box(2, 5, 0, 14, 16, 2), box(2, 5, 14, 14, 16, 16));
    public CrucibleBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(WATER, 0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(WATER); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new CrucibleEntity(p, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModAlchemy.ENTITY.get(), CrucibleEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleEntity crucible)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.WATER_BUCKET) && crucible.getWater() < 1000) {
            if (!level.isClientSide) {
                crucible.fillWater();
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.is(ModItems.WAND.get()) && player.isShiftKeyDown()) {
            if (!level.isClientSide) crucible.empty();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    public static InteractionResult tryConvert(UseOnContext context) {
        Level level = context.getLevel(); BlockPos pos = context.getClickedPos(); Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        if (state.is(ModAlchemy.CRUCIBLE.get()) && player != null && player.isShiftKeyDown()) {
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrucibleEntity c) c.empty();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!state.is(Blocks.CAULDRON) && !state.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (player == null || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;
        if (!level.isClientSide) {
            // TC4 replaces the cauldron; the newly created tile starts empty.
            level.setBlockAndUpdate(pos, ModAlchemy.CRUCIBLE.get().defaultBlockState());
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof CrucibleEntity c && c.isBoiling() && random.nextInt(3) == 0)
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + .2 + random.nextDouble() * .6,
                    pos.getY() + c.getFluidHeight(), pos.getZ() + .2 + random.nextDouble() * .6, 0, .03, 0);
    }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide && entity instanceof net.minecraft.world.entity.LivingEntity
                && entity.tickCount % 10 == 0 && level.getBlockEntity(pos) instanceof CrucibleEntity c
                && c.isBoiling() && entity.getY() < pos.getY() + c.getFluidHeight())
            entity.hurt(level.damageSources().inFire(), 1);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState s, Level l, BlockPos p) {
        return l.getBlockEntity(p) instanceof CrucibleEntity c ? Math.min(15, (int) (c.getAspects().visSize() * .14F) + (c.getAspects().size() > 0 ? 1 : 0)) : 0;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof CrucibleEntity c) c.empty();
        super.onRemove(state, level, pos, replacement, moving);
    }
}

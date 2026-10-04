package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.items.tools.ItemWand;
import thaumcraft.common.research.ModResearch;
import thaumcraft.common.research.ResearchTableBlock;

/**
 * Arcane worktable — port of the meta-15 branch of TC4 {@code BlockTable} (1.7.10).
 * A plain research table (original: table metadata 0/1, modern: PART 0) is converted
 * by right-clicking it with a wand; there is no crafting recipe, matching the original.
 *
 * <p>Documented deviations:
 * <ul>
 *   <li>the arcane worktable is its own block + {@code BlockItem} (original shared the one
 *       {@code blockTable} item across metas; {@code damageDropped} line 110 kept meta 15
 *       pointing at itself, so the behaviour is equivalent);</li>
 *   <li>the original rendered one flat {@code woodplain} cube through
 *       {@code BlockTable#getIcon} for every table meta; this port keeps the modern
 *       table model (as the research table port does) with a dedicated woodplain texture;</li>
 *   <li>the original TESR/table decor is not part of this slice.</li>
 * </ul>
 */
public final class ArcaneWorkbenchBlock extends BaseEntityBlock {
    public static final RegistryObject<Block> ARCANE_WORKBENCH = ModOres.BLOCKS.register("arcane_workbench",
            () -> new ArcaneWorkbenchBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
                    .strength(2).noOcclusion()));
    public static final RegistryObject<Item> ITEM = ModItems.ITEMS.register("arcane_workbench",
            () -> new BlockItem(ARCANE_WORKBENCH.get(), new Item.Properties()));

    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Thaumcraft.MODID);
    public static final RegistryObject<BlockEntityType<ArcaneWorkbenchBlockEntity>> ARCANE_WORKBENCH_ENTITY =
            BLOCK_ENTITIES.register("arcane_workbench", () -> BlockEntityType.Builder
                    .of(ArcaneWorkbenchBlockEntity::new, ARCANE_WORKBENCH.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    // Same table silhouette as the research table (original BlockTable bounds).
    private static final VoxelShape SHAPE = Shapes.or(box(0, 12, 0, 16, 16, 16), box(0, 0, 4, 16, 4, 12),
            box(2, 4, 6, 6, 12, 10), box(10, 4, 6, 14, 12, 10));

    public ArcaneWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // Original meta 15 carries no facing; nothing to add.
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneWorkbenchBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Original BlockTable#onBlockActivated lines 140-154: GUI 13 unless sneaking. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity be) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inventory, ignored) ->
                    new ArcaneWorkbenchMenu(id, inventory, be),
                    Component.translatable("container.thaumcraft.arcane_workbench")), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Original BlockTable#breakBlock lines 96-99: drop the whole table inventory. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity be) {
            for (int i = 0; i < ArcaneWorkbenchBlockEntity.SIZE; i++) {
                ItemStack stack = be.removeItemNoUpdate(i);
                if (i != ArcaneWorkbenchBlockEntity.SLOT_RESULT && !stack.isEmpty()) {
                    popResource(level, pos, stack);
                }
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    /**
     * Port of BlockTable#onWandRightClick lines 237-256 ({@code md <= 1}): convert the
     * plain table, park a non-staff wand in slot 10 clearing the hand, play the click;
     * the original then continued into block activation which opened the GUI unless the
     * player was sneaking — {@code ItemWand#useOn} consumes the click here, so this
     * method opens it instead. Conversion itself happens regardless of sneaking, as in
     * the original (onItemUseFirst had no sneak check).
     *
     * @return PASS when the clicked block is not a convertible plain table
     */
    public static InteractionResult tryConvertPlainTable(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModResearch.TABLE.get()) || state.getValue(ResearchTableBlock.PART) != 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, ARCANE_WORKBENCH.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity table) {
                ItemStack wand = context.getItemInHand();
                if (!ItemWand.isStaff(wand)) {
                    table.setItem(ArcaneWorkbenchBlockEntity.SLOT_WAND, wand.copy());
                    // Original line 246: setInventorySlotContents(currentItem, null),
                    // unconditional — creative hands are cleared too.
                    wand.shrink(1);
                }
            }
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.NEUTRAL, 0.15F, 0.5F);
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer serverPlayer && !player.isShiftKeyDown()
                    && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity be) {
                NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inventory, ignored) ->
                        new ArcaneWorkbenchMenu(id, inventory, be),
                        Component.translatable("container.thaumcraft.arcane_workbench")), pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

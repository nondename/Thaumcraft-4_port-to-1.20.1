package thaumcraft.common.blocks;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of TC4 {@code TileArcaneWorkbench}/{@code TileMagicWorkbench} (1.7.10): an 11-slot
 * container — grid 0-8, reserved legacy result 9, wand 10 — plus the recompute hook
 * (original TileMagicWorkbench lines 15/51/64/81: every slot change refreshes the preview
 * through the open container).
 *
 * <p>Slots NBT is vanilla {@link ContainerHelper}; the original saved its stack list in the
 * same 0-10 order. Unpaid previews are now per menu and are discarded from old saves;
 * inputs and wand NBT retain their original indices.
 */
public final class ArcaneWorkbenchBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_RESULT = 9;
    public static final int SLOT_WAND = 10;
    public static final int SIZE = 11;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    /** Each viewer owns a result preview; only ingredients and the wand are shared. */
    private final Set<ArcaneWorkbenchMenu> menus = new LinkedHashSet<>();
    private int updateDepth;

    public ArcaneWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneWorkbenchBlock.ARCANE_WORKBENCH_ENTITY.get(), pos, state);
    }

    // ---------------------------------------------------------------- Container

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SIZE ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SIZE) {
            items.set(slot, stack);
            setChanged();
        }
    }

    /**
     * Compatibility alias for older callers. Previews are no longer stored in this inventory.
     */
    public void setItemSoftly(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SIZE) {
            items.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SIZE) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(slot);
        if (removed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out;
        if (removed.getCount() <= amount) {
            out = removed;
            items.set(slot, ItemStack.EMPTY);
        } else {
            out = removed.split(amount);
            if (removed.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
        }
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SIZE) {
            return ItemStack.EMPTY;
        }
        ItemStack out = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return out;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        notifyMenu();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    /** Original canInteractWith distance: 64.0 (TileMagicWorkbench/Container line 80). */
    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.thaumcraft.arcane_workbench");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ArcaneWorkbenchMenu(id, inventory, this);
    }

    // ---------------------------------------------------------------- eventHandler

    /** Server side: hooks the open menu up for preview recomputes (original line 21). */
    void addMenu(ArcaneWorkbenchMenu menu) {
        menus.add(menu);
    }

    void removeMenu(ArcaneWorkbenchMenu menu) {
        menus.remove(menu);
    }

    void beginCraft() {
        updateDepth++;
    }

    void endCraft() {
        updateDepth--;
        setChanged();
    }

    /** Original setInventorySlotContents/decrStackSize → eventHandler.onCraftMatrixChanged. */
    private void notifyMenu() {
        if (updateDepth == 0 && level != null && !level.isClientSide) {
            for (ArcaneWorkbenchMenu menu : Set.copyOf(menus)) {
                menu.onCraftMatrixChanged();
            }
        }
    }

    // ---------------------------------------------------------------- NBT

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // Slot 9 in older saves was only a preview, never a paid-for item.
        items.set(SLOT_RESULT, ItemStack.EMPTY);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        items.set(SLOT_RESULT, ItemStack.EMPTY);
    }
}

package thaumcraft.common.blocks;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ModRecipes;
import thaumcraft.common.items.tools.ItemWand;

/**
 * Port of TC4 {@code ContainerArcaneWorkbench} (1.7.10, full source read): slots
 * output(0, 160/64) - wand(1, 160/24) - grid(2-10, 40+24c/40+24r) - player main
 * (11-37, 16+18c/151+18r) - hotbar (38-46, 16+18c/209), the preview recompute
 * ({@code onCraftMatrixChanged} lines 45-66) and the shift-click rules
 * ({@code transferStackInSlot} lines 83-132).
 *
 * <p>Documented deviations:
 * <ul>
 *   <li>the client menu wraps a local {@link SimpleContainer} filled by the vanilla slot
 *       sync instead of the client block entity (1.7.10 shared one tile entity instance
 *       across logical sides); the preview recompute runs server-side only — the original
 *       recomputed on both sides, which yields the identical slot 9 value;</li>
 *   <li>the original {@code slotClick} quirks (buttons forced to 0 on slots 0/1,
 *       creative clone fixup) and {@code func_94530_a} (no drag-painting into table
 *       slots) live behind 1.20.1-private {@code doClick}/{@code canItemQuickReplace},
 *       so they are not ported; outcomes are equivalent or cosmetic;</li>
 *   <li>{@code SlotLimitedByWand}'s 64 stack limit and no-op {@code onPickupFromSlot}
 *       equal the vanilla {@link Slot} defaults and are not subclassed.</li>
 * </ul>
 */
public final class ArcaneWorkbenchMenu extends AbstractContainerMenu {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Thaumcraft.MODID);
    /** Client factory: the buffer carries the block pos (vanilla sync contract); the client
     *  renders from its local container, see class javadoc. */
    public static final RegistryObject<MenuType<ArcaneWorkbenchMenu>> MENU_TYPE = MENUS.register(
            "arcane_workbench", () -> IForgeMenuType.create((windowId, inventory, data) -> {
                data.readBlockPos();
                return new ArcaneWorkbenchMenu(windowId, inventory,
                        new SimpleContainer(ArcaneWorkbenchBlockEntity.SIZE));
            }));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private final Inventory playerInventory;
    /** Server: the block entity. Client: a local container kept in sync by packets. */
    private final Container table;
    @Nullable
    private final ArcaneWorkbenchBlockEntity be; // server only
    private final ContainerLevelAccess access;
    private final GridContainer grid = new GridContainer();

    /** Server side, opened through NetworkHooks.openScreen with the block pos. */
    public ArcaneWorkbenchMenu(int id, Inventory inventory, ArcaneWorkbenchBlockEntity be) {
        super(MENU_TYPE.get(), id);
        this.playerInventory = inventory;
        this.table = be;
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        buildSlots(inventory);
        be.setMenu(this);   // original line 21: tileEntity.eventHandler = this
        onCraftMatrixChanged(); // original line 42
    }

    /** Client side, produced by the MenuType factory above. */
    public ArcaneWorkbenchMenu(int id, Inventory inventory, Container localTable) {
        super(MENU_TYPE.get(), id);
        this.playerInventory = inventory;
        this.table = localTable;
        this.be = null;
        this.access = ContainerLevelAccess.NULL;
        buildSlots(inventory);
    }

    private void buildSlots(Inventory inventory) {
        addSlot(new ArcaneResultSlot());                                        // menu 0  -> table 9
        addSlot(new WandSlot());                                                // menu 1  -> table 10
        for (int row = 0; row < 3; row++) {                                     // menu 2-10 -> table 0-8
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(table, row * 3 + col, 40 + col * 24, 40 + row * 24));
            }
        }
        for (int row = 0; row < 3; row++) {                                     // menu 11-37
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 16 + col * 18, 151 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {                                     // menu 38-46
            addSlot(new Slot(inventory, col, 16 + col * 18, 209));
        }
    }

    /** For the screen: wand, grid contents and recipe lookups (client-side). */
    public Container getTable() {
        return table;
    }

    public CraftingContainer getGrid() {
        return grid;
    }

    /**
     * Port of ContainerArcaneWorkbench#onCraftMatrixChanged lines 45-66: the vanilla recipe
     * always wins the preview; only when it produced nothing and a wand sits in slot 10 is
     * the arcane recipe shown, gated by a dry-run vis check (doit=false, no charge).
     */
    public void onCraftMatrixChanged() {
        if (be == null) {
            return; // client: slot sync from the server is authoritative (see class javadoc)
        }
        Level level = playerInventory.player.level();
        ItemStack vanilla = ModRecipes.vanillaResult(level, grid);
        be.setItemSoftly(ArcaneWorkbenchBlockEntity.SLOT_RESULT, vanilla);
        if (vanilla.isEmpty()) {
            ItemStack wand = table.getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND);
            if (wand.getItem() instanceof ItemWand) {
                AspectList aspects = ModRecipes.arcaneAspects(level, grid);
                if (ItemWand.consumeAllVisCrafting(wand, playerInventory.player, aspects, false)) {
                    be.setItemSoftly(ArcaneWorkbenchBlockEntity.SLOT_RESULT,
                            ModRecipes.arcaneResult(level, grid));
                }
            }
        }
    }

    /** Original onContainerClosed lines 68-73: only unhooks the event handler. */
    @Override
    public void removed(Player player) {
        if (be != null) {
            be.setMenu(null);
        }
    }

    /** Original canInteractWith lines 75-81: same block entity within 64.0 (squared). */
    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockEntity(pos) == be
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    /** Port of transferStackInSlot lines 83-132 (same index ranges and branches). */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack out = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack in = slot.getItem();
            out = in.copy();
            if (index == 0) {
                if (!moveItemStackTo(in, 11, 47, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(in, out);
            } else if (index >= 11 && index < 38) {
                if (in.getItem() instanceof ItemWand && !ItemWand.isStaff(in)) {
                    if (!moveItemStackTo(in, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                    slot.onQuickCraft(in, out);
                } else if (!moveItemStackTo(in, 38, 47, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 38 && index < 47) {
                if (in.getItem() instanceof ItemWand && !ItemWand.isStaff(in)) {
                    if (!moveItemStackTo(in, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                    slot.onQuickCraft(in, out);
                } else if (!moveItemStackTo(in, 11, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(in, 11, 47, false)) {
                return ItemStack.EMPTY;
            }
            if (in.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (in.getCount() == out.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, in);
        }
        return out;
    }

    /** Result slot: the arcane payment of SlotCraftingArcaneWorkbench#onPickupFromSlot. */
    private final class ArcaneResultSlot extends ResultSlot {
        ArcaneResultSlot() {
            super(playerInventory.player, grid, table, ArcaneWorkbenchBlockEntity.SLOT_RESULT, 160, 64);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            // Original SlotCraftingArcaneWorkbench#onPickupFromSlot order: capture the
            // aspects + parked wand while the grid is intact, consume one of each
            // ingredient, then pay (the original paid via the container after onPickup).
            Level level = player.level();
            AspectList aspects = ModRecipes.arcaneAspects(level, grid);
            ItemStack wand = table.getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND);

            // Consume the grid BEFORE super: RecipeManager.getRemainingItemsFor falls back
            // to returning the grid's own stacks whenever no minecraft:crafting_shaped
            // recipe matches, and arcane recipes never are one — vanilla ResultSlot would
            // remove each ingredient and then refill every slot straight from that
            // fallback (infinite free crafts). With an emptied grid both branches of the
            // vanilla loop see nothing to remove or restore. Container-item remainders
            // are applied here for the same reason; none of the current recipes have any.
            for (int i = 0; i < grid.getContainerSize(); i++) {
                ItemStack ingredient = grid.getItem(i);
                if (ingredient.isEmpty()) {
                    continue;
                }
                ItemStack remainder = ingredient.getItem().hasCraftingRemainingItem()
                        ? new ItemStack(ingredient.getItem().getCraftingRemainingItem())
                        : ItemStack.EMPTY;
                grid.removeItem(i, 1);
                if (!remainder.isEmpty() && grid.getItem(i).isEmpty()) {
                    grid.setItem(i, remainder);
                }
            }

            // Super still fires the craft event/stats (both gated on removeCount from
            // onQuickCraft) and the Slot.setChanged bookkeeping; its decrement loop is
            // now a no-op. Each grid write above re-triggered the preview through
            // decrStackSize parity (original grid behaviour).
            super.onTake(player, stack);
            if (aspects.size() > 0 && wand.getItem() instanceof ItemWand) {
                // The actual drain is guarded to the server inside consumeAllVis.
                ItemWand.consumeAllVisCrafting(wand, player, aspects, true);
            }
        }
    }

    /** Original SlotLimitedByWand#isItemValid: a wand, but never a staff. */
    private final class WandSlot extends Slot {
        WandSlot() {
            super(table, ArcaneWorkbenchBlockEntity.SLOT_WAND, 160, 24);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ItemWand && !ItemWand.isStaff(stack);
        }
    }

    /** Read-through view of table slots 0-8 used for recipe matching and the result slot. */
    private final class GridContainer implements CraftingContainer {
        @Override
        public int getContainerSize() {
            return 9;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < 9; i++) {
                if (!table.getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return table.getItem(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            table.setItem(slot, stack);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return table.removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return table.removeItemNoUpdate(slot);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < 9; i++) {
                table.removeItemNoUpdate(i);
            }
        }

        @Override
        public void setChanged() {
            table.setChanged();
        }

        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            NonNullList<ItemStack> out = NonNullList.withSize(9, ItemStack.EMPTY);
            for (int i = 0; i < 9; i++) {
                out.set(i, table.getItem(i));
            }
            return out;
        }

        @Override
        public boolean stillValid(Player player) {
            return table.stillValid(player);
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
            for (ItemStack stack : getItems()) {
                contents.accountStack(stack);
            }
        }
    }
}

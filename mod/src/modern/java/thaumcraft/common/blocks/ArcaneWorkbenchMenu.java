package thaumcraft.common.blocks;

import java.util.List;
import javax.annotation.Nullable;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
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
 *       recomputed on both sides. Each modern viewer has an independent, unpaid result
 *       container so armor discounts cannot affect another viewer's eligibility;</li>
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
    private final SimpleContainer result = new SimpleContainer(1);
    @Nullable
    private Recipe<CraftingContainer> selectedRecipe;
    private AspectList selectedCost = new AspectList();

    /** Server side, opened through NetworkHooks.openScreen with the block pos. */
    public ArcaneWorkbenchMenu(int id, Inventory inventory, ArcaneWorkbenchBlockEntity be) {
        super(MENU_TYPE.get(), id);
        this.playerInventory = inventory;
        this.table = be;
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        buildSlots(inventory);
        be.addMenu(this);
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
        addSlot(new ArcaneResultSlot());                                        // menu 0 -> local result
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
        selectedRecipe = null;
        selectedCost = new AspectList();
        ItemStack preview = ItemStack.EMPTY;
        var vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        if (vanilla.isPresent()) {
            selectedRecipe = vanilla.get();
            preview = selectedRecipe.assemble(grid, level.registryAccess());
        }
        if (preview.isEmpty()) {
            selectedRecipe = null;
            ItemStack wand = table.getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND);
            if (wand.getItem() instanceof ItemWand) {
                AspectList aspects = ModRecipes.arcaneAspects(level, grid);
                if (ItemWand.consumeAllVisCrafting(wand, playerInventory.player, aspects, false)) {
                    selectedRecipe = ModRecipes.arcaneMatch(level, grid).map(ModRecipes.RecipeWithType::recipe).orElse(null);
                    selectedCost = aspects;
                    if (selectedRecipe != null) {
                        preview = selectedRecipe.assemble(grid, level.registryAccess());
                    }
                }
            }
        }
        result.setItem(0, preview);
    }

    @Override
    public void broadcastChanges() {
        // Vis NBT and worn discount gear can change without a grid slot write.
        onCraftMatrixChanged();
        super.broadcastChanges();
    }

    /** Original onContainerClosed lines 68-73: only unhooks the event handler. */
    @Override
    public void removed(Player player) {
        if (be != null) {
            be.removeMenu(this);
        }
        super.removed(player);
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
        if (slot != null && slot.mayPickup(player) && slot.hasItem()) {
            ItemStack in = slot.getItem();
            out = in.copy();
            if (index == 0) {
                // Never transfer only part of a multi-item recipe result: that would
                // leave unpaid preview items behind or discard the remainder.
                if (!canFitResult(in)) {
                    return ItemStack.EMPTY;
                }
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
            slot.onTake(player, index == 0 ? out : in);
        }
        return out;
    }

    private boolean canFitResult(ItemStack output) {
        int remaining = output.getCount();
        for (int i = 11; i < 47; i++) {
            Slot slot = slots.get(i);
            ItemStack existing = slot.getItem();
            if (slot.mayPlace(output) && (existing.isEmpty() || ItemStack.isSameItemSameTags(existing, output))) {
                remaining -= Math.max(0, Math.min(slot.getMaxStackSize(output), output.getMaxStackSize()) - existing.getCount());
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    /** Result slot: the arcane payment of SlotCraftingArcaneWorkbench#onPickupFromSlot. */
    private final class ArcaneResultSlot extends ResultSlot {
        ArcaneResultSlot() {
            super(playerInventory.player, grid, result, 0, 160, 64);
        }

        @Override
        public boolean mayPickup(Player player) {
            onCraftMatrixChanged();
            return hasItem() && (be == null || stillValid(player));
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (be == null || selectedRecipe == null) return;
            var remainders = selectedRecipe.getRemainingItems(grid);
            ItemStack wand = table.getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND);
            be.beginCraft();
            try {
                // Charge exactly the selected arcane recipe; vanilla-first crafts are free.
                if (selectedCost.size() > 0
                        && !ItemWand.consumeAllVisCrafting(wand, player, selectedCost, true)) {
                    throw new IllegalStateException("Workbench result taken without sufficient vis");
                }
                checkTakeAchievements(stack);
                for (int i = 0; i < grid.getContainerSize(); i++) {
                    if (!grid.getItem(i).isEmpty()) grid.removeItem(i, 1);
                    ItemStack remainder = remainders.get(i);
                    if (remainder.isEmpty()) continue;
                    ItemStack left = grid.getItem(i);
                    if (left.isEmpty()) {
                        grid.setItem(i, remainder);
                    } else if (ItemStack.isSameItemSameTags(left, remainder)) {
                        remainder.grow(left.getCount());
                        grid.setItem(i, remainder);
                    } else if (!player.getInventory().add(remainder)) {
                        player.drop(remainder, false);
                    }
                }
            } finally {
                be.endCraft();
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

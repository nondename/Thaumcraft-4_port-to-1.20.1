package thaumcraft.common.research;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import thaumcraft.common.lib.network.ModNetwork;

public final class ResearchMenu extends AbstractContainerMenu {
    public static final Aspect[] ASPECTS = Aspect.aspects.values().toArray(new Aspect[0]);
    private final ContainerLevelAccess access;
    private final DataSlot first = DataSlot.standalone();
    private final DataSlot second = DataSlot.standalone();
    private final DataSlot result = DataSlot.standalone();
    public ResearchMenu(int id, Inventory inventory) { this(id, inventory, ContainerLevelAccess.NULL); }
    public ResearchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModResearch.MENU.get(), id);
        this.access = access;
        first.set(-1); second.set(-1); result.set(-1);
        addDataSlot(first); addDataSlot(second); addDataSlot(result);
        net.minecraft.world.Container tools = access.evaluate((level, pos) ->
                level.getBlockEntity(pos) instanceof ResearchTableEntity table
                        ? (net.minecraft.world.Container) table : new net.minecraft.world.SimpleContainer(1),
                new net.minecraft.world.SimpleContainer(1));
        addSlot(new net.minecraft.world.inventory.Slot(tools, 0, 14, 10) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModResearch.SCRIBING_TOOLS.get()); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new net.minecraft.world.inventory.Slot(inventory, col + row * 9 + 9, 48 + col * 18, 175 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new net.minecraft.world.inventory.Slot(inventory, col, 48 + col * 18, 233));
    }
    public int first() { return first.get(); }
    public int second() { return second.get(); }
    public int result() { return result.get(); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        boolean moved = index == 0 ? moveItemStackTo(stack, 1, 37, true)
                : stack.is(ModResearch.SCRIBING_TOOLS.get()) && moveItemStackTo(stack, 0, 1, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }
    @Override public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> ResearchTableBlock.isComplete(level, pos)
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        var knowledge = player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
        if (knowledge == null) return false;
        if (id >= 0 && id < ASPECTS.length && knowledge.hasDiscoveredAspect(ASPECTS[id])) {
            if (first.get() < 0 || second.get() >= 0) { first.set(id); second.set(-1); }
            else second.set(id);
            result.set(-1);
            return true;
        }
        if (id == 100) { first.set(-1); second.set(-1); result.set(-1); return true; }
        if (id == 101 && first.get() >= 0 && second.get() >= 0) {
            Aspect combined = knowledge.combine(ASPECTS[first.get()], ASPECTS[second.get()]);
            result.set(-2);
            if (combined != null) {
                for (int i = 0; i < ASPECTS.length; i++) if (ASPECTS[i] == combined) result.set(i);
                ModNetwork.syncThaumometerKnowledge(player);
            }
            return true;
        }
        return false;
    }
}

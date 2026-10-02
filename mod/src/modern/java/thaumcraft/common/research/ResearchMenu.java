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
    }
    public int first() { return first.get(); }
    public int second() { return second.get(); }
    public int result() { return result.get(); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
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

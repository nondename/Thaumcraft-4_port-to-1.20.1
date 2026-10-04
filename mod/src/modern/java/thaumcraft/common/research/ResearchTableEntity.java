package thaumcraft.common.research;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ResearchTableEntity extends BlockEntity implements net.minecraft.world.Container {
    private ItemStack tools = ItemStack.EMPTY;
    private ItemStack notes = ItemStack.EMPTY;
    public ResearchTableEntity(BlockPos pos, BlockState state) { super(ModResearch.TABLE_ENTITY.get(), pos, state); }
    public void setTools(ItemStack stack) { tools = stack.copy(); tools.setCount(1); setChanged(); }
    public boolean hasTools() { return tools.is(ModResearch.SCRIBING_TOOLS.get()); }
    public ItemStack takeTools() { ItemStack result = tools; tools = ItemStack.EMPTY; setChanged(); return result; }
    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return tools.isEmpty() && notes.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? tools : slot == 1 ? notes : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int count) {
        if (slot == 1) { ItemStack out=notes.split(count); setChanged(); return out; }
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack out = tools.split(count); setChanged(); return out;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 0 ? takeTools() : removeItem(slot,1); }
    @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) setTools(stack); else if(slot==1) {notes=stack.copy(); notes.setCount(1); setChanged();} }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }
    @Override public void clearContent() { takeTools(); notes=ItemStack.EMPTY; setChanged(); }
    @Override public void load(CompoundTag tag) { super.load(tag); tools = ItemStack.of(tag.getCompound("Tools")); notes=ItemStack.of(tag.getCompound("Notes")); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Tools", tools.save(new CompoundTag())); tag.put("Notes",notes.save(new CompoundTag())); }
}

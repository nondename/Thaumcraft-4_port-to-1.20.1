package thaumcraft.common.research;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ResearchTableEntity extends BlockEntity {
    private ItemStack tools = ItemStack.EMPTY;
    public ResearchTableEntity(BlockPos pos, BlockState state) { super(ModResearch.TABLE_ENTITY.get(), pos, state); }
    public void setTools(ItemStack stack) { tools = stack.copy(); tools.setCount(1); setChanged(); }
    public boolean hasTools() { return tools.is(ModResearch.SCRIBING_TOOLS.get()); }
    public ItemStack takeTools() { ItemStack result = tools; tools = ItemStack.EMPTY; setChanged(); return result; }
    @Override public void load(CompoundTag tag) { super.load(tag); tools = ItemStack.of(tag.getCompound("Tools")); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Tools", tools.save(new CompoundTag())); }
}

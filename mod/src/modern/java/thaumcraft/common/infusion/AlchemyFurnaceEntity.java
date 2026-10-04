package thaumcraft.common.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.RecipeAspects;

/** Fuel-driven distillation, with one unit transferred to a stacked alembic per tick. */
public final class AlchemyFurnaceEntity extends BlockEntity implements Container {
    private final ItemStack[] items={ItemStack.EMPTY,ItemStack.EMPTY};
    private AspectList buffer=new AspectList(); private int burn,cook;
    public boolean burning() {return burn>0;}
    public final net.minecraft.world.inventory.ContainerData data=new net.minecraft.world.inventory.ContainerData() {
        public int get(int i) {return i==0?burn:cook;}
        public void set(int i,int value) {if(i==0)burn=value;else cook=value;}
        public int getCount() {return 2;}
    };
    public AlchemyFurnaceEntity(BlockPos pos,BlockState state) {super(ModInfusion.FURNACE_ENTITY.get(),pos,state);}
    public AspectList buffer() {return buffer.copy();}
    public static void tick(Level level,BlockPos pos,BlockState state,AlchemyFurnaceEntity furnace) {
        if(furnace.burn>0) {furnace.burn--;furnace.setChanged();}
        var value=RecipeAspects.get(furnace.items[0],level);
        boolean can=value!=null && value.visSize()>0 && furnace.buffer.visSize()+value.visSize()<=256;
        if(can && furnace.burn==0) {
            int fuel=ForgeHooks.getBurnTime(furnace.items[1],null);
            if(fuel>0) {var remainder=furnace.items[1].getCraftingRemainingItem();furnace.items[1].shrink(1);
                if(furnace.items[1].isEmpty())furnace.items[1]=remainder;furnace.burn=fuel;furnace.setChanged();}
        }
        if(can && furnace.burn>0) {
            if(++furnace.cook>=100) {furnace.buffer.add(value);furnace.items[0].shrink(1);furnace.cook=0;}furnace.setChanged();
        } else furnace.cook=0;
        if(state.getValue(InfusionDeviceBlock.ACTIVE)!=(furnace.burn>0))level.setBlock(pos,state.setValue(InfusionDeviceBlock.ACTIVE,furnace.burn>0),3);
        for(int y=1;y<=5;y++) {
            if(!(level.getBlockEntity(pos.above(y)) instanceof EssentiaStoreEntity store) || !store.getBlockState().is(ModInfusion.ALEMBIC.get()))break;
            for(var aspect:furnace.buffer.getAspects()) {
                if(store.add(aspect,1)) {furnace.buffer.remove(aspect,1);furnace.setChanged();return;}
            }
        }
    }
    @Override public int getContainerSize() {return 2;}
    @Override public boolean isEmpty() {return items[0].isEmpty() && items[1].isEmpty();}
    @Override public ItemStack getItem(int slot) {return slot>=0 && slot<2?items[slot]:ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count) {var out=getItem(slot).split(count);setChanged();return out;}
    @Override public ItemStack removeItemNoUpdate(int slot) {var out=getItem(slot);if(slot>=0 && slot<2)items[slot]=ItemStack.EMPTY;return out;}
    @Override public void setItem(int slot,ItemStack stack) {if(slot>=0 && slot<2) {items[slot]=stack;setChanged();}}
    @Override public void clearContent() {items[0]=ItemStack.EMPTY;items[1]=ItemStack.EMPTY;setChanged();}
    @Override public boolean stillValid(Player player) {return level!=null && level.getBlockEntity(worldPosition)==this && player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override protected void saveAdditional(CompoundTag tag) {super.saveAdditional(tag);tag.put("Input",items[0].save(new CompoundTag()));tag.put("Fuel",items[1].save(new CompoundTag()));tag.putInt("Burn",burn);tag.putInt("Cook",cook);buffer.writeToNBT(tag,"Buffer");}
    @Override public void load(CompoundTag tag) {super.load(tag);items[0]=ItemStack.of(tag.getCompound("Input"));items[1]=ItemStack.of(tag.getCompound("Fuel"));burn=Math.max(0,tag.getInt("Burn"));cook=Math.max(0,Math.min(99,tag.getInt("Cook")));buffer=new AspectList();buffer.readFromNBT(tag,"Buffer");}
}

package thaumcraft.common.infusion;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;

public final class AlchemyMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    public boolean burning() {return data.get(0)>0;}
    private final ContainerData data;
    public int progress() {return data.get(1);}
    public AlchemyMenu(int id,Inventory inventory,Container container,ContainerLevelAccess access) {
        super(ModInfusion.MENU.get(),id);this.access=access;
        data=container instanceof AlchemyFurnaceEntity furnace?furnace.data:new SimpleContainerData(2);
        addDataSlots(data);
        addSlot(new Slot(container,0,80,26));
        addSlot(new Slot(container,1,80,47) {
            @Override public boolean mayPlace(ItemStack stack) {return ForgeHooks.getBurnTime(stack,null)>0;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,142));
    }
    @Override public boolean stillValid(Player player) {return stillValid(access,player,ModInfusion.FURNACE.get());}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size() || !slots.get(index).hasItem())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();var copy=stack.copy();
        if(!(index<2?moveItemStackTo(stack,2,38,true):ForgeHooks.getBurnTime(stack,null)>0?
                moveItemStackTo(stack,1,2,false):moveItemStackTo(stack,0,1,false)))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}

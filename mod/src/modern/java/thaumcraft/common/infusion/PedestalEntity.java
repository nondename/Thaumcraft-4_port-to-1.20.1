package thaumcraft.common.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

public final class PedestalEntity extends BlockEntity {
    private ItemStack item=ItemStack.EMPTY;
    public PedestalEntity(BlockPos pos,BlockState state) {super(ModInfusion.PEDESTAL_ENTITY.get(),pos,state);}
    public ItemStack item() {return item;}
    public void set(ItemStack value) {item=value.copy();changed();}
    public void changed() {setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public void load(CompoundTag tag) {super.load(tag);item=ItemStack.of(tag.getCompound("Item"));}
    @Override protected void saveAdditional(CompoundTag tag) {super.saveAdditional(tag);tag.put("Item",item.save(new CompoundTag()));}
    @Override public CompoundTag getUpdateTag() {return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
}

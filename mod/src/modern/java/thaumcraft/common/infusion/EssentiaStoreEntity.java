package thaumcraft.common.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;

public final class EssentiaStoreEntity extends BlockEntity {
    private Aspect aspect; private int amount;
    public EssentiaStoreEntity(BlockPos pos,BlockState state) {super(ModInfusion.STORE_ENTITY.get(),pos,state);}
    public Aspect aspect() {return aspect;}
    public int amount() {return amount;}
    public int capacity() {return getBlockState().is(ModInfusion.ALEMBIC.get())?32:64;}
    public boolean add(Aspect type,int units) {
        if(type==null || units<=0 || amount+units>capacity() || amount>0 && aspect!=type)return false;
        aspect=type;amount+=units;changed();return true;
    }
    public boolean take(Aspect type,int units) {
        if(type!=aspect || units<=0 || amount<units)return false;
        amount-=units;if(amount==0)aspect=null;changed();return true;
    }
    private void changed() {setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public void load(CompoundTag tag) {super.load(tag);aspect=Aspect.getAspect(tag.getString("Aspect"));amount=aspect==null?0:Math.max(0,Math.min(capacity(),tag.getInt("Amount")));if(amount==0)aspect=null;}
    @Override protected void saveAdditional(CompoundTag tag) {super.saveAdditional(tag);tag.putString("Aspect",aspect==null?"":aspect.getTag());tag.putInt("Amount",amount);}
    @Override public CompoundTag getUpdateTag() {return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
}

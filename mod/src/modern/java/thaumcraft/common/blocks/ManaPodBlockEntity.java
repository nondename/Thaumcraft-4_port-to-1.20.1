package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;

/** Stores the single TC4 aspect carried by a Mana Pod. */
public final class ManaPodBlockEntity extends BlockEntity {
    private Aspect aspect;

    public ManaPodBlockEntity(BlockPos pos, BlockState state) {
        super(ModMagicalForestContent.MANA_POD_ENTITY.get(), pos, state);
    }

    public Aspect getAspect() {
        return aspect;
    }

    public void setAspect(Aspect aspect) {
        this.aspect = aspect;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (aspect != null) tag.putString("aspect", aspect.getTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        aspect = tag.contains("aspect") ? Aspect.getAspect(tag.getString("aspect")) : null;
    }
}

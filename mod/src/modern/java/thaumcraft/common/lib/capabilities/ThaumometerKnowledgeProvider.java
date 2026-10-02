package thaumcraft.common.lib.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class ThaumometerKnowledgeProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<IThaumometerKnowledge> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final IThaumometerKnowledge knowledge = new ThaumometerKnowledge();
    private final LazyOptional<IThaumometerKnowledge> optional = LazyOptional.of(() -> knowledge);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        return CAPABILITY.orEmpty(capability, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return knowledge.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        knowledge.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}

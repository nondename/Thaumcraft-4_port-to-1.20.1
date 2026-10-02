package thaumcraft.common.lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Persistent player discoveries used by thaumometer scans. */
public interface IThaumometerKnowledge {
    boolean hasScannedItem(ResourceLocation itemId);

    void scanItem(ResourceLocation itemId);

    boolean hasDiscoveredAspect(Aspect aspect);

    boolean hasDiscoveredParents(Aspect aspect);

    void discoverAspect(Aspect aspect);

    int getAspectPool(Aspect aspect);

    int awardAspect(Aspect aspect, int amount);

    AspectList getDiscoveredAspects();

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);
}

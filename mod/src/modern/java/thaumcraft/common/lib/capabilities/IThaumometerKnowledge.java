package thaumcraft.common.lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.Set;

/** Persistent player discoveries used by thaumometer scans and research progression. */
public interface IThaumometerKnowledge {
    boolean hasScannedItem(ResourceLocation itemId);

    void scanItem(ResourceLocation itemId);

    boolean hasScannedEntity(ResourceLocation entityId);

    void scanEntity(ResourceLocation entityId);

    boolean hasDiscoveredAspect(Aspect aspect);

    boolean hasDiscoveredParents(Aspect aspect);

    void discoverAspect(Aspect aspect);

    int getAspectPool(Aspect aspect);

    int awardAspect(Aspect aspect, int amount);
    boolean spendAspects(AspectList cost);
    int getWarp();
    void addWarp(int permanent,int normal,int temporary);
    void decayTemporaryWarp();

    Aspect combine(Aspect first, Aspect second);

    AspectList getDiscoveredAspects();

    boolean hasResearch(String key);

    boolean grantResearch(String key);

    boolean revokeResearch(String key);

    void clearResearch();

    Set<String> getResearchKeys();

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);
}

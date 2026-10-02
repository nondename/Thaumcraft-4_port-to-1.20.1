package thaumcraft.common.lib.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.HashSet;
import java.util.Set;

/** Default TC4-style persistent aspect and scan knowledge for one player. */
public final class ThaumometerKnowledge implements IThaumometerKnowledge {
    private static final String TAG_SCANNED_ITEMS = "scannedItems";
    private static final String TAG_LEGACY_SCANNED_BLOCKS = "scannedBlocks";
    private static final String TAG_DISCOVERED_ASPECTS = "discoveredAspects";
    private static final int ASPECT_POOL_CAP = 100;

    private final Set<String> scannedItems = new HashSet<>();
    private final AspectList discoveredAspects = new AspectList();

    public ThaumometerKnowledge() {
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            discoveredAspects.add(aspect, 0);
        }
    }

    @Override
    public boolean hasScannedItem(ResourceLocation itemId) {
        return itemId != null && scannedItems.contains(itemId.toString());
    }

    @Override
    public void scanItem(ResourceLocation itemId) {
        if (itemId != null) {
            scannedItems.add(itemId.toString());
        }
    }

    @Override
    public boolean hasDiscoveredAspect(Aspect aspect) {
        return aspect != null && discoveredAspects.aspects.containsKey(aspect);
    }

    @Override
    public boolean hasDiscoveredParents(Aspect aspect) {
        if (aspect == null) {
            return false;
        }
        Aspect[] components = aspect.getComponents();
        if (components == null) {
            return true;
        }
        for (Aspect component : components) {
            if (!hasDiscoveredAspect(component)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void discoverAspect(Aspect aspect) {
        if (aspect != null && !hasDiscoveredAspect(aspect)) {
            discoveredAspects.add(aspect, 0);
        }
    }

    @Override
    public int getAspectPool(Aspect aspect) {
        return aspect == null ? 0 : Math.max(0, discoveredAspects.getAmount(aspect));
    }

    /** Mirrors TC4's first-discovery bonus and diminishing returns above the pool cap. */
    @Override
    public int awardAspect(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) {
            return 0;
        }
        int awarded = 0;
        if (!hasDiscoveredAspect(aspect)) {
            discoverAspect(aspect);
            amount += 2;
            awarded = amount;
        }
        int current = getAspectPool(aspect);
        if (current >= ASPECT_POOL_CAP) {
            amount = (int) Math.sqrt(amount);
        }
        if (amount > 1 && current >= ASPECT_POOL_CAP * 1.25F) {
            amount = 1;
        }
        if (amount > 0) {
            discoveredAspects.add(aspect, amount);
            awarded = amount;
        }
        return awarded;
    }

    @Override
    public AspectList getDiscoveredAspects() {
        return discoveredAspects.copy();
    }

    /** Same pool costs and discovery bonus as PacketAspectCombinationToServer in the reference. */
    @Override
    public Aspect combine(Aspect first, Aspect second) {
        if (first == null || second == null || !hasDiscoveredAspect(first) || !hasDiscoveredAspect(second)) return null;
        if (getAspectPool(first) < (first == second ? 2 : 1) || getAspectPool(second) < 1) return null;
        for (Aspect result : Aspect.getCompoundAspects()) {
            Aspect[] parents = result.getComponents();
            if ((parents[0] == first && parents[1] == second) || (parents[1] == first && parents[0] == second)) {
                discoveredAspects.aspects.put(first, getAspectPool(first) - 1);
                discoveredAspects.aspects.put(second, getAspectPool(second) - 1);
                awardAspect(result, 1);
                return result;
            }
        }
        return null;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        discoveredAspects.writeToNBT(tag, TAG_DISCOVERED_ASPECTS);
        ListTag items = new ListTag();
        for (String itemId : scannedItems) {
            items.add(net.minecraft.nbt.StringTag.valueOf(itemId));
        }
        tag.put(TAG_SCANNED_ITEMS, items);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        scannedItems.clear();
        discoveredAspects.aspects.clear();
        discoveredAspects.readFromNBT(tag, TAG_DISCOVERED_ASPECTS);
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            discoveredAspects.aspects.putIfAbsent(aspect, 0);
        }
        readScannedItems(tag.getList(TAG_SCANNED_ITEMS, Tag.TAG_STRING));
        // Keep discoveries written by the earlier block-ID based prototype.
        readScannedItems(tag.getList(TAG_LEGACY_SCANNED_BLOCKS, Tag.TAG_STRING));
    }

    private void readScannedItems(ListTag items) {
        for (int index = 0; index < items.size(); index++) {
            String value = items.getString(index);
            if (ResourceLocation.tryParse(value) != null) {
                scannedItems.add(value);
            }
        }
    }
}

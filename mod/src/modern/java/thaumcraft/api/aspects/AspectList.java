package thaumcraft.api.aspects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * Mutable Thaumcraft aspect/amount collection.
 *
 * Behaviour mirrors the maintained TC4 implementation while using the modern
 * Minecraft 1.20.1 NBT API.
 */
public class AspectList implements Serializable {
    public final LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();

    public AspectList() {
    }

    public AspectList copy() {
        AspectList out = new AspectList();
        for (Aspect aspect : getAspects()) {
            out.add(aspect, getAmount(aspect));
        }
        return out;
    }

    public int size() {
        return aspects.size();
    }

    public int visSize() {
        int amount = 0;
        for (Aspect aspect : aspects.keySet()) {
            amount += getAmount(aspect);
        }
        return amount;
    }

    public Aspect[] getAspects() {
        return aspects.keySet().toArray(new Aspect[1]);
    }

    public Aspect[] getPrimalAspects() {
        AspectList primals = new AspectList();
        for (Aspect aspect : aspects.keySet()) {
            if (aspect.isPrimal()) {
                primals.add(aspect, 1);
            }
        }
        return primals.aspects.keySet().toArray(new Aspect[1]);
    }

    public Aspect[] getAspectsSorted() {
        try {
            Aspect[] out = aspects.keySet().toArray(new Aspect[0]);
            boolean change;
            do {
                change = false;
                for (int index = 0; index < out.length - 1; index++) {
                    Aspect first = out[index];
                    Aspect second = out[index + 1];
                    if (first == null || second == null || first.getTag().compareTo(second.getTag()) <= 0) {
                        continue;
                    }
                    out[index] = second;
                    out[index + 1] = first;
                    change = true;
                }
            } while (change);
            return out;
        } catch (Exception ignored) {
            return getAspects();
        }
    }

    public Aspect[] getAspectsSortedAmount() {
        try {
            Aspect[] out = aspects.keySet().toArray(new Aspect[1]);
            boolean change;
            do {
                change = false;
                for (int index = 0; index < out.length - 1; index++) {
                    int firstAmount = getAmount(out[index]);
                    int secondAmount = getAmount(out[index + 1]);
                    if (firstAmount <= 0 || secondAmount <= 0 || secondAmount <= firstAmount) {
                        continue;
                    }
                    Aspect first = out[index];
                    out[index] = out[index + 1];
                    out[index + 1] = first;
                    change = true;
                }
            } while (change);
            return out;
        } catch (Exception ignored) {
            return getAspects();
        }
    }

    public int getAmount(Aspect key) {
        if (key == null) {
            return 0;
        }
        Integer amount = aspects.get(key);
        return amount == null ? 0 : amount;
    }

    public boolean reduce(Aspect key, int amount) {
        if (key == null) {
            return false;
        }
        if (getAmount(key) >= amount) {
            aspects.put(key, getAmount(key) - amount);
            return true;
        }
        return false;
    }

    public AspectList remove(Aspect key, int amount) {
        if (key == null) {
            return this;
        }
        int remaining = getAmount(key) - amount;
        if (remaining <= 0) {
            aspects.remove(key);
        } else {
            aspects.put(key, remaining);
        }
        return this;
    }

    public AspectList remove(Aspect key) {
        if (key != null) {
            aspects.remove(key);
        }
        return this;
    }

    public AspectList add(Aspect aspect, int amount) {
        if (aspect == null) {
            return this;
        }
        if (aspects.containsKey(aspect)) {
            amount += aspects.get(aspect);
        }
        aspects.put(aspect, amount);
        return this;
    }

    public AspectList merge(Aspect aspect, int amount) {
        if (aspect == null) {
            return this;
        }
        if (aspects.containsKey(aspect) && amount < aspects.get(aspect)) {
            amount = aspects.get(aspect);
        }
        aspects.put(aspect, amount);
        return this;
    }

    public AspectList add(AspectList input) {
        for (Aspect aspect : input.getAspects()) {
            add(aspect, input.getAmount(aspect));
        }
        return this;
    }

    public AspectList merge(AspectList input) {
        for (Aspect aspect : input.getAspects()) {
            merge(aspect, input.getAmount(aspect));
        }
        return this;
    }

    public void readFromNBT(CompoundTag tag) {
        readFromNBT(tag, "Aspects");
    }

    public void readFromNBT(CompoundTag tag, String label) {
        aspects.clear();
        ListTag list = tag.getList(label, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            if (!entry.contains("key", Tag.TAG_STRING)) {
                continue;
            }
            add(Aspect.getAspect(entry.getString("key")), entry.getInt("amount"));
        }
    }

    public void writeToNBT(CompoundTag tag) {
        writeToNBT(tag, "Aspects");
    }

    public void writeToNBT(CompoundTag tag, String label) {
        ListTag list = new ListTag();
        tag.put(label, list);
        for (Aspect aspect : getAspects()) {
            if (aspect == null) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putString("key", aspect.getTag());
            entry.putInt("amount", getAmount(aspect));
            list.add(entry);
        }
    }
}

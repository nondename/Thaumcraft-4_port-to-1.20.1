package thaumcraft.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.AspectList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Incremental 1.20.1 implementation of the public Thaumcraft API.
 *
 * TC4 object aspects are exposed through separate item and block registries.
 * A block may fall back to its item form when no block-specific entry exists,
 * but blocks without a useful item representation can still carry aspects.
 */
public final class ThaumcraftApi {
    public static final Map<Item, AspectList> objectTags = new ConcurrentHashMap<>();
    /** Exact NBT-specific tags; item-only tags remain the wildcard fallback. */
    private static final Map<ItemStackKey, AspectList> itemStackTags = new ConcurrentHashMap<>();
    public static final Map<Block, AspectList> blockTags = new ConcurrentHashMap<>();

    private ThaumcraftApi() {
    }

    public static void registerObjectTag(ItemStack stack, AspectList aspects) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.isEmpty()) {
            registerObjectTag(stack.getItem(), aspects);
            return;
        }
        itemStackTags.put(new ItemStackKey(stack.getItem(), tag.copy()), copyOrEmpty(aspects));
    }

    public static void registerObjectTag(Item item, AspectList aspects) {
        if (item == null) {
            return;
        }
        objectTags.put(item, copyOrEmpty(aspects));
    }

    public static void registerBlockTag(Block block, AspectList aspects) {
        if (block == null) {
            return;
        }
        blockTags.put(block, copyOrEmpty(aspects));
    }

    public static boolean exists(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemStackKey key = stackKey(stack);
        return (key != null && itemStackTags.containsKey(key)) || objectTags.containsKey(stack.getItem());
    }

    public static boolean exists(Block block) {
        return getBlockAspects(block) != null;
    }

    public static AspectList getObjectAspects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ItemStackKey key = stackKey(stack);
        AspectList aspects = key == null ? null : itemStackTags.get(key);
        if (aspects == null) {
            aspects = objectTags.get(stack.getItem());
        }
        return aspects == null ? null : aspects.copy();
    }

    public static AspectList getBlockAspects(Block block) {
        if (block == null) {
            return null;
        }

        AspectList direct = blockTags.get(block);
        if (direct != null) {
            return direct.copy();
        }

        Item item = block.asItem();
        if (item != net.minecraft.world.item.Items.AIR) {
            var holder = item.builtInRegistryHolder();
            if (holder.is(ItemTags.LOGS)) {
                return new AspectList().add(thaumcraft.api.aspects.Aspect.TREE, 4);
            }
            if (holder.is(ItemTags.PLANKS)) {
                return new AspectList().add(thaumcraft.api.aspects.Aspect.TREE, 1);
            }
            if (holder.is(ItemTags.LEAVES)) {
                return new AspectList().add(thaumcraft.api.aspects.Aspect.PLANT, 2)
                        .add(thaumcraft.api.aspects.Aspect.AIR, 1);
            }
            if (holder.is(ItemTags.SAPLINGS)) {
                return new AspectList().add(thaumcraft.api.aspects.Aspect.PLANT, 2)
                        .add(thaumcraft.api.aspects.Aspect.TREE, 1);
            }
        }
        AspectList fallback = objectTags.get(item);
        return fallback == null ? null : fallback.copy();
    }

    public static int getRegisteredObjectTagCount() {
        return objectTags.size() + itemStackTags.size();
    }

    public static int getRegisteredBlockTagCount() {
        return blockTags.size();
    }

    private static AspectList copyOrEmpty(AspectList aspects) {
        return aspects == null ? new AspectList() : aspects.copy();
    }

    private static ItemStackKey stackKey(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null || tag.isEmpty() ? null : new ItemStackKey(stack.getItem(), tag.copy());
    }

    private record ItemStackKey(Item item, CompoundTag tag) {
    }
}

package thaumcraft.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    public static final Map<Block, AspectList> blockTags = new ConcurrentHashMap<>();

    private ThaumcraftApi() {
    }

    public static void registerObjectTag(ItemStack stack, AspectList aspects) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        registerObjectTag(stack.getItem(), aspects);
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
        return stack != null && !stack.isEmpty() && objectTags.containsKey(stack.getItem());
    }

    public static boolean exists(Block block) {
        return getBlockAspects(block) != null;
    }

    public static AspectList getObjectAspects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        AspectList aspects = objectTags.get(stack.getItem());
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
        AspectList fallback = objectTags.get(item);
        return fallback == null ? null : fallback.copy();
    }

    public static int getRegisteredObjectTagCount() {
        return objectTags.size();
    }

    public static int getRegisteredBlockTagCount() {
        return blockTags.size();
    }

    private static AspectList copyOrEmpty(AspectList aspects) {
        return aspects == null ? new AspectList() : aspects.copy();
    }
}

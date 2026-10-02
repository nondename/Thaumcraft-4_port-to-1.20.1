package thaumcraft.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Incremental 1.20.1 implementation of the public Thaumcraft API.
 *
 * This first slice ports the object-aspect registry used by scanning,
 * essentia and recipe-derived aspect lookup. Additional TC4 API surface is
 * migrated here as the dependent systems are ported.
 */
public final class ThaumcraftApi {
    public static final Map<Item, AspectList> objectTags = new ConcurrentHashMap<>();

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
        objectTags.put(item, aspects == null ? new AspectList() : aspects.copy());
    }

    public static boolean exists(ItemStack stack) {
        return stack != null && !stack.isEmpty() && objectTags.containsKey(stack.getItem());
    }

    public static AspectList getObjectAspects(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        AspectList aspects = objectTags.get(stack.getItem());
        return aspects == null ? null : aspects.copy();
    }

    public static int getRegisteredObjectTagCount() {
        return objectTags.size();
    }
}

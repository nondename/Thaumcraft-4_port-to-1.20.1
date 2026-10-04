package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** TC4 Ethereal Essence: a stack carries the aspect payload in the legacy "Aspects" NBT list. */
public final class ItemWispEssence extends Item {
    public ItemWispEssence(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Aspect aspect, int amount) {
        ItemStack stack = new ItemStack(ModItems.WISP_ESSENCE.get());
        AspectList aspects = new AspectList();
        aspects.add(aspect, amount);
        aspects.writeToTag(stack.getOrCreateTag(), "Aspects");
        return stack;
    }

    public static AspectList getAspects(ItemStack stack) {
        AspectList aspects = new AspectList();
        if (stack.hasTag()) {
            aspects.readFromTag(stack.getTag(), "Aspects");
        }
        return aspects;
    }
}

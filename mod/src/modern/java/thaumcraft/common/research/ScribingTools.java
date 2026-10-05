package thaumcraft.common.research;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Ink is a refillable supply, not the lifetime of the pen and bottle. */
public final class ScribingTools extends Item {
    public ScribingTools(Properties properties) { super(properties); }
    public static boolean hasInk(ItemStack stack) {
        return stack.is(ModResearch.SCRIBING_TOOLS.get()) && stack.getDamageValue()<stack.getMaxDamage();
    }
    public static boolean consumeInk(ItemStack stack) {
        if(!hasInk(stack))return false;
        stack.setDamageValue(stack.getDamageValue()+1);
        return true;
    }
}

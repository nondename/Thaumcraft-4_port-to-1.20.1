package thaumcraft.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

/**
 * TC4 thaumcraft.api.IVisDiscountGear: gear granting a percentage vis discount
 * when casting from a wand. Signature mirrors the 1.7.10 interface.
 */
public interface IVisDiscountGear {
    int getVisDiscount(ItemStack stack, Player player, Aspect aspect);
}

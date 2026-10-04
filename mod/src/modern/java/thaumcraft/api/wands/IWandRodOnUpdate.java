package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Port of TC4 api/wands/IWandRodOnUpdate: periodic per-rod effect while the wand is in a player's inventory. */
public interface IWandRodOnUpdate {
    void onUpdate(ItemStack itemstack, Player player);
}

package thaumcraft.api.wands;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Port of TC4 api/wands/IWandable (1.7.10).
 *
 * <p>Modern adaptation: the original block-coordinate {@code onWandRightClick(world, stack,
 * player, x, y, z, side, md)} overload is dropped — block interactions now happen directly in
 * {@code ItemWand#useOn}. The tile flow is: ItemWand ray-tests aura node bounds (0.3..0.7,
 * from BlockAiry#setBlockBoundsBasedOnState), stores IIUX/Y/Z in the wand NBT (as
 * ItemWandCasting#setObjectInUse did) and starts the use animation; the block entity then
 * receives tick/stop callbacks exactly like the original IWandable tile contract.
 */
public interface IWandable {
    /** Called every 5th tick (count % 5 == 0) while the player right-click-holds the wand on this tile. */
    void onUsingWandTick(ItemStack wandstack, Player player, int count);

    /** Called when the player releases the wand (original: onWandStoppedUsing). */
    default void onWandStoppedUsing(ItemStack wandstack, Level level, Player player, int count) {
    }
}

package thaumcraft.api.nodes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * TC4 thaumcraft.api.nodes.IRevealer: gear that shows aura nodes to its wearer.
 * Worn in the helmet slot it renders the nodes through walls (TileNodeRenderer#186-191).
 */
public interface IRevealer {
    boolean showNodes(ItemStack stack, LivingEntity wearer);
}

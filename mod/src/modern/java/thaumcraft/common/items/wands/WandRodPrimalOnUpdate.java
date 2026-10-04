package thaumcraft.common.items.wands;

import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.common.items.tools.ItemWand;

/**
 * Port of TC4 common/items/wands/WandRodPrimalOnUpdate (1.7.10): rods with an aspect affinity
 * slowly refill that primal when the wand is below 10% capacity — aspect-specific rod every
 * 200 ticks (+1), the primal staff's random variant every 50 ticks.
 */
public class WandRodPrimalOnUpdate implements IWandRodOnUpdate {
    Aspect aspect;
    ArrayList<Aspect> primals;

    public WandRodPrimalOnUpdate(Aspect aspect) {
        this.aspect = aspect;
    }

    public WandRodPrimalOnUpdate() {
        this.aspect = null;
        this.primals = Aspect.getPrimalAspects();
    }

    @Override
    public void onUpdate(ItemStack itemstack, Player player) {
        if (this.aspect != null) {
            if (player.tickCount % 200 == 0
                    && ItemWand.getVis(itemstack, aspect) < ItemWand.getMaxVis(itemstack) / 10) {
                ItemWand.addVis(itemstack, aspect, 1, true);
            }
        } else if (player.tickCount % 50 == 0) {
            ArrayList<Aspect> q = new ArrayList<>();
            for (Aspect as : this.primals) {
                if (ItemWand.getVis(itemstack, as) < ItemWand.getMaxVis(itemstack) / 10) {
                    q.add(as);
                }
            }
            if (!q.isEmpty()) {
                ItemWand.addVis(itemstack, q.get(player.level().random.nextInt(q.size())), 1, true);
            }
        }
    }
}

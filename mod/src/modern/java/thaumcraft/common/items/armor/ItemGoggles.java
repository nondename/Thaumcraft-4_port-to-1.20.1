package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.IRevealer;

/**
 * Goggles of Revealing — port of TC4 ItemGoggles (decompiled): a rare helmet repaired
 * with a gold ingot, granting a 5% vis discount and, through {@link IRevealer}, the
 * through-wall aura node readout. Durability 350 and helmet defense 1 match the
 * original "SPECIAL" armor material; the worn layer keeps the vanilla material
 * texture until a custom armor layer is ported.
 */
public class ItemGoggles extends ArmorItem implements IVisDiscountGear, IRevealer {

    public ItemGoggles(Properties properties) {
        // TC4 armorMatSpecial {1,3,2,1}: the helmet piece defends 1, like leather.
        super(ArmorMaterials.LEATHER, Type.HELMET, properties);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE; // TC4 ItemGoggles#getRarity
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        // TC4 ItemGoggles#63-64: repaired with gold ingots; the SPECIAL material had no
        // vanilla repair ingredient, so gold is the only option here as well.
        return repairCandidate.is(Items.GOLD_INGOT);
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) {
        return 5; // TC4 ItemGoggles#getVisDiscount
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity wearer) {
        return true; // TC4 ItemGoggles#showNodes
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // TC4 ItemGoggles#40-43: one dark purple "tc.visdiscount: 5%" line.
        tooltip.add(Component.literal(ChatFormatting.DARK_PURPLE
                + Component.translatable("tc.visdiscount").getString() + ": "
                + getVisDiscount(stack, null, null) + "%"));
    }
}

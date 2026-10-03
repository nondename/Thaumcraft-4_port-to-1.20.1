package thaumcraft.common.items.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * TC4's {@code ThaumcraftApi.armorMatSpecial} as a 1.20.1 {@link ArmorMaterial}:
 * {@code EnumHelper.addArmorMaterial("SPECIAL", 25, {1,3,2,1}, 25)} in the original.
 *
 * <p>The material name is namespace-qualified on purpose — Forge's
 * {@code HumanoidArmorLayer.getArmorResource} splits it on {@code ':'} and resolves
 * {@code thaumcraft:textures/models/armor/goggles_layer_1.png}, which holds the original
 * worn texture ({@code textures/models/goggles.png}, md5 5B4E07B173FB3C4E14A630D7881BE69B
 * across decompiled/, thaumcraft_src/ and the legacy src/main copy). The equip sound is
 * the leather one: the original 4-argument EnumHelper default for "SPECIAL" is not
 * recoverable from the repository.
 */
public final class GogglesArmorMaterial implements ArmorMaterial {
    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        // Not used: ItemGoggles pins durability to the original 350 via Item.Properties.
        return 350;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        // TC4 armorMatSpecial reduction {1,3,2,1} — the helmet piece defends 1.
        return switch (type) {
            case HELMET -> 1;
            case CHESTPLATE -> 3;
            case LEGGINGS -> 2;
            case BOOTS -> 1;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 25; // TC4 armorMatSpecial enchantability
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.GOLD_INGOT); // ItemGoggles repairs with gold (override wins anyway).
    }

    @Override
    public String getName() {
        return "thaumcraft:goggles";
    }

    @Override
    public float getToughness() {
        return 0.0F; // Toughness did not exist in 1.7.10.
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}

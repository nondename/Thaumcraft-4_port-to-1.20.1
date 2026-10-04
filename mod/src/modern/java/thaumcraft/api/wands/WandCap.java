package thaumcraft.api.wands;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

/**
 * Port of TC4 api/wands/WandCap (1.7.10, read from decompiled/): registry of wand cap
 * variants — vis cost modifiers and craft cost (craft cost doubles as the per-primal
 * aspect cost of the arcane wand recipe: cost = capCost x rodCost).
 *
 * <p>Documented deviation: the representative item is a {@code Supplier<Item>} instead of a
 * fixed ItemStack, because 1.20.1 subtypes are separate registry ids (see ModWandParts) and
 * the registry entries are built at class-load time, before items freeze.
 */
public class WandCap {
    private String tag;
    private int craftCost;
    float baseCostModifier;
    List<Aspect> specialCostModifierAspects;
    float specialCostModifier;
    ResourceLocation texture;
    Supplier<Item> item;
    public static LinkedHashMap<String, WandCap> caps = new LinkedHashMap<>();

    public WandCap(String tag, float discount, Supplier<Item> item, int craftCost) {
        this.setTag(tag);
        this.baseCostModifier = discount;
        this.specialCostModifierAspects = null;
        this.texture = new ResourceLocation("thaumcraft", "textures/models/wand_cap_" + this.getTag() + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        caps.put(tag, this);
    }

    public WandCap(String tag, float discount, List<Aspect> specialAspects, float discountSpecial, Supplier<Item> item, int craftCost) {
        this.setTag(tag);
        this.baseCostModifier = discount;
        this.specialCostModifierAspects = specialAspects;
        this.specialCostModifier = discountSpecial;
        this.texture = new ResourceLocation("thaumcraft", "textures/models/wand_cap_" + this.getTag() + ".png");
        this.item = item;
        this.setCraftCost(craftCost);
        caps.put(tag, this);
    }

    public float getBaseCostModifier() {
        return this.baseCostModifier;
    }

    public List<Aspect> getSpecialCostModifierAspects() {
        return this.specialCostModifierAspects;
    }

    public float getSpecialCostModifier() {
        return this.specialCostModifier;
    }

    public ResourceLocation getTexture() {
        return this.texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public ItemStack getItem() {
        return new ItemStack(this.item.get());
    }

    public void setItem(Supplier<Item> item) {
        this.item = item;
    }

    /** True when the stack is this cap's item (original compared ItemStacks). */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == this.item.get();
    }

    public int getCraftCost() {
        return this.craftCost;
    }

    public void setCraftCost(int craftCost) {
        this.craftCost = craftCost;
    }

    public String getResearch() {
        return "CAP_" + this.getTag();
    }
}

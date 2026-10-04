package thaumcraft.api.wands;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Port of TC4 api/wands/StaffRod (1.7.10): a rod registered under {@code tag + "_staff"} that
 * reuses the wand rod texture ({@code textures/models/wand_rod_<tag>.png}) and can carry runes.
 * Data-only in this slice — staff mechanics (capacity x1.5 wand usage, recipes) are deferred.
 */
public class StaffRod extends WandRod {
    boolean runes = false;

    public StaffRod(String tag, int capacity, Supplier<Item> item, int craftCost) {
        super(tag + "_staff", capacity, item, craftCost);
        this.texture = new ResourceLocation("thaumcraft", "textures/models/wand_rod_" + tag + ".png");
    }

    public StaffRod(String tag, int capacity, Supplier<Item> item, int craftCost, IWandRodOnUpdate onUpdate, ResourceLocation texture) {
        super(tag + "_staff", capacity, item, craftCost, onUpdate, texture);
    }

    public StaffRod(String tag, int capacity, Supplier<Item> item, int craftCost, IWandRodOnUpdate onUpdate) {
        super(tag + "_staff", capacity, item, craftCost, onUpdate);
        this.texture = new ResourceLocation("thaumcraft", "textures/models/wand_rod_" + tag + ".png");
    }

    public StaffRod(String tag, int capacity, Supplier<Item> item, int craftCost, ResourceLocation texture) {
        super(tag + "_staff", capacity, item, craftCost, texture);
    }

    public boolean hasRunes() {
        return this.runes;
    }

    public void setRunes(boolean runes) {
        this.runes = runes;
    }
}

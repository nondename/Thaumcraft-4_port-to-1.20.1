package thaumcraft.common.items.wands;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;

/**
 * Wand parts — port of the wand section of TC4 ConfigItems (1.7.10).
 *
 * <p>1.7.10 had two subtype items (ItemWandCap: damage 0-8; ItemWandRod: damage 0-7, 50-57,
 * 100). Here every subtype is its own registry id named after the original texture
 * (wand_cap_iron, wand_rod_greatwood, staff_rod_primal...), and the WandCap/WandRod registry
 * data (tags, capacities, cost modifiers, craft costs, onUpdate affinities) is copied 1:1 from
 * ConfigItems#init/postInit. The wood rod maps to vanilla stick (original WAND_ROD_WOOD item
 * = Items.stick). Copper/silver caps were config-gated (foundCopperIngot/foundSilverIngot) —
 * modern port registers them unconditionally (documented deviation; no such config exists).
 * Staff cores are data-only in this slice: no staff mechanics (user decision 2026-10-04).
 */
public final class ModWandParts {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Thaumcraft.MODID);

    // ---- caps (ids = original ItemWandCap.registerIcons names) ----
    public static final RegistryObject<Item> CAP_IRON = cap("iron");
    public static final RegistryObject<Item> CAP_GOLD = cap("gold");
    public static final RegistryObject<Item> CAP_THAUMIUM = cap("thaumium");
    public static final RegistryObject<Item> CAP_COPPER = cap("copper");
    public static final RegistryObject<Item> CAP_SILVER = cap("silver");
    public static final RegistryObject<Item> CAP_SILVER_INERT = cap("silver_inert");
    public static final RegistryObject<Item> CAP_THAUMIUM_INERT = cap("thaumium_inert");
    public static final RegistryObject<Item> CAP_VOID = cap("void");
    public static final RegistryObject<Item> CAP_VOID_INERT = cap("void_inert");

    // ---- wand rod cores (ids = original ItemWandRod damage 0-7 names) ----
    public static final RegistryObject<Item> ROD_GREATWOOD = rod("greatwood");
    public static final RegistryObject<Item> ROD_OBSIDIAN = rod("obsidian");
    public static final RegistryObject<Item> ROD_SILVERWOOD = rod("silverwood");
    public static final RegistryObject<Item> ROD_ICE = rod("ice");
    public static final RegistryObject<Item> ROD_QUARTZ = rod("quartz");
    public static final RegistryObject<Item> ROD_REED = rod("reed");
    public static final RegistryObject<Item> ROD_BLAZE = rod("blaze");
    public static final RegistryObject<Item> ROD_BONE = rod("bone");

    // ---- staff cores (ids = original ItemWandRod damage 50-57, 100 names) ----
    public static final RegistryObject<Item> STAFF_GREATWOOD = staff("greatwood");
    public static final RegistryObject<Item> STAFF_OBSIDIAN = staff("obsidian");
    public static final RegistryObject<Item> STAFF_SILVERWOOD = staff("silverwood");
    public static final RegistryObject<Item> STAFF_ICE = staff("ice");
    public static final RegistryObject<Item> STAFF_QUARTZ = staff("quartz");
    public static final RegistryObject<Item> STAFF_REED = staff("reed");
    public static final RegistryObject<Item> STAFF_BLAZE = staff("blaze");
    public static final RegistryObject<Item> STAFF_BONE = staff("bone");
    public static final RegistryObject<Item> STAFF_PRIMAL = ITEMS.register(
            "staff_rod_primal", () -> new Item(new Item.Properties()));

    private static RegistryObject<Item> cap(String tag) {
        return ITEMS.register("wand_cap_" + tag, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> rod(String tag) {
        return ITEMS.register("wand_rod_" + tag, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> staff(String tag) {
        return ITEMS.register("staff_rod_" + tag, () -> new Item(new Item.Properties()));
    }

    // ---- registry data: ConfigItems#init (lines 287-311) + postInit (314-324) ----
    /** Caps: tag, base cost modifier, [special aspects, special modifier], item, craft cost. */
    public static final WandCap WAND_CAP_IRON = new WandCap("iron", 1.1F, () -> CAP_IRON.get(), 1);
    public static final WandCap WAND_CAP_GOLD = new WandCap("gold", 1.0F, () -> CAP_GOLD.get(), 3);
    public static final WandCap WAND_CAP_THAUMIUM = new WandCap("thaumium", 0.9F, () -> CAP_THAUMIUM.get(), 6);
    public static final WandCap WAND_CAP_VOID = new WandCap("void", 0.8F, () -> CAP_VOID.get(), 9);
    public static final WandCap WAND_CAP_COPPER = new WandCap(
            "copper", 1.1F, List.of(Aspect.ORDER, Aspect.ENTROPY), 1.0F, () -> CAP_COPPER.get(), 2);
    public static final WandCap WAND_CAP_SILVER = new WandCap(
            "silver", 1.0F, List.of(Aspect.AIR, Aspect.EARTH, Aspect.FIRE, Aspect.WATER), 0.95F, () -> CAP_SILVER.get(), 4);

    /** Rods: tag, capacity (whole vis), item, craft cost, [primal affinity]. */
    public static final WandRod WAND_ROD_WOOD = new WandRod("wood", 25, () -> Items.STICK, 1);
    public static final WandRod WAND_ROD_GREATWOOD = new WandRod("greatwood", 50, () -> ROD_GREATWOOD.get(), 3);
    public static final WandRod WAND_ROD_OBSIDIAN = new WandRod(
            "obsidian", 75, () -> ROD_OBSIDIAN.get(), 6, new WandRodPrimalOnUpdate(Aspect.EARTH));
    public static final WandRod WAND_ROD_BLAZE = new WandRod(
            "blaze", 75, () -> ROD_BLAZE.get(), 6, new WandRodPrimalOnUpdate(Aspect.FIRE));
    public static final WandRod WAND_ROD_ICE = new WandRod(
            "ice", 75, () -> ROD_ICE.get(), 6, new WandRodPrimalOnUpdate(Aspect.WATER));
    public static final WandRod WAND_ROD_QUARTZ = new WandRod(
            "quartz", 75, () -> ROD_QUARTZ.get(), 6, new WandRodPrimalOnUpdate(Aspect.ORDER));
    public static final WandRod WAND_ROD_BONE = new WandRod(
            "bone", 75, () -> ROD_BONE.get(), 6, new WandRodPrimalOnUpdate(Aspect.ENTROPY));
    public static final WandRod WAND_ROD_REED = new WandRod(
            "reed", 75, () -> ROD_REED.get(), 6, new WandRodPrimalOnUpdate(Aspect.AIR));
    public static final WandRod WAND_ROD_SILVERWOOD = new WandRod("silverwood", 100, () -> ROD_SILVERWOOD.get(), 9);
    // original line 300: WAND_ROD_BLAZE.setGlowing(true)
    static {
        WAND_ROD_BLAZE.setGlowing(true);
    }

    /** Staff rods: StaffRod ctor prefixes "_staff" to the tag (as in the original). */
    public static final StaffRod STAFF_ROD_GREATWOOD = new StaffRod("greatwood", 125, () -> STAFF_GREATWOOD.get(), 8);
    public static final StaffRod STAFF_ROD_OBSIDIAN = new StaffRod(
            "obsidian", 175, () -> STAFF_OBSIDIAN.get(), 14, new WandRodPrimalOnUpdate(Aspect.EARTH));
    public static final StaffRod STAFF_ROD_BLAZE = new StaffRod(
            "blaze", 175, () -> STAFF_BLAZE.get(), 14, new WandRodPrimalOnUpdate(Aspect.FIRE));
    public static final StaffRod STAFF_ROD_ICE = new StaffRod(
            "ice", 175, () -> STAFF_ICE.get(), 14, new WandRodPrimalOnUpdate(Aspect.WATER));
    public static final StaffRod STAFF_ROD_QUARTZ = new StaffRod(
            "quartz", 175, () -> STAFF_QUARTZ.get(), 14, new WandRodPrimalOnUpdate(Aspect.ORDER));
    public static final StaffRod STAFF_ROD_BONE = new StaffRod(
            "bone", 175, () -> STAFF_BONE.get(), 14, new WandRodPrimalOnUpdate(Aspect.ENTROPY));
    public static final StaffRod STAFF_ROD_REED = new StaffRod(
            "reed", 175, () -> STAFF_REED.get(), 14, new WandRodPrimalOnUpdate(Aspect.AIR));
    public static final StaffRod STAFF_ROD_SILVERWOOD = new StaffRod("silverwood", 250, () -> STAFF_SILVERWOOD.get(), 24);
    public static final StaffRod STAFF_ROD_PRIMAL = new StaffRod(
            "primal", 250, () -> STAFF_PRIMAL.get(), 32, new WandRodPrimalOnUpdate());
    // original lines 310-311
    static {
        STAFF_ROD_PRIMAL.setRunes(true);
        STAFF_ROD_BLAZE.setGlowing(true);
    }

    private ModWandParts() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

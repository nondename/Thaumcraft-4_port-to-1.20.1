package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.tools.ItemThaumometer;

/**
 * Forge item registry for the incremental 1.20.1 port.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Thaumcraft.MODID);

    public static final RegistryObject<Item> THAUMOMETER = ITEMS.register(
            "thaumometer",
            () -> new ItemThaumometer(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> WISP_ESSENCE = ITEMS.register(
            "wisp_essence",
            () -> new ItemWispEssence(new Item.Properties())
    );

    // TC4 wand caps live under their original texture ids (see ModWandParts); the iron cap
    // was renamed from "iron_wand_cap" to "wand_cap_iron" together with its family.
    public static final RegistryObject<Item> WAND = ITEMS.register("wand",
            () -> new thaumcraft.common.items.tools.ItemWand(
                    thaumcraft.common.items.tools.ItemWand.wandProperties().stacksTo(1)));
    public static final RegistryObject<Item> THAUMONOMICON = ITEMS.register("thaumonomicon", () -> new thaumcraft.common.items.tools.ItemThaumonomicon(new Item.Properties().stacksTo(1)));

    // TC4 ItemGoggles: durability 350 (Item.Properties#durability wins over the material default).
    public static final RegistryObject<Item> GOGGLES = ITEMS.register(
            "gogglesrevealing",
            () -> new thaumcraft.common.items.armor.ItemGoggles(new Item.Properties().durability(350))
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}

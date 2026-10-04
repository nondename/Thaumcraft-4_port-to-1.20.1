package thaumcraft.common.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.common.blocks.ModOres;

/** TC4 resource/nugget subtypes flattened into stable modern item identifiers. */
public final class ModMetals {
    public static final RegistryObject<Item> THAUMIUM_INGOT = metal("thaumium_ingot");
    public static final RegistryObject<Item> THAUMIUM_NUGGET = metal("thaumium_nugget");
    public static final RegistryObject<Item> VOID_INGOT = metal("void_ingot");
    public static final RegistryObject<Item> VOID_NUGGET = metal("void_nugget");
    public static final RegistryObject<Block> THAUMIUM_BLOCK = ModOres.BLOCKS.register("thaumium_block", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0F, 10.0F)));
    public static final RegistryObject<Item> THAUMIUM_BLOCK_ITEM = ModItems.ITEMS.register("thaumium_block", () ->
            new BlockItem(THAUMIUM_BLOCK.get(), new Item.Properties()));

    private static RegistryObject<Item> metal(String name) {
        return ModItems.ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    /** Force definitions to load before the shared registries are attached to Forge. */
    public static void init() {}
    private ModMetals() {}
}

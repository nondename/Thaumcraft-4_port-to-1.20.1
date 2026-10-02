package thaumcraft.common.research;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ModOres;
import thaumcraft.common.items.ModItems;

public final class ModResearch {
    public static final RegistryObject<Block> TABLE = ModOres.BLOCKS.register("table", () ->
            new ResearchTableBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2).noOcclusion()));
    public static final RegistryObject<Item> TABLE_ITEM = ModItems.ITEMS.register("table", () -> new BlockItem(TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> PHIAL = ModItems.ITEMS.register("phial", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SCRIBING_TOOLS = ModItems.ITEMS.register("scribing_tools", () -> new Item(new Item.Properties().durability(100)));
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Thaumcraft.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Thaumcraft.MODID);
    public static final RegistryObject<BlockEntityType<ResearchTableEntity>> TABLE_ENTITY = ENTITIES.register("table", () ->
            BlockEntityType.Builder.of(ResearchTableEntity::new, TABLE.get()).build(null));
    public static final RegistryObject<MenuType<ResearchMenu>> MENU = MENUS.register("research", () ->
            new MenuType<>(ResearchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(IEventBus bus) { ENTITIES.register(bus); MENUS.register(bus); }
    private ModResearch() {}
}

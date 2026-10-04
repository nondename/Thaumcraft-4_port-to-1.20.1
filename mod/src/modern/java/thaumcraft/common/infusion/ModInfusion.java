package thaumcraft.common.infusion;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumcraft.common.blocks.ModOres;
import thaumcraft.common.items.ModItems;

public final class ModInfusion {
    public static final RegistryObject<Item> GREATWOOD_PLANKS_ITEM=thaumcraft.common.blocks.ModMagicalTrees.GREATWOOD_PLANKS_ITEM;
    public static final RegistryObject<Item> SILVERWOOD_PLANKS_ITEM=thaumcraft.common.blocks.ModMagicalTrees.SILVERWOOD_PLANKS_ITEM;
    public static final RegistryObject<Block> ARCANE_STONE=ModOres.BLOCKS.register("arcane_stone",() -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));
    public static final RegistryObject<Item> ARCANE_STONE_ITEM=item("arcane_stone",ARCANE_STONE);
    public static final RegistryObject<Block> PEDESTAL=block("arcane_pedestal","pedestal");
    public static final RegistryObject<Item> PEDESTAL_ITEM=item("arcane_pedestal",PEDESTAL);
    public static final RegistryObject<Block> MATRIX=block("runic_matrix","matrix");
    public static final RegistryObject<Item> MATRIX_ITEM=item("runic_matrix",MATRIX);
    public static final RegistryObject<Block> FURNACE=block("alchemy_furnace","furnace");
    public static final RegistryObject<Item> FURNACE_ITEM=item("alchemy_furnace",FURNACE);
    public static final RegistryObject<Block> ALEMBIC=block("alembic","alembic");
    public static final RegistryObject<Item> ALEMBIC_ITEM=item("alembic",ALEMBIC);
    public static final RegistryObject<Block> JAR=block("warded_jar","jar");
    public static final RegistryObject<Item> JAR_ITEM=item("warded_jar",JAR);
    public static final RegistryObject<Item> FILTER=ModItems.ITEMS.register("essentia_filter",() -> new Item(new Item.Properties()));
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumcraft");
    public static final RegistryObject<BlockEntityType<PedestalEntity>> PEDESTAL_ENTITY=ENTITIES.register("arcane_pedestal",() -> BlockEntityType.Builder.of(PedestalEntity::new,PEDESTAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<EssentiaStoreEntity>> STORE_ENTITY=ENTITIES.register("essentia_store",() -> BlockEntityType.Builder.of(EssentiaStoreEntity::new,JAR.get(),ALEMBIC.get()).build(null));
    public static final RegistryObject<BlockEntityType<AlchemyFurnaceEntity>> FURNACE_ENTITY=ENTITIES.register("alchemy_furnace",() -> BlockEntityType.Builder.of(AlchemyFurnaceEntity::new,FURNACE.get()).build(null));
    public static final RegistryObject<BlockEntityType<InfusionMatrixEntity>> MATRIX_ENTITY=ENTITIES.register("runic_matrix",() -> BlockEntityType.Builder.of(InfusionMatrixEntity::new,MATRIX.get()).build(null));
    public static final RegistryObject<MenuType<AlchemyMenu>> MENU=MENUS.register("alchemy_furnace",() -> IForgeMenuType.create((id,inventory,buffer) -> {
        buffer.readBlockPos();return new AlchemyMenu(id,inventory,new net.minecraft.world.SimpleContainer(2),net.minecraft.world.inventory.ContainerLevelAccess.NULL);
    }));
    private static RegistryObject<Block> block(String id,String kind) {return ModOres.BLOCKS.register(id,() -> new InfusionDeviceBlock(kind,BlockBehaviour.Properties.copy(Blocks.STONE).noOcclusion()));}
    private static RegistryObject<Item> item(String id,RegistryObject<Block> block) {return ModItems.ITEMS.register(id,() -> new BlockItem(block.get(),new Item.Properties()));}
    public static void register(IEventBus bus) {ENTITIES.register(bus);MENUS.register(bus);InfusionRecipe.init();}
    private ModInfusion() {}
}

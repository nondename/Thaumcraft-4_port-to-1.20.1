package thaumcraft.common.alchemy;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ModOres;
import thaumcraft.common.items.ModItems;

public final class ModAlchemy {
    public static final RegistryObject<Block> NITOR = ModOres.BLOCKS.register("nitor", () ->
            new NitorBlock(BlockBehaviour.Properties.copy(Blocks.TORCH).noCollission().noOcclusion().lightLevel(s -> 15)));
    public static final RegistryObject<Item> NITOR_ITEM = ModItems.ITEMS.register("nitor", () -> new BlockItem(NITOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALUMENTUM = ModItems.ITEMS.register("alumentum", () -> new Item(new Item.Properties()) {
        @Override public int getBurnTime(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> type) { return 6400; }
    });
    public static final RegistryObject<Item> BALANCED_SHARD = ModItems.ITEMS.register("balanced_shard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SALIS_MUNDUS = ModItems.ITEMS.register("salis_mundus", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Block> FLUX_GAS = ModOres.BLOCKS.register("flux_gas", () ->
            new FluxBlock(true, BlockBehaviour.Properties.copy(Blocks.COBWEB).noCollission().noOcclusion().noLootTable().strength(0).randomTicks()));
    public static final RegistryObject<Block> FLUX_GOO = ModOres.BLOCKS.register("flux_goo", () ->
            new FluxBlock(false, BlockBehaviour.Properties.copy(Blocks.COBWEB).noCollission().noOcclusion().noLootTable().strength(0).randomTicks()));
    public static final RegistryObject<Block> CRUCIBLE = ModOres.BLOCKS.register("crucible", () ->
            new CrucibleBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON).noOcclusion()));
    public static final RegistryObject<Item> CRUCIBLE_ITEM = ModItems.ITEMS.register("crucible", () ->
            new BlockItem(CRUCIBLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> VOID_SEED = ModItems.ITEMS.register("void_seed", () -> new Item(new Item.Properties()));
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Thaumcraft.MODID);
    public static final RegistryObject<BlockEntityType<CrucibleEntity>> ENTITY = ENTITIES.register("crucible", () ->
            BlockEntityType.Builder.of(CrucibleEntity::new, CRUCIBLE.get()).build(null));
    public static void register(IEventBus bus) { ENTITIES.register(bus); }
    private ModAlchemy() {}
}

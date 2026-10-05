package thaumcraft.common.blocks;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ManaBeanItem;
import thaumcraft.common.items.ModItems;

/** TC4 Magical Forest flora that is not part of the magical-tree family. */
public final class ModMagicalForestContent {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Thaumcraft.MODID);

    public static final RegistryObject<Block> MANA_POD = ModOres.BLOCKS.register("mana_pod", () ->
            new ManaPodBlock(BlockBehaviour.Properties.copy(Blocks.COCOA)
                    .strength(0.5F)
                    .randomTicks()
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(ManaPodBlock.AGE))));

    public static final RegistryObject<Block> MANASHROOM = ModOres.BLOCKS.register("manashroom", () ->
            new ManashroomBlock(BlockBehaviour.Properties.copy(Blocks.RED_MUSHROOM)
                    .noOcclusion()
                    .lightLevel(state -> 8)));

    public static final RegistryObject<Item> MANA_BEAN = ModItems.ITEMS.register("mana_bean", () ->
            new ManaBeanItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<Item> MANASHROOM_ITEM = ModItems.ITEMS.register("manashroom", () ->
            new net.minecraft.world.item.BlockItem(MANASHROOM.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<ManaPodBlockEntity>> MANA_POD_ENTITY =
            BLOCK_ENTITIES.register("mana_pod", () ->
                    BlockEntityType.Builder.of(ManaPodBlockEntity::new, MANA_POD.get()).build(null));

    /** Force block/item definitions before their shared DeferredRegisters are attached. */
    public static void init() {}

    public static void registerBlockEntities(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModMagicalForestContent() {}
}

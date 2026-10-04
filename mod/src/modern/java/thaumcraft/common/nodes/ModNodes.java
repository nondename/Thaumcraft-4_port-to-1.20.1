package thaumcraft.common.nodes;

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
import thaumcraft.common.blocks.ModOres;
import thaumcraft.common.items.ItemAuraNode;
import thaumcraft.common.items.ModItems;

/** Registry entries for the TC4 aura-node system. */
public final class ModNodes {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Thaumcraft.MODID);

    public static final RegistryObject<Block> AURA_NODE = ModOres.BLOCKS.register("aura_node", () ->
            new AuraNodeBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)
                    .noCollission()
                    .noOcclusion()
                    .strength(2.0F, 200.0F)
                    .lightLevel(state -> 8)));

    /**
     * TC4 exposes exactly one metadata-0 BlockAiry item in its creative tab. Placing it creates
     * a randomly generated node; node type/modifier variants are not separate creative items.
     */
    public static final RegistryObject<Item> AURA_NODE_ITEM = ModItems.ITEMS.register("aura_node", () ->
            new ItemAuraNode(AURA_NODE.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockEntityType<AuraNodeBlockEntity>> AURA_NODE_ENTITY =
            BLOCK_ENTITIES.register("aura_node", () ->
                    BlockEntityType.Builder.of(AuraNodeBlockEntity::new, AURA_NODE.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModNodes() {
    }
}

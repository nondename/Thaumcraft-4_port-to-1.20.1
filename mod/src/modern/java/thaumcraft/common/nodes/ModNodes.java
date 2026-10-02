package thaumcraft.common.nodes;

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

/** Registry entries for the TC4 aura-node system. */
public final class ModNodes {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Thaumcraft.MODID);

    public static final RegistryObject<Block> AURA_NODE = ModOres.BLOCKS.register("aura_node", () ->
            new AuraNodeBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)));

    public static final RegistryObject<BlockEntityType<AuraNodeBlockEntity>> AURA_NODE_ENTITY =
            BLOCK_ENTITIES.register("aura_node", () ->
                    BlockEntityType.Builder.of(AuraNodeBlockEntity::new, AURA_NODE.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModNodes() {
    }
}

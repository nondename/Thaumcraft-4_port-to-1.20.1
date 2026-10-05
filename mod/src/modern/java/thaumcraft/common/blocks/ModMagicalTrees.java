package thaumcraft.common.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.ModItems;

/**
 * TC4 magical wood family, flattened from BlockMagicalLog/BlockMagicalLeaves/BlockCustomPlant metadata.
 */
public final class ModMagicalTrees {
    public static final ResourceKey<ConfiguredFeature<?, ?>> GREATWOOD_TREE_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, new ResourceLocation(Thaumcraft.MODID, "greatwood_tree"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> SILVERWOOD_TREE_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, new ResourceLocation(Thaumcraft.MODID, "silverwood_tree"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> SILVERWOOD_SAPLING_TREE_KEY = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, new ResourceLocation(Thaumcraft.MODID, "silverwood_tree_sapling"));

    public static final RegistryObject<Block> GREATWOOD_LOG = ModOres.BLOCKS.register("greatwood_log", () ->
            new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG).strength(2.5F)));
    public static final RegistryObject<Block> SILVERWOOD_LOG = ModOres.BLOCKS.register("silverwood_log", () ->
            new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG).strength(2.5F).lightLevel(s -> 0)));

    public static final RegistryObject<Block> GREATWOOD_PLANKS = ModOres.BLOCKS.register("greatwood_planks", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
    public static final RegistryObject<Block> SILVERWOOD_PLANKS = ModOres.BLOCKS.register("silverwood_planks", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));

    public static final RegistryObject<Block> GREATWOOD_LEAVES = ModOres.BLOCKS.register("greatwood_leaves", () ->
            new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
    public static final RegistryObject<Block> SILVERWOOD_LEAVES = ModOres.BLOCKS.register("silverwood_leaves", () ->
            new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).lightLevel(s -> 7)));

    public static final RegistryObject<Block> GREATWOOD_SAPLING = ModOres.BLOCKS.register("greatwood_sapling", () ->
            new SaplingBlock(new GreatwoodGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
    public static final RegistryObject<Block> SILVERWOOD_SAPLING = ModOres.BLOCKS.register("silverwood_sapling", () ->
            new SaplingBlock(new SilverwoodGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
    public static final RegistryObject<Block> SHIMMERLEAF = ModOres.BLOCKS.register("shimmerleaf", ShimmerleafBlock::new);

    public static final RegistryObject<Item> GREATWOOD_LOG_ITEM = blockItem("greatwood_log", GREATWOOD_LOG);
    public static final RegistryObject<Item> SILVERWOOD_LOG_ITEM = blockItem("silverwood_log", SILVERWOOD_LOG);
    public static final RegistryObject<Item> GREATWOOD_PLANKS_ITEM = blockItem("greatwood_planks", GREATWOOD_PLANKS);
    public static final RegistryObject<Item> SILVERWOOD_PLANKS_ITEM = blockItem("silverwood_planks", SILVERWOOD_PLANKS);
    public static final RegistryObject<Item> GREATWOOD_LEAVES_ITEM = blockItem("greatwood_leaves", GREATWOOD_LEAVES);
    public static final RegistryObject<Item> SILVERWOOD_LEAVES_ITEM = blockItem("silverwood_leaves", SILVERWOOD_LEAVES);
    public static final RegistryObject<Item> GREATWOOD_SAPLING_ITEM = blockItem("greatwood_sapling", GREATWOOD_SAPLING);
    public static final RegistryObject<Item> SILVERWOOD_SAPLING_ITEM = blockItem("silverwood_sapling", SILVERWOOD_SAPLING);
    public static final RegistryObject<Item> SHIMMERLEAF_ITEM = blockItem("shimmerleaf", SHIMMERLEAF);

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    /** Force class initialization before the shared block DeferredRegister is attached to the bus. */
    public static void init() {
    }

    public static void registerAspects() {
        ThaumcraftApi.registerBlockTag(GREATWOOD_LOG.get(), new AspectList().add(Aspect.TREE, 4));
        ThaumcraftApi.registerObjectTag(GREATWOOD_LOG_ITEM.get(), new AspectList().add(Aspect.TREE, 4));
        ThaumcraftApi.registerBlockTag(SILVERWOOD_LOG.get(), new AspectList().add(Aspect.TREE, 4).add(Aspect.MAGIC, 1));
        ThaumcraftApi.registerObjectTag(SILVERWOOD_LOG_ITEM.get(), new AspectList().add(Aspect.TREE, 4).add(Aspect.MAGIC, 1));
        ThaumcraftApi.registerBlockTag(GREATWOOD_LEAVES.get(), new AspectList().add(Aspect.PLANT, 1));
        ThaumcraftApi.registerBlockTag(SILVERWOOD_LEAVES.get(), new AspectList().add(Aspect.PLANT, 1).add(Aspect.MAGIC, 1));
        ThaumcraftApi.registerObjectTag(GREATWOOD_SAPLING_ITEM.get(), new AspectList().add(Aspect.PLANT, 1).add(Aspect.TREE, 1));
        ThaumcraftApi.registerObjectTag(SILVERWOOD_SAPLING_ITEM.get(), new AspectList().add(Aspect.PLANT, 1).add(Aspect.TREE, 1).add(Aspect.MAGIC, 1));
        AspectList shimmerleaf = new AspectList().add(Aspect.PLANT, 2).add(Aspect.POISON, 1).add(Aspect.MAGIC, 2);
        ThaumcraftApi.registerBlockTag(SHIMMERLEAF.get(), shimmerleaf);
        ThaumcraftApi.registerObjectTag(SHIMMERLEAF_ITEM.get(), shimmerleaf);
    }

    private static final class GreatwoodGrower extends AbstractTreeGrower {
        @Override
        protected @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return GREATWOOD_TREE_KEY;
        }
    }

    private static final class SilverwoodGrower extends AbstractTreeGrower {
        @Override
        protected @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
            return SILVERWOOD_SAPLING_TREE_KEY;
        }
    }

    private ModMagicalTrees() {
    }
}

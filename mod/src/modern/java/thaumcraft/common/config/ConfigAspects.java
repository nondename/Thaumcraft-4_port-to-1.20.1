package thaumcraft.common.config;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Incremental port of TC4 object aspect assignments.
 *
 * Values in this slice are direct mappings from vanilla objects represented in
 * the maintained 1.12.2 ConfigAspects reference. 1.20-only content is left
 * unassigned until an explicit compatibility policy is defined rather than
 * inventing gameplay values during the parity port.
 */
public final class ConfigAspects {
    private static boolean initialized;

    private ConfigAspects() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        registerMaterials();
        registerTools();
        registerTerrain();
        registerPlants();
        registerWool();
        registerUtilityBlocks();
    }

    private static void registerMaterials() {
        register(Items.IRON_INGOT, new AspectList().add(Aspect.METAL, 4));
        register(Items.GOLD_INGOT, new AspectList().add(Aspect.METAL, 3).add(Aspect.GREED, 2));
        register(Items.DIAMOND, new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.GREED, 4));
        register(Items.EMERALD, new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.GREED, 5));
        register(Items.REDSTONE, new AspectList().add(Aspect.ENERGY, 2).add(Aspect.MECHANISM, 1));
        register(Items.COAL, new AspectList().add(Aspect.ENERGY, 2).add(Aspect.FIRE, 2));
        register(Items.IRON_NUGGET, new AspectList().add(Aspect.METAL, 1));
    }

    private static void registerTools() {
        register(Items.IRON_PICKAXE, new AspectList().add(Aspect.TOOL, 3).add(Aspect.METAL, 3));
        register(Items.IRON_AXE, new AspectList().add(Aspect.TOOL, 3).add(Aspect.METAL, 3));
        register(Items.IRON_SHOVEL, new AspectList().add(Aspect.TOOL, 2).add(Aspect.METAL, 2));
        register(Items.IRON_SWORD, new AspectList().add(Aspect.TOOL, 3).add(Aspect.WEAPON, 3).add(Aspect.METAL, 3));
        register(Items.IRON_HOE, new AspectList().add(Aspect.TOOL, 2).add(Aspect.METAL, 2).add(Aspect.HARVEST, 1));

        register(Items.DIAMOND_PICKAXE, new AspectList().add(Aspect.TOOL, 4).add(Aspect.CRYSTAL, 5));
        register(Items.DIAMOND_SWORD, new AspectList().add(Aspect.TOOL, 4).add(Aspect.WEAPON, 4).add(Aspect.CRYSTAL, 4));
        register(Items.DIAMOND_AXE, new AspectList().add(Aspect.TOOL, 4).add(Aspect.CRYSTAL, 4));
    }

    private static void registerTerrain() {
        register(Blocks.STONE, new AspectList().add(Aspect.EARTH, 2));
        register(Blocks.COBBLESTONE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.ENTROPY, 1));
        register(Blocks.DIRT, new AspectList().add(Aspect.EARTH, 2));
        register(Blocks.GRASS_BLOCK, new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1));
        register(Blocks.PODZOL, new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1));
        register(Blocks.MYCELIUM, new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1));
        register(Blocks.SAND, new AspectList().add(Aspect.EARTH, 1).add(Aspect.ENTROPY, 1));
        register(Blocks.GRAVEL, new AspectList().add(Aspect.EARTH, 2));
        register(Blocks.END_STONE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.DARKNESS, 1));
        register(Blocks.BEDROCK, new AspectList()
                .add(Aspect.VOID, 16)
                .add(Aspect.ENTROPY, 16)
                .add(Aspect.EARTH, 16)
                .add(Aspect.DARKNESS, 16));

        register(Blocks.MOSSY_COBBLESTONE,
                new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1).add(Aspect.MAGIC, 1));
        register(Blocks.STONE_BRICKS, new AspectList().add(Aspect.EARTH, 2));
        register(Blocks.MOSSY_STONE_BRICKS, new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1));
        register(Blocks.CRACKED_STONE_BRICKS, new AspectList().add(Aspect.EARTH, 1).add(Aspect.ENTROPY, 1));
        register(Blocks.CHISELED_STONE_BRICKS, new AspectList().add(Aspect.EARTH, 1).add(Aspect.ORDER, 1));

        register(Blocks.SANDSTONE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.ENTROPY, 1));
        register(Blocks.CHISELED_SANDSTONE, new AspectList().add(Aspect.MAGIC, 1));
        register(Blocks.SMOOTH_SANDSTONE, new AspectList().add(Aspect.ORDER, 1));

        register(Blocks.COAL_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.ENERGY, 2).add(Aspect.FIRE, 1));
        register(Blocks.REDSTONE_ORE,
                new AspectList().add(Aspect.EARTH, 1).add(Aspect.ENERGY, 2).add(Aspect.MECHANISM, 2));

        register(Blocks.OBSIDIAN, new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 2).add(Aspect.DARKNESS, 1));
        register(Blocks.ICE, new AspectList().add(Aspect.COLD, 4));
        register(Blocks.PACKED_ICE, new AspectList().add(Aspect.COLD, 3).add(Aspect.EARTH, 1));
        register(Blocks.CLAY, new AspectList().add(Aspect.EARTH, 3).add(Aspect.WATER, 3));
        register(Blocks.GLASS, new AspectList().add(Aspect.CRYSTAL, 1));

        register(Blocks.NETHERRACK, new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 1));
        register(Blocks.SOUL_SAND, new AspectList().add(Aspect.EARTH, 1).add(Aspect.TRAP, 1).add(Aspect.SOUL, 1));
        register(Blocks.NETHER_BRICKS, new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 1));
    }

    private static void registerPlants() {
        AspectList leaves = new AspectList().add(Aspect.PLANT, 1);
        register(Blocks.OAK_LEAVES, leaves);
        register(Blocks.SPRUCE_LEAVES, leaves);
        register(Blocks.BIRCH_LEAVES, leaves);
        register(Blocks.JUNGLE_LEAVES, leaves);
        register(Blocks.ACACIA_LEAVES, leaves);
        register(Blocks.DARK_OAK_LEAVES, leaves);

        register(Blocks.CACTUS,
                new AspectList().add(Aspect.PLANT, 3).add(Aspect.WATER, 1).add(Aspect.ENTROPY, 1));
        register(Blocks.PUMPKIN, new AspectList().add(Aspect.CROP, 2));
        register(Blocks.MELON, new AspectList().add(Aspect.CROP, 2));
        register(Blocks.VINE, new AspectList().add(Aspect.PLANT, 1));
        register(Blocks.LILY_PAD, new AspectList().add(Aspect.PLANT, 2).add(Aspect.WATER, 1));

        AspectList flower = new AspectList().add(Aspect.PLANT, 1).add(Aspect.LIFE, 1).add(Aspect.SENSES, 1);
        register(Blocks.DANDELION, flower);
        register(Blocks.POPPY, flower);

        register(Blocks.BROWN_MUSHROOM,
                new AspectList().add(Aspect.PLANT, 1).add(Aspect.DARKNESS, 1).add(Aspect.EARTH, 1));
        register(Blocks.RED_MUSHROOM,
                new AspectList().add(Aspect.PLANT, 1).add(Aspect.DARKNESS, 1).add(Aspect.FIRE, 1));
        register(Blocks.DEAD_BUSH, new AspectList().add(Aspect.PLANT, 1).add(Aspect.ENTROPY, 1));
    }

    private static void registerWool() {
        AspectList wool = new AspectList().add(Aspect.CLOTH, 4).add(Aspect.CRAFT, 1);
        register(Blocks.WHITE_WOOL, wool);
        register(Blocks.ORANGE_WOOL, wool);
        register(Blocks.MAGENTA_WOOL, wool);
        register(Blocks.LIGHT_BLUE_WOOL, wool);
        register(Blocks.YELLOW_WOOL, wool);
        register(Blocks.LIME_WOOL, wool);
        register(Blocks.PINK_WOOL, wool);
        register(Blocks.GRAY_WOOL, wool);
        register(Blocks.LIGHT_GRAY_WOOL, wool);
        register(Blocks.CYAN_WOOL, wool);
        register(Blocks.PURPLE_WOOL, wool);
        register(Blocks.BLUE_WOOL, wool);
        register(Blocks.BROWN_WOOL, wool);
        register(Blocks.GREEN_WOOL, wool);
        register(Blocks.RED_WOOL, wool);
        register(Blocks.BLACK_WOOL, wool);
    }

    private static void registerUtilityBlocks() {
        AspectList torch = new AspectList().add(Aspect.LIGHT, 1);
        register(Blocks.TORCH, torch);
        registerBlockOnly(Blocks.WALL_TORCH, torch);
    }

    private static void register(Item item, AspectList aspects) {
        ThaumcraftApi.registerObjectTag(new ItemStack(item), aspects);
    }

    private static void register(Block block, AspectList aspects) {
        ThaumcraftApi.registerBlockTag(block, aspects);
        Item item = block.asItem();
        if (item != Items.AIR) {
            register(item, aspects);
        }
    }

    private static void registerBlockOnly(Block block, AspectList aspects) {
        ThaumcraftApi.registerBlockTag(block, aspects);
    }
}

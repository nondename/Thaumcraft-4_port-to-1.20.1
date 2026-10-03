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
        registerFood();
        registerMobDrops();
        registerAlchemyMaterials();
        registerMiscellaneousItems();
        registerTerrain();
        registerPlants();
        registerWool();
        registerUtilityBlocks();
        registerAdditionalObjects();
    }

    private static void registerMaterials() {
        register(Items.IRON_INGOT, new AspectList().add(Aspect.METAL, 4));
        register(Items.GOLD_INGOT, new AspectList().add(Aspect.METAL, 3).add(Aspect.GREED, 2));
        register(Items.DIAMOND, new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.GREED, 4));
        register(Items.EMERALD, new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.GREED, 5));
        register(Items.REDSTONE, new AspectList().add(Aspect.ENERGY, 2).add(Aspect.MECHANISM, 1));
        register(Items.COAL, new AspectList().add(Aspect.ENERGY, 2).add(Aspect.FIRE, 2));
        register(Items.STICK, new AspectList().add(Aspect.TREE, 1));
        register(Items.STRING, new AspectList().add(Aspect.BEAST, 1).add(Aspect.CLOTH, 1));
        register(Items.FEATHER, new AspectList().add(Aspect.FLIGHT, 2).add(Aspect.AIR, 1));
        register(Items.LEATHER, new AspectList().add(Aspect.CLOTH, 2).add(Aspect.BEAST, 1).add(Aspect.ARMOR, 1));
        register(Items.BONE, new AspectList().add(Aspect.DEATH, 2).add(Aspect.FLESH, 1));
        register(Items.GUNPOWDER, new AspectList().add(Aspect.FIRE, 4).add(Aspect.ENTROPY, 4));
        register(Items.ENDER_PEARL,
                new AspectList().add(Aspect.ELDRITCH, 4).add(Aspect.MAGIC, 2).add(Aspect.TRAVEL, 4));
        register(Items.IRON_NUGGET, new AspectList().add(Aspect.METAL, 1));
        register(Items.GOLD_NUGGET, new AspectList().add(Aspect.METAL, 1));
        register(Items.QUARTZ, new AspectList().add(Aspect.CRYSTAL, 1).add(Aspect.ENERGY, 1));
        register(Items.BRICK, new AspectList().add(Aspect.EARTH, 1).add(Aspect.FIRE, 1));
        register(Items.NETHER_BRICK, new AspectList().add(Aspect.FIRE, 1));
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
        register(Items.DIAMOND_SHOVEL, new AspectList().add(Aspect.TOOL, 3).add(Aspect.CRYSTAL, 3));
        register(Items.DIAMOND_HOE, new AspectList().add(Aspect.TOOL, 3).add(Aspect.CRYSTAL, 3).add(Aspect.HARVEST, 2));

        register(Items.GOLDEN_PICKAXE, new AspectList().add(Aspect.TOOL, 2).add(Aspect.METAL, 4));
        register(Items.GOLDEN_SWORD, new AspectList().add(Aspect.TOOL, 2).add(Aspect.WEAPON, 2).add(Aspect.METAL, 4));
        register(Items.GOLDEN_AXE, new AspectList().add(Aspect.TOOL, 2).add(Aspect.METAL, 4));
        register(Items.GOLDEN_SHOVEL, new AspectList().add(Aspect.TOOL, 1).add(Aspect.METAL, 4));
        register(Items.GOLDEN_HOE, new AspectList().add(Aspect.TOOL, 1).add(Aspect.METAL, 4).add(Aspect.HARVEST, 1));

        register(Items.STONE_PICKAXE, new AspectList().add(Aspect.TOOL, 2).add(Aspect.EARTH, 3));
        register(Items.STONE_SWORD, new AspectList().add(Aspect.TOOL, 2).add(Aspect.WEAPON, 2).add(Aspect.EARTH, 3));
        register(Items.STONE_AXE, new AspectList().add(Aspect.TOOL, 2).add(Aspect.EARTH, 3));
        register(Items.STONE_SHOVEL, new AspectList().add(Aspect.TOOL, 1).add(Aspect.EARTH, 2));
        register(Items.STONE_HOE, new AspectList().add(Aspect.TOOL, 1).add(Aspect.EARTH, 2).add(Aspect.HARVEST, 1));

        register(Items.WOODEN_PICKAXE, new AspectList().add(Aspect.TOOL, 1).add(Aspect.TREE, 3));
        register(Items.WOODEN_SWORD, new AspectList().add(Aspect.TOOL, 1).add(Aspect.WEAPON, 1).add(Aspect.TREE, 3));
        register(Items.WOODEN_AXE, new AspectList().add(Aspect.TOOL, 1).add(Aspect.TREE, 3));
        register(Items.WOODEN_SHOVEL, new AspectList().add(Aspect.TOOL, 1).add(Aspect.TREE, 2));
        register(Items.WOODEN_HOE, new AspectList().add(Aspect.TOOL, 1).add(Aspect.TREE, 2).add(Aspect.HARVEST, 1));
    }

    private static void registerFood() {
        register(Items.APPLE, new AspectList().add(Aspect.CROP, 2).add(Aspect.HUNGER, 1));
        register(Items.BREAD, new AspectList().add(Aspect.PLANT, 2).add(Aspect.LIFE, 2));
        register(Items.COOKED_BEEF, new AspectList().add(Aspect.CRAFT, 1).add(Aspect.FLESH, 4).add(Aspect.HUNGER, 4));
        register(Items.COOKED_PORKCHOP,
                new AspectList().add(Aspect.CRAFT, 1).add(Aspect.FLESH, 3).add(Aspect.HUNGER, 3));
        register(Items.COOKED_CHICKEN, new AspectList().add(Aspect.CRAFT, 1).add(Aspect.FLESH, 4).add(Aspect.HUNGER, 3));
        register(Items.COOKED_COD, new AspectList().add(Aspect.CRAFT, 1).add(Aspect.FLESH, 4).add(Aspect.HUNGER, 3));
        register(Items.COOKED_SALMON, new AspectList().add(Aspect.CRAFT, 1).add(Aspect.FLESH, 4).add(Aspect.HUNGER, 3));
        register(Items.GOLDEN_CARROT, new AspectList().add(Aspect.PLANT, 2).add(Aspect.METAL, 4).add(Aspect.SENSES, 2));
        register(Items.GLISTERING_MELON_SLICE,
                new AspectList().add(Aspect.PLANT, 2).add(Aspect.METAL, 4).add(Aspect.HEAL, 2));
        register(Items.WHEAT, new AspectList().add(Aspect.CROP, 2).add(Aspect.HUNGER, 1));
        register(Items.MELON_SLICE, new AspectList().add(Aspect.HUNGER, 1));
    }

    private static void registerMobDrops() {
        register(Items.ROTTEN_FLESH, new AspectList().add(Aspect.MAN, 1).add(Aspect.FLESH, 2));
        register(Items.SPIDER_EYE, new AspectList().add(Aspect.SENSES, 2).add(Aspect.BEAST, 2).add(Aspect.POISON, 2));
        register(Items.BLAZE_ROD, new AspectList().add(Aspect.FIRE, 4).add(Aspect.MAGIC, 2));
        register(Items.GHAST_TEAR, new AspectList().add(Aspect.WATER, 1).add(Aspect.UNDEAD, 4).add(Aspect.SOUL, 4));
        register(Items.MAGMA_CREAM, new AspectList().add(Aspect.FIRE, 3).add(Aspect.SLIME, 2));
        register(Items.SLIME_BALL, new AspectList().add(Aspect.SLIME, 2));
    }

    private static void registerAlchemyMaterials() {
        register(Items.NETHER_WART, new AspectList().add(Aspect.PLANT, 1).add(Aspect.MAGIC, 1));
        register(Items.WHEAT_SEEDS, new AspectList().add(Aspect.PLANT, 1));
        register(Items.MELON_SEEDS, new AspectList().add(Aspect.PLANT, 1));
        register(Items.PUMPKIN_SEEDS, new AspectList().add(Aspect.PLANT, 1));
        register(Items.WATER_BUCKET, new AspectList().add(Aspect.WATER, 3));
        register(Items.LAVA_BUCKET, new AspectList().add(Aspect.FIRE, 3).add(Aspect.EARTH, 1));
    }

    private static void registerMiscellaneousItems() {
        register(Items.FLINT, new AspectList().add(Aspect.EARTH, 1).add(Aspect.TOOL, 1));
        register(Items.BUCKET, new AspectList().add(Aspect.METAL, 8).add(Aspect.VOID, 1));
        // In the 1.12 reference these are complex tags: the values above their
        // base aspect depend on recipe-derived aspects, which this stage has
        // not ported yet.
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
        // TC4 registered these by ore-dictionary name (oreIron, oreGold, ...); the flat 1.20.1
        // port registers the vanilla blocks directly, incl. deepslate twins (1.7.10 had none).
        // TC4's separate lit_redstone_ore block is a LIT blockstate property in 1.20.1, so it
        // scans as plain REDSTONE_ORE; quartz ore is NETHER_QUARTZ_ORE here.
        register(Blocks.IRON_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.METAL, 3));
        register(Blocks.DEEPSLATE_IRON_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.METAL, 3));
        register(Blocks.GOLD_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.METAL, 2).add(Aspect.GREED, 1));
        register(Blocks.DEEPSLATE_GOLD_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.METAL, 2).add(Aspect.GREED, 1));
        register(Blocks.DIAMOND_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.GREED, 3).add(Aspect.CRYSTAL, 3));
        register(Blocks.DEEPSLATE_DIAMOND_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.GREED, 3).add(Aspect.CRYSTAL, 3));
        register(Blocks.LAPIS_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.SENSES, 3));
        register(Blocks.DEEPSLATE_LAPIS_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.SENSES, 3));
        register(Blocks.EMERALD_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.GREED, 4).add(Aspect.CRYSTAL, 3));
        register(Blocks.NETHER_QUARTZ_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.CRYSTAL, 3));

        register(Blocks.OBSIDIAN, new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 2).add(Aspect.DARKNESS, 1));
        register(Blocks.ICE, new AspectList().add(Aspect.COLD, 4));
        register(Blocks.PACKED_ICE, new AspectList().add(Aspect.COLD, 3).add(Aspect.EARTH, 1));
        register(Blocks.CLAY, new AspectList().add(Aspect.EARTH, 3).add(Aspect.WATER, 3));
        register(Blocks.TERRACOTTA, new AspectList().add(Aspect.EARTH, 4).add(Aspect.FIRE, 1));
        AspectList coloredTerracotta = new AspectList()
                .add(Aspect.EARTH, 3).add(Aspect.FIRE, 1).add(Aspect.SENSES, 1);
        register(Blocks.WHITE_TERRACOTTA, coloredTerracotta);
        register(Blocks.ORANGE_TERRACOTTA, coloredTerracotta);
        register(Blocks.MAGENTA_TERRACOTTA, coloredTerracotta);
        register(Blocks.LIGHT_BLUE_TERRACOTTA, coloredTerracotta);
        register(Blocks.YELLOW_TERRACOTTA, coloredTerracotta);
        register(Blocks.LIME_TERRACOTTA, coloredTerracotta);
        register(Blocks.PINK_TERRACOTTA, coloredTerracotta);
        register(Blocks.GRAY_TERRACOTTA, coloredTerracotta);
        register(Blocks.LIGHT_GRAY_TERRACOTTA, coloredTerracotta);
        register(Blocks.CYAN_TERRACOTTA, coloredTerracotta);
        register(Blocks.PURPLE_TERRACOTTA, coloredTerracotta);
        register(Blocks.BLUE_TERRACOTTA, coloredTerracotta);
        register(Blocks.BROWN_TERRACOTTA, coloredTerracotta);
        register(Blocks.GREEN_TERRACOTTA, coloredTerracotta);
        register(Blocks.RED_TERRACOTTA, coloredTerracotta);
        register(Blocks.BLACK_TERRACOTTA, coloredTerracotta);
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

    private static void registerAdditionalObjects() {
        register(Items.PAPER, new AspectList().add(Aspect.MIND, 1));
        register(Items.BOOK, new AspectList().add(Aspect.MIND, 3));
        register(Items.SHIELD, new AspectList().add(Aspect.ARMOR, 2).add(Aspect.TREE, 3).add(Aspect.METAL, 1));
        register(Items.ELYTRA, new AspectList().add(Aspect.FLIGHT, 5).add(Aspect.ELDRITCH, 3).add(Aspect.CLOTH, 2));
        register(Items.TOTEM_OF_UNDYING, new AspectList().add(Aspect.LIFE, 5).add(Aspect.MAGIC, 4).add(Aspect.HEAL, 4).add(Aspect.ELDRITCH, 3));
        register(Items.SHULKER_SHELL, new AspectList().add(Aspect.VOID, 3).add(Aspect.ELDRITCH, 3).add(Aspect.ARMOR, 2));
        register(Items.SPECTRAL_ARROW, new AspectList().add(Aspect.WEAPON, 1).add(Aspect.LIGHT, 2));
        register(Items.DRAGON_BREATH, new AspectList().add(Aspect.ELDRITCH, 4).add(Aspect.MAGIC, 3).add(Aspect.AIR, 2));
        register(Items.END_CRYSTAL, new AspectList().add(Aspect.ELDRITCH, 3).add(Aspect.MAGIC, 3).add(Aspect.CRYSTAL, 3));
        register(Blocks.CRAFTING_TABLE, new AspectList().add(Aspect.CRAFT, 4));
        register(Blocks.OBSERVER, new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.SENSES, 2));
        register(Blocks.END_STONE_BRICKS, new AspectList().add(Aspect.EARTH, 2).add(Aspect.ELDRITCH, 2));
        register(Blocks.END_ROD, new AspectList().add(Aspect.LIGHT, 2).add(Aspect.ELDRITCH, 2).add(Aspect.AIR, 1));
        register(Blocks.MAGMA_BLOCK, new AspectList().add(Aspect.FIRE, 4).add(Aspect.EARTH, 1).add(Aspect.SLIME, 1));
        register(Blocks.BONE_BLOCK, new AspectList().add(Aspect.DEATH, 2).add(Aspect.EARTH, 1));
        register(Blocks.NETHER_WART_BLOCK, new AspectList().add(Aspect.PLANT, 2).add(Aspect.FIRE, 2).add(Aspect.TRAP, 2));
        register(Blocks.RED_NETHER_BRICKS, new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 2));
        register(Blocks.DIRT_PATH, new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1));
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

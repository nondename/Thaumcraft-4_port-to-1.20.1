package thaumcraft.common.config;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Incremental port of TC4 object aspect assignments.
 *
 * Values in this bootstrap slice are copied from the maintained 1.12.2
 * ConfigAspects reference before the full table is migrated.
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

        ThaumcraftApi.registerObjectTag(new ItemStack(Items.IRON_INGOT),
                new AspectList().add(Aspect.METAL, 4));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.GOLD_INGOT),
                new AspectList().add(Aspect.METAL, 3).add(Aspect.GREED, 2));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.DIAMOND),
                new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.GREED, 4));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.EMERALD),
                new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.GREED, 5));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.REDSTONE),
                new AspectList().add(Aspect.ENERGY, 2).add(Aspect.MECHANISM, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Items.COAL),
                new AspectList().add(Aspect.ENERGY, 2).add(Aspect.FIRE, 2));

        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.OBSIDIAN.asItem()),
                new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 2).add(Aspect.DARKNESS, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.GLASS.asItem()),
                new AspectList().add(Aspect.CRYSTAL, 1));
        ThaumcraftApi.registerObjectTag(new ItemStack(Blocks.CACTUS.asItem()),
                new AspectList().add(Aspect.PLANT, 3).add(Aspect.WATER, 1).add(Aspect.ENTROPY, 1));
    }
}

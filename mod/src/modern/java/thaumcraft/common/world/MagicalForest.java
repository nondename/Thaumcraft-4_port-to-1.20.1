package thaumcraft.common.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import thaumcraft.Thaumcraft;

/** TC4 Magical Forest biome registration hook for Forge's overworld biome source. */
public final class MagicalForest {
    public static final ResourceKey<Biome> MAGICAL_FOREST = ResourceKey.create(
            Registries.BIOME, new ResourceLocation(Thaumcraft.MODID, "magical_forest"));

    private static boolean registered;

    public static void registerOverworldBiome() {
        if (registered) {
            return;
        }
        registered = true;
        // TC4 Magical Forest is temperate (0.7 temperature / 0.6 rainfall). WARM is the closest
        // legacy Forge climate bucket and lets the biome participate in normal overworld creation.
        BiomeManager.addBiome(BiomeManager.BiomeType.WARM, new BiomeManager.BiomeEntry(MAGICAL_FOREST, 10));
    }

    private MagicalForest() {
    }
}

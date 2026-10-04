package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;

/**
 * TC4's structure-linked aura-node pass adapted to modern structure starts.
 *
 * <p>The legacy generator queried {@code MapGenScatteredFeature} before rolling the normal 1/36
 * wild-node chance. In 1.20.1 the direct equivalents are desert pyramids, jungle pyramids,
 * swamp huts and igloos. A valid start in the current chunk receives one normal random node at
 * the chunk locator anchor, three blocks above the world surface, and suppresses the wild node
 * pass for that chunk.</p>
 */
public final class StructureAuraNodeFeature extends Feature<NoneFeatureConfiguration> {
    private static final List<ResourceKey<Structure>> SCATTERED_STRUCTURES = List.of(
            BuiltinStructures.DESERT_PYRAMID,
            BuiltinStructures.JUNGLE_TEMPLE,
            BuiltinStructures.SWAMP_HUT,
            BuiltinStructures.IGLOO
    );

    public StructureAuraNodeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = new ChunkPos(context.origin());
        if (!hasScatteredStructureStart(level, chunkPos)) {
            return false;
        }

        // MapGenScatteredFeature#getNearestStructurePos used the structure start chunk's +8/+8
        // locator position. Reusing that anchor is closer to TC4 than using the modern bounding-box
        // centre, whose position can shift with individual structure pieces.
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 3;
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockPos pos = new BlockPos(x, y, z);
        return AuraNodeFeature.placeRandomNode(level, pos, context.random(), false, false, false);
    }

    static boolean hasScatteredStructureStart(WorldGenLevel level, ChunkPos chunkPos) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);

        for (ResourceKey<Structure> key : SCATTERED_STRUCTURES) {
            Structure structure = registry.get(key);
            if (structure == null) {
                continue;
            }
            StructureStart start = chunk.getStartForStructure(structure);
            if (start != null && start.isValid()) {
                return true;
            }
        }
        return false;
    }
}

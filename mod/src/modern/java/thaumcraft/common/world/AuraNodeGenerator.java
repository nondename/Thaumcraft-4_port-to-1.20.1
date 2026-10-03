package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

import java.util.List;

/**
 * Modern implementation of TC4 4.2.3.5 ThaumcraftWorldGenerator.createRandomNodeAt.
 *
 * <p>BiomeDictionary itself no longer exists in the old form, so biome classification is delegated
 * to {@link BiomeAuraResolver}. The node-type odds, modifier odds, aspect seeding, environment
 * scan, spread math and silverwood/eerie/small flags intentionally follow TC4.</p>
 */
public final class AuraNodeGenerator {
    public static final int NODE_RARITY = 36;
    public static final int SPECIAL_NODE_RARITY = 18;

    public record GeneratedNode(NodeType type, NodeModifier modifier, AspectList aspects) {
    }

    /** Normal wild/creative node generation. */
    public static GeneratedNode generate(WorldGenLevel level, BlockPos pos, RandomSource random) {
        return generate(level, pos, random, false, false, false);
    }

    /**
     * TC4-compatible generation flags used by wild nodes, silverwood nodes and eerie structures.
     */
    public static GeneratedNode generate(WorldGenLevel level, BlockPos pos, RandomSource random,
                                         boolean silverwood, boolean eerie, boolean small) {
        List<Aspect> basicAspects = Aspect.getPrimalAspects();
        List<Aspect> complexAspects = Aspect.getCompoundAspects();
        if (basicAspects.isEmpty() || complexAspects.isEmpty()) {
            return new GeneratedNode(NodeType.NORMAL, null, new AspectList().add(Aspect.AIR, 1));
        }

        NodeType type = pickType(random, silverwood, eerie);
        NodeModifier modifier = pickModifier(random);

        BiomeAuraResolver.Profile biome = BiomeAuraResolver.resolve(level.getBiome(pos));
        int biomeAura = biome.auraLevel();

        // Original TC4 taint-biome rule: pure nodes are immune; other nodes gain aura and have a
        // 50% chance to become TAINTED, gaining another 50% aura. The tag is wired in advance for
        // the future 1.20.1 taint biome.
        if (type != NodeType.PURE && biome.tainted()) {
            biomeAura = (int) (biomeAura * 1.5F);
            if (random.nextBoolean()) {
                type = NodeType.TAINTED;
                biomeAura = (int) (biomeAura * 1.5F);
            }
        }

        if (silverwood || small) {
            biomeAura /= 4;
        }
        biomeAura = Math.max(2, biomeAura);

        int value = random.nextInt(biomeAura / 2) + biomeAura / 2;
        Aspect biomeAspect = biome.randomAspect(random);
        AspectList aspects = new AspectList();

        if (biomeAspect != null) {
            aspects.add(biomeAspect, 2);
        } else {
            aspects.add(complexAspects.get(random.nextInt(complexAspects.size())), 1);
            aspects.add(basicAspects.get(random.nextInt(basicAspects.size())), 1);
        }

        for (int i = 0; i < 3; i++) {
            if (!random.nextBoolean()) {
                continue;
            }
            if (random.nextInt(SPECIAL_NODE_RARITY) == 0) {
                aspects.merge(complexAspects.get(random.nextInt(complexAspects.size())), 1);
            } else {
                aspects.merge(basicAspects.get(random.nextInt(basicAspects.size())), 1);
            }
        }

        if (type == NodeType.HUNGRY) {
            aspects.merge(Aspect.HUNGER, 2);
            if (random.nextBoolean()) {
                aspects.merge(Aspect.GREED, 1);
            }
        } else if (type == NodeType.PURE) {
            aspects.merge(random.nextBoolean() ? Aspect.LIFE : Aspect.ORDER, 2);
        } else if (type == NodeType.DARK) {
            if (random.nextBoolean()) aspects.merge(Aspect.DEATH, 1);
            if (random.nextBoolean()) aspects.merge(Aspect.UNDEAD, 1);
            if (random.nextBoolean()) aspects.merge(Aspect.ENTROPY, 1);
            if (random.nextBoolean()) aspects.merge(Aspect.DARKNESS, 1);
        }

        addEnvironmentAspects(level, pos, aspects);

        Aspect[] sorted = aspects.getAspectsSorted();
        int[] spread = new int[sorted.length];
        float spreadTotal = 0.0F;
        for (int i = 0; i < sorted.length; i++) {
            spread[i] = aspects.getAmount(sorted[i]) == 2
                    ? 50 + random.nextInt(25)
                    : 25 + random.nextInt(50);
            spreadTotal += spread[i];
        }

        if (spreadTotal > 0.0F) {
            for (int i = 0; i < sorted.length; i++) {
                // AspectList.merge is intentionally max(), not addition. This mirrors the TC4
                // implementation exactly and preserves its low-value edge cases.
                aspects.merge(sorted[i], (int) (spread[i] / spreadTotal * value));
            }
        }
        return new GeneratedNode(type, modifier, aspects);
    }

    private static NodeType pickType(RandomSource random, boolean silverwood, boolean eerie) {
        if (silverwood) {
            return NodeType.PURE;
        }
        if (eerie) {
            return NodeType.DARK;
        }
        if (random.nextInt(SPECIAL_NODE_RARITY) != 0) {
            return NodeType.NORMAL;
        }
        return switch (random.nextInt(10)) {
            case 0, 1, 2 -> NodeType.DARK;
            case 3, 4, 5 -> NodeType.UNSTABLE;
            case 6, 7, 8 -> NodeType.PURE;
            default -> NodeType.HUNGRY;
        };
    }

    private static NodeModifier pickModifier(RandomSource random) {
        if (random.nextInt(Math.max(1, SPECIAL_NODE_RARITY / 2)) != 0) {
            return null;
        }
        return switch (random.nextInt(3)) {
            case 0 -> NodeModifier.BRIGHT;
            case 1 -> NodeModifier.PALE;
            default -> NodeModifier.FADING;
        };
    }

    private static void addEnvironmentAspects(WorldGenLevel level, BlockPos center, AspectList aspects) {
        int water = 0;
        int lava = 0;
        int stone = 0;
        int foliage = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var state = level.getBlockState(cursor);
                    if (state.getFluidState().is(FluidTags.WATER)) water++;
                    else if (state.getFluidState().is(FluidTags.LAVA)) lava++;
                    else if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.STONE)) stone++;
                    if (state.is(BlockTags.LEAVES)) foliage++;
                }
            }
        }

        if (water > 100) aspects.merge(Aspect.WATER, 1);
        if (lava > 100) {
            aspects.merge(Aspect.FIRE, 1);
            aspects.merge(Aspect.EARTH, 1);
        }
        if (stone > 500) aspects.merge(Aspect.EARTH, 1);
        if (foliage > 100) aspects.merge(Aspect.PLANT, 1);
    }

    private AuraNodeGenerator() {
    }
}

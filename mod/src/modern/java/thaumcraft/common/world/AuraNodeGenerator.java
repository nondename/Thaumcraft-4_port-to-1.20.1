package thaumcraft.common.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * World-generation side of the TC4 aura-node algorithm.
 *
 * <p>The original 1/36 chunk rarity is handled by the placed-feature JSON. This class keeps the
 * old special-node odds and the important biome/surroundings influence on a node's aspect pool.</p>
 */
public final class AuraNodeGenerator {
    public static final int NODE_RARITY = 36;
    public static final int SPECIAL_NODE_RARITY = 18;

    public record GeneratedNode(NodeType type, NodeModifier modifier, AspectList aspects) {
    }

    public static GeneratedNode generate(WorldGenLevel level, BlockPos pos, RandomSource random) {
        NodeType type = pickType(random);
        NodeModifier modifier = pickModifier(random);
        AspectList aspects = generateAspects(level, pos, random, type);
        return new GeneratedNode(type, modifier, aspects);
    }

    private static NodeType pickType(RandomSource random) {
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

    private static AspectList generateAspects(WorldGenLevel level, BlockPos pos, RandomSource random, NodeType type) {
        Biome biome = level.getBiome(pos).value();
        int biomeAura = estimateBiomeAura(biome);
        int totalVis = random.nextInt(Math.max(1, biomeAura / 2)) + biomeAura / 2;

        Map<Aspect, Integer> weights = new LinkedHashMap<>();
        addWeight(weights, pickBiomeAspect(biome, pos, random), 2);

        List<Aspect> primals = Aspect.getPrimalAspects();
        List<Aspect> compounds = Aspect.getCompoundAspects();
        for (int i = 0; i < 3; i++) {
            if (!random.nextBoolean()) continue;
            Aspect picked;
            if (random.nextInt(SPECIAL_NODE_RARITY) == 0 && !compounds.isEmpty()) {
                picked = compounds.get(random.nextInt(compounds.size()));
            } else {
                picked = primals.get(random.nextInt(primals.size()));
            }
            addWeight(weights, picked, 1);
        }

        switch (type) {
            case HUNGRY -> {
                addWeight(weights, Aspect.HUNGER, 2);
                if (random.nextBoolean()) addWeight(weights, Aspect.GREED, 1);
            }
            case PURE -> addWeight(weights, random.nextBoolean() ? Aspect.LIFE : Aspect.ORDER, 2);
            case DARK -> {
                if (random.nextBoolean()) addWeight(weights, Aspect.DEATH, 1);
                if (random.nextBoolean()) addWeight(weights, Aspect.UNDEAD, 1);
                if (random.nextBoolean()) addWeight(weights, Aspect.ENTROPY, 1);
                if (random.nextBoolean()) addWeight(weights, Aspect.DARKNESS, 1);
            }
            case TAINTED -> addWeight(weights, Aspect.TAINT, 2);
            default -> {
            }
        }

        addEnvironmentAspects(level, pos, weights);
        if (weights.isEmpty()) addWeight(weights, Aspect.AIR, 1);

        List<Map.Entry<Aspect, Integer>> entries = new ArrayList<>(weights.entrySet());
        int[] spread = new int[entries.size()];
        int spreadTotal = 0;
        for (int i = 0; i < entries.size(); i++) {
            spread[i] = entries.get(i).getValue() >= 2
                    ? 50 + random.nextInt(25)
                    : 25 + random.nextInt(50);
            spreadTotal += spread[i];
        }

        AspectList result = new AspectList();
        for (int i = 0; i < entries.size(); i++) {
            int amount = Math.max(1, (int) ((float) spread[i] / (float) spreadTotal * (float) totalVis));
            result.add(entries.get(i).getKey(), amount);
        }
        return result;
    }

    private static int estimateBiomeAura(Biome biome) {
        int aura = 60;
        float rain = biome.getModifiedClimateSettings().downfall();
        float temperature = biome.getBaseTemperature();
        if (rain > 0.75F) aura += 10;
        if (temperature < 0.2F) aura += 5;
        if (temperature > 1.5F) aura -= 5;
        return Math.max(20, aura);
    }

    private static Aspect pickBiomeAspect(Biome biome, BlockPos pos, RandomSource random) {
        float rain = biome.getModifiedClimateSettings().downfall();
        float temperature = biome.getBaseTemperature();
        if (temperature > 1.2F) return Aspect.FIRE;
        if (temperature < 0.2F) return random.nextBoolean() ? Aspect.COLD : Aspect.ORDER;
        if (rain > 0.8F) return random.nextBoolean() ? Aspect.WATER : Aspect.PLANT;
        if (pos.getY() > 110) return Aspect.AIR;
        return switch (random.nextInt(4)) {
            case 0 -> Aspect.AIR;
            case 1 -> Aspect.EARTH;
            case 2 -> Aspect.WATER;
            default -> Aspect.ORDER;
        };
    }

    private static void addEnvironmentAspects(WorldGenLevel level, BlockPos center, Map<Aspect, Integer> weights) {
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
                    if (state.getFluidState().is(FluidTags.LAVA)) lava++;
                    if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.STONE)) stone++;
                    if (state.is(BlockTags.LEAVES)) foliage++;
                }
            }
        }

        if (water > 100) addWeight(weights, Aspect.WATER, 1);
        if (lava > 100) {
            addWeight(weights, Aspect.FIRE, 1);
            addWeight(weights, Aspect.EARTH, 1);
        }
        if (stone > 500) addWeight(weights, Aspect.EARTH, 1);
        if (foliage > 100) addWeight(weights, Aspect.PLANT, 1);
    }

    private static void addWeight(Map<Aspect, Integer> weights, Aspect aspect, int amount) {
        if (aspect != null && amount > 0) {
            weights.merge(aspect, amount, Integer::sum);
        }
    }

    private AuraNodeGenerator() {
    }
}

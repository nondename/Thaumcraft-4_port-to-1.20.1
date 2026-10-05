package thaumcraft.common.blocks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.ManaBeanItem;
import thaumcraft.common.world.MagicalForest;

/** Faithful 1.20.1 equivalent of TC4 BlockManaPod + TileManaPod. */
public final class ManaPodBlock extends BaseEntityBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;

    public ManaPodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double minY = switch (state.getValue(AGE)) {
            case 0 -> 12.0 / 16.0;
            case 1 -> 10.0 / 16.0;
            case 2 -> 8.0 / 16.0;
            case 3 -> 6.0 / 16.0;
            case 4 -> 5.0 / 16.0;
            case 5 -> 4.0 / 16.0;
            case 6 -> 3.0 / 16.0;
            default -> 2.0 / 16.0;
        };
        return Shapes.box(0.25, minY, 0.25, 0.75, 1.0, 0.75);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(MagicalForest.MAGICAL_FOREST)
                && level.getBlockState(pos.above()).is(BlockTags.LOGS);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        if (random.nextInt(30) != 0) return;

        int age = state.getValue(AGE);
        if (age < 7) {
            age++;
            state = state.setValue(AGE, age);
            level.setBlock(pos, state, 3);
        }
        if (!(level.getBlockEntity(pos) instanceof ManaPodBlockEntity pod) || age <= 2) return;

        if (age == 3) crossPollinate(level, pos, pod, random);
        if (pod.getAspect() == null) {
            if (random.nextInt(8) == 0) pod.setAspect(Aspect.PLANT);
            else {
                List<Aspect> primals = Aspect.getPrimalAspects();
                pod.setAspect(primals.get(random.nextInt(primals.size())));
            }
        }
    }

    private static void crossPollinate(ServerLevel level, BlockPos pos, ManaPodBlockEntity pod, RandomSource random) {
        List<Aspect> nearby = new ArrayList<>();
        if (pod.getAspect() != null) nearby.add(pod.getAspect());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof ManaPodBlockEntity other
                    && other.getAspect() != null && !nearby.contains(other.getAspect())) {
                nearby.add(other.getAspect());
            }
        }
        if (nearby.size() > 1) {
            List<Aspect> candidates = new ArrayList<>();
            for (Aspect a : nearby) {
                candidates.add(a);
                for (Aspect b : nearby) {
                    if (a == b) continue;
                    Aspect combo = findCombination(a, b);
                    if (combo != null) {
                        candidates.add(combo);
                        candidates.add(combo);
                    }
                }
            }
            pod.setAspect(candidates.get(random.nextInt(candidates.size())));
        } else if (nearby.size() == 1 && pod.getAspect() == null) {
            pod.setAspect(nearby.get(0));
        }
    }

    private static @Nullable Aspect findCombination(Aspect a, Aspect b) {
        for (Aspect candidate : Aspect.aspects.values()) {
            Aspect[] parts = candidate.getComponents();
            if (parts != null && parts.length == 2
                    && ((parts[0] == a && parts[1] == b) || (parts[0] == b && parts[1] == a))) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int age = state.getValue(AGE);
        if (age < 2) return List.of();
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        Aspect aspect = entity instanceof ManaPodBlockEntity pod && pod.getAspect() != null
                ? pod.getAspect() : Aspect.PLANT;
        int count = age == 7 && params.getLevel().random.nextFloat() > 0.33F ? 2 : 1;
        List<ItemStack> drops = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack bean = new ItemStack(ModMagicalForestContent.MANA_BEAN.get());
            ManaBeanItem.setAspect(bean, aspect);
            drops.add(bean);
        }
        return drops;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManaPodBlockEntity(pos, state);
    }
}

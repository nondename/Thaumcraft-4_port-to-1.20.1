package thaumcraft.common.nodes;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.ItemWispEssence;
import thaumcraft.common.sounds.ModSounds;
import thaumcraft.common.world.AuraNodeGenerator;

import java.util.function.Consumer;

/**
 * Invisible carrier block for a TC4 aura node.
 *
 * <p>The small outline shape is intentional: nodes have no collision, but the thaumometer must
 * still be able to ray-pick them exactly like the old airy node block.</p>
 */
public final class AuraNodeBlock extends BaseEntityBlock {
    private static final VoxelShape SCAN_SHAPE = box(3.0D, 3.0D, 3.0D, 13.0D, 13.0D, 13.0D);

    public AuraNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuraNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModNodes.AURA_NODE_ENTITY.get(), AuraNodeBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SCAN_SHAPE;
    }

    /**
     * Original TC4 BlockAiry metadata 0 calls ThaumcraftWorldGenerator.createRandomNodeAt when a
     * player places the single creative Aura Node item. Worldgen uses setBlock directly and is
     * therefore unaffected by this placement hook.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)
                || !(placer instanceof Player)
                || !stack.is(ModNodes.AURA_NODE_ITEM.get())) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
            var generated = AuraNodeGenerator.generate(serverLevel, pos, level.random);
            node.initialize(generated.type(), generated.modifier(), generated.aspects());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && !isMoving && level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
                for (Aspect aspect : node.getAspects().getAspects()) {
                    if (aspect == null) {
                        continue;
                    }
                    int amount = node.getAspects().getAmount(aspect);
                    if (amount < 5) {
                        continue;
                    }

                    // TC4 BlockAiry uses <= amount in steps of ten: floor(amount / 10) + 1.
                    int drops = amount / 10 + 1;
                    for (int i = 0; i < drops; i++) {
                        Containers.dropItemStack(
                                level,
                                pos.getX() + 0.5D,
                                pos.getY() + 0.5D,
                                pos.getZ() + 0.5D,
                                ItemWispEssence.create(aspect, 2));
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
                // Legacy BlockAiry has a 50% chance to emit infusedStoneSparkle on a hit.
                if (target instanceof BlockHitResult hit && level.random.nextBoolean()) {
                    spawnHitSparkle(level, hit);
                }
                return true;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                spawnDestroyBurst(level, pos);
                level.playLocalSound(
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        ModSounds.CRAFT_FAIL.get(),
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F,
                        false);
                return true;
            }
        });
    }

    private static void spawnHitSparkle(Level level, BlockHitResult hit) {
        Vec3 location = hit.getLocation();
        var face = hit.getDirection();
        double nx = face.getStepX() * 0.025D;
        double ny = face.getStepY() * 0.025D;
        double nz = face.getStepZ() * 0.025D;

        for (int i = 0; i < 3; i++) {
            double jitterX = (level.random.nextDouble() - 0.5D) * 0.08D;
            double jitterY = (level.random.nextDouble() - 0.5D) * 0.08D;
            double jitterZ = (level.random.nextDouble() - 0.5D) * 0.08D;
            level.addParticle(
                    ParticleTypes.END_ROD,
                    location.x + nx + jitterX,
                    location.y + ny + jitterY,
                    location.z + nz + jitterZ,
                    nx * 0.35D + jitterX * 0.15D,
                    ny * 0.35D + jitterY * 0.15D,
                    nz * 0.35D + jitterZ * 0.15D);
        }
    }

    private static void spawnDestroyBurst(Level level, BlockPos pos) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;

        for (int i = 0; i < 32; i++) {
            Vec3 direction = new Vec3(
                    level.random.nextGaussian(),
                    level.random.nextGaussian(),
                    level.random.nextGaussian());
            if (direction.lengthSqr() < 1.0E-6D) {
                direction = new Vec3(0.0D, 1.0D, 0.0D);
            } else {
                direction = direction.normalize();
            }

            double speed = 0.025D + level.random.nextDouble() * 0.065D;
            Vec3 velocity = direction.scale(speed);
            Vec3 origin = direction.scale(level.random.nextDouble() * 0.20D);
            level.addParticle(
                    ParticleTypes.END_ROD,
                    cx + origin.x,
                    cy + origin.y,
                    cz + origin.z,
                    velocity.x,
                    velocity.y,
                    velocity.z);
        }
    }
}

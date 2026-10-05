package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/** TC4 manashroom: luminous magical fungus that confuses creatures touching it. */
public final class ManashroomBlock extends BushBlock {
    public ManashroomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - random.nextDouble()) * 0.4;
            double y = pos.getY() + 0.3;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - random.nextDouble()) * 0.4;
            level.addParticle(new DustParticleOptions(new Vector3f(0.5F, 0.3F, 0.8F), 0.5F), x, y, z, 0.0, 0.015, 0.0);
        }
    }
}

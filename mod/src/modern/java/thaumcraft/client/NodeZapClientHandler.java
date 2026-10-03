package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Client-side TC4-style node discharge zap between a donor node and the consuming node. */
public final class NodeZapClientHandler {
    private NodeZapClientHandler() {
    }

    public static void spawn(BlockPos from, BlockPos to) {
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            return;
        }

        Vec3 start = Vec3.atCenterOf(from);
        Vec3 end = Vec3.atCenterOf(to);
        double distance = start.distanceTo(end);
        int segments = Mth.clamp((int) Math.ceil(distance * 8.0D), 10, 32);

        var random = level.random;
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / (double) segments;
            Vec3 base = start.lerp(end, t);

            // Keep both ends locked to the node centers and let the middle of the bolt wander.
            double envelope = Math.sin(Math.PI * t);
            double jitter = 0.14D * envelope;
            double x = base.x + (random.nextDouble() - 0.5D) * jitter;
            double y = base.y + (random.nextDouble() - 0.5D) * jitter;
            double z = base.z + (random.nextDouble() - 0.5D) * jitter;

            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, 0.0D, 0.0D);
            if ((i & 1) == 0) {
                level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}

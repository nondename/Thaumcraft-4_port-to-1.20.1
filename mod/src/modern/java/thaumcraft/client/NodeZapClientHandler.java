package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Client-side TC4-style node discharge bolt.
 *
 * <p>The original PacketFXBlockZap did not spawn a chain of particles. It created an
 * FXLightningBolt with a 10-tick lifetime, type 0 purple additive rendering and a fractal
 * path. Keep the same shape/lifetime model here and render it as camera-facing ribbons.</p>
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class NodeZapClientHandler {
    private static final int LIFETIME_TICKS = 10;
    private static final List<ActiveZap> ACTIVE = new ArrayList<>();

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
        if (start.distanceToSqr(end) < 1.0E-6D) {
            return;
        }

        ACTIVE.add(new ActiveZap(start, end, level.getGameTime(), level.random.nextLong()));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || ACTIVE.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        ACTIVE.removeIf(zap -> now - zap.createdTick >= LIFETIME_TICKS);
        if (ACTIVE.isEmpty()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();

        for (ActiveZap zap : ACTIVE) {
            float age = Math.max(0.0F, Math.min(1.0F,
                    (float) (now - zap.createdTick) / (float) LIFETIME_TICKS));

            // TC4 type 0 uses a dark purple outer additive pass and a bright pink-white core.
            float outerAlpha = (1.0F - age) * 0.40F;
            float innerAlpha = 1.0F - age * 0.50F;

            drawPath(vertices, pose, camera, zap.mainPath, 0.050F,
                    153, 77, 153, alpha(outerAlpha));
            drawPath(vertices, pose, camera, zap.mainPath, 0.020F,
                    255, 153, 255, alpha(innerAlpha));

            for (List<Vec3> branch : zap.branches) {
                drawPath(vertices, pose, camera, branch, 0.030F,
                        153, 77, 153, alpha(outerAlpha * 0.75F));
                drawPath(vertices, pose, camera, branch, 0.011F,
                        255, 153, 255, alpha(innerAlpha * 0.80F));
            }
        }

        buffers.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    private static int alpha(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }

    /** Render one jagged polyline as a camera-facing ribbon instead of isolated particles. */
    private static void drawPath(VertexConsumer vertices, PoseStack.Pose pose, Vec3 camera,
                                 List<Vec3> points, float width,
                                 int red, int green, int blue, int alpha) {
        if (points.size() < 2 || alpha <= 0) {
            return;
        }

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 start = points.get(i);
            Vec3 end = points.get(i + 1);
            Vec3 direction = end.subtract(start);
            if (direction.lengthSqr() < 1.0E-8D) {
                continue;
            }

            Vec3 midpoint = start.add(end).scale(0.5D);
            Vec3 toCamera = camera.subtract(midpoint);
            Vec3 side = direction.cross(toCamera);
            if (side.lengthSqr() < 1.0E-8D) {
                side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
                if (side.lengthSqr() < 1.0E-8D) {
                    side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
                }
            }
            side = side.normalize().scale(width);

            Vec3 a = start.subtract(side);
            Vec3 b = end.subtract(side);
            Vec3 c = end.add(side);
            Vec3 d = start.add(side);

            vertex(vertices, pose, a, red, green, blue, alpha);
            vertex(vertices, pose, b, red, green, blue, alpha);
            vertex(vertices, pose, c, red, green, blue, alpha);
            vertex(vertices, pose, d, red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point,
                               int red, int green, int blue, int alpha) {
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private static List<Vec3> buildFractalPath(Vec3 start, Vec3 end, Random random,
                                               int iterations, double initialAmplitude) {
        List<Vec3> points = new ArrayList<>();
        points.add(start);
        points.add(end);

        Vec3 axis = end.subtract(start).normalize();
        Vec3 basisA = axis.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (basisA.lengthSqr() < 1.0E-8D) {
            basisA = axis.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        basisA = basisA.normalize();
        Vec3 basisB = axis.cross(basisA).normalize();

        double amplitude = initialAmplitude;
        for (int pass = 0; pass < iterations; pass++) {
            List<Vec3> refined = new ArrayList<>(points.size() * 2 - 1);
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i);
                Vec3 b = points.get(i + 1);
                refined.add(a);

                Vec3 midpoint = a.add(b).scale(0.5D);
                double offA = (random.nextDouble() * 2.0D - 1.0D) * amplitude;
                double offB = (random.nextDouble() * 2.0D - 1.0D) * amplitude;
                midpoint = midpoint.add(basisA.scale(offA)).add(basisB.scale(offB));
                refined.add(midpoint);
            }
            refined.add(points.get(points.size() - 1));
            points = refined;
            amplitude *= 0.52D;
        }
        return points;
    }

    private static List<List<Vec3>> buildBranches(List<Vec3> main, Vec3 start, Vec3 end, Random random) {
        List<List<Vec3>> branches = new ArrayList<>();
        if (main.size() < 8) {
            return branches;
        }

        Vec3 forward = end.subtract(start).normalize();
        double length = start.distanceTo(end);
        int wanted = length > 2.5D ? 3 : 2;
        for (int n = 0; n < wanted; n++) {
            int index = 2 + random.nextInt(Math.max(1, main.size() - 4));
            Vec3 origin = main.get(index);

            Vec3 randomVec = new Vec3(
                    random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 2.0D - 1.0D
            );
            Vec3 lateral = randomVec.subtract(forward.scale(randomVec.dot(forward)));
            if (lateral.lengthSqr() < 1.0E-8D) {
                continue;
            }
            lateral = lateral.normalize();

            double branchLength = length * (0.15D + random.nextDouble() * 0.18D);
            Vec3 branchEnd = origin
                    .add(forward.scale(branchLength * 0.35D))
                    .add(lateral.scale(branchLength));
            branches.add(buildFractalPath(origin, branchEnd, random, 2,
                    Math.max(0.04D, branchLength * 0.12D)));
        }
        return branches;
    }

    private static final class ActiveZap {
        private final long createdTick;
        private final List<Vec3> mainPath;
        private final List<List<Vec3>> branches;

        private ActiveZap(Vec3 start, Vec3 end, long createdTick, long seed) {
            this.createdTick = createdTick;
            Random random = new Random(seed);
            double length = start.distanceTo(end);
            this.mainPath = buildFractalPath(start, end, random, 4,
                    Math.max(0.06D, Math.min(0.45D, length * 0.18D)));
            this.branches = buildBranches(mainPath, start, end, random);
        }
    }
}

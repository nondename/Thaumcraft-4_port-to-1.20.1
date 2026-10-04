package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.sounds.ModSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Client-side port of TC4's type-0 FXLightningBolt used for aura-node discharges. */
public final class NodeZapClientHandler {
    private static final int DURATION = 10;
    private static final int SPEED = 5;
    private static final int MAIN_SEGMENTS = 128; // defaultFractal(): seven 2-way subdivision passes.
    private static final float BASE_WIDTH = 0.03F;
    private static final float MULTIPLIER = 4.0F;
    private static final List<ActiveZap> ACTIVE = new ArrayList<>();

    private NodeZapClientHandler() {
    }

    public static void spawn(BlockPos from, BlockPos to) {
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || from.equals(to)) {
            return;
        }

        long now = level.getGameTime();
        ACTIVE.removeIf(zap -> zap.isExpired(now));
        ACTIVE.add(new ActiveZap(from.immutable(), to.immutable(), now, level.random.nextLong()));

        // Exact TC4 PacketFXBlockZap sound: thaumcraft:zap, volume 0.1, pitch 1.0..1.2.
        level.playLocalSound(
                from.getX() + 0.5D,
                from.getY() + 0.5D,
                from.getZ() + 0.5D,
                ModSounds.ZAP.get(),
                SoundSource.MASTER,
                0.1F,
                1.0F + level.random.nextFloat() * 0.2F,
                false
        );
    }

    /** Render all zaps whose consuming/target node is this block entity. */
    public static void renderForNode(AuraNodeBlockEntity node, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource) {
        if (ACTIVE.isEmpty() || node.getLevel() == null) {
            return;
        }

        long now = node.getLevel().getGameTime();
        ACTIVE.removeIf(zap -> zap.isExpired(now));
        if (ACTIVE.isEmpty()) {
            return;
        }

        BlockPos target = node.getBlockPos();
        Vec3 targetOrigin = Vec3.atLowerCornerOf(target);
        Vec3 cameraLocal = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(targetOrigin);
        PoseStack.Pose pose = poseStack.last();

        // TC4 renders type-0 lightning twice using the original p_large/p_small textures.
        VertexConsumer outerVertices = bufferSource.getBuffer(ThaumcraftRenderTypes.nodeLightning(false));
        VertexConsumer innerVertices = bufferSource.getBuffer(ThaumcraftRenderTypes.nodeLightning(true));

        for (ActiveZap zap : ACTIVE) {
            if (!zap.to.equals(target)) {
                continue;
            }

            int particleAge = zap.ageAt(now);
            float boltAge = particleAge >= 0
                    ? Math.min(1.0F, (float) particleAge / (float) zap.maxAge)
                    : 0.0F;
            float outerAlpha = (1.0F - boltAge) * 0.40F;
            float innerAlpha = 1.0F - boltAge * 0.50F;

            int growLength = Math.max(1, (int) (zap.length * 3.0D));
            float progressiveAge = particleAge + partialTick + growLength;
            int renderLength = (int) (progressiveAge / (float) growLength * MAIN_SEGMENTS);
            renderLength = Math.max(0, Math.min(MAIN_SEGMENTS, renderLength));

            for (BoltSegment segment : zap.segments) {
                if (segment.segmentNo > renderLength) {
                    continue;
                }

                double viewerDistance = cameraLocal.distanceTo(segment.start);
                float width = BASE_WIDTH
                        * ((float) viewerDistance / 5.0F + 1.0F)
                        * (1.0F + segment.light) * 0.5F;

                drawSegment(outerVertices, pose, cameraLocal, segment, width,
                        153, 77, 153, alpha(outerAlpha * segment.light));
                drawSegment(innerVertices, pose, cameraLocal, segment, width,
                        255, 153, 255, alpha(innerAlpha * segment.light));
            }
        }
    }

    private static int alpha(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }

    /** Render one bolt segment as the same camera-facing textured ribbon used by TC4. */
    private static void drawSegment(VertexConsumer vertices, PoseStack.Pose pose, Vec3 camera,
                                    BoltSegment segment, float width,
                                    int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }

        Vec3 direction = segment.end.subtract(segment.start);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }

        Vec3 midpoint = segment.start.add(segment.end).scale(0.5D);
        Vec3 toCamera = camera.subtract(midpoint);
        Vec3 side = direction.cross(toCamera);
        if (side.lengthSqr() < 1.0E-8D) {
            side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() < 1.0E-8D) {
                side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
            }
        }
        side = side.normalize().scale(width);

        // The original bolt samples the middle column of p_large/p_small and lets the texture's
        // vertical falloff form the soft lightning ribbon.
        vertex(vertices, pose, segment.end.subtract(side), 0.5F, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, segment.start.subtract(side), 0.5F, 0.0F, red, green, blue, alpha);
        vertex(vertices, pose, segment.start.add(side), 0.5F, 1.0F, red, green, blue, alpha);
        vertex(vertices, pose, segment.end.add(side), 0.5F, 1.0F, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point,
                               float u, float v, int red, int green, int blue, int alpha) {
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /**
     * Port of FXLightningBoltCommon.defaultFractal(). TC4 subdivides the bolt seven times and
     * creates short forked copies during the first four passes. This replaces the old port's two
     * or three manually-added 15-33% side tentacles.
     */
    private static List<BoltSegment> buildDefaultFractal(Vec3 start, Vec3 end, Random random, double length) {
        List<BoltSegment> segments = new ArrayList<>();
        segments.add(new BoltSegment(start, end, 1.0F, 0, 0));

        double[] divisors = {8.0D, 12.0D, 17.0D, 23.0D, 30.0D, 34.0D, 40.0D};
        float[] splitChances = {0.7F, 0.5F, 0.5F, 0.5F, 0.0F, 0.0F, 0.0F};
        float[] splitAngles = {45.0F, 50.0F, 55.0F, 60.0F, 0.0F, 0.0F, 0.0F};
        int nextSplit = 0;

        for (int pass = 0; pass < divisors.length; pass++) {
            List<BoltSegment> old = segments;
            segments = new ArrayList<>(old.size() * 3);
            double amount = length * MULTIPLIER / divisors[pass];

            for (BoltSegment segment : old) {
                Vec3 difference = segment.end.subtract(segment.start);
                Vec3 midpointBase = segment.start.add(difference.scale(0.5D));
                Vec3 offset = randomPerpendicular(difference, random)
                        .scale((random.nextFloat() - 0.5F) * amount);
                Vec3 midpoint = midpointBase.add(offset);

                int firstNo = segment.segmentNo * 2;
                int secondNo = firstNo + 1;
                segments.add(new BoltSegment(segment.start, midpoint, segment.light, firstNo, segment.splitNo));

                if (splitChances[pass] > 0.0F && random.nextFloat() < splitChances[pass]) {
                    Vec3 nextDifference = segment.end.subtract(midpoint);
                    Vec3 splitAxis = randomPerpendicular(nextDifference, random);
                    double splitAngle = Math.toRadians(
                            (random.nextFloat() * 0.66F + 0.33F) * splitAngles[pass]);
                    Vec3 splitOffset = rotateAroundAxis(nextDifference, splitAxis, splitAngle).scale(0.1D);
                    nextSplit++;
                    segments.add(new BoltSegment(
                            midpoint,
                            segment.end.add(splitOffset),
                            segment.light * 0.5F,
                            secondNo,
                            nextSplit
                    ));
                }

                segments.add(new BoltSegment(midpoint, segment.end, segment.light, secondNo, segment.splitNo));
            }
        }

        return segments;
    }

    private static Vec3 randomPerpendicular(Vec3 vector, Random random) {
        if (vector.lengthSqr() < 1.0E-12D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }

        Vec3 axis = vector.normalize();
        Vec3 perpendicular = axis.cross(new Vec3(1.0D, 0.0D, 0.0D));
        if (perpendicular.lengthSqr() < 1.0E-8D) {
            perpendicular = axis.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        perpendicular = perpendicular.normalize();
        return rotateAroundAxis(perpendicular, axis, random.nextFloat() * Math.PI * 2.0D);
    }

    private static Vec3 rotateAroundAxis(Vec3 vector, Vec3 axis, double angle) {
        Vec3 unitAxis = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return vector.scale(cos)
                .add(unitAxis.cross(vector).scale(sin))
                .add(unitAxis.scale(unitAxis.dot(vector) * (1.0D - cos)));
    }

    private static final class BoltSegment {
        private final Vec3 start;
        private final Vec3 end;
        private final float light;
        private final int segmentNo;
        private final int splitNo;

        private BoltSegment(Vec3 start, Vec3 end, float light, int segmentNo, int splitNo) {
            this.start = start;
            this.end = end;
            this.light = light;
            this.segmentNo = segmentNo;
            this.splitNo = splitNo;
        }
    }

    private static final class ActiveZap {
        private final BlockPos to;
        private final long createdTick;
        private final int initialAge;
        private final int maxAge;
        private final double length;
        private final List<BoltSegment> segments;

        private ActiveZap(BlockPos from, BlockPos to, long createdTick, long seed) {
            this.to = to;
            this.createdTick = createdTick;

            Vec3 start = new Vec3(
                    from.getX() - to.getX() + 0.5D,
                    from.getY() - to.getY() + 0.5D,
                    from.getZ() - to.getZ() + 0.5D
            );
            Vec3 end = new Vec3(0.5D, 0.5D, 0.5D);
            this.length = start.distanceTo(end);
            this.initialAge = -(int) (length * 3.0D);

            // Match FXLightningBoltCommon's random consumption: the base constructor first rolls
            // its default age, then the duration constructor replaces it with duration +/- 50%.
            Random random = new Random(seed);
            random.nextInt(3);
            this.maxAge = DURATION + random.nextInt(DURATION) - DURATION / 2;
            this.segments = buildDefaultFractal(start, end, random, length);
        }

        private int ageAt(long gameTime) {
            return initialAge + (int) Math.max(0L, gameTime - createdTick) * SPEED;
        }

        private boolean isExpired(long gameTime) {
            return ageAt(gameTime) >= maxAge;
        }
    }
}

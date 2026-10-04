package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.sounds.ModSounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Client-side port of TC4's type-0 FXLightningBolt used for aura-node discharges.
 *
 * <p>This deliberately mirrors FXLightningBoltCommon/FXLightningBolt instead of approximating
 * them with independent billboard segments. The original bolt keeps a linked segment graph,
 * smooths the ribbon through neighbouring directions, prunes branch tails after subdivision,
 * sorts branches by light and renders rounded textured caps. Those details are what make the
 * TC4 bolt look like one continuous electrical arc instead of a pile of jagged quads.</p>
 */
public final class NodeZapClientHandler {
    private static final int DURATION = 10;
    private static final int SPEED = 5;
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

        // Exact PacketFXBlockZap sound.
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

    /** Render all zaps whose target/consuming node is this block entity. */
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

        Minecraft minecraft = Minecraft.getInstance();
        BlockPos target = node.getBlockPos();
        Vec3 targetOrigin = Vec3.atLowerCornerOf(target);
        Vec3 viewerLocal = minecraft.player != null
                ? minecraft.player.position().subtract(targetOrigin)
                : minecraft.gameRenderer.getMainCamera().getPosition().subtract(targetOrigin);

        // FXLightningBolt receives one view vector from the particle renderer and uses it for every
        // segment. Keeping it constant is important: recomputing a vector-to-camera per segment
        // twists neighbouring quads independently and creates the ugly rope-like result.
        var camera = minecraft.gameRenderer.getMainCamera();
        Vec3 viewVector = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer outerVertices = bufferSource.getBuffer(ThaumcraftRenderTypes.nodeLightning(false));
        VertexConsumer innerVertices = bufferSource.getBuffer(ThaumcraftRenderTypes.nodeLightning(true));

        for (ActiveZap zap : ACTIVE) {
            if (!zap.to.equals(target)) {
                continue;
            }

            int particleAge = zap.ageAt(now);
            float boltAge = particleAge >= 0
                    ? Mth.clamp((float) particleAge / (float) zap.maxAge, 0.0F, 1.0F)
                    : 0.0F;

            // Type 0 from FXLightningBolt: dark-purple large pass, then pink-white small pass.
            float outerAlpha = (1.0F - boltAge) * 0.40F;
            float innerAlpha = 1.0F - boltAge * 0.50F;

            int growLength = Math.max(1, (int) (zap.length * 3.0D));
            int renderLength = (int) ((particleAge + partialTick + growLength)
                    / (float) growLength * zap.numMainSegments);

            renderPass(outerVertices, pose, viewerLocal, viewVector, zap, renderLength,
                    153, 77, 153, outerAlpha);
            renderPass(innerVertices, pose, viewerLocal, viewVector, zap, renderLength,
                    255, 153, 255, innerAlpha);
        }
    }

    private static void renderPass(VertexConsumer vertices, PoseStack.Pose pose,
                                   Vec3 viewerLocal, Vec3 viewVector,
                                   ActiveZap zap, int renderLength,
                                   int red, int green, int blue, float mainAlpha) {
        for (BoltSegment segment : zap.segments) {
            if (segment.segmentNo > renderLength) {
                continue;
            }

            float width = BASE_WIDTH
                    * ((float) viewerLocal.distanceTo(segment.startPoint.point) / 5.0F + 1.0F)
                    * (1.0F + segment.light) * 0.5F;
            int segmentAlpha = alpha(mainAlpha * segment.light);
            if (segmentAlpha <= 0) {
                continue;
            }

            // Exact TC4 joint construction: cross the fixed camera vector with the averaged
            // previous/next segment directions, then compensate by sin(half-angle). This makes
            // adjacent ribbon quads meet cleanly instead of each segment facing the camera alone.
            Vec3 diff1 = safeScale(viewVector.cross(segment.prevDiff), width / safeSin(segment.sinPrev));
            Vec3 diff2 = safeScale(viewVector.cross(segment.nextDiff), width / safeSin(segment.sinNext));

            Vec3 start = segment.startPoint.point;
            Vec3 end = segment.endPoint.point;

            vertex(vertices, pose, end.subtract(diff2), 0.5F, 0.0F, red, green, blue, segmentAlpha);
            vertex(vertices, pose, start.subtract(diff1), 0.5F, 0.0F, red, green, blue, segmentAlpha);
            vertex(vertices, pose, start.add(diff1), 0.5F, 1.0F, red, green, blue, segmentAlpha);
            vertex(vertices, pose, end.add(diff2), 0.5F, 1.0F, red, green, blue, segmentAlpha);

            // TC4 extends p_large/p_small one ribbon-width beyond each open end. These textured
            // half-quads create the soft rounded tips visible in the original bolt.
            if (segment.next == null) {
                Vec3 roundEnd = end.add(normalizeSafe(segment.diff).scale(width));
                vertex(vertices, pose, roundEnd.subtract(diff2), 0.0F, 0.0F, red, green, blue, segmentAlpha);
                vertex(vertices, pose, end.subtract(diff2), 0.5F, 0.0F, red, green, blue, segmentAlpha);
                vertex(vertices, pose, end.add(diff2), 0.5F, 1.0F, red, green, blue, segmentAlpha);
                vertex(vertices, pose, roundEnd.add(diff2), 0.0F, 1.0F, red, green, blue, segmentAlpha);
            }
            if (segment.prev == null) {
                Vec3 roundEnd = start.subtract(normalizeSafe(segment.diff).scale(width));
                vertex(vertices, pose, start.subtract(diff1), 0.5F, 0.0F, red, green, blue, segmentAlpha);
                vertex(vertices, pose, roundEnd.subtract(diff1), 0.0F, 0.0F, red, green, blue, segmentAlpha);
                vertex(vertices, pose, roundEnd.add(diff1), 0.0F, 1.0F, red, green, blue, segmentAlpha);
                vertex(vertices, pose, start.add(diff1), 0.5F, 1.0F, red, green, blue, segmentAlpha);
            }
        }
    }

    private static double safeSin(double value) {
        return Math.max(1.0E-4D, Math.abs(value));
    }

    private static Vec3 safeScale(Vec3 vector, double scale) {
        if (!Double.isFinite(scale) || vector.lengthSqr() < 1.0E-16D) {
            return Vec3.ZERO;
        }
        return vector.scale(scale);
    }

    private static Vec3 normalizeSafe(Vec3 vector) {
        return vector.lengthSqr() < 1.0E-16D ? Vec3.ZERO : vector.normalize();
    }

    private static int alpha(float value) {
        return Mth.clamp(Math.round(value * 255.0F), 0, 255);
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

    /** Exact seven calls made by FXLightningBoltCommon.defaultFractal(). */
    private static void buildDefaultFractal(BoltGraph graph) {
        graph.fractal(2, graph.length * MULTIPLIER / 8.0D, 0.7F, 0.1D, 45.0D);
        graph.fractal(2, graph.length * MULTIPLIER / 12.0D, 0.5F, 0.1D, 50.0D);
        graph.fractal(2, graph.length * MULTIPLIER / 17.0D, 0.5F, 0.1D, 55.0D);
        graph.fractal(2, graph.length * MULTIPLIER / 23.0D, 0.5F, 0.1D, 60.0D);
        graph.fractal(2, graph.length * MULTIPLIER / 30.0D, 0.0F, 0.0D, 0.0D);
        graph.fractal(2, graph.length * MULTIPLIER / 34.0D, 0.0F, 0.0D, 0.0D);
        graph.fractal(2, graph.length * MULTIPLIER / 40.0D, 0.0F, 0.0D, 0.0D);
        graph.finalizeBolt();
    }

    /** Modern Vec3 version of the old WRVector3.getPerpendicular(). Intentionally NOT normalized. */
    private static Vec3 getPerpendicular(Vec3 vector) {
        if (vector.z == 0.0D) {
            // WRVector3.zCrossProduct(vec) -> [-y, x, 0]
            return new Vec3(-vector.y, vector.x, 0.0D);
        }
        // WRVector3.xCrossProduct(vec) -> [0, z, -y]
        return new Vec3(0.0D, vector.z, -vector.y);
    }

    /** Modern Vec3 version of WRVector3.xCrossProduct(). Intentionally NOT normalized. */
    private static Vec3 xCrossProduct(Vec3 vector) {
        return new Vec3(0.0D, vector.z, -vector.y);
    }

    private static Vec3 rotateDegrees(Vec3 vector, double degrees, Vec3 axis) {
        if (axis.lengthSqr() < 1.0E-16D) {
            return vector;
        }
        Vec3 unitAxis = axis.normalize();
        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // Rodrigues rotation; equivalent to WRMat4.rotationMat(...).translate(...).
        return vector.scale(cos)
                .add(unitAxis.cross(vector).scale(sin))
                .add(unitAxis.scale(unitAxis.dot(vector) * (1.0D - cos)));
    }

    private static double anglePreNorm(Vec3 a, Vec3 b) {
        return Math.acos(Mth.clamp(a.dot(b), -1.0D, 1.0D));
    }

    private static final class BoltPoint {
        private final Vec3 point;
        private final Vec3 basePoint;
        private final Vec3 offsetVec;

        private BoltPoint(Vec3 basePoint, Vec3 offsetVec) {
            this.basePoint = basePoint;
            this.offsetVec = offsetVec;
            this.point = basePoint.add(offsetVec);
        }

        private static BoltPoint plain(Vec3 point) {
            return new BoltPoint(point, Vec3.ZERO);
        }
    }

    private static final class BoltSegment {
        private final BoltPoint startPoint;
        private final BoltPoint endPoint;
        private final float light;
        private final int segmentNo;
        private final int splitNo;
        private final Vec3 diff;

        private BoltSegment prev;
        private BoltSegment next;
        private Vec3 prevDiff;
        private Vec3 nextDiff;
        private double sinPrev = 1.0D;
        private double sinNext = 1.0D;

        private BoltSegment(BoltPoint startPoint, BoltPoint endPoint,
                            float light, int segmentNo, int splitNo) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.light = light;
            this.segmentNo = segmentNo;
            this.splitNo = splitNo;
            this.diff = endPoint.point.subtract(startPoint.point);
        }

        private void calcEndDiffs() {
            Vec3 thisNorm = normalizeSafe(diff);

            if (prev != null) {
                Vec3 prevNorm = normalizeSafe(prev.diff);
                prevDiff = normalizeSafe(thisNorm.add(prevNorm));
                sinPrev = Math.sin(anglePreNorm(thisNorm, prevNorm.scale(-1.0D)) / 2.0D);
            } else {
                prevDiff = thisNorm;
                sinPrev = 1.0D;
            }

            if (next != null) {
                Vec3 nextNorm = normalizeSafe(next.diff);
                nextDiff = normalizeSafe(thisNorm.add(nextNorm));
                sinNext = Math.sin(anglePreNorm(thisNorm, nextNorm.scale(-1.0D)) / 2.0D);
            } else {
                nextDiff = thisNorm;
                sinNext = 1.0D;
            }
        }
    }

    /** Faithful stateful port of FXLightningBoltCommon's segment graph. */
    private static final class BoltGraph {
        private List<BoltSegment> segments = new ArrayList<>();
        private final Map<Integer, Integer> splitParents = new HashMap<>();
        private final Random random;
        private final double length;
        private int numMainSegments = 1;
        private int numSplits = 0;
        private boolean finalized = false;

        private BoltGraph(Vec3 start, Vec3 end, Random random) {
            this.random = random;
            this.length = end.subtract(start).length();
            this.segments.add(new BoltSegment(BoltPoint.plain(start), BoltPoint.plain(end), 1.0F, 0, 0));
        }

        private void fractal(int splits, double amount, float splitChance,
                             double splitLength, double splitAngle) {
            if (finalized) {
                return;
            }

            List<BoltSegment> oldSegments = segments;
            segments = new ArrayList<>();
            BoltSegment prev = null;

            for (BoltSegment segment : oldSegments) {
                prev = segment.prev;
                Vec3 subSegment = segment.diff.scale(1.0D / splits);
                BoltPoint[] newPoints = new BoltPoint[splits + 1];
                Vec3 startPoint = segment.startPoint.point;
                newPoints[0] = segment.startPoint;
                newPoints[splits] = segment.endPoint;

                for (int i = 1; i < splits; i++) {
                    // This is intentionally unnormalised, exactly like WRVector3.getPerpendicular.
                    // Its magnitude shrinks together with the current segment and is the crucial
                    // reason TC4's later fractal passes become fine detail instead of violent noise.
                    Vec3 randomOffset = rotateDegrees(
                            getPerpendicular(segment.diff),
                            random.nextFloat() * 360.0D,
                            segment.diff
                    ).scale((random.nextFloat() - 0.5F) * amount);
                    Vec3 basePoint = startPoint.add(subSegment.scale(i));
                    newPoints[i] = new BoltPoint(basePoint, randomOffset);
                }

                for (int i = 0; i < splits; i++) {
                    BoltSegment next = new BoltSegment(
                            newPoints[i], newPoints[i + 1], segment.light,
                            segment.segmentNo * splits + i, segment.splitNo
                    );
                    next.prev = prev;
                    if (prev != null) {
                        prev.next = next;
                    }

                    if (i != 0 && random.nextFloat() < splitChance) {
                        Vec3 splitRot = rotateDegrees(
                                xCrossProduct(next.diff),
                                random.nextFloat() * 360.0D,
                                next.diff
                        );
                        Vec3 branchDiff = rotateDegrees(
                                next.diff,
                                (random.nextFloat() * 0.66F + 0.33F) * splitAngle,
                                splitRot
                        ).scale(splitLength);

                        numSplits++;
                        splitParents.put(numSplits, next.splitNo);
                        BoltPoint branchEnd = new BoltPoint(
                                newPoints[i + 1].basePoint,
                                newPoints[i + 1].offsetVec.add(branchDiff)
                        );
                        BoltSegment branch = new BoltSegment(
                                newPoints[i], branchEnd,
                                segment.light / 2.0F,
                                next.segmentNo,
                                numSplits
                        );
                        branch.prev = prev;
                        segments.add(branch);
                    }

                    prev = next;
                    segments.add(next);
                }

                if (segment.next != null) {
                    segment.next.prev = prev;
                }
            }

            numMainSegments *= splits;
        }

        private void finalizeBolt() {
            if (finalized) {
                return;
            }
            finalized = true;

            // FXLightningBoltCommon.calculateCollisionAndDiffs(): find the last segment belonging
            // to every split and remove branch subdivisions that have run past their parent tail.
            segments.sort(Comparator
                    .comparingInt((BoltSegment segment) -> segment.splitNo)
                    .thenComparingInt(segment -> segment.segmentNo));

            Map<Integer, Integer> lastActiveSegment = new HashMap<>();
            int lastSplitCalc = 0;
            int lastActive = 0;
            for (BoltSegment segment : segments) {
                if (segment.splitNo > lastSplitCalc) {
                    lastActiveSegment.put(lastSplitCalc, lastActive);
                    lastSplitCalc = segment.splitNo;
                    Integer parent = splitParents.get(segment.splitNo);
                    lastActive = parent == null ? 0 : lastActiveSegment.getOrDefault(parent, 0);
                }
                lastActive = segment.segmentNo;
            }
            lastActiveSegment.put(lastSplitCalc, lastActive);

            lastSplitCalc = 0;
            lastActive = lastActiveSegment.getOrDefault(0, Integer.MAX_VALUE);
            Iterator<BoltSegment> iterator = segments.iterator();
            while (iterator.hasNext()) {
                BoltSegment segment = iterator.next();
                if (lastSplitCalc != segment.splitNo) {
                    lastSplitCalc = segment.splitNo;
                    lastActive = lastActiveSegment.getOrDefault(segment.splitNo, Integer.MAX_VALUE);
                }
                if (segment.segmentNo > lastActive) {
                    iterator.remove();
                } else {
                    segment.calcEndDiffs();
                }
            }

            // Original SegmentLightSorter draws full-light main segments before dimmer branches.
            segments.sort((a, b) -> Float.compare(b.light, a.light));
        }
    }

    private static final class ActiveZap {
        private final BlockPos to;
        private final long createdTick;
        private final int initialAge;
        private final int maxAge;
        private final double length;
        private final int numMainSegments;
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

            Random random = new Random(seed);
            // FXLightningBoltCommon base constructor consumes one nextInt(3) before the duration
            // constructor replaces particleMaxAge with duration +/- 50%.
            random.nextInt(3);
            this.maxAge = DURATION + random.nextInt(DURATION) - DURATION / 2;

            BoltGraph graph = new BoltGraph(start, end, random);
            buildDefaultFractal(graph);
            this.numMainSegments = graph.numMainSegments;
            this.segments = List.copyOf(graph.segments);
        }

        private int ageAt(long gameTime) {
            return initialAge + (int) Math.max(0L, gameTime - createdTick) * SPEED;
        }

        private boolean isExpired(long gameTime) {
            return ageAt(gameTime) >= maxAge;
        }
    }
}

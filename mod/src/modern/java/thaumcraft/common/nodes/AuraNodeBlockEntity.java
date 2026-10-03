package thaumcraft.common.nodes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.config.EntityAspects;
import thaumcraft.common.lib.network.ModNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent current/base vis, type and modifier for a TC4-style aura node.
 *
 * <p>The server tick mirrors the important parts of TC4 4.2.3.5 TileNode: neighbouring stronger
 * nodes discharge weaker nodes, current vis recharges toward base vis, drained aspects decay,
 * Hungry nodes pull in entities and eat nearby breakable blocks, and elapsed real time can refill
 * a node after its chunk has been unloaded. Node stabilizer locks are added when the stabilizer
 * block itself is ported.</p>
 */
public final class AuraNodeBlockEntity extends BlockEntity implements INode {
    /** TC4 Config.hardNode defaults to true. Kept as parity until the modern config screen exists. */
    private static final boolean HARD_NODE_BEHAVIOUR = true;

    private final AspectList aspects = new AspectList();
    private final AspectList baseAspects = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier;
    private String nodeId = "";

    private int count;
    private int regeneration = -1;
    private int wait;
    private long lastActive;
    private boolean catchUp;

    public AuraNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModNodes.AURA_NODE_ENTITY.get(), pos, state);
    }

    public void initialize(NodeType type, NodeModifier modifier, AspectList generatedAspects) {
        nodeType = type == null ? NodeType.NORMAL : type;
        nodeModifier = modifier;
        aspects.aspects.clear();
        baseAspects.aspects.clear();
        if (generatedAspects != null) {
            aspects.add(generatedAspects);
            baseAspects.add(generatedAspects);
        }
        regeneration = -1;
        wait = 0;
        catchUp = false;
        ensureNodeId();
        setChangedAndSync();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AuraNodeBlockEntity node) {
        if (!(level instanceof ServerLevel serverLevel) || node.isRemoved()) {
            return;
        }
        node.tickServer(serverLevel);
    }

    private void tickServer(ServerLevel level) {
        // TC4 calls the first Hungry-node pass before incrementing TileNode.count.
        boolean changed = handleHungryEntities(level);
        count++;

        changed |= handleDischarge(level);
        changed |= handleRecharge(level);
        changed |= handleHungryBlocks(level);
        if (changed && !isRemoved()) {
            setChangedAndSync();
        }
    }

    /**
     * TC4 TileNode.handleHungryNodeFirst with Config.hardNode=true: living/vulnerable entities in
     * a 15-block radius are dragged toward the node. Anything reaching the core takes void damage;
     * when that kills an entity the node can replenish or permanently gain one primal aspect from
     * the victim.
     */
    private boolean handleHungryEntities(ServerLevel level) {
        if (nodeType != NodeType.HUNGRY || !HARD_NODE_BEHAVIOUR) {
            return false;
        }

        Vec3 center = Vec3.atCenterOf(worldPosition);
        AABB bounds = new AABB(worldPosition).inflate(15.0D);
        boolean changed = false;

        for (Entity entity : level.getEntities((Entity) null, bounds, Entity::isAlive)) {
            if (entity instanceof Player player && player.getAbilities().invulnerable) {
                continue;
            }
            if (entity.isInvulnerable()) {
                continue;
            }

            // Original getDistanceTo() returns squared distance and compares it with 2.0.
            if (center.distanceToSqr(entity.position()) < 2.0D) {
                entity.hurt(level.damageSources().fellOutOfWorld(), 1.0F);
                if (!entity.isAlive()) {
                    changed |= absorbKilledEntity(level, entity);
                }
            }

            Vec3 towardNode = center.subtract(entity.position()).scale(1.0D / 15.0D);
            double normalizedDistance = towardNode.length();
            double pull = 1.0D - normalizedDistance;
            if (pull <= 0.0D || normalizedDistance < 1.0E-6D) {
                continue;
            }

            pull *= pull;
            Vec3 direction = towardNode.scale(1.0D / normalizedDistance);
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(
                    motion.x + direction.x * pull * 0.15D,
                    motion.y + direction.y * pull * 0.25D,
                    motion.z + direction.z * pull * 0.15D
            );
        }
        return changed;
    }

    private boolean absorbKilledEntity(ServerLevel level, Entity entity) {
        AspectList scanned = EntityAspects.get(entity);
        if (scanned == null || scanned.size() <= 0) {
            return false;
        }

        AspectList primals = reduceToPrimals(scanned);
        if (primals.size() <= 0) {
            return false;
        }

        Aspect[] candidates = primals.getAspects();
        Aspect aspect = candidates[level.random.nextInt(primals.size())];
        if (aspect == null) {
            return false;
        }

        if (aspects.getAmount(aspect) < baseAspects.getAmount(aspect)) {
            addCurrentCapped(aspect, 1);
            return true;
        }

        // Exact TC4 Hungry-node growth test: rand(1 + base*2) < victim primal amount.
        int bound = 1 + baseAspects.getAmount(aspect) * 2;
        if (level.random.nextInt(Math.max(1, bound)) < primals.getAmount(aspect)) {
            baseAspects.add(aspect, 1);
            return true;
        }
        return false;
    }

    private static AspectList reduceToPrimals(AspectList source) {
        AspectList out = new AspectList();
        for (Aspect aspect : source.getAspects()) {
            if (aspect != null) {
                addPrimalComponents(out, aspect, source.getAmount(aspect));
            }
        }
        return out;
    }

    private static void addPrimalComponents(AspectList out, Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) {
            return;
        }
        if (aspect.isPrimal()) {
            out.add(aspect, amount);
            return;
        }
        Aspect[] components = aspect.getComponents();
        if (components == null || components.length == 0) {
            out.add(aspect, amount);
            return;
        }
        for (Aspect component : components) {
            addPrimalComponents(out, component, amount);
        }
    }

    /**
     * TC4 TileNode.handleHungryNodeSecond. Every 50 ticks the node traces toward a random point up
     * to roughly 15 blocks away and destroys the first non-air block if its hardness is in [0, 5).
     */
    private boolean handleHungryBlocks(ServerLevel level) {
        if (nodeType != NodeType.HUNGRY || count % 50 != 0) {
            return false;
        }

        int tx = worldPosition.getX() + level.random.nextInt(16) - level.random.nextInt(16);
        int ty = worldPosition.getY() + level.random.nextInt(16) - level.random.nextInt(16);
        int tz = worldPosition.getZ() + level.random.nextInt(16) - level.random.nextInt(16);
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, tx, tz);
        if (ty > surface) {
            ty = surface;
        }

        BlockPos target = firstHungryRayHit(level, Vec3.atCenterOf(worldPosition),
                new Vec3(tx + 0.5D, ty + 0.5D, tz + 0.5D));
        if (target == null || Vec3.atCenterOf(worldPosition).distanceToSqr(Vec3.atCenterOf(target)) >= 256.0D) {
            return false;
        }

        BlockState state = level.getBlockState(target);
        float hardness = state.getDestroySpeed(level, target);
        if (hardness >= 0.0F && hardness < 5.0F) {
            level.destroyBlock(target, true);
        }
        return false;
    }

    /**
     * TC4 used rayTraceIgnoringSource. Sampling the segment at quarter-block resolution preserves
     * the important rule (first encountered block, source ignored) without depending on an Entity
     * collision context, which a block entity does not have on 1.20.1.
     */
    private BlockPos firstHungryRayHit(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(1, (int) Math.ceil(delta.length() * 4.0D));
        for (int step = 1; step <= steps; step++) {
            Vec3 point = start.add(delta.scale((double) step / (double) steps));
            BlockPos pos = BlockPos.containing(point);
            if (pos.equals(worldPosition)) {
                continue;
            }
            if (!level.getBlockState(pos).isAir()) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** TC4 TileNode.handleDischarge: a stronger nearby node gradually consumes a weaker one. */
    private boolean handleDischarge(ServerLevel level) {
        if (nodeModifier == NodeModifier.FADING) {
            return false;
        }

        boolean shiny = nodeType == NodeType.HUNGRY || nodeModifier == NodeModifier.BRIGHT;
        int interval = nodeModifier == null ? 2 : (shiny ? 1 : (nodeModifier == NodeModifier.PALE ? 3 : 2));
        if (count % interval != 0) {
            return false;
        }

        var random = level.random;
        int dx = random.nextInt(5) - random.nextInt(5);
        int dy = random.nextInt(5) - random.nextInt(5);
        int dz = random.nextInt(5) - random.nextInt(5);
        if (nodeModifier == NodeModifier.PALE && random.nextBoolean()) {
            return false;
        }
        if (dx == 0 && dy == 0 && dz == 0) {
            return false;
        }

        BlockPos donorPos = worldPosition.offset(dx, dy, dz);
        if (!(level.getBlockEntity(donorPos) instanceof AuraNodeBlockEntity donor) || donor.isRemoved()) {
            return false;
        }

        int donorStrength = (donor.aspects.visSize() + donor.baseAspects.visSize()) / 2;
        int thisStrength = (aspects.visSize() + baseAspects.visSize()) / 2;
        if (donorStrength >= thisStrength || donor.aspects.size() <= 0) {
            return false;
        }

        Aspect[] donorAspects = donor.aspects.getAspects();
        if (donorAspects.length == 0) {
            return false;
        }
        Aspect aspect = donorAspects[random.nextInt(donorAspects.length)];
        if (aspect == null) {
            return false;
        }

        boolean consumed = false;
        if (aspects.getAmount(aspect) < baseAspects.getAmount(aspect) && donor.takeCurrent(aspect, 1)) {
            addCurrentCapped(aspect, 1);
            consumed = true;
        } else if (donor.takeCurrent(aspect, 1)) {
            // Original chance: 1 / (1 + floor(base / multiplier)). Hungry and Bright use 1.5.
            double divisor = shiny ? 1.5D : 1.0D;
            int bound = 1 + (int) (baseAspects.getAmount(aspect) / divisor);
            if (random.nextInt(Math.max(1, bound)) == 0) {
                baseAspects.add(aspect, 1);

                if (nodeModifier == NodeModifier.PALE && random.nextInt(100) == 0) {
                    nodeModifier = null;
                    regeneration = -1;
                }

                // In TC4 the donor permanently loses one point of base vis one third of the time.
                if (random.nextInt(3) == 0) {
                    donor.decreaseBase(aspect, 1);
                }
            }
            consumed = true;
        }

        if (!consumed) {
            return false;
        }

        donor.ensureRegeneration();
        donor.wait = donor.regeneration / 2;
        donor.setChangedAndSync();
        ModNetwork.sendAuraNodeZap(level, donorPos, worldPosition);
        return true;
    }

    /** TC4 TileNode.handleRecharge including unloaded-chunk catch-up and drained-aspect decay. */
    private boolean handleRecharge(ServerLevel level) {
        ensureRegeneration();
        boolean changed = false;

        if (catchUp) {
            catchUp = false;
            long intervalMillis = (long) regeneration * 75L;
            int amount = intervalMillis > 0L
                    ? (int) ((System.currentTimeMillis() - lastActive) / intervalMillis)
                    : 0;
            if (amount > 0) {
                int attempts = Math.min(amount, baseAspects.visSize());
                for (int i = 0; i < attempts; i++) {
                    List<Aspect> depleted = getDepletedAspects();
                    if (depleted.isEmpty()) {
                        break;
                    }
                    Aspect aspect = depleted.get(level.random.nextInt(depleted.size()));
                    addCurrentCapped(aspect, 1);
                    changed = true;
                }
            }
        }

        if (count % 1200 == 0) {
            Aspect[] current = aspects.getAspects();
            for (Aspect aspect : current) {
                if (aspect == null || aspects.getAmount(aspect) > 0) {
                    continue;
                }

                decreaseBase(aspect, 1);
                if (level.random.nextInt(20) == 0 || baseAspects.getAmount(aspect) <= 0) {
                    aspects.remove(aspect);
                    baseAspects.remove(aspect);

                    if (level.random.nextInt(5) == 0) {
                        if (nodeModifier == NodeModifier.BRIGHT) {
                            nodeModifier = null;
                        } else if (nodeModifier == null) {
                            nodeModifier = NodeModifier.PALE;
                        }
                        if (nodeModifier == NodeModifier.PALE && level.random.nextInt(5) == 0) {
                            nodeModifier = NodeModifier.FADING;
                        }
                        regeneration = -1;
                    }
                }
                changed = true;
                break;
            }

            if (aspects.size() <= 0) {
                level.removeBlock(worldPosition, false);
                return false;
            }
        }

        if (wait > 0) {
            wait--;
        }

        if (regeneration > 0 && wait == 0 && count % regeneration == 0) {
            lastActive = System.currentTimeMillis();
            List<Aspect> depleted = getDepletedAspects();
            if (!depleted.isEmpty()) {
                Aspect aspect = depleted.get(level.random.nextInt(depleted.size()));
                addCurrentCapped(aspect, 1);
                changed = true;
            }
        }

        return changed;
    }

    private List<Aspect> getDepletedAspects() {
        List<Aspect> depleted = new ArrayList<>();
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect != null && aspects.getAmount(aspect) < baseAspects.getAmount(aspect)) {
                depleted.add(aspect);
            }
        }
        return depleted;
    }

    private void ensureRegeneration() {
        if (regeneration >= 0) {
            return;
        }
        regeneration = 600;
        if (nodeModifier == NodeModifier.BRIGHT) {
            regeneration = 400;
        } else if (nodeModifier == NodeModifier.PALE) {
            regeneration = 900;
        } else if (nodeModifier == NodeModifier.FADING) {
            regeneration = 0;
        }
    }

    private boolean takeCurrent(Aspect aspect, int amount) {
        return aspect != null && amount > 0 && aspects.reduce(aspect, amount);
    }

    private void addCurrentCapped(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) {
            return;
        }
        int room = Math.max(0, baseAspects.getAmount(aspect) - aspects.getAmount(aspect));
        if (room > 0) {
            aspects.add(aspect, Math.min(room, amount));
        }
    }

    private void decreaseBase(Aspect aspect, int amount) {
        if (aspect == null || amount <= 0) {
            return;
        }
        int next = Math.max(0, baseAspects.getAmount(aspect) - amount);
        if (next <= 0) {
            baseAspects.remove(aspect);
        } else {
            baseAspects.remove(aspect);
            baseAspects.add(aspect, next);
        }
    }

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public AspectList getBaseAspects() {
        return baseAspects;
    }

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType type) {
        nodeType = type == null ? NodeType.NORMAL : type;
        setChangedAndSync();
    }

    @Override
    public NodeModifier getNodeModifier() {
        return nodeModifier;
    }

    public void setNodeModifier(NodeModifier modifier) {
        nodeModifier = modifier;
        regeneration = -1;
        setChangedAndSync();
    }

    @Override
    public String getNodeId() {
        ensureNodeId();
        return nodeId;
    }

    /** ResourceLocation-safe, stable per-world-position discovery key. */
    public String getScanKey() {
        ensureNodeId();
        return nodeId.replace(':', '/').replace(',', '/').replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }

    private void ensureNodeId() {
        if (!nodeId.isEmpty()) return;
        String dimension = level == null ? "unknown" : level.dimension().location().toString();
        nodeId = dimension + ":" + worldPosition.getX() + ":" + worldPosition.getY() + ":" + worldPosition.getZ();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ensureNodeId();
        tag.putString("nodeId", nodeId);
        tag.putString("type", nodeType.name());
        tag.putString("modifier", nodeModifier == null ? "" : nodeModifier.name());
        tag.putLong("lastActive", lastActive);
        aspects.writeToNBT(tag, "Aspects");
        baseAspects.writeToNBT(tag, "AspectsBase");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nodeId = tag.getString("nodeId");
        nodeType = parseType(tag.getString("type"));
        nodeModifier = parseModifier(tag.getString("modifier"));
        lastActive = tag.getLong("lastActive");
        regeneration = -1;
        wait = 0;
        catchUp = false;
        aspects.readFromNBT(tag, "Aspects");
        baseAspects.readFromNBT(tag, "AspectsBase");
        if (baseAspects.size() == 0 && aspects.size() > 0) {
            baseAspects.add(aspects);
        }

        int regen = regenerationForModifier(nodeModifier);
        long intervalMillis = (long) regen * 75L;
        long now = System.currentTimeMillis();
        if (regen > 0 && lastActive > 0L && now > lastActive + intervalMillis) {
            catchUp = true;
        }
    }

    private static int regenerationForModifier(NodeModifier modifier) {
        if (modifier == NodeModifier.BRIGHT) return 400;
        if (modifier == NodeModifier.PALE) return 900;
        if (modifier == NodeModifier.FADING) return 0;
        return 600;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static NodeType parseType(String value) {
        try {
            return NodeType.valueOf(value);
        } catch (Exception ignored) {
            return NodeType.NORMAL;
        }
    }

    private static NodeModifier parseModifier(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return NodeModifier.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}

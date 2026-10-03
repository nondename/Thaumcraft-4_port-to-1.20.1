package thaumcraft.common.items.tools;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.nodes.AuraNodeBlockEntity;

/** One target resolver shared by the server scan and the lens readout. */
public final class ThaumometerTargets {
    /** TC4 ItemThaumometer#doScan: blocks via getMovingObjectPositionFromPlayer (5 blocks). */
    private static final double BLOCK_RANGE = 5.0;
    /** TC4 ItemThaumometer#doScan: entities via EntityUtils#getPointedEntity (10 blocks). */
    private static final double ENTITY_RANGE = 10.0;
    /** TC4 EntityUtils#getPointedEntity minrange argument. */
    private static final double ENTITY_MIN_RANGE = 0.5;

    public record Target(ItemStack stack, Component name, String identity,
                         net.minecraft.world.entity.Entity entity, AuraNodeBlockEntity node,
                         AspectList forced) {
        public Target(ItemStack stack, Component name, String identity) {
            this(stack, name, identity, null, null, null);
        }

        public Target(ItemStack stack, Component name, String identity, net.minecraft.world.entity.Entity entity) {
            this(stack, name, identity, entity, null, null);
        }

        public static Target node(AuraNodeBlockEntity node) {
            return new Target(ItemStack.EMPTY, Component.literal("Aura Node"),
                    "node:" + node.getNodeId(), null, node, null);
        }

        public AspectList aspects(net.minecraft.world.level.Level level) {
            if (forced != null) return forced;
            if (node != null) return node.getAspects();
            return entity == null ? thaumcraft.common.config.RecipeAspects.get(stack, level)
                    : thaumcraft.common.config.EntityAspects.get(entity);
        }

        /**
         * TC4 ScanManager#generateNodeAspects: scanning a node pays out max(4, amount / 10)
         * per aspect plus the node-type bonus, while the lens keeps showing the raw sums.
         */
        public AspectList awardAspects(net.minecraft.world.level.Level level) {
            if (node == null) return aspects(level);
            AspectList raw = node.getAspects();
            AspectList out = new AspectList();
            for (Aspect aspect : raw.getAspectsSorted()) {
                if (aspect != null) out.merge(aspect, Math.max(4, raw.getAmount(aspect) / 10));
            }
            switch (node.getNodeType()) {
                case UNSTABLE -> out.merge(Aspect.ENTROPY, 4);
                case HUNGRY -> out.merge(Aspect.HUNGER, 4);
                case TAINTED -> out.merge(Aspect.TAINT, 4);
                case PURE -> {
                    out.merge(Aspect.HEAL, 2);
                    out.add(Aspect.ORDER, 2);
                }
                case DARK -> {
                    out.merge(Aspect.DEATH, 2);
                    out.add(Aspect.DARKNESS, 2);
                }
                case NORMAL -> { }
            }
            return out.size() > 0 ? out : null;
        }

        private ResourceLocation discoveryId() {
            if (node != null) {
                return ResourceLocation.fromNamespaceAndPath("thaumcraft", "node/" + node.getScanKey());
            }
            if (entity == null) {
                if (forced != null) {
                    // Fluids: TC4 hashed fluid blocks by Block id, so water and a water bucket differ.
                    return ResourceLocation.fromNamespaceAndPath("thaumcraft",
                            "block/" + identity.substring("item:".length()).replace(':', '/'));
                }
                return BuiltInRegistries.ITEM.getKey(stack.getItem());
            }
            if (entity instanceof Player player) {
                // TC4 ScanManager#generateEntityHash: "player_" + name, tracked per player.
                return ResourceLocation.fromNamespaceAndPath("thaumcraft",
                        "player/" + player.getScoreboardName().toLowerCase(java.util.Locale.ROOT));
            }
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                    id.getPath() + thaumcraft.common.config.EntityAspects.variant(entity));
        }

        public boolean scanned(thaumcraft.common.lib.capabilities.IThaumometerKnowledge knowledge) {
            if (node != null) return knowledge.hasScannedEntity(discoveryId());
            return entity == null ? knowledge.hasScannedItem(discoveryId()) : knowledge.hasScannedEntity(discoveryId());
        }

        public void markScanned(thaumcraft.common.lib.capabilities.IThaumometerKnowledge knowledge) {
            if (node != null) {
                knowledge.scanEntity(discoveryId());
            } else if (entity == null) {
                knowledge.scanItem(discoveryId());
            } else {
                knowledge.scanEntity(discoveryId());
            }
        }

        /**
         * TC4 ItemThaumometerRenderer: aura nodes always read out as “?” — the node type and
         * modifier live on their own colored line drawn by the renderer after a scan.
         */
        public Component readoutName(thaumcraft.common.lib.capabilities.IThaumometerKnowledge knowledge) {
            if (node != null) return Component.literal("?");
            return name;
        }
    }

    public static Target find(Player player) {
        var level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 entityEnd = start.add(look.scale(ENTITY_RANGE));
        Vec3 blockEnd = start.add(look.scale(BLOCK_RANGE));

        // TC4 EntityUtils#getPointedEntity: nearest bounding-box hit with a clear line of
        // sight wins and outranks the block, nothing closer than the 0.5-block minrange.
        net.minecraft.world.entity.Entity selected = null;
        double nearestEntity = Double.MAX_VALUE;
        for (var entity : level.getEntities(player, player.getBoundingBox()
                .expandTowards(entityEnd.subtract(start)).inflate(1),
                e -> e.isAlive() && !e.isSpectator())) {
            if (entity.distanceTo(player) < ENTITY_MIN_RANGE) continue;
            var los = level.clip(new ClipContext(start, entity.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (los.getType() != HitResult.Type.MISS) continue;
            var box = entity.getBoundingBox().inflate(Math.max(0.8F, entity.getPickRadius()));
            var intersection = box.contains(start) ? java.util.Optional.of(start) : box.clip(start, entityEnd);
            if (intersection.isPresent()) {
                double distance = start.distanceToSqr(intersection.get());
                if (distance < nearestEntity) {
                    nearestEntity = distance;
                    selected = entity;
                }
            }
        }
        if (selected != null && !(selected instanceof ItemEntity)) {
            return new Target(ItemStack.EMPTY, selected.getDisplayName(), "entity:" + selected.getUUID()
                    + thaumcraft.common.config.EntityAspects.variant(selected), selected);
        }
        if (selected instanceof ItemEntity dropped) {
            ItemStack stack = dropped.getItem().copy();
            stack.setCount(1);
            return new Target(stack, stack.getHoverName(), "entity:" + selected.getUUID() + ":" + itemKey(stack));
        }

        // TC4 Item#getMovingObjectPositionFromPlayer(world, player, true): 5 blocks,
        // and the ray stops on liquids so fluids are scan targets themselves.
        var hit = level.clip(new ClipContext(start, blockEnd, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        var pos = hit.getBlockPos();
        if (level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
            return Target.node(node);
        }

        var state = level.getBlockState(pos);
        ItemStack stack = state.getBlock().getCloneItemStack(state, hit, level, pos, player);
        if (stack.isEmpty() || state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
            // TC4 keeps itemless blocks scannable (fluids, fire, end portal): ConfigAspects
            // registers them as block tags and hashes them by Block id, never by a bucket.
            AspectList forced = thaumcraft.api.ThaumcraftApi.getBlockAspects(state.getBlock());
            if (forced == null) return null;
            return new Target(ItemStack.EMPTY, state.getBlock().getName(),
                    "item:" + BuiltInRegistries.BLOCK.getKey(state.getBlock()), null, null, forced);
        }
        stack = stack.copy();
        stack.setCount(1);
        return new Target(stack, state.getBlock().getName(), "item:" + itemKey(stack));
    }

    private static String itemKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + ":" + (stack.hasTag() ? stack.getTag() : "");
    }

    private ThaumometerTargets() {
    }
}

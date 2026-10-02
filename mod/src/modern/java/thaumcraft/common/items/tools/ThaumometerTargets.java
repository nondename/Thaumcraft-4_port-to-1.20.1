package thaumcraft.common.items.tools;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.nodes.AuraNodeBlockEntity;

/** One target resolver shared by the server scan and the lens readout. */
public final class ThaumometerTargets {
    public record Target(ItemStack stack, Component name, String identity,
                         net.minecraft.world.entity.Entity entity, AuraNodeBlockEntity node) {
        public Target(ItemStack stack, Component name, String identity) {
            this(stack, name, identity, null, null);
        }

        public Target(ItemStack stack, Component name, String identity, net.minecraft.world.entity.Entity entity) {
            this(stack, name, identity, entity, null);
        }

        public static Target node(AuraNodeBlockEntity node) {
            return new Target(ItemStack.EMPTY, Component.literal("Aura Node"),
                    "node:" + node.getNodeId(), null, node);
        }

        public thaumcraft.api.aspects.AspectList aspects(net.minecraft.world.level.Level level) {
            if (node != null) return node.getAspects();
            return entity == null ? thaumcraft.common.config.RecipeAspects.get(stack, level)
                    : thaumcraft.common.config.EntityAspects.get(entity);
        }

        private ResourceLocation discoveryId() {
            if (node != null) {
                return ResourceLocation.fromNamespaceAndPath("thaumcraft", "node/" + node.getScanKey());
            }
            if (entity == null) return BuiltInRegistries.ITEM.getKey(stack.getItem());
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

        /** TC4 shows only “Aura Node” until scanned, then reveals type/modifier. */
        public Component readoutName(thaumcraft.common.lib.capabilities.IThaumometerKnowledge knowledge) {
            if (node == null || knowledge == null || !scanned(knowledge)) return name;
            String text = pretty(node.getNodeType().name());
            if (node.getNodeModifier() != null) {
                text += ", " + pretty(node.getNodeModifier().name());
            }
            return Component.literal(text);
        }

        private static String pretty(String value) {
            String lower = value.toLowerCase(java.util.Locale.ROOT);
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }

    public static Target find(Player player) {
        var level = player.level();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(10));
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY, player));
        double nearest = start.distanceToSqr(hit.getLocation());
        net.minecraft.world.entity.Entity selected = null;
        for (var entity : level.getEntities(player, player.getBoundingBox()
                .expandTowards(end.subtract(start)).inflate(1), e -> e.isAlive() && !e.isSpectator()
                && (e instanceof ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb || e.isPickable()))) {
            var box = entity.getBoundingBox().inflate(0.25);
            var intersection = box.contains(start) ? java.util.Optional.of(start) : box.clip(start, end);
            if (intersection.isPresent() && start.distanceToSqr(intersection.get()) < nearest) {
                nearest = start.distanceToSqr(intersection.get());
                selected = entity;
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
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        var pos = hit.getBlockPos();
        if (level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
            return Target.node(node);
        }

        var state = level.getBlockState(pos);
        ItemStack stack = state.getBlock().getCloneItemStack(state, hit, level, pos, player);
        if (state.is(Blocks.WATER)) stack = new ItemStack(Items.WATER_BUCKET);
        if (state.is(Blocks.LAVA)) stack = new ItemStack(Items.LAVA_BUCKET);
        if (stack.isEmpty()) return null;
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

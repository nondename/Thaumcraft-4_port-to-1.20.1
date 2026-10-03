package thaumcraft.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.nodes.ModNodes;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Test/parity commands for validating TC4 aura nodes during the 1.20.1 port. */
public final class NodeCommands {
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thaumcraft")
                .then(Commands.literal("node")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(context -> spawnReferenceNode(context.getSource(), NodeType.NORMAL, null))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(NodeType.values())
                                                        .map(type -> type.name().toLowerCase(Locale.ROOT)), builder))
                                        .executes(context -> spawnFromArguments(context.getSource(),
                                                StringArgumentType.getString(context, "type"), "none"))
                                        .then(Commands.argument("modifier", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        new String[]{"none", "bright", "pale", "fading"}, builder))
                                                .executes(context -> spawnFromArguments(context.getSource(),
                                                        StringArgumentType.getString(context, "type"),
                                                        StringArgumentType.getString(context, "modifier"))))))
                        .then(Commands.literal("spawnall")
                                .executes(context -> spawnAllReferenceNodes(context.getSource())))
                        .then(Commands.literal("info")
                                .executes(context -> showLookedAtNode(context.getSource())))));
    }

    private static int spawnFromArguments(CommandSourceStack source, String rawType, String rawModifier) {
        NodeType type = parseType(rawType);
        if (type == null) {
            source.sendFailure(Component.literal("Unknown node type: " + rawType));
            return 0;
        }
        NodeModifier modifier = parseModifier(rawModifier);
        if (!"none".equalsIgnoreCase(rawModifier) && modifier == null) {
            source.sendFailure(Component.literal("Unknown node modifier: " + rawModifier));
            return 0;
        }
        return spawnReferenceNode(source, type, modifier);
    }

    private static int spawnReferenceNode(CommandSourceStack source, NodeType type, NodeModifier modifier) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(4.0D));
        BlockPos pos = findReplaceable(player, BlockPos.containing(target));
        if (pos == null) {
            source.sendFailure(Component.literal("No replaceable position found for the test node"));
            return 0;
        }

        if (!createNode(player, pos, type, modifier, referenceAspects(type))) {
            source.sendFailure(Component.literal("Aura node block entity was not created"));
            return 0;
        }

        BlockPos createdAt = pos;
        source.sendSuccess(() -> Component.literal("Spawned " + describe(type, modifier)
                + " aura node at " + createdAt.toShortString()), false);
        return 1;
    }

    /**
     * Creates all six TC4 node types in one row, plus BRIGHT/PALE/FADING normal nodes in a second row.
     * Intended for direct visual comparison against 4.2.3.5.
     */
    private static int spawnAllReferenceNodes(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(7.0D));
        BlockPos center = BlockPos.containing(target);
        int created = 0;

        NodeType[] types = NodeType.values();
        int startX = center.getX() - (types.length - 1);
        for (int i = 0; i < types.length; i++) {
            BlockPos requested = new BlockPos(startX + i * 2, center.getY(), center.getZ());
            BlockPos pos = findReplaceable(player, requested);
            if (pos != null && createNode(player, pos, types[i], null, referenceAspects(types[i]))) {
                created++;
            }
        }

        NodeModifier[] modifiers = NodeModifier.values();
        int modifierStartX = center.getX() - (modifiers.length - 1);
        for (int i = 0; i < modifiers.length; i++) {
            BlockPos requested = new BlockPos(modifierStartX + i * 2, center.getY() + 2, center.getZ());
            BlockPos pos = findReplaceable(player, requested);
            if (pos != null && createNode(player, pos, NodeType.NORMAL, modifiers[i], referenceAspects(NodeType.NORMAL))) {
                created++;
            }
        }

        int finalCreated = created;
        source.sendSuccess(() -> Component.literal("Spawned " + finalCreated
                + " aura-node references (6 types + 3 modifiers)"), false);
        return created;
    }

    private static BlockPos findReplaceable(ServerPlayer player, BlockPos requested) {
        BlockPos pos = requested;
        for (int i = 0; i < 6 && !player.level().getBlockState(pos).canBeReplaced(); i++) {
            pos = pos.above();
        }
        return player.level().getBlockState(pos).canBeReplaced() ? pos : null;
    }

    private static boolean createNode(ServerPlayer player, BlockPos pos, NodeType type,
                                      NodeModifier modifier, AspectList aspects) {
        player.level().setBlock(pos, ModNodes.AURA_NODE.get().defaultBlockState(), 3);
        if (!(player.level().getBlockEntity(pos) instanceof AuraNodeBlockEntity node)) {
            player.level().removeBlock(pos, false);
            return false;
        }
        node.initialize(type, modifier, aspects);
        return true;
    }

    private static AspectList referenceAspects(NodeType type) {
        return switch (type) {
            case NORMAL -> new AspectList()
                    .add(Aspect.AIR, 32)
                    .add(Aspect.WATER, 17);
            case UNSTABLE -> new AspectList()
                    .add(Aspect.AIR, 28)
                    .add(Aspect.FIRE, 24)
                    .add(Aspect.ENTROPY, 16);
            case DARK -> new AspectList()
                    .add(Aspect.DARKNESS, 32)
                    .add(Aspect.ENTROPY, 20);
            case TAINTED -> new AspectList()
                    .add(Aspect.TAINT, 32)
                    .add(Aspect.ENTROPY, 20);
            case HUNGRY -> new AspectList()
                    .add(Aspect.HUNGER, 32)
                    .add(Aspect.GREED, 18);
            case PURE -> new AspectList()
                    .add(Aspect.AURA, 32)
                    .add(Aspect.ORDER, 20);
        };
    }

    private static NodeType parseType(String value) {
        try {
            return NodeType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static NodeModifier parseModifier(String value) {
        if (value == null || value.isBlank() || "none".equalsIgnoreCase(value)) return null;
        try {
            return NodeModifier.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String describe(NodeType type, NodeModifier modifier) {
        return (modifier == null ? "" : modifier.name() + " ") + type.name();
    }

    private static int showLookedAtNode(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            return 0;
        }
        HitResult hit = player.pick(10.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)
                || !(player.level().getBlockEntity(blockHit.getBlockPos()) instanceof AuraNodeBlockEntity node)) {
            source.sendFailure(Component.literal("Look at an aura node within 10 blocks"));
            return 0;
        }

        String aspects = Arrays.stream(node.getAspects().getAspectsSorted())
                .filter(a -> a != null)
                .map(a -> a.getTag() + "=" + node.getAspects().getAmount(a)
                        + "/" + node.getBaseAspects().getAmount(a))
                .collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("Node " + node.getNodeId()
                + " type=" + node.getNodeType()
                + " modifier=" + (node.getNodeModifier() == null ? "none" : node.getNodeModifier())
                + " aspects=[" + aspects + "]"), false);
        return 1;
    }

    private NodeCommands() {
    }
}

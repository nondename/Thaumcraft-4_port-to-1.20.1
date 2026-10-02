package thaumcraft.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.nodes.ModNodes;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Temporary but useful parity commands while aura-node generation/rendering is being verified. */
public final class NodeCommands {
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thaumcraft")
                .then(Commands.literal("node")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(context -> spawnReferenceNode(context.getSource())))
                        .then(Commands.literal("info")
                                .executes(context -> showLookedAtNode(context.getSource())))));
    }

    private static int spawnReferenceNode(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(4.0D));
        BlockPos pos = BlockPos.containing(target);
        for (int i = 0; i < 5 && !player.level().getBlockState(pos).canBeReplaced(); i++) {
            pos = pos.above();
        }
        if (!player.level().getBlockState(pos).canBeReplaced()) {
            source.sendFailure(Component.literal("No replaceable position found for the test node"));
            return 0;
        }

        player.level().setBlock(pos, ModNodes.AURA_NODE.get().defaultBlockState(), 3);
        if (!(player.level().getBlockEntity(pos) instanceof AuraNodeBlockEntity node)) {
            source.sendFailure(Component.literal("Aura node block entity was not created"));
            return 0;
        }

        // Stable reference node for comparing 1.20.1 against the supplied TC4 screenshots/video.
        node.initialize(NodeType.NORMAL, null, new AspectList()
                .add(Aspect.AIR, 32)
                .add(Aspect.WATER, 17));
        BlockPos createdAt = pos;
        source.sendSuccess(() -> Component.literal("Spawned NORMAL aura node at " + createdAt.toShortString()
                + " [aer=32, aqua=17]"), false);
        return 1;
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

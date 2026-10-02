package thaumcraft.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import thaumcraft.api.aspects.Aspect;

public final class AspectCommands {
    private AspectCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("thaumcraft")
                .then(Commands.literal("aspects")
                        .executes(context -> showSummary(context.getSource())))
                .then(Commands.literal("aspect")
                        .then(Commands.argument("tag", StringArgumentType.word())
                                .executes(context -> showAspect(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "tag")
                                )))));
    }

    private static int showSummary(CommandSourceStack source) {
        int total = Aspect.aspects.size();
        int primal = Aspect.getPrimalAspects().size();
        int compound = Aspect.getCompoundAspects().size();

        source.sendSuccess(
                () -> Component.literal("Thaumcraft aspects: " + total
                        + " total, " + primal + " primal, " + compound + " compound"),
                false
        );
        return total;
    }

    private static int showAspect(CommandSourceStack source, String requestedTag) {
        Aspect aspect = Aspect.getAspect(requestedTag);
        if (aspect == null) {
            source.sendFailure(Component.literal("Unknown Thaumcraft aspect: " + requestedTag));
            return 0;
        }

        Aspect[] components = aspect.getComponents();
        String composition;
        if (aspect.isPrimal()) {
            composition = "primal";
        } else {
            composition = components[0].getTag() + " + " + components[1].getTag();
        }

        String aliasNote = requestedTag.equals(aspect.getTag())
                ? ""
                : " (alias -> " + aspect.getTag() + ")";

        source.sendSuccess(
                () -> Component.literal(requestedTag + aliasNote
                        + ": " + composition
                        + ", color=#" + String.format("%06X", aspect.getColor() & 0xFFFFFF)
                        + ", blend=" + aspect.getBlend()),
                false
        );
        return 1;
    }
}

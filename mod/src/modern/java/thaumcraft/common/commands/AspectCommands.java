package thaumcraft.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.Arrays;
import java.util.stream.Collectors;

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
                                ))))
                .then(Commands.literal("aspectlist")
                        .executes(context -> testAspectList(context.getSource())))
                .then(Commands.literal("objecttags")
                        .executes(context -> showObjectTagSummary(context.getSource())))
                .then(Commands.literal("heldaspects")
                        .executes(context -> showHeldAspects(context.getSource())))
                .then(Commands.literal("lookaspects")
                        .executes(context -> showLookedAtBlockAspects(context.getSource()))));
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

    private static int testAspectList(CommandSourceStack source) {
        AspectList original = new AspectList()
                .add(Aspect.AIR, 3)
                .add(Aspect.METAL, 2)
                .add(Aspect.MAGIC, 4);

        boolean reduced = original.reduce(Aspect.AIR, 1);
        original.remove(Aspect.METAL, 1);

        CompoundTag nbt = new CompoundTag();
        original.writeToNBT(nbt);

        ListTag serialized = nbt.getList("Aspects", Tag.TAG_COMPOUND);

        CompoundTag legacyAlias = new CompoundTag();
        legacyAlias.putString("key", "alkimia");
        legacyAlias.putInt("amount", 5);
        serialized.add(legacyAlias);

        CompoundTag unknown = new CompoundTag();
        unknown.putString("key", "unknown_future_aspect");
        unknown.putInt("amount", 99);
        serialized.add(unknown);

        AspectList restored = new AspectList();
        restored.readFromNBT(nbt);

        boolean passed = reduced
                && restored.size() == 3
                && restored.getAmount(Aspect.AIR) == 2
                && restored.getAmount(Aspect.METAL) == 1
                && restored.getAmount(Aspect.MAGIC) == 9
                && restored.visSize() == 12
                && restored.getAmount(null) == 0;

        if (passed) {
            source.sendSuccess(
                    () -> Component.literal("AspectList PASS: add/remove/reduce + NBT round-trip + alkimia alias + unknown-skip"),
                    false
            );
            source.sendSuccess(
                    () -> Component.literal("restored: aer=2, metallum=1, praecantatio=9, total=12"),
                    false
            );
            return 1;
        }

        source.sendFailure(Component.literal(
                "AspectList FAIL: size=" + restored.size()
                        + ", aer=" + restored.getAmount(Aspect.AIR)
                        + ", metallum=" + restored.getAmount(Aspect.METAL)
                        + ", praecantatio=" + restored.getAmount(Aspect.MAGIC)
                        + ", total=" + restored.visSize()
        ));
        return 0;
    }

    private static int showObjectTagSummary(CommandSourceStack source) {
        int items = ThaumcraftApi.getRegisteredObjectTagCount();
        int blocks = ThaumcraftApi.getRegisteredBlockTagCount();
        source.sendSuccess(
                () -> Component.literal("Thaumcraft aspect tags: " + items
                        + " item tags, " + blocks + " block tags"),
                false
        );
        return items + blocks;
    }

    private static int showHeldAspects(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("Hold an item in your main hand"));
            return 0;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return sendAspects(source, itemId, ThaumcraftApi.getObjectAspects(stack));
    }

    private static int showLookedAtBlockAspects(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("Look at a block within 5 blocks"));
            return 0;
        }

        BlockState state = player.level().getBlockState(blockHit.getBlockPos());
        Block block = state.getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        return sendAspects(source, blockId, ThaumcraftApi.getBlockAspects(block));
    }

    private static int sendAspects(CommandSourceStack source, String objectId, AspectList aspects) {
        if (aspects == null || aspects.size() == 0) {
            source.sendFailure(Component.literal("No Thaumcraft aspects registered for " + objectId));
            return 0;
        }

        String values = Arrays.stream(aspects.getAspectsSorted())
                .filter(aspect -> aspect != null)
                .map(aspect -> aspect.getTag() + "=" + aspects.getAmount(aspect))
                .collect(Collectors.joining(", "));

        source.sendSuccess(
                () -> Component.literal(objectId + " -> " + values + " (total=" + aspects.visSize() + ")"),
                false
        );
        return aspects.visSize();
    }
}

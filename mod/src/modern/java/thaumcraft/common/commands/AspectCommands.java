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
        ThaumcraftCommands.register(dispatcher);
        dispatcher.register(Commands.literal("thaumcraft").requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("aspects").executes(c -> showSummary(c.getSource())))
                        .then(Commands.literal("aspect").then(Commands.argument("tag", StringArgumentType.word())
                                .executes(c -> showAspect(c.getSource(), StringArgumentType.getString(c,"tag")))))
                        .then(Commands.literal("aspectlist").executes(c -> testAspectList(c.getSource())))
                        .then(Commands.literal("objecttags").executes(c -> showObjectTagSummary(c.getSource())))
                        .then(Commands.literal("heldaspects").executes(c -> showHeldAspects(c.getSource())))
                        .then(Commands.literal("lookaspects").executes(c -> showLookedAtBlockAspects(c.getSource())))));
    }
    private static int showSummary(CommandSourceStack source) {
        int total = Aspect.aspects.size();
        int primal = Aspect.getPrimalAspects().size();
        int compound = Aspect.getCompoundAspects().size();

        source.sendSuccess(
                () -> Component.translatable("tc.debug.summary", total, primal, compound),
                false
        );
        return total;
    }

    private static int showAspect(CommandSourceStack source, String requestedTag) {
        Aspect aspect = Aspect.getAspect(requestedTag);
        if (aspect == null) {
            source.sendFailure(Component.translatable("tc.debug.unknown", requestedTag));
            return 0;
        }

        Aspect[] components = aspect.getComponents();
        Component composition;
        if (aspect.isPrimal()) {
            composition = Component.translatable("tc.debug.primal");
        } else {
            composition = Component.literal(components[0].getTag() + " + " + components[1].getTag());
        }

        Component aliasNote = requestedTag.equals(aspect.getTag())
                ? Component.empty() : Component.translatable("tc.debug.alias", aspect.getTag());

        source.sendSuccess(
                () -> Component.translatable("tc.debug.aspect", requestedTag, aliasNote, composition, String.format("%06X", aspect.getColor() & 0xFFFFFF), aspect.getBlend()),
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
                    () -> Component.translatable("tc.debug.pass"),
                    false
            );
            source.sendSuccess(
                    () -> Component.translatable("tc.debug.restored"),
                    false
            );
            return 1;
        }

        source.sendFailure(Component.translatable("tc.debug.fail", restored.size(), restored.getAmount(Aspect.AIR), restored.getAmount(Aspect.METAL), restored.getAmount(Aspect.MAGIC), restored.visSize()));
        return 0;
    }

    private static int showObjectTagSummary(CommandSourceStack source) {
        int items = ThaumcraftApi.getRegisteredObjectTagCount();
        int blocks = ThaumcraftApi.getRegisteredBlockTagCount();
        source.sendSuccess(
                () -> Component.translatable("tc.debug.tags", items, blocks),
                false
        );
        return items + blocks;
    }

    private static int showHeldAspects(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("tc.debug.player"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("tc.debug.hold"));
            return 0;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return sendAspects(source, itemId, ThaumcraftApi.getObjectAspects(stack));
    }

    private static int showLookedAtBlockAspects(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("tc.debug.player"));
            return 0;
        }

        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.translatable("tc.debug.look"));
            return 0;
        }

        BlockState state = player.level().getBlockState(blockHit.getBlockPos());
        Block block = state.getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        return sendAspects(source, blockId, ThaumcraftApi.getBlockAspects(block));
    }

    private static int sendAspects(CommandSourceStack source, String objectId, AspectList aspects) {
        if (aspects == null || aspects.size() == 0) {
            source.sendFailure(Component.translatable("tc.debug.none", objectId));
            return 0;
        }

        String values = Arrays.stream(aspects.getAspectsSorted())
                .filter(aspect -> aspect != null)
                .map(aspect -> aspect.getTag() + "=" + aspects.getAmount(aspect))
                .collect(Collectors.joining(", "));

        source.sendSuccess(
                () -> Component.translatable("tc.debug.values", objectId, values, aspects.visSize()),
                false
        );
        return aspects.visSize();
    }
}

package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.IThaumometerKnowledge;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import thaumcraft.common.lib.network.ModNetwork;
import thaumcraft.common.sounds.ModSounds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Hold to scan blocks and dropped items, matching TC4's sustained scanning action. The scan
 * target must remain under the player's crosshair until the thaumometer finishes.
 */
public class ItemThaumometer extends Item {
    private static final int SCAN_DURATION_TICKS = 25;

    /**
     * TC4 kept the active ScanResult as transient item logic state, not in the ItemStack NBT.
     * Keeping it per player preserves that behaviour and, importantly, avoids forcing the modern
     * first-person item renderer to re-equip the thaumometer when scanning starts/finishes.
     */
    private static final Map<UUID, String> ACTIVE_SCAN_TARGETS = new ConcurrentHashMap<>();

    public ItemThaumometer(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ThaumometerRendererHolder.INSTANCE;
            }
        });
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return SCAN_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        var scan = ThaumometerTargets.find(player);
        String target = scan == null ? null : scan.identity();
        if (target == null) {
            return InteractionResultHolder.pass(stack);
        }

        var knowledge = player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
        if (knowledge == null) return InteractionResultHolder.fail(stack);
        Component error = rejection(scan, knowledge, level);
        if (error != null) {
            if (!level.isClientSide) player.displayClientMessage(error, true);
            return InteractionResultHolder.fail(stack);
        }

        ACTIVE_SCAN_TARGETS.put(player.getUUID(), target);
        player.startUsingItem(hand);

        // SUCCESS makes modern Minecraft play a hand swing/equip reaction. TC4 simply entered the
        // 25-tick use state with EnumAction.none, so CONSUME is the modern no-swing equivalent.
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) {
            return;
        }

        String startedTarget = ACTIVE_SCAN_TARGETS.getOrDefault(player.getUUID(), "");
        var scan = ThaumometerTargets.find(player);
        String currentTarget = scan == null ? null : scan.identity();
        if (startedTarget.isEmpty() || !startedTarget.equals(currentTarget)) {
            clearScan(player);
            player.stopUsingItem();
            return;
        }

        if (level.isClientSide && remainingUseDuration % 2 == 0) {
            level.playLocalSound(
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.CAMERA_TICKS.get(),
                    SoundSource.PLAYERS,
                    0.2F,
                    0.45F + level.random.nextFloat() * 0.1F,
                    false
            );
        }

        if (remainingUseDuration <= 5) {
            if (!level.isClientSide) {
                finishScan(level, player, scan);
            }
            clearScan(player);
            player.stopUsingItem();
        }
    }

    private static final class ThaumometerRendererHolder {
        private static final BlockEntityWithoutLevelRenderer INSTANCE =
                new thaumcraft.client.ThaumometerItemRenderer();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            clearScan(player);
        }
    }

    private static void clearScan(Player player) {
        ACTIVE_SCAN_TARGETS.remove(player.getUUID());
    }

    private static Component rejection(ThaumometerTargets.Target target, IThaumometerKnowledge knowledge, Level level) {
        if (target.scanned(knowledge)) {
            return Component.translatable("tc.scan.already_scanned", target.name());
        }
        var aspects = target.aspects(level);
        if (aspects == null || aspects.size() == 0) return Component.translatable("tc.scan.no_aspects", target.name());
        for (Aspect aspect : aspects.getAspects()) {
            if (aspect == null || aspect.isPrimal()) continue;
            for (Aspect parent : aspect.getComponents()) {
                if (!knowledge.hasDiscoveredAspect(parent)) {
                    return Component.translatable("tc.scan.missing_parent", parent.getName());
                }
            }
        }
        return null;
    }

    private static void finishScan(Level level, Player player, ThaumometerTargets.Target target) {
        var knowledge = player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
        if (target == null || knowledge == null || rejection(target, knowledge, level) != null) return;
        // TC4 awards ScanManager#generateNodeAspects for nodes (normalized + type bonus),
        // while every other target pays out the same list the lens displays.
        var aspects = target.awardAspects(level);
        target.markScanned(knowledge);
        if (aspects != null) {
            for (Aspect aspect : aspects.getAspectsSorted()) {
                if (aspect != null) knowledge.awardAspect(aspect, aspects.getAmount(aspect));
            }
        }
        ModNetwork.syncThaumometerKnowledge(player);
        player.displayClientMessage(Component.translatable("tc.scan.success", target.name()), true);
    }
}

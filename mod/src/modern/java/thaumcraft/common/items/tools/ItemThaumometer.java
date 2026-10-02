package thaumcraft.common.items.tools;

import net.minecraft.core.registries.BuiltInRegistries;
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
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.IThaumometerKnowledge;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import thaumcraft.common.lib.network.ModNetwork;
import thaumcraft.common.sounds.ModSounds;

import java.util.function.Consumer;

/**
 * Hold to scan blocks and dropped items, matching TC4's sustained scanning action. The scan
 * target must remain under the player's crosshair until the thaumometer finishes.
 */
public class ItemThaumometer extends Item {
    private static final int SCAN_DURATION_TICKS = 25;
    private static final double SCAN_RANGE = 10.0;
    private static final String SCAN_TARGET_TAG = "ThaumometerTarget";

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
        stack.getOrCreateTag().putString(SCAN_TARGET_TAG, target);
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) {
            return;
        }

        String startedTarget = stack.hasTag() ? stack.getTag().getString(SCAN_TARGET_TAG) : "";
        var scan = ThaumometerTargets.find(player);
        String currentTarget = scan == null ? null : scan.identity();
        if (startedTarget.isEmpty() || !startedTarget.equals(currentTarget)) {
            releaseUsing(stack, level, player, remainingUseDuration);
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
            releaseUsing(stack, level, player, remainingUseDuration);
            player.stopUsingItem();
        }
    }

    private static final class ThaumometerRendererHolder {
        private static final BlockEntityWithoutLevelRenderer INSTANCE =
                new thaumcraft.client.ThaumometerItemRenderer();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (stack.hasTag()) {
            stack.getTag().remove(SCAN_TARGET_TAG);
            if (stack.getTag().isEmpty()) {
                stack.setTag(null);
            }
        }
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
        var aspects = target.aspects(level);
        target.markScanned(knowledge);
        for (Aspect aspect : aspects.getAspectsSorted()) {
            if (aspect != null) knowledge.awardAspect(aspect, aspects.getAmount(aspect));
        }
        ModNetwork.syncThaumometerKnowledge(player);
        level.playSound(null, player.blockPosition(), ModSounds.CAMERA_CLACK.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        player.displayClientMessage(Component.translatable("tc.scan.success", target.name()), true);
    }
}

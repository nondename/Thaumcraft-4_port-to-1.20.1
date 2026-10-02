package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.capabilities.IThaumometerKnowledge;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import thaumcraft.common.lib.network.ModNetwork;
import thaumcraft.common.sounds.ModSounds;

import java.util.function.Consumer;

/**
 * Hold to scan a block, matching TC4's sustained scanning action. The scan
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
        String target = getTargetKey(level, player);
        if (target == null) {
            return InteractionResultHolder.pass(stack);
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
        String currentTarget = getTargetKey(level, player);
        if (startedTarget.isEmpty() || !startedTarget.equals(currentTarget)) {
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
                finishBlockScan(level, player);
            }
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

    private static void finishBlockScan(Level level, Player player) {
        HitResult hit = player.pick(SCAN_RANGE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        ItemStack target = getBlockScanStack(level, player, blockHit);
        if (target.isEmpty()) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(target.getItem());
        AspectList aspects = ThaumcraftApi.getObjectAspects(target);
        IThaumometerKnowledge knowledge = player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
        if (knowledge == null) {
            return;
        }
        if (knowledge.hasScannedItem(itemId)) {
            player.displayClientMessage(Component.translatable("tc.scan.already_scanned", target.getHoverName()), false);
            return;
        }
        if (aspects == null || aspects.size() == 0) {
            player.displayClientMessage(
                    Component.translatable("tc.scan.no_aspects", level.getBlockState(pos).getBlock().getName()),
                    false
            );
            return;
        }

        for (Aspect aspect : aspects.getAspects()) {
            if (aspect != null && !aspect.isPrimal() && !knowledge.hasDiscoveredParents(aspect)) {
                player.displayClientMessage(
                        Component.translatable("tc.scan.missing_parent", aspect.getTag()),
                        false
                );
                return;
            }
        }

        knowledge.scanItem(itemId);

        for (Aspect aspect : aspects.getAspectsSorted()) {
            if (aspect == null || !knowledge.hasDiscoveredParents(aspect)) {
                continue;
            }
            knowledge.awardAspect(aspect, aspects.getAmount(aspect));
        }
        ModNetwork.syncThaumometerKnowledge(player);
    }

    private static String getTargetKey(Level level, Player player) {
        HitResult hit = player.pick(SCAN_RANGE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = blockHit.getBlockPos();
        ItemStack target = getBlockScanStack(level, player, blockHit);
        if (target.isEmpty()) {
            return null;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(target.getItem());
        return itemId + ":" + (target.hasTag() ? target.getTag().toString() : "");
    }

    public static ItemStack getBlockScanStack(Level level, Player player, BlockHitResult hit) {
        if (level == null || player == null || hit == null) {
            return ItemStack.EMPTY;
        }
        BlockPos pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        ItemStack picked = state.getBlock().getCloneItemStack(state, hit, level, pos, player);
        if (picked != null && !picked.isEmpty() && ThaumcraftApi.getObjectAspects(picked) != null) {
            return picked;
        }

        if (state.is(net.minecraft.world.level.block.Blocks.WATER)) {
            ItemStack water = new ItemStack(Items.WATER_BUCKET);
            return ThaumcraftApi.getObjectAspects(water) == null ? ItemStack.EMPTY : water;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.LAVA)) {
            ItemStack lava = new ItemStack(Items.LAVA_BUCKET);
            return ThaumcraftApi.getObjectAspects(lava) == null ? ItemStack.EMPTY : lava;
        }
        return ItemStack.EMPTY;
    }
}

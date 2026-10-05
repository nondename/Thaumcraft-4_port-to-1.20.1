package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.ManaPodBlockEntity;
import thaumcraft.common.blocks.ModMagicalForestContent;
import thaumcraft.common.world.MagicalForest;

/** TC4 Mana Bean carrying exactly one aspect and able to seed a Mana Pod. */
public final class ManaBeanItem extends Item {
    private static final String ASPECT_TAG = "aspect";

    public ManaBeanItem(Properties properties) {
        super(properties);
    }

    public static void setAspect(ItemStack stack, Aspect aspect) {
        if (aspect != null) stack.getOrCreateTag().putString(ASPECT_TAG, aspect.getTag());
    }

    public static Aspect getAspect(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(ASPECT_TAG)
                ? Aspect.getAspect(stack.getTag().getString(ASPECT_TAG)) : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != net.minecraft.core.Direction.DOWN) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos logPos = context.getClickedPos();
        BlockPos podPos = logPos.below();
        if (!level.getBiome(logPos).is(MagicalForest.MAGICAL_FOREST)
                || !level.getBlockState(logPos).is(BlockTags.LOGS)
                || !level.getBlockState(podPos).canBeReplaced()) return InteractionResult.FAIL;

        if (!level.isClientSide) {
            level.setBlock(podPos, ModMagicalForestContent.MANA_POD.get().defaultBlockState(), 3);
            if (level.getBlockEntity(podPos) instanceof ManaPodBlockEntity pod) {
                pod.setAspect(getAspect(context.getItemInHand()));
            }
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = getAspect(stack);
        if (aspect != null) tooltip.add(Component.literal(aspect.getName()).withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}

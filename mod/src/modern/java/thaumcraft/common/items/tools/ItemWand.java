package thaumcraft.common.items.tools;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.sounds.ModSounds;

/** Iron-capped wooden wand: the original bookshelf transformation, without a vis cost. */
public final class ItemWand extends Item {
    /** TC4 stores vis in hundredths; wooden rods hold 25 of each primal. */
    public static final int CAPACITY = 2500;
    public static int getVis(ItemStack stack, thaumcraft.api.aspects.Aspect aspect) {
        if (aspect == null || !aspect.isPrimal() || !stack.hasTag()) return 0;
        return net.minecraft.util.Mth.clamp(stack.getTag().getInt(aspect.getTag()), 0, CAPACITY);
    }
    public static void setVis(ItemStack stack, thaumcraft.api.aspects.Aspect aspect, int amount) {
        if (aspect != null && aspect.isPrimal())
            stack.getOrCreateTag().putInt(aspect.getTag(), net.minecraft.util.Mth.clamp(amount, 0, CAPACITY));
    }
    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("tc.wand.capacity", CAPACITY / 100));
        for (var aspect : thaumcraft.api.aspects.Aspect.getPrimalAspects())
            lines.add(net.minecraft.network.chat.Component.literal(aspect.getName() + ": "
                    + String.format(java.util.Locale.ROOT, "%.2f", getVis(stack, aspect) / 100.0F)).withStyle(
                            style -> style.withColor(aspect.getColor())));
    }
    public ItemWand(Properties properties) { super(properties); }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null || !level.getBlockState(pos).is(Blocks.BOOKSHELF)) return InteractionResult.PASS;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;
        if (level instanceof ServerLevel server && level.removeBlock(pos, false)) {
            var book = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    new ItemStack(ModItems.THAUMONOMICON.get()));
            book.setDeltaMovement(0, 0, 0);
            book.setDefaultPickUpDelay();
            level.addFreshEntity(book);
            server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    32, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, pos, ModSounds.WAND.get(), SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

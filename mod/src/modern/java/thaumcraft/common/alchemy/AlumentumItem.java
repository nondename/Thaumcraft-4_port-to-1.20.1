package thaumcraft.common.alchemy;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** TC4 fuel and throwable explosive; using the item does not require research again. */
public final class AlumentumItem extends Item {
    public AlumentumItem(Properties properties) { super(properties); }
    @Override public int getBurnTime(ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> type) { return 6400; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        if(!level.isClientSide) {
            var projectile=new AlumentumProjectile(level,player);
            projectile.setItem(stack.copyWithCount(1));
            projectile.shootFromRotation(player,player.getXRot(),player.getYRot(),0,0.75F,1);
            if(!level.addFreshEntity(projectile)) return InteractionResultHolder.fail(stack);
            level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS,0.3F,0.4F/(level.random.nextFloat()*0.4F+0.8F));
            if(!player.getAbilities().instabuild)stack.shrink(1);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}

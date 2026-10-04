package thaumcraft.common.research;

import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.lib.network.ModNetwork;

/** The revelation gate only; the full TC4 warp event catalogue is a separate slice. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class EldritchProgression {
    public static final RegistryObject<Item> BRAIN=ModItems.ITEMS.register("zombie_brain",() -> new Item(new Item.Properties()
        .food(new FoodProperties.Builder().nutrition(2).saturationMod(.1F).build())) {
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
            var result=super.finishUsingItem(stack,level,entity);
            if(!level.isClientSide && entity instanceof net.minecraft.world.entity.player.Player player) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION,200));entity.addEffect(new MobEffectInstance(MobEffects.HUNGER,200));
                var knowledge=ResearchProgression.knowledge(player);
                if(knowledge!=null) {if(level.random.nextInt(10)==0)knowledge.addWarp(0,1,0);else knowledge.addWarp(0,0,1+level.random.nextInt(3));ModNetwork.syncThaumometerKnowledge(player);}
            }
            return result;
        }
    });
    public static void init() {}
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        var mob=event.getEntity();
        if(!mob.level().isClientSide && mob instanceof Zombie && event.isRecentlyHit() && mob.getRandom().nextInt(10)-event.getLootingLevel()<1)
            event.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),new ItemStack(BRAIN.get())));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount%1200!=0)return;
        var data=ResearchProgression.knowledge(event.player);if(data==null)return;
        data.decayTemporaryWarp();
        if(event.player.getRandom().nextInt(10)==0) {
            boolean changed=false;
            if(data.getWarp()>25)changed|=data.grantResearch("ELDRITCHMINOR");
            if(data.getWarp()>50)changed|=data.grantResearch("ELDRITCHMAJOR");
            if(changed)event.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.progress.revelation"),false);
        }
        ModNetwork.syncThaumometerKnowledge(event.player);
    }
}

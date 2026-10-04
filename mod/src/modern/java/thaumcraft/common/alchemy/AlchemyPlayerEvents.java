package thaumcraft.common.alchemy;

import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="thaumcraft")
public final class AlchemyPlayerEvents {
    @SubscribeEvent public static void toss(ItemTossEvent event) {
        if(!event.getPlayer().level().isClientSide)
            event.getEntity().getPersistentData().putUUID("ThaumcraftThrower",event.getPlayer().getUUID());
    }
}

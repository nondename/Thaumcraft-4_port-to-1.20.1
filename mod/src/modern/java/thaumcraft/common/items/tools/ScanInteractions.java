package thaumcraft.common.items.tools;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;

/** Scanner input takes precedence over trading, mounting and other mob interactions. */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ScanInteractions {
    @SubscribeEvent
    public static void tags(net.minecraftforge.event.TagsUpdatedEvent event) {
        thaumcraft.common.config.RecipeAspects.clear();
    }
    @SubscribeEvent
    public static void reload(net.minecraftforge.event.OnDatapackSyncEvent event) {
        thaumcraft.common.config.RecipeAspects.clear();
    }
    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) { scan(event); }
    @SubscribeEvent
    public static void interactAt(PlayerInteractEvent.EntityInteractSpecific event) { scan(event); }
    private static void scan(PlayerInteractEvent event) {
        if (!(event.getItemStack().getItem() instanceof ItemThaumometer scanner)) return;
        var result = scanner.use(event.getLevel(), event.getEntity(), event.getHand());
        // Only take the click over when the scanner actually engaged; a PASS keeps
        // trading, mounting and vanilla mob interactions working.
        if (result.getResult() == net.minecraft.world.InteractionResult.PASS) return;
        event.setCancellationResult(result.getResult());
        event.setCanceled(true);
    }
}

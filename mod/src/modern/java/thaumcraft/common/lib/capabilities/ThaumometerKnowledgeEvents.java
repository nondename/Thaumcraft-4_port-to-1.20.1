package thaumcraft.common.lib.capabilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.network.ModNetwork;

@Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ThaumometerKnowledgeEvents {
    private static final ResourceLocation ID = new ResourceLocation(Thaumcraft.MODID, "thaumometer_knowledge");

    private ThaumometerKnowledgeEvents() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IThaumometerKnowledge.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            ThaumometerKnowledgeProvider provider = new ThaumometerKnowledgeProvider();
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ThaumometerKnowledgeProvider.CAPABILITY).ifPresent(oldData ->
                event.getEntity().getCapability(ThaumometerKnowledgeProvider.CAPABILITY).ifPresent(newData ->
                        newData.deserializeNBT(oldData.serializeNBT())));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ModNetwork.syncThaumometerKnowledge(event.getEntity());
    }

    @SubscribeEvent
    public static void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        ModNetwork.syncThaumometerKnowledge(event.getEntity());
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ModNetwork.syncThaumometerKnowledge(event.getEntity());
    }
}

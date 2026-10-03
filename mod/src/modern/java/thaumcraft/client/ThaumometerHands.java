package thaumcraft.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;

/**
 * Small client-side hooks kept next to the thaumometer renderer.
 *
 * <p>Important: there is deliberately no RenderHandEvent override here. The 1.12.2 port
 * does not drive the thaumometer through the vanilla two-handed map pose; its baked item
 * perspective matrix owns first-person placement. Keeping the same split also prevents
 * camera pitch from laying the scanner flat when the player looks up or down.</p>
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ThaumometerHands {
    private ThaumometerHands() {
    }

    @SubscribeEvent
    public static void recipes(net.minecraftforge.client.event.RecipesUpdatedEvent event) {
        thaumcraft.common.config.RecipeAspects.clear();
    }
}

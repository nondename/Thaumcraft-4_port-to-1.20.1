package thaumcraft.client;

import java.util.List;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.items.tools.ItemWand;

/** Select the baked rod/cap combination in every item display context, including the table. */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WandModelVariants {
    // Order is shared with the monotonically increasing model predicates in wand.json.
    private static final List<String> RODS = List.of("wood", "greatwood", "obsidian", "blaze",
            "ice", "quartz", "bone", "reed", "silverwood");
    private static final List<String> CAPS = List.of("iron", "gold", "thaumium", "void", "copper", "silver");

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.WAND.get(),
                new ResourceLocation(Thaumcraft.MODID, "parts"), (stack, level, entity, seed) -> {
                    int rod = RODS.indexOf(ItemWand.getRod(stack).getTag());
                    int cap = CAPS.indexOf(ItemWand.getCap(stack).getTag());
                    return rod < 0 || cap < 0 ? 0 : (rod * CAPS.size() + cap) / 64.0F;
                }));
    }

    private WandModelVariants() {}
}

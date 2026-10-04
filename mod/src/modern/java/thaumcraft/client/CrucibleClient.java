package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.alchemy.*;
import thaumcraft.common.items.ModItems;

@Mod.EventBusSubscriber(modid=Thaumcraft.MODID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class CrucibleClient {
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Block event) {
        event.register((state, world, pos, index) -> world == null || pos == null ? 0x3F76E4
                : BiomeColors.getAverageWaterColor(world, pos), ModAlchemy.CRUCIBLE.get());
    }
    @Mod.EventBusSubscriber(modid=Thaumcraft.MODID, value=Dist.CLIENT)
    public static final class Hud {
        @SubscribeEvent public static void render(RenderGuiOverlayEvent.Post event) {
            if (!event.getOverlay().id().getPath().equals("hotbar")) return;
            var mc=Minecraft.getInstance();
            if (mc.player==null || mc.level==null || mc.options.hideGui || !(mc.hitResult instanceof BlockHitResult hit)) return;
            boolean view=mc.player.getMainHandItem().is(ModItems.WAND.get()) || mc.player.getOffhandItem().is(ModItems.WAND.get())
                    || mc.player.getMainHandItem().is(ModItems.THAUMOMETER.get())
                    || mc.player.getInventory().armor.get(3).is(ModItems.GOGGLES.get());
            if (!view || !(mc.level.getBlockEntity(hit.getBlockPos()) instanceof CrucibleEntity c)) return;
            var g=event.getGuiGraphics(); int x=mc.getWindow().getGuiScaledWidth()/2+14, y=mc.getWindow().getGuiScaledHeight()/2-35;
            g.drawString(mc.font, Component.translatable(c.isBoiling()?"tc.crucible.boiling":"tc.crucible.cold"), x,y,0xFFFFFF);
            g.drawString(mc.font,c.getWater()+" / 1000 mB",x,y+12,0x83BFFF);
            int i=0;
            for (Aspect aspect:c.getAspects().getAspectsSorted()) {
                if (aspect==null) continue;
                int color=aspect.getColor(); g.setColor(((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,1);
                g.blit(aspect.getImage(),x+(i%6)*30,y+27+(i/6)*22,16,16,0,0,32,32,32,32); g.setColor(1,1,1,1);
                g.drawString(mc.font,Integer.toString(c.getAspects().getAmount(aspect)),x+(i%6)*30+12,y+35+(i/6)*22,0xFFFFFF); i++;
            }
        }
    }
}

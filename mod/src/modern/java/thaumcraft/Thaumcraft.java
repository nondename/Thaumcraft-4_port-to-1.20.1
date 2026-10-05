package thaumcraft;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.commands.AspectCommands;
import thaumcraft.common.config.ConfigAspects;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeEvents;
import thaumcraft.common.lib.network.ModNetwork;
import thaumcraft.common.nodes.ModNodes;
import thaumcraft.common.sounds.ModSounds;

/** Forge 1.20.1 entry point for the Thaumcraft 4 port. */
@Mod(Thaumcraft.MODID)
public final class Thaumcraft {
    public static final String MODID = "thaumcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Thaumcraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        thaumcraft.common.items.ModMetals.init();
        thaumcraft.common.alchemy.ModAlchemy.register(modEventBus);
        thaumcraft.common.alchemy.ModAlchemyEntities.register(modEventBus);
        thaumcraft.common.alchemy.CrucibleRecipe.init();
        thaumcraft.common.infusion.ModInfusion.register(modEventBus);
        // Magical Forest item/block definitions must exist before the shared DeferredRegisters attach.
        thaumcraft.common.blocks.ModMagicalForestContent.init();
        ModItems.register(modEventBus);
        thaumcraft.common.items.wands.ModWandParts.register(modEventBus);
        ModNodes.register(modEventBus);
        thaumcraft.common.blocks.ModMagicalTrees.init();
        thaumcraft.common.blocks.ModOres.register(modEventBus);
        thaumcraft.common.blocks.ModMagicalForestContent.registerBlockEntities(modEventBus);
        thaumcraft.common.research.ModResearch.register(modEventBus);
        thaumcraft.common.blocks.ArcaneWorkbenchBlock.register(modEventBus);
        thaumcraft.common.blocks.ArcaneWorkbenchMenu.register(modEventBus);
        thaumcraft.common.crafting.ModRecipes.register(modEventBus);
        thaumcraft.common.world.InfusedOreFeature.register(modEventBus);
        thaumcraft.common.world.AuraNodeFeature.register(modEventBus);
        thaumcraft.common.world.MagicalTreeFeature.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        ModSounds.register(modEventBus);
        modEventBus.addListener(ThaumometerKnowledgeEvents::registerCapabilities);
        ModNetwork.register();

        validateAspectRegistry();
        ConfigAspects.init();
        MinecraftForge.EVENT_BUS.addListener(AspectCommands::onRegisterCommands);
        LOGGER.info("Thaumcraft 4 port loaded on Minecraft 1.20.1 with {} object aspect tags",
                ThaumcraftApi.getRegisteredObjectTagCount());
    }

    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            thaumcraft.common.blocks.ModOres.registerAspects();
            thaumcraft.common.blocks.ModMagicalTrees.registerAspects();
            thaumcraft.common.world.MagicalForest.registerOverworldBiome();
        });
        event.enqueueWork(thaumcraft.common.config.ConfigAspects::initModItems);
    }

    private static void validateAspectRegistry() {
        int total = Aspect.aspects.size();
        int primals = Aspect.getPrimalAspects().size();
        int compounds = Aspect.getCompoundAspects().size();
        if (total != 48 || primals != 6 || compounds != 42) {
            throw new IllegalStateException("TC4 aspect registry parity failure: "
                    + total + " total / " + primals + " primal / " + compounds + " compound");
        }
        LOGGER.info("TC4 aspect registry initialized: {} total ({} primal, {} compound)", total, primals, compounds);
    }
}

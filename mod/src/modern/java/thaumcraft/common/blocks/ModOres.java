package thaumcraft.common.blocks;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.nodes.ModNodes;

/** Flattened metadata 0..7 from TC4 BlockCustomOre. */
public final class ModOres {
    public static final String[] NAMES = {"air", "fire", "water", "earth", "order", "entropy"};
    public static final Aspect[] ASPECTS = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};
    public static final int[] COLORS = {0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577};

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Thaumcraft.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Thaumcraft.MODID);
    public static final Map<String, RegistryObject<Block>> ORES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> SHARDS = new LinkedHashMap<>();

    public static final RegistryObject<Item> QUICKSILVER =
            ModItems.ITEMS.register("quicksilver", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> AMBER =
            ModItems.ITEMS.register("amber", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Block> CINNABAR_ORE = BLOCKS.register("cinnabar_ore", () ->
            new Block(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(1.5F, 5.0F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> CINNABAR_ORE_ITEM =
            ModItems.ITEMS.register("cinnabar_ore", () -> new BlockItem(CINNABAR_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Block> AMBER_ORE = BLOCKS.register("amber_ore", () ->
            new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(1.5F, 5.0F).requiresCorrectToolForDrops(), UniformInt.of(1, 4)));
    public static final RegistryObject<Item> AMBER_ORE_ITEM =
            ModItems.ITEMS.register("amber_ore", () -> new BlockItem(AMBER_ORE.get(), new Item.Properties()));

    static {
        for (String name : NAMES) {
            SHARDS.put(name, ModItems.ITEMS.register(name + "_shard", () -> new Item(new Item.Properties())));
            RegistryObject<Block> ore = BLOCKS.register(name + "_infused_stone", () ->
                    new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                            .strength(1.5F, 5.0F).requiresCorrectToolForDrops(), UniformInt.of(0, 3)));
            ORES.put(name, ore);
            ModItems.ITEMS.register(name + "_infused_stone", () -> new BlockItem(ore.get(), new Item.Properties()));
        }
        TABS.register("thaumcraft", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.thaumcraft"))
                .icon(() -> new ItemStack(ModItems.THAUMOMETER.get()))
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.WAND.get());
                    output.accept(ModItems.THAUMONOMICON.get());
                    output.accept(ModItems.IRON_WAND_CAP.get());
                    output.accept(ModItems.THAUMOMETER.get());
                    // TC4 ConfigItems#371-373: the goggles register right after the thaumometer.
                    output.accept(ModItems.GOGGLES.get());
                    output.accept(thaumcraft.common.research.ModResearch.TABLE_ITEM.get());
                    output.accept(thaumcraft.common.research.ModResearch.SCRIBING_TOOLS.get());
                    output.accept(thaumcraft.common.research.ModResearch.PHIAL.get());
                    // TC4 BlockCustomOre creative order: cinnabar, six infused stones, amber.
                    output.accept(CINNABAR_ORE.get());
                    SHARDS.values().forEach(item -> output.accept(item.get()));
                    ORES.values().forEach(block -> output.accept(block.get()));
                    output.accept(AMBER_ORE.get());
                    output.accept(QUICKSILVER.get());
                    output.accept(AMBER.get());
                    // TC4 BlockAiry contributes exactly one metadata-0 Aura Node creative item.
                    output.accept(ModNodes.AURA_NODE_ITEM.get());
                }).build());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        TABS.register(bus);
    }

    public static void registerAspects() {
        ThaumcraftApi.registerObjectTag(ModItems.IRON_WAND_CAP.get(), new AspectList().add(Aspect.METAL, 3));
        ThaumcraftApi.registerObjectTag(ModItems.WAND.get(), new AspectList().add(Aspect.METAL, 4));
        ThaumcraftApi.registerObjectTag(ModItems.THAUMONOMICON.get(), new AspectList().add(Aspect.TREE, 2).add(Aspect.MIND, 4).add(Aspect.MAGIC, 2));

        AspectList cinnabar = new AspectList()
                .add(Aspect.EARTH, 1).add(Aspect.METAL, 2).add(Aspect.EXCHANGE, 2).add(Aspect.POISON, 1);
        ThaumcraftApi.registerBlockTag(CINNABAR_ORE.get(), cinnabar);
        ThaumcraftApi.registerObjectTag(CINNABAR_ORE_ITEM.get(), cinnabar);

        AspectList amberOre = new AspectList().add(Aspect.EARTH, 1).add(Aspect.TRAP, 3).add(Aspect.CRYSTAL, 2);
        ThaumcraftApi.registerBlockTag(AMBER_ORE.get(), amberOre);
        ThaumcraftApi.registerObjectTag(AMBER_ORE_ITEM.get(), amberOre);
        ThaumcraftApi.registerObjectTag(AMBER.get(), new AspectList().add(Aspect.TRAP, 2).add(Aspect.CRYSTAL, 2));
        ThaumcraftApi.registerObjectTag(QUICKSILVER.get(), new AspectList()
                .add(Aspect.METAL, 3).add(Aspect.POISON, 1).add(Aspect.EXCHANGE, 2));

        for (int i = 0; i < NAMES.length; i++) {
            ThaumcraftApi.registerObjectTag(SHARDS.get(NAMES[i]).get(), new AspectList()
                    .add(Aspect.MAGIC, 1).add(ASPECTS[i], 2).add(Aspect.CRYSTAL, 1));
            AspectList ore = new AspectList().add(Aspect.EARTH, 1).add(ASPECTS[i], 3).add(Aspect.CRYSTAL, 2);
            ThaumcraftApi.registerBlockTag(ORES.get(NAMES[i]).get(), ore);
            ThaumcraftApi.registerObjectTag(ORES.get(NAMES[i]).get().asItem(), ore);
        }
    }

    private ModOres() {}
}

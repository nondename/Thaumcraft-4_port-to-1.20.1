package thaumcraft.common.blocks;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.ConstantInt;
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

/** Flattened metadata 1..6 from TC4 BlockCustomOre, with the original shard values. */
public final class ModOres {
    public static final String[] NAMES = {"air", "fire", "water", "earth", "order", "entropy"};
    public static final Aspect[] ASPECTS = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};
    public static final int[] COLORS = {0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577};
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Thaumcraft.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Thaumcraft.MODID);
    public static final Map<String, RegistryObject<Block>> ORES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> SHARDS = new LinkedHashMap<>();

    static {
        for (String name : NAMES) {
            SHARDS.put(name, ModItems.ITEMS.register(name + "_shard", () -> new Item(new Item.Properties())));
            RegistryObject<Block> ore = BLOCKS.register(name + "_infused_stone", () ->
                    new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                            .strength(3.0F, 5.0F).requiresCorrectToolForDrops(), ConstantInt.of(4)));
            ORES.put(name, ore);
            ModItems.ITEMS.register(name + "_infused_stone", () -> new BlockItem(ore.get(), new Item.Properties()));
        }
        TABS.register("thaumcraft", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.thaumcraft"))
                .icon(() -> new ItemStack(ModItems.THAUMOMETER.get()))
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.THAUMOMETER.get());
                    output.accept(thaumcraft.common.research.ModResearch.TABLE_ITEM.get());
                    output.accept(thaumcraft.common.research.ModResearch.SCRIBING_TOOLS.get());
                    output.accept(thaumcraft.common.research.ModResearch.PHIAL.get());
                    SHARDS.values().forEach(item -> output.accept(item.get()));
                    ORES.values().forEach(block -> output.accept(block.get()));
                }).build());
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        TABS.register(bus);
    }

    public static void registerAspects() {
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

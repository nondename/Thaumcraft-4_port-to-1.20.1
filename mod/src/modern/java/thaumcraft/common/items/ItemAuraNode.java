package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** Creative-only-in-practice TC4 Aura Node placer (legacy BlockAiry metadata 0). */
public final class ItemAuraNode extends BlockItem {
    public ItemAuraNode(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        // The original key is tile.blockAiry.0.name = Aura Node. Keep a clean modern fallback
        // until the remaining legacy lang catalogue is migrated wholesale.
        return Component.literal("Aura Node");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Place a randomly generated node").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Creative Mode Only").withStyle(ChatFormatting.ITALIC));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return RendererHolder.INSTANCE;
            }
        });
    }

    private static final class RendererHolder {
        private static final BlockEntityWithoutLevelRenderer INSTANCE =
                new thaumcraft.client.AuraNodeItemRenderer();
    }
}

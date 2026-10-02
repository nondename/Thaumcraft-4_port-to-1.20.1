package thaumcraft.common.items.tools;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * First functional 1.20.1 Thaumometer milestone.
 *
 * Right-clicking a block resolves its registered TC4 aspects and reports them
 * to the player. Research persistence and the original scanning animation are
 * intentionally left for the next porting stage.
 */
public class ItemThaumometer extends Item {
    public ItemThaumometer(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            var player = context.getPlayer();
            if (player != null) {
                Block block = level.getBlockState(context.getClickedPos()).getBlock();
                String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
                AspectList aspects = ThaumcraftApi.getBlockAspects(block);

                if (aspects == null || aspects.size() == 0) {
                    player.displayClientMessage(
                            Component.literal("No Thaumcraft aspects registered for " + blockId),
                            false
                    );
                } else {
                    StringBuilder result = new StringBuilder(blockId).append(": ");
                    boolean first = true;
                    for (Aspect aspect : aspects.getAspectsSorted()) {
                        if (aspect == null) {
                            continue;
                        }
                        if (!first) {
                            result.append(", ");
                        }
                        result.append(aspect.getTag()).append(" x").append(aspects.getAmount(aspect));
                        first = false;
                    }
                    player.displayClientMessage(Component.literal(result.toString()), false);
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

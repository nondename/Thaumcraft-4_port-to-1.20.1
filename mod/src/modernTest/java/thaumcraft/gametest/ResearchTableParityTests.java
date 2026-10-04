package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.common.research.ModResearch;
import thaumcraft.common.research.ResearchMenu;
import thaumcraft.common.research.ResearchTableBlock;
import thaumcraft.common.research.ResearchTableEntity;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class ResearchTableParityTests {
    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void pairsAndToolsSurviveReload(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        var block = (ResearchTableBlock) ModResearch.TABLE.get();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos partner = pos.relative(direction);
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            level.setBlockAndUpdate(partner, block.defaultBlockState());
            ItemStack tools = new ItemStack(ModResearch.SCRIBING_TOOLS.get());
            tools.setDamageValue(17);
            player.setItemInHand(InteractionHand.OFF_HAND, tools);
            block.use(level.getBlockState(pos), level, pos, player, InteractionHand.OFF_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            helper.assertTrue(ResearchTableBlock.isComplete(level, pos), "Both tables must pair for " + direction);
            helper.assertTrue(level.getBlockState(pos).getValue(ResearchTableBlock.FACING) == direction,
                    "Paired model must face its secondary half");
            var table = (ResearchTableEntity) level.getBlockEntity(pos);
            CompoundTag data = table.saveWithoutMetadata();
            table.clearContent(); table.load(data);
            helper.assertTrue(table.getItem(0).getDamageValue() == 17 && table.getItem(0).getCount() == 1,
                    "Tools must retain ink usage and count across reload");
            var menu = new ResearchMenu(1500, player.getInventory(), ContainerLevelAccess.create(level, pos));
            helper.assertTrue(menu.slots.size() == 38 && menu.stillValid(player), "Table must expose tools, notes and 36 inventory slots");
            helper.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.STONE)), "Ink slot must reject other items");
            ItemStack original = menu.quickMoveStack(player, 0);
            helper.assertTrue(original.is(ModResearch.SCRIBING_TOOLS.get()) && table.isEmpty()
                    && menu.stillValid(player), "Tools must shift out without invalidating the assembled table");
            helper.assertTrue(menu.quickMoveStack(player, 37).is(ModResearch.SCRIBING_TOOLS.get())
                    && table.hasTools(), "Tools must shift back into the table");
            level.setBlockAndUpdate(partner, Blocks.AIR.defaultBlockState());
            helper.assertTrue(!menu.stillValid(player) && table.isEmpty()
                    && level.getBlockState(pos).getValue(ResearchTableBlock.PART) == 0,
                    "Breaking secondary must drop tools once and restore primary to plain table");
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }
}

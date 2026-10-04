package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.common.blocks.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.blocks.ArcaneWorkbenchMenu;
import thaumcraft.common.items.ModMetals;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class MetalsTests {
    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void storageRecipesPreserveMetal(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        var menu = new ArcaneWorkbenchMenu(1600, player.getInventory(), new SimpleContainer(ArcaneWorkbenchBlockEntity.SIZE));
        var grid = menu.getGrid();
        for (String metal : new String[] {"thaumium", "void"}) {
            var ingot = metal.equals("thaumium") ? ModMetals.THAUMIUM_INGOT.get() : ModMetals.VOID_INGOT.get();
            var nugget = metal.equals("thaumium") ? ModMetals.THAUMIUM_NUGGET.get() : ModMetals.VOID_NUGGET.get();
            var compress = (CraftingRecipe) level.getRecipeManager().byKey(new ResourceLocation("thaumcraft", metal + "_ingot_from_nuggets")).orElseThrow();
            var split = (CraftingRecipe) level.getRecipeManager().byKey(new ResourceLocation("thaumcraft", metal + "_nuggets_from_ingot")).orElseThrow();
            grid.clearContent();
            for (int i = 0; i < 9; i++) grid.setItem(i, new ItemStack(nugget));
            helper.assertTrue(compress.matches(grid, level), "Nine nuggets must match ingot recipe for " + metal);
            ItemStack result = compress.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(ingot) && result.getCount() == 1, "Nine nuggets must produce exactly one ingot");
            grid.setItem(8, ItemStack.EMPTY);
            helper.assertTrue(!compress.matches(grid, level), "Eight nuggets must not produce an ingot");
            grid.clearContent(); grid.setItem(4, result);
            helper.assertTrue(split.matches(grid, level), "Ingot can occupy the centre cell");
            result = split.assemble(grid, level.registryAccess());
            helper.assertTrue(result.is(nugget) && result.getCount() == 9, "Ingot split must return exactly nine nuggets");
        }
        var compress = (CraftingRecipe) level.getRecipeManager().byKey(new ResourceLocation("thaumcraft", "thaumium_block")).orElseThrow();
        var split = (CraftingRecipe) level.getRecipeManager().byKey(new ResourceLocation("thaumcraft", "thaumium_ingots_from_block")).orElseThrow();
        grid.clearContent();
        for (int i = 0; i < 9; i++) grid.setItem(i, new ItemStack(ModMetals.THAUMIUM_INGOT.get()));
        helper.assertTrue(compress.matches(grid, level), "Nine ingots must match block recipe");
        ItemStack block = compress.assemble(grid, level.registryAccess());
        helper.assertTrue(block.is(ModMetals.THAUMIUM_BLOCK_ITEM.get()) && block.getCount() == 1, "Block recipe returns one block");
        grid.clearContent(); grid.setItem(0, block);
        helper.assertTrue(split.matches(grid, level) && split.assemble(grid, level.registryAccess()).getCount() == 9,
                "Block round trip must return nine ingots");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, ModMetals.THAUMIUM_BLOCK.get().defaultBlockState());
        var drops = Block.getDrops(level.getBlockState(pos), level, pos, null, player, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModMetals.THAUMIUM_BLOCK_ITEM.get())
                && drops.get(0).getCount() == 1, "Mined thaumium block must drop itself once");
        menu.removed(player);
        helper.succeed();
    }
}

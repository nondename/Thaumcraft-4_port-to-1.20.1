package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.ModOres;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.items.tools.ThaumometerTargets;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledge;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import thaumcraft.common.research.ModResearch;
import thaumcraft.common.research.ResearchTableBlock;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class SurvivalSliceTests {
    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void combinationSpendsAndPersists(GameTestHelper helper) {
        var knowledge = new ThaumometerKnowledge();
        knowledge.awardAspect(Aspect.EARTH, 4);
        knowledge.awardAspect(Aspect.WATER, 3);
        helper.assertTrue(knowledge.combine(Aspect.EARTH, Aspect.WATER) == Aspect.LIFE, "Earth + Water must discover Life");
        helper.assertTrue(knowledge.getAspectPool(Aspect.EARTH) == 3 && knowledge.getAspectPool(Aspect.WATER) == 2, "Both input pools must be charged once");
        helper.assertTrue(knowledge.getAspectPool(Aspect.LIFE) == 3, "First discovery grants the original +2 bonus");
        helper.assertTrue(knowledge.combine(Aspect.LIFE, Aspect.EARTH) == Aspect.PLANT, "Life + Earth must unlock plant scans");
        helper.assertTrue(knowledge.combine(Aspect.FIRE, Aspect.AIR) == null, "Empty pools cannot produce research");
        var copy = new ThaumometerKnowledge();
        copy.deserializeNBT(knowledge.serializeNBT());
        helper.assertTrue(copy.hasDiscoveredAspect(Aspect.PLANT) && copy.getAspectPool(Aspect.WATER) == 2, "Saved pools and discoveries must survive reload");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void recipesOreLootAndTags(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "thaumometer")).orElseThrow();
        helper.assertTrue(recipe instanceof ShapedRecipe && recipe.getResultItem(level.registryAccess()).is(ModItems.THAUMOMETER.get()), "Survival scanner recipe must load");
        helper.assertTrue(recipe.getIngredients().get(1).test(new ItemStack(ModOres.SHARDS.get("air").get()))
                && recipe.getIngredients().get(7).test(new ItemStack(ModOres.SHARDS.get("fire").get())), "Different primal shards must be accepted");
        var iron = new ItemStack(Items.IRON_PICKAXE);
        var silk = iron.copy(); silk.enchant(Enchantments.SILK_TOUCH, 1);
        for (String name : ModOres.NAMES) {
            var ore = ModOres.ORES.get(name).get();
            var drops = Block.getDrops(ore.defaultBlockState(), level, helper.absolutePos(BlockPos.ZERO), null, null, iron);
            // TC4 BlockCustomOre#113: 1 + rand(2 + fortune) shards, so 1..2 without Fortune.
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModOres.SHARDS.get(name).get())
                            && drops.get(0).getCount() >= 1 && drops.get(0).getCount() <= 2,
                    "Ore must drop its matching shard (1..2 without Fortune): " + name);
            var silkDrops = Block.getDrops(ore.defaultBlockState(), level, helper.absolutePos(BlockPos.ZERO), null, null, silk);
            helper.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(ore.asItem()), "Silk Touch must preserve ore");
        }
        helper.assertTrue(ThaumcraftApi.getObjectAspects(new ItemStack(Items.OAK_LOG)).getAmount(Aspect.TREE) == 4, "Dropped logs and placed logs must resolve the same aspects");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void tablePairFormsAndBreaks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos first = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos second = first.east();
        level.setBlock(first, ModResearch.TABLE.get().defaultBlockState(), 3);
        level.setBlock(second, ModResearch.TABLE.get().defaultBlockState(), 3);
        var player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModResearch.SCRIBING_TOOLS.get()));
        var state = level.getBlockState(first);
        state.use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(first), Direction.UP, first, false));
        helper.assertTrue(ResearchTableBlock.isComplete(level, first), "Two tables and scribing tools must form a workstation");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Installed tools must leave the player's hand");
        level.destroyBlock(second, false);
        helper.assertTrue(!ResearchTableBlock.isComplete(level, first) && level.getBlockState(first).getValue(ResearchTableBlock.PART) == 0, "Breaking either half must dismantle the workstation");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void droppedItemScanCannotPassThroughWall(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.setPos(start); player.setYRot(0); player.setXRot(0);
        var glass = new ItemEntity(level, start.x, player.getEyeY() - 0.1, start.z + 3, new ItemStack(Items.GLASS));
        glass.setNoGravity(true); level.addFreshEntity(glass);
        var target = ThaumometerTargets.find(player);
        helper.assertTrue(target != null && target.stack().is(Items.GLASS), "Scanner must target dropped glass");
        var scanner = new ItemStack(ModItems.THAUMOMETER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, scanner);
        scanner.getItem().use(level, player, InteractionHand.MAIN_HAND);
        BlockPos wall = BlockPos.containing(start.x, player.getEyeY(), start.z + 1.5);
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        scanner.getItem().onUseTick(level, player, scanner, 5);
        var knowledge = player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElseThrow(IllegalStateException::new);
        helper.assertTrue(!knowledge.hasScannedItem(ResourceLocation.fromNamespaceAndPath("minecraft", "glass")), "Target change must cancel rather than award the old item");
        level.removeBlock(wall, false);
        scanner.getItem().use(level, player, InteractionHand.MAIN_HAND);
        scanner.getItem().onUseTick(level, player, scanner, 5);
        helper.assertTrue(knowledge.hasDiscoveredAspect(Aspect.CRYSTAL), "Completed glass scan must discover Crystal");
        helper.succeed();
    }
}

package thaumcraft.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.blocks.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.blocks.ArcaneWorkbenchMenu;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.items.tools.ItemWand;
import thaumcraft.common.items.wands.ModWandParts;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class WandVariantsTests {
    private static final String[] RODS = {"wood", "greatwood", "obsidian", "blaze",
            "ice", "quartz", "bone", "reed", "silverwood"};
    private static final int[] CAPACITIES = {25, 50, 75, 75, 75, 75, 75, 75, 100};
    private static final String[] CAPS = {"iron", "gold", "thaumium", "void", "copper", "silver"};

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void allNormalWandCombinations(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        var menu = new ArcaneWorkbenchMenu(1200, player.getInventory(),
                new SimpleContainer(ArcaneWorkbenchBlockEntity.SIZE));
        var grid = menu.getGrid();
        var recipe = (ArcaneWandRecipe) helper.getLevel().getRecipeManager()
                .byKey(new ResourceLocation("thaumcraft", "arcane_wand")).orElseThrow();
        for (int r = 0; r < RODS.length; r++) {
            WandRod rod = WandRod.rods.get(RODS[r]);
            for (String tag : CAPS) {
                WandCap cap = WandCap.caps.get(tag);
                grid.clearContent();
                grid.setItem(2, cap.getItem());
                grid.setItem(4, rod.getItem());
                grid.setItem(6, cap.getItem());
                helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Parts must match: " + RODS[r] + "/" + tag);
                ItemStack wand = recipe.assemble(grid, helper.getLevel().registryAccess());
                if (r == 0 && tag.equals("iron")) {
                    helper.assertTrue(wand.isEmpty(), "Iron/wood belongs to the free vanilla recipe");
                    wand = new ItemStack(ModItems.WAND.get());
                } else {
                    helper.assertTrue(!wand.isEmpty() && ItemWand.getRod(wand) == rod && ItemWand.getCap(wand) == cap,
                            "Assembled wand must preserve both parts");
                }
                helper.assertTrue(ItemWand.getMaxVis(wand) == CAPACITIES[r] * 100, "Wrong capacity for " + RODS[r]);
                ItemStack saved = ItemStack.of(wand.save(new net.minecraft.nbt.CompoundTag()));
                helper.assertTrue(ItemWand.getCap(saved) == cap && ItemWand.getRod(saved) == rod, "Parts must survive NBT reload");
                for (Aspect primal : Aspect.getPrimalAspects()) {
                    int cost = cap.getCraftCost() * rod.getCraftCost();
                    helper.assertTrue(recipe.getAspects(grid).getAmount(primal) == cost, "Assembly cost must be cap x rod");
                    float expected = switch (tag) {
                        case "iron" -> 1.1F;
                        case "gold" -> 1.0F;
                        case "thaumium" -> 0.9F;
                        case "void" -> 0.8F;
                        case "copper" -> primal == Aspect.ORDER || primal == Aspect.ENTROPY ? 1.0F : 1.1F;
                        default -> primal == Aspect.ORDER || primal == Aspect.ENTROPY ? 1.0F : 0.95F;
                    };
                    helper.assertTrue(Math.abs(ItemWand.getConsumptionModifier(wand, null, primal, true) - expected) < 0.0001F,
                            "Wrong cap discount for " + tag + "/" + primal.getTag());
                    ItemWand.addVis(wand, primal, 1000, true);
                    helper.assertTrue(ItemWand.getVis(wand, primal) == ItemWand.getMaxVis(wand), "Charging must stop at capacity");
                }
                AspectList charge = new AspectList().add(Aspect.AIR, 1).add(Aspect.ORDER, 1);
                int airBefore = ItemWand.getVis(wand, Aspect.AIR);
                helper.assertTrue(ItemWand.consumeAllVisCrafting(wand, player, charge, false)
                        && ItemWand.getVis(wand, Aspect.AIR) == airBefore, "Preview must not spend vis");
                int airCost = (int) (100 * ItemWand.getConsumptionModifier(wand, null, Aspect.AIR, true));
                helper.assertTrue(ItemWand.consumeAllVisCrafting(wand, player, charge, true)
                        && ItemWand.getVis(wand, Aspect.AIR) == airBefore - airCost, "Craft must spend discounted vis once");
                ItemWand.storeVis(wand, Aspect.ORDER, 0);
                airBefore = ItemWand.getVis(wand, Aspect.AIR);
                helper.assertTrue(!ItemWand.consumeAllVisCrafting(wand, player, charge, true)
                        && ItemWand.getVis(wand, Aspect.AIR) == airBefore, "Insufficient aspect must prevent partial payment");
            }
        }
        menu.removed(player);
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void inertAndMismatchedCapsCannotAssemble(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        var menu = new ArcaneWorkbenchMenu(1201, player.getInventory(),
                new SimpleContainer(ArcaneWorkbenchBlockEntity.SIZE));
        var grid = menu.getGrid();
        var recipe = (ArcaneWandRecipe) helper.getLevel().getRecipeManager()
                .byKey(new ResourceLocation("thaumcraft", "arcane_wand")).orElseThrow();
        for (var item : new net.minecraft.world.item.Item[] {ModWandParts.CAP_SILVER_INERT.get(),
                ModWandParts.CAP_THAUMIUM_INERT.get(), ModWandParts.CAP_VOID_INERT.get()}) {
            grid.setItem(2, new ItemStack(item));
            grid.setItem(4, ModWandParts.WAND_ROD_GREATWOOD.getItem());
            grid.setItem(6, new ItemStack(item));
            helper.assertTrue(!recipe.matches(grid, helper.getLevel())
                    && recipe.assemble(grid, helper.getLevel().registryAccess()).isEmpty(), "Inert caps must not assemble");
        }
        grid.setItem(2, ModWandParts.WAND_CAP_GOLD.getItem());
        grid.setItem(6, ModWandParts.WAND_CAP_IRON.getItem());
        helper.assertTrue(!recipe.matches(grid, helper.getLevel())
                && recipe.assemble(grid, helper.getLevel().registryAccess()).isEmpty(), "Different caps must not assemble");
        menu.removed(player);
        helper.succeed();
    }
}

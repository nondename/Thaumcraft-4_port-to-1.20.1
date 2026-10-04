package thaumcraft.gametest;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.blocks.ArcaneWorkbenchBlock;
import thaumcraft.common.blocks.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.blocks.ArcaneWorkbenchMenu;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.items.tools.ItemWand;
import thaumcraft.common.items.wands.ModWandParts;
import thaumcraft.common.nodes.AuraNodeBlockEntity;
import thaumcraft.common.nodes.ModNodes;
import thaumcraft.common.research.ModResearch;
import thaumcraft.common.research.ResearchTableBlock;

/**
 * GameTests for the wand parts + arcane workbench slice: registry data ported from
 * TC4 ConfigItems, the four arcane recipes, plain-table conversion, the workbench
 * preview/payment rules of ContainerArcaneWorkbench#onCraftMatrixChanged and
 * SlotCraftingArcaneWorkbench#onPickupFromSlot, and TileNode wand charging.
 */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class WandWorkbenchTests {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }

    /** ConfigItems#init/postInit registry data + the original wand NBT contract. */
    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void wandPartsCapacityAndNbt(GameTestHelper helper) {
        // Craft costs of the six caps (original lines 287-311).
        helper.assertTrue(WandCap.caps.get("iron") == ModWandParts.WAND_CAP_IRON
                        && ModWandParts.WAND_CAP_IRON.getCraftCost() == 1,
                "Iron cap must register with craft cost 1");
        helper.assertTrue(ModWandParts.WAND_CAP_GOLD.getCraftCost() == 3
                        && ModWandParts.WAND_CAP_COPPER.getCraftCost() == 2
                        && ModWandParts.WAND_CAP_SILVER.getCraftCost() == 4
                        && ModWandParts.WAND_CAP_THAUMIUM.getCraftCost() == 6
                        && ModWandParts.WAND_CAP_VOID.getCraftCost() == 9,
                "Cap craft costs must match ConfigItems exactly");
        // Rod capacities (original lines 293-300) and staff cores (314-324).
        helper.assertTrue(WandRod.rods.get("wood") == ModWandParts.WAND_ROD_WOOD
                        && ModWandParts.WAND_ROD_WOOD.getCapacity() == 25
                        && ModWandParts.WAND_ROD_GREATWOOD.getCapacity() == 50
                        && ModWandParts.WAND_ROD_SILVERWOOD.getCapacity() == 100,
                "Wand rod capacities must match ConfigItems (25/50/100)");
        helper.assertTrue(WandRod.rods.get("greatwood_staff") == ModWandParts.STAFF_ROD_GREATWOOD
                        && ModWandParts.STAFF_ROD_GREATWOOD.getCapacity() == 125
                        && ModWandParts.STAFF_ROD_PRIMAL.getCapacity() == 250,
                "Staff cores must register under the _staff tag with capacities 125..250");

        // Default wand: iron cap + wood rod, storage = capacity x100 (original getMaxVis).
        ItemStack wand = new ItemStack(ModItems.WAND.get());
        helper.assertTrue(ItemWand.getCap(wand) == ModWandParts.WAND_CAP_IRON
                        && ItemWand.getRod(wand) == ModWandParts.WAND_ROD_WOOD
                        && !ItemWand.isStaff(wand),
                "A fresh wand must default to iron/wood");
        helper.assertTrue(ItemWand.getMaxVis(wand) == 2500, "Iron/wood wand stores 25 x 100 = 2500 per primal");
        ItemWand.setRod(wand, ModWandParts.WAND_ROD_GREATWOOD);
        helper.assertTrue(ItemWand.getMaxVis(wand) == 5000, "Greatwood rod raises storage to 50 x 100");

        // Storage is in hundredths: 10 whole vis = 1000 units.
        ItemWand.addVis(wand, Aspect.AIR, 10, true);
        helper.assertTrue(ItemWand.getVis(wand, Aspect.AIR) == 1000, "addVis stores hundredths of whole vis");

        // Stale cap tag falls back to iron (StaleWandTags contract).
        wand.getOrCreateTag().putString("cap", "vanished");
        helper.assertTrue(ItemWand.getCap(wand) == ModWandParts.WAND_CAP_IRON, "A stale cap tag must fall back to iron");

        // Staffs: isStaff + storage, never placed into the wand slot (menu asserts that below).
        ItemStack staff = new ItemStack(ModItems.WAND.get());
        ItemWand.setRod(staff, ModWandParts.STAFF_ROD_GREATWOOD);
        helper.assertTrue(ItemWand.isStaff(staff) && ItemWand.getMaxVis(staff) == 12500,
                "Greatwood staff core: isStaff and 125 x 100 storage");

        // Creative wands of the original getSubItems: 4 stacks, each filled to the brim.
        var creative = new ArrayList<ItemStack>();
        ItemWand.addCreativeWands(creative::add);
        helper.assertTrue(creative.size() == 4, "Creative tab must list the original four wands");
        for (ItemStack stack : creative) {
            for (Aspect primal : Aspect.getPrimalAspects()) {
                helper.assertTrue(ItemWand.getVis(stack, primal) == ItemWand.getMaxVis(stack),
                        "Creative wands must start full");
            }
        }
        helper.assertTrue(ItemWand.isSceptre(creative.get(3)) && ItemWand.getMaxVis(creative.get(3)) == 15000,
                "The fourth creative wand is a silverwood sceptre (100 x 150)");
        helper.succeed();
    }

    /** Recipe loading, matching and the original costs of the four arcane recipes. */
    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void arcaneRecipesLoadAndMatch(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        for(String key:new String[]{"CAP_gold","ROD_greatwood","GOGGLES"})thaumcraft.common.research.ResearchProgression.knowledge(player).grantResearch(key);
        // Recipe grids run through the menu's read-through view over a local container.
        var menu = new ArcaneWorkbenchMenu(990, player.getInventory(),
                new SimpleContainer(ArcaneWorkbenchBlockEntity.SIZE));
        var grid = menu.getGrid();

        // Goggles: arcane recipe with the exact ConfigRecipes cost (AIR/FIRE/WATER/EARTH 5, ENTROPY/ORDER 3).
        var goggles = level.getRecipeManager().byKey(id("goggles")).orElseThrow();
        helper.assertTrue(goggles instanceof ArcaneRecipe, "Goggles must load as a thaumcraft:arcane recipe");
        AspectList gcost = ((ArcaneRecipe) goggles).getAspects();
        helper.assertTrue(gcost.getAmount(Aspect.AIR) == 5 && gcost.getAmount(Aspect.FIRE) == 5
                        && gcost.getAmount(Aspect.WATER) == 5 && gcost.getAmount(Aspect.EARTH) == 5
                        && gcost.getAmount(Aspect.ENTROPY) == 3 && gcost.getAmount(Aspect.ORDER) == 3,
                "Goggles vis cost must be 5/5/5/5/3/3");
        helper.assertTrue(((ArcaneRecipe) goggles).getResearch().equals("GOGGLES"),
                "Goggles must retain their original research gate");
        helper.assertTrue(goggles.getIngredients().get(0).test(new ItemStack(Items.LEATHER))
                        && goggles.getIngredients().get(1).test(new ItemStack(Items.GOLD_INGOT))
                        && goggles.getIngredients().get(6).test(new ItemStack(ModItems.THAUMOMETER.get())),
                "Goggles ingredients must stay leather/gold/thaumometer");

        // Gold cap: NNN / N N of gold nuggets, ORDER/FIRE/AIR x gold craft cost (3).
        var gold = level.getRecipeManager().byKey(id("wand_cap_gold")).orElseThrow();
        helper.assertTrue(gold instanceof ArcaneRecipe, "Gold cap must be an arcane recipe");
        ArcaneRecipe goldRecipe = (ArcaneRecipe) gold;
        AspectList gcap = goldRecipe.getAspects();
        helper.assertTrue(gcap.size() == 3 && gcap.getAmount(Aspect.ORDER) == 3
                        && gcap.getAmount(Aspect.FIRE) == 3 && gcap.getAmount(Aspect.AIR) == 3,
                "Gold cap cost must be ORDER/FIRE/AIR x3");
        for (int i : new int[] {0, 1, 2, 3, 5}) {
            grid.setItem(i, new ItemStack(Items.GOLD_NUGGET));
        }
        helper.assertTrue(goldRecipe.matches(grid, level), "The NNN/N N nugget grid must match the gold cap recipe");
        helper.assertTrue(goldRecipe.assemble(grid, level.registryAccess()).is(ModWandParts.CAP_GOLD.get()),
                "Gold cap recipe must assemble into a gold cap");
        grid.clearContent();

        // Copper cap: same grid from copper nuggets, craft cost 2.
        var copper = level.getRecipeManager().byKey(id("wand_cap_copper")).orElseThrow();
        helper.assertTrue(copper instanceof ArcaneRecipe, "Copper cap must be an arcane recipe");
        AspectList ccap = ((ArcaneRecipe) copper).getAspects();
        helper.assertTrue(ccap.getAmount(Aspect.ORDER) == 2 && ccap.getAmount(Aspect.FIRE) == 2
                        && ccap.getAmount(Aspect.AIR) == 2,
                "Copper cap cost must be ORDER/FIRE/AIR x2");
        helper.assertTrue(!copper.getIngredients().get(0).test(ItemStack.EMPTY),
                "The copper nugget ingredient must reject empty slots");

        // Arcane wand: anti-diagonal caps + centre rod, cost = cap x rod craft cost.
        var wandRecipe = level.getRecipeManager().byKey(id("arcane_wand")).orElseThrow();
        helper.assertTrue(wandRecipe instanceof ArcaneWandRecipe, "The wand recipe must load as thaumcraft:arcane_wand");
        ArcaneWandRecipe wandTyped = (ArcaneWandRecipe) wandRecipe;
        grid.setItem(2, new ItemStack(ModWandParts.CAP_GOLD.get()));
        grid.setItem(6, new ItemStack(ModWandParts.CAP_GOLD.get()));
        grid.setItem(4, new ItemStack(ModWandParts.ROD_GREATWOOD.get()));
        helper.assertTrue(wandTyped.matches(grid, level), "Anti-diagonal caps with a centre rod must match");
        ItemStack out = wandTyped.assemble(grid, level.registryAccess());
        helper.assertTrue(ItemWand.getCap(out) == ModWandParts.WAND_CAP_GOLD
                        && ItemWand.getRod(out) == ModWandParts.WAND_ROD_GREATWOOD,
                "The assembled wand must take the parts' cap and rod");
        AspectList wcost = wandTyped.getAspects(grid);
        helper.assertTrue(wcost.getAmount(Aspect.EARTH) == 9 && wcost.size() == 6,
                "Wand cost = cap(3) x rod(3) = 9 per primal");

        // Wood + iron: matches (no research gate) but assemble is EMPTY — original line 54.
        grid.setItem(2, new ItemStack(ModWandParts.CAP_IRON.get()));
        grid.setItem(6, new ItemStack(ModWandParts.CAP_IRON.get()));
        grid.setItem(4, new ItemStack(Items.STICK));
        helper.assertTrue(wandTyped.matches(grid, level), "Wood+iron still matches, as in the original");
        helper.assertTrue(wandTyped.assemble(grid, level.registryAccess()).isEmpty(),
                "Wood+iron must not assemble through the arcane recipe (original line 54)");
        var vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(vanilla.isPresent()
                        && vanilla.get().assemble(grid, level.registryAccess()).is(ModItems.WAND.get()),
                "The plain wand.json recipe wins the wood+iron grid (vanilla-first rule)");
        helper.succeed();
    }

    /** BlockTable#onWandRightClick: plain table -> arcane worktable, wand parked or kept. */
    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void plainTableConvertsToWorkbench(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos plain = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(plain, ModResearch.TABLE.get().defaultBlockState(), 3);
        var player = helper.makeMockPlayer();
        for(String key:new String[]{"CAP_gold","ROD_greatwood","GOGGLES"})thaumcraft.common.research.ResearchProgression.knowledge(player).grantResearch(key);

        // A normal wand converts the table and is parked in slot 10, hand cleared.
        var wand = new ItemStack(ModItems.WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var ctx = new UseOnContext(level, player, InteractionHand.MAIN_HAND, wand,
                new BlockHitResult(Vec3.atCenterOf(plain), Direction.UP, plain, false));
        helper.assertTrue(wand.getItem().useOn(ctx) != net.minecraft.world.InteractionResult.PASS,
                "A wand click on a plain table must be consumed");
        helper.assertTrue(level.getBlockState(plain).is(ArcaneWorkbenchBlock.ARCANE_WORKBENCH.get()),
                "The plain table must convert into the arcane workbench");
        helper.assertTrue(level.getBlockEntity(plain) instanceof ArcaneWorkbenchBlockEntity,
                "The workbench must carry its block entity");
        var parked = ((ArcaneWorkbenchBlockEntity) level.getBlockEntity(plain))
                .getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND);
        helper.assertTrue(parked.is(ModItems.WAND.get()) && ItemWand.getCap(parked) == ModWandParts.WAND_CAP_IRON,
                "The clicked wand must be parked in slot 10");
        helper.assertTrue(player.getMainHandItem().isEmpty(),
                "The original clears the hand unconditionally (original line 246)");

        // Clicking the workbench again is a plain pass — no double conversion.
        var wand2 = new ItemStack(ModItems.WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand2);
        var ctx2 = new UseOnContext(level, player, InteractionHand.MAIN_HAND, wand2,
                new BlockHitResult(Vec3.atCenterOf(plain), Direction.UP, plain, false));
        helper.assertTrue(wand2.getItem().useOn(ctx2) == net.minecraft.world.InteractionResult.PASS,
                "An already converted workbench must not consume the click");
        helper.assertTrue(((ArcaneWorkbenchBlockEntity) level.getBlockEntity(plain))
                        .getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND).getCount() == 1,
                "Slot 10 must still hold exactly the first wand");

        // A staff converts the table but is neither parked nor consumed.
        BlockPos staffPos = plain.east();
        level.setBlock(staffPos, ModResearch.TABLE.get().defaultBlockState(), 3);
        var staff = new ItemStack(ModItems.WAND.get());
        ItemWand.setRod(staff, ModWandParts.STAFF_ROD_GREATWOOD);
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var ctx3 = new UseOnContext(level, player, InteractionHand.MAIN_HAND, staff,
                new BlockHitResult(Vec3.atCenterOf(staffPos), Direction.UP, staffPos, false));
        helper.assertTrue(staff.getItem().useOn(ctx3) != net.minecraft.world.InteractionResult.PASS,
                "A staff click on a plain table must be consumed");
        helper.assertTrue(level.getBlockState(staffPos).is(ArcaneWorkbenchBlock.ARCANE_WORKBENCH.get()),
                "The staff must convert the table as well");
        helper.assertTrue(((ArcaneWorkbenchBlockEntity) level.getBlockEntity(staffPos))
                        .getItem(ArcaneWorkbenchBlockEntity.SLOT_WAND).isEmpty()
                        && player.getMainHandItem().is(ModItems.WAND.get()),
                "A staff is kept in hand and never parked (original isStaff check)");
        helper.succeed();
    }

    /** onCraftMatrixChanged preview rules + SlotCraftingArcaneWorkbench payment. */
    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void workbenchPreviewAndVisPayment(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, ArcaneWorkbenchBlock.ARCANE_WORKBENCH.get().defaultBlockState(), 3);
        var be = (ArcaneWorkbenchBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockPlayer();
        for(String key:new String[]{"CAP_gold","ROD_greatwood","GOGGLES"})thaumcraft.common.research.ResearchProgression.knowledge(player).grantResearch(key);
        var menu = new ArcaneWorkbenchMenu(991, player.getInventory(), be);

        // Without a wand the arcane result never appears.
        for (int i : new int[] {0, 2, 3, 5}) {
            be.setItem(i, new ItemStack(Items.LEATHER));
        }
        for (int i : new int[] {1, 7}) {
            be.setItem(i, new ItemStack(Items.GOLD_INGOT));
        }
        for (int i : new int[] {6, 8}) {
            be.setItem(i, new ItemStack(ModItems.THAUMOMETER.get()));
        }
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "No wand in slot 10: the arcane preview must stay empty");

        // A wand without enough vis: the dry-run gate keeps the preview empty.
        var wand = new ItemStack(ModItems.WAND.get());
        ItemWand.setCap(wand, ModWandParts.WAND_CAP_GOLD);
        for (Aspect primal : Aspect.getPrimalAspects()) {
            ItemWand.storeVis(wand, primal, 100);
        }
        be.setItem(ArcaneWorkbenchBlockEntity.SLOT_WAND, wand);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "An underfunded wand must not preview (dry-run gate)");

        // Charged wand: goggles appear.
        for (Aspect primal : Aspect.getPrimalAspects()) {
            ItemWand.storeVis(wand, primal, 1000);
        }
        menu.onCraftMatrixChanged();
        helper.assertTrue(menu.getSlot(0).getItem().is(ModItems.GOGGLES.get()),
                "A charged wand must preview the goggles");
        helper.assertTrue(menu.getSlot(1).mayPlace(wand), "The wand slot must accept a wand");

        // The wand slot rejects staffs (original SlotLimitedByWand#isItemValid).
        var staff = new ItemStack(ModItems.WAND.get());
        ItemWand.setRod(staff, ModWandParts.STAFF_ROD_GREATWOOD);
        helper.assertTrue(!menu.getSlot(1).mayPlace(staff), "The wand slot must reject staffs");

        // Taking the result pays the exact cost (gold cap modifier 1.0) and empties the grid.
        menu.getSlot(0).onTake(player, menu.getSlot(0).remove(1));
        helper.assertTrue(ItemWand.getVis(wand, Aspect.AIR) == 500 && ItemWand.getVis(wand, Aspect.FIRE) == 500
                        && ItemWand.getVis(wand, Aspect.WATER) == 500 && ItemWand.getVis(wand, Aspect.EARTH) == 500
                        && ItemWand.getVis(wand, Aspect.ENTROPY) == 700 && ItemWand.getVis(wand, Aspect.ORDER) == 700,
                "Crafting must charge 5/5/5/5/3/3 whole vis through a 1.0 modifier cap");
        for (int i = 0; i < 9; i++) {
            helper.assertTrue(be.getItem(i).isEmpty(), "Taking the result must shrink every grid slot by one");
        }

        // Vanilla-first: wand.json wins even though ArcaneWandRecipe matches the grid too.
        be.setItem(2, new ItemStack(ModWandParts.CAP_IRON.get()));
        be.setItem(6, new ItemStack(ModWandParts.CAP_IRON.get()));
        be.setItem(4, new ItemStack(Items.STICK));
        menu.onCraftMatrixChanged();
        helper.assertTrue(menu.getSlot(0).getItem().is(ModItems.WAND.get()),
                "Vanilla-first: wand.json must win the wood+iron grid in the preview");
        helper.succeed();
    }

    /** TileNode#onUsingWandTick: a held wand charged from a node's aspects. */
    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void nodeTapChargesWand(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, ModNodes.AURA_NODE.get().defaultBlockState(), 3);
        helper.assertTrue(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity, "The node must carry its block entity");
        var node = (AuraNodeBlockEntity) level.getBlockEntity(pos);
        var source = new AspectList();
        source.add(Aspect.EARTH, 10);
        node.initialize(NodeType.NORMAL, NodeModifier.BRIGHT, source);
        var player = helper.makeMockPlayer();
        for(String key:new String[]{"CAP_gold","ROD_greatwood","GOGGLES"})thaumcraft.common.research.ResearchProgression.knowledge(player).grantResearch(key);

        // count must be a multiple of 5 (original onUsingTick gating); base tap rate is 1
        // without the NODETAPPER research (command-driven in this port).
        var wand = new ItemStack(ModItems.WAND.get());
        node.onUsingWandTick(wand, player, 5);
        helper.assertTrue(ItemWand.getVis(wand, Aspect.EARTH) == 100,
                "One tap must store one whole vis (100 storage units)");
        helper.assertTrue(node.getAspects().getAmount(Aspect.EARTH) == 9,
                "The node must spend the tapped vis");

        // A full wand has no room: nothing moves.
        var full = new ItemStack(ModItems.WAND.get());
        for (Aspect primal : Aspect.getPrimalAspects()) {
            ItemWand.storeVis(full, primal, ItemWand.getMaxVis(full));
        }
        node.onUsingWandTick(full, player, 10);
        helper.assertTrue(node.getAspects().getAmount(Aspect.EARTH) == 9,
                "A full wand must not drain the node");
        helper.succeed();
    }
}

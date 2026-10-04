package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.ArcaneWorkbenchBlock;
import thaumcraft.common.blocks.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.blocks.ArcaneWorkbenchMenu;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.items.tools.ItemWand;
import thaumcraft.common.items.wands.ModWandParts;

/** Exercise real menu clicks, shared viewers, save/load and block destruction. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class WorkbenchSafetyTests {
    private record Fixture(ArcaneWorkbenchBlockEntity table, Player player, ArcaneWorkbenchMenu menu) {}

    private static Fixture fixture(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, ArcaneWorkbenchBlock.ARCANE_WORKBENCH.get().defaultBlockState(), 3);
        var table = (ArcaneWorkbenchBlockEntity) helper.getLevel().getBlockEntity(pos);
        var player = helper.makeMockPlayer();
        player.setPos(Vec3.atCenterOf(pos));
        return new Fixture(table, player, new ArcaneWorkbenchMenu(1, player.getInventory(), table));
    }

    private static ItemStack goldWand(int vis) {
        var wand = new ItemStack(ModItems.WAND.get());
        ItemWand.setCap(wand, ModWandParts.WAND_CAP_GOLD);
        for (Aspect aspect : Aspect.getPrimalAspects()) ItemWand.storeVis(wand, aspect, vis);
        return wand;
    }

    private static void goldCaps(Fixture f, int count, int vis) {
        f.table.setItem(ArcaneWorkbenchBlockEntity.SLOT_WAND, goldWand(vis));
        for (int slot : new int[]{0, 1, 2, 3, 5}) f.table.setItem(slot, new ItemStack(Items.GOLD_NUGGET, count));
    }

    private static void fillInventory(Player player) {
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.BEDROCK, 64));
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void pickupChargesOnceAndStopsAtVisLimit(GameTestHelper helper) {
        Fixture f = fixture(helper);
        goldCaps(f, 4, 1000);
        for (int crafts = 1; crafts <= 3; crafts++) {
            f.menu.clicked(0, 0, ClickType.PICKUP, f.player);
            helper.assertTrue(f.menu.getCarried().is(ModWandParts.CAP_GOLD.get())
                    && f.menu.getCarried().getCount() == crafts, "Each pickup must give exactly one cap");
            for (int slot : new int[]{0, 1, 2, 3, 5})
                helper.assertTrue(f.table.getItem(slot).getCount() == 4 - crafts,
                        "A craft must consume one ingredient from a stack, not two");
            helper.assertTrue(ItemWand.getVis(f.table.getItem(10), Aspect.AIR) == 1000 - 300 * crafts,
                    "Each cap must cost exactly 300 storage units");
        }
        helper.assertTrue(f.menu.getSlot(0).getItem().isEmpty(), "Payment must immediately refresh the preview");
        f.menu.clicked(0, 0, ClickType.PICKUP, f.player);
        helper.assertTrue(f.menu.getCarried().getCount() == 3 && f.table.getItem(0).getCount() == 1,
                "An underfunded fourth craft must not award or consume anything");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void shiftCraftChargesEveryResult(GameTestHelper helper) {
        Fixture f = fixture(helper);
        goldCaps(f, 4, 1000);
        f.menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        int caps = f.player.getInventory().items.stream().filter(s -> s.is(ModWandParts.CAP_GOLD.get()))
                .mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(caps == 3 && f.table.getItem(0).getCount() == 1,
                "Shift-click must stop after three paid crafts");
        helper.assertTrue(ItemWand.getVis(f.table.getItem(10), Aspect.AIR) == 100,
                "Shift-click must pay for every output exactly once");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void fullInventoryDoesNotConsumeCraft(GameTestHelper helper) {
        Fixture f = fixture(helper);
        goldCaps(f, 2, 1000);
        fillInventory(f.player);
        f.menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        helper.assertTrue(f.table.getItem(0).getCount() == 2
                        && ItemWand.getVis(f.table.getItem(10), Aspect.AIR) == 1000
                        && f.menu.getSlot(0).getItem().is(ModWandParts.CAP_GOLD.get()),
                "A full inventory must not consume ingredients, vis or the preview");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void multiItemResultRequiresRoomForWholeCraft(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.table.setItem(0, new ItemStack(Items.OAK_LOG, 3));
        f.table.setItem(10, goldWand(1000));
        fillInventory(f.player);
        f.player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 63));
        f.menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        helper.assertTrue(f.table.getItem(0).getCount() == 3 && f.player.getInventory().getItem(0).getCount() == 63,
                "Room for one plank must not consume a four-plank craft");
        f.player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 60));
        f.menu.clicked(0, 0, ClickType.QUICK_MOVE, f.player);
        helper.assertTrue(f.table.getItem(0).getCount() == 2 && f.player.getInventory().getItem(0).getCount() == 64,
                "Exactly one complete four-plank craft must fit");
        helper.assertTrue(ItemWand.getVis(f.table.getItem(10), Aspect.AIR) == 1000, "Vanilla crafts must not cost vis");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void vanillaRecipeReturnsBucketsExactlyOnce(GameTestHelper helper) {
        Fixture f = fixture(helper);
        for (int i = 0; i < 3; i++) f.table.setItem(i, new ItemStack(Items.MILK_BUCKET));
        f.table.setItem(3, new ItemStack(Items.SUGAR, 3));
        f.table.setItem(4, new ItemStack(Items.EGG, 3));
        f.table.setItem(5, new ItemStack(Items.SUGAR, 3));
        for (int i = 6; i < 9; i++) f.table.setItem(i, new ItemStack(Items.WHEAT, 3));
        f.menu.clicked(0, 0, ClickType.PICKUP, f.player);
        helper.assertTrue(f.menu.getCarried().is(Items.CAKE), "Cake recipe must produce a cake");
        for (int i = 0; i < 3; i++) helper.assertTrue(f.table.getItem(i).is(Items.BUCKET)
                && f.table.getItem(i).getCount() == 1, "Each milk bucket must return one empty bucket");
        for (int i = 3; i < 9; i++) helper.assertTrue(f.table.getItem(i).getCount() == 2,
                "Stacked cake ingredients must be decremented only once");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void twoViewersHaveIndependentDiscountsAndStayUpdated(GameTestHelper helper) {
        Fixture f = fixture(helper);
        goldCaps(f, 2, 290);
        var second = helper.makeMockPlayer();
        second.setPos(f.player.position());
        second.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GOGGLES.get()));
        var secondMenu = new ArcaneWorkbenchMenu(2, second.getInventory(), f.table);
        helper.assertTrue(f.menu.getSlot(0).getItem().isEmpty()
                        && secondMenu.getSlot(0).getItem().is(ModWandParts.CAP_GOLD.get()),
                "Only the viewer with a five-percent discount can afford 290-unit vis");
        secondMenu.clicked(0, 0, ClickType.PICKUP, second);
        helper.assertTrue(f.table.getItem(0).getCount() == 1 && ItemWand.getVis(f.table.getItem(10), Aspect.AIR) == 5,
                "The second viewer must pay their own discounted cost");
        helper.assertTrue(f.menu.getSlot(0).getItem().isEmpty() && secondMenu.getSlot(0).getItem().isEmpty(),
                "Both previews must refresh after the shared wand is depleted");
        secondMenu.removed(second);
        f.table.setItem(10, goldWand(1000));
        helper.assertTrue(f.menu.getSlot(0).getItem().is(ModWandParts.CAP_GOLD.get()),
                "Closing another viewer must not disconnect the first viewer");
        // Removing worn discount gear must invalidate a previously affordable preview at pickup.
        f.table.setItem(10, goldWand(290));
        f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.GOGGLES.get()));
        f.menu.broadcastChanges();
        f.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        f.menu.clicked(0, 0, ClickType.PICKUP, f.player);
        helper.assertTrue(f.menu.getCarried().isEmpty() && f.table.getItem(0).getCount() == 1,
                "A stale discounted preview must not be craftable without the discount gear");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void saveLoadPreservesInputsAndDiscardsPreview(GameTestHelper helper) {
        Fixture f = fixture(helper);
        goldCaps(f, 3, 1000);
        CompoundTag saved = f.table.saveWithoutMetadata();
        var restored = new ArcaneWorkbenchBlockEntity(f.table.getBlockPos(), f.table.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.getItem(0).getCount() == 3
                        && ItemStack.isSameItemSameTags(restored.getItem(10), f.table.getItem(10))
                        && restored.getItem(9).isEmpty(), "Save/load must retain inputs and wand NBT, never the preview");
        f.table.setItemSoftly(9, new ItemStack(ModWandParts.CAP_GOLD.get()));
        helper.assertTrue(f.table.saveWithoutMetadata().getList("Items", 10).size() == 6,
                "Old shared result slots must be excluded from the saved inventory");
        f.menu.removed(f.player);
        var reopened = new ArcaneWorkbenchMenu(3, f.player.getInventory(), f.table);
        helper.assertTrue(reopened.getSlot(0).getItem().is(ModWandParts.CAP_GOLD.get()),
                "Reopening must rebuild the unpaid preview from saved ingredients");
        helper.succeed();
    }

    @GameTest(templateNamespace = "thaumcraft", template = "empty")
    public static void breakingTableDropsInputsAndWandButNeverPreview(GameTestHelper helper) {
        Fixture f = fixture(helper);
        goldCaps(f, 2, 1000);
        // Simulate an old save which persisted the result alongside its unpaid inputs.
        f.table.setItemSoftly(9, new ItemStack(ModWandParts.CAP_GOLD.get()));
        BlockPos pos = f.table.getBlockPos();
        helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        int nuggets = drops.stream().filter(e -> e.getItem().is(Items.GOLD_NUGGET))
                .mapToInt(e -> e.getItem().getCount()).sum();
        int wands = drops.stream().filter(e -> e.getItem().is(ModItems.WAND.get()))
                .mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(nuggets == 10 && wands == 1
                        && drops.stream().noneMatch(e -> e.getItem().is(ModWandParts.CAP_GOLD.get())),
                "Breaking must return exactly ten nuggets and one wand, never a free crafted cap");
        helper.assertTrue(!f.menu.getSlot(0).mayPickup(f.player), "A removed table's stale preview cannot be taken");
        helper.succeed();
    }
}

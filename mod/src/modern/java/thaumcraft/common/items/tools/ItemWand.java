package thaumcraft.common.items.tools;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.IWandRodOnUpdate;
import thaumcraft.api.wands.IWandable;
import thaumcraft.api.wands.StaffRod;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.blocks.ArcaneWorkbenchBlock;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.items.wands.ModWandParts;
import thaumcraft.common.nodes.ModNodes;
import thaumcraft.common.research.ResearchTableBlock;
import thaumcraft.common.sounds.ModSounds;

/**
 * Port of TC4 ItemWandCasting (decompiled/common/items/wands/ItemWandCasting.java, 1.7.10).
 *
 * <p>NBT contract is identical to the original: {@code "rod"} / {@code "cap"} strings,
 * {@code "sceptre"} byte, per-primal vis ints in hundredths of a vis point, and the
 * {@code IIUX/Y/Z} object-in-use coordinates while charging a node (setItemInUse analog).
 *
 * <p>Documented deviations:
 * <ul>
 *   <li>foci (focus NBT, focus tooltips, focus on-use ticks) are not ported yet;</li>
 *   <li>{@code IArchitect} (line 51 of the original) is not ported;</li>
 *   <li>the vis discount comes from armor pieces only — the original summed baubles,
 *       armor and the vis-exhaust potions (WandManager#getotalVisDiscount lines 50-86);
 *       neither baubles nor those potions exist in the modern port;</li>
 *   <li>the tooltip player is resolved client-side ({@code Minecraft.getInstance().player})
 *       because modern appendHoverText has no player parameter;</li>
 *   <li>node charge drain visuals (drainEntity/drainColor) are deferred: the modern
 *       onUseTick/onWandStoppedUsing flow is server-side only.</li>
 * </ul>
 */
public final class ItemWand extends Item {
    private static final String TAG_ROD = "rod";
    private static final String TAG_CAP = "cap";
    private static final String TAG_SCEPTRE = "sceptre";
    private static final String TAG_IIX = "IIUX";
    private static final String TAG_IIY = "IIY";
    private static final String TAG_IIZ = "IIZ";
    /** "Weapon modifier" +6 attack damage uuid of the original setRod (Item#itemModifierUUID). */
    private static final UUID STAFF_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    /** DecimalFormat("#######.##") of the original tooltip (line 53). */
    private static final DecimalFormat FORMATTER = new DecimalFormat("#######.##");

    public ItemWand(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new thaumcraft.client.WandChargeAnimation());
    }

    /** Vis and the node target change NBT while using the wand; keep the action continuous. */
    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return sameWand(oldStack, newStack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !sameWand(oldStack, newStack);
    }

    private static boolean sameWand(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() == newStack.getItem()
                && oldStack.getItem() instanceof ItemWand
                && getRod(oldStack) == getRod(newStack)
                && getCap(oldStack) == getCap(newStack)
                && isSceptre(oldStack) == isSceptre(newStack);
    }

    // ---------------------------------------------------------------- vis storage
    // getAllVis / getAspectsWithRoom / storeAllVis / getVis / storeVis — original lines 208-252.

    public static AspectList getAllVis(ItemStack is) {
        AspectList out = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            if (is.hasTag() && is.getTag().contains(aspect.getTag(), Tag.TAG_INT)) {
                out.merge(aspect, is.getTag().getInt(aspect.getTag()));
            } else {
                out.merge(aspect, 0);
            }
        }
        return out;
    }

    public static AspectList getAspectsWithRoom(ItemStack wandstack) {
        AspectList out = new AspectList();
        AspectList cur = getAllVis(wandstack);
        for (Aspect aspect : cur.getAspects()) {
            if (cur.getAmount(aspect) < getMaxVis(wandstack)) {
                out.add(aspect, 1);
            }
        }
        return out;
    }

    public static void storeAllVis(ItemStack is, AspectList in) {
        for (Aspect aspect : in.getAspects()) {
            is.getOrCreateTag().putInt(aspect.getTag(), in.getAmount(aspect));
        }
    }

    /** Raw NBT read, like the original (no clamping — a stale tag may exceed capacity). */
    public static int getVis(ItemStack is, Aspect aspect) {
        if (is != null && aspect != null && is.hasTag() && is.getTag().contains(aspect.getTag(), Tag.TAG_INT)) {
            return is.getTag().getInt(aspect.getTag());
        }
        return 0;
    }

    public static void storeVis(ItemStack is, Aspect aspect, int amount) {
        is.getOrCreateTag().putInt(aspect.getTag(), amount);
    }

    /** Kept for callers of the first port revision; same contract as storeVis. */
    public static void setVis(ItemStack stack, Aspect aspect, int amount) {
        storeVis(stack, aspect, amount);
    }

    // ---------------------------------------------------------------- rod / cap / sceptre
    // getRod / setRod / getCap / setCap / isStaff / isSceptre / getMaxVis — original lines 494-536.

    public static WandCap getCap(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_CAP, Tag.TAG_STRING)) {
            WandCap cap = WandCap.caps.get(stack.getTag().getString(TAG_CAP));
            if (cap != null) {
                return cap;
            }
            // Stale tag from removed content falls back to iron (StaleWandTags contract).
        }
        return ModWandParts.WAND_CAP_IRON;
    }

    public static void setCap(ItemStack stack, WandCap cap) {
        stack.getOrCreateTag().putString(TAG_CAP, cap.getTag());
    }

    public static WandRod getRod(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_ROD, Tag.TAG_STRING)) {
            WandRod rod = WandRod.rods.get(stack.getTag().getString(TAG_ROD));
            if (rod != null) {
                return rod;
            }
            // Stale tag from removed content falls back to wood.
        }
        return ModWandParts.WAND_ROD_WOOD;
    }

    public static void setRod(ItemStack stack, WandRod rod) {
        stack.getOrCreateTag().putString(TAG_ROD, rod.getTag());
        if (rod instanceof StaffRod) {
            // Original setRod lines 501-513: staff cores carry +6 generic.attack_damage.
            // Documented micro-deviation: the modifier only applies while held in the main
            // hand (1.20.1 attribute NBT is slot-bound; 1.7.10 had no slot concept).
            stack.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(STAFF_DAMAGE_UUID, "Weapon modifier", 6.0,
                            AttributeModifier.Operation.ADDITION),
                    EquipmentSlot.MAINHAND);
        }
    }

    public static boolean isStaff(ItemStack stack) {
        return getRod(stack) instanceof StaffRod;
    }

    public static boolean isSceptre(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_SCEPTRE);
    }

    public static boolean hasRunes(ItemStack stack) {
        return getRod(stack) instanceof StaffRod rod && rod.hasRunes();
    }

    /** Original getMaxVis: rod capacity in whole vis x 100 storage, x150 for sceptres. */
    public static int getMaxVis(ItemStack stack) {
        return getRod(stack).getCapacity() * (isSceptre(stack) ? 150 : 100);
    }

    // ---------------------------------------------------------------- consumption

    /**
     * Port of getTotalVisDiscount (WandManager lines 50-86) minus baubles and the
     * vis-exhaust potions: sum of IVisDiscountGear armor pieces / 100.
     */
    public static float getTotalVisDiscount(Player player, Aspect aspect) {
        if (player == null) {
            return 0.0F;
        }
        int total = 0;
        for (ItemStack armor : player.getInventory().armor) {
            if (!armor.isEmpty() && armor.getItem() instanceof IVisDiscountGear gear) {
                total += gear.getVisDiscount(armor, player, aspect);
            }
        }
        return total / 100.0F;
    }

    /** Original getConsumptionModifier lines 254-275 (focus frugal term deferred with foci). */
    public static float getConsumptionModifier(ItemStack is, Player player, Aspect aspect, boolean crafting) {
        WandCap cap = getCap(is);
        float consumptionModifier = cap.getSpecialCostModifierAspects() != null
                && cap.getSpecialCostModifierAspects().contains(aspect)
                ? cap.getSpecialCostModifier()
                : cap.getBaseCostModifier();
        if (player != null) {
            consumptionModifier -= getTotalVisDiscount(player, aspect);
        }
        // TC4: consumptionModifier -= getFocusFrugal(is) / 10.0F when a focus is present.
        if (isSceptre(is)) {
            consumptionModifier -= 0.1F;
        }
        return Math.max(consumptionModifier, 0.1F);
    }

    /** Original consumeVis lines 299-307. */
    public static boolean consumeVis(ItemStack is, Player player, Aspect aspect, int amount, boolean crafting) {
        amount = (int) (amount * getConsumptionModifier(is, player, aspect, crafting));
        if (getVis(is, aspect) >= amount) {
            storeVis(is, aspect, getVis(is, aspect) - amount);
            return true;
        }
        return false;
    }

    /** Original consumeAllVisCrafting lines 309-322 (recipe costs are whole vis, x100 here). */
    public static boolean consumeAllVisCrafting(ItemStack is, Player player, AspectList aspects, boolean doit) {
        if (aspects != null && aspects.size() != 0) {
            AspectList nl = new AspectList();
            for (Aspect aspect : aspects.getAspects()) {
                int cost = aspects.getAmount(aspect) * 100;
                nl.add(aspect, cost);
            }
            return consumeAllVis(is, player, nl, doit, true);
        }
        return false;
    }

    /** Original consumeAllVis lines 324-350. */
    public static boolean consumeAllVis(ItemStack is, Player player, AspectList aspects, boolean doit, boolean crafting) {
        if (aspects != null && aspects.size() != 0) {
            AspectList nl = new AspectList();
            for (Aspect aspect : aspects.getAspects()) {
                int cost = aspects.getAmount(aspect);
                cost = (int) (cost * getConsumptionModifier(is, player, aspect, crafting));
                nl.add(aspect, cost);
            }
            for (Aspect aspect : nl.getAspects()) {
                if (getVis(is, aspect) < nl.getAmount(aspect)) {
                    return false;
                }
            }
            if (doit && player != null && !player.level().isClientSide) {
                for (Aspect aspect : nl.getAspects()) {
                    storeVis(is, aspect, getVis(is, aspect) - nl.getAmount(aspect));
                }
            }
            return true;
        }
        return false;
    }

    /** Original addVis lines 352-364: amount is in whole vis, storage is hundredths. */
    public static int addVis(ItemStack is, Aspect aspect, int amount, boolean doit) {
        if (!aspect.isPrimal()) {
            return 0;
        }
        int storeAmount = getVis(is, aspect) + amount * 100;
        int leftover = Math.max(storeAmount - getMaxVis(is), 0);
        if (doit) {
            storeVis(is, aspect, Math.min(storeAmount, getMaxVis(is)));
        }
        return leftover / 100;
    }

    /** Original addRealVis lines 366-378: amount is already in storage units (hundredths). */
    public static int addRealVis(ItemStack is, Aspect aspect, int amount, boolean doit) {
        if (!aspect.isPrimal()) {
            return 0;
        }
        int storeAmount = getVis(is, aspect) + amount;
        int leftover = Math.max(storeAmount - getMaxVis(is), 0);
        if (doit) {
            storeVis(is, aspect, Math.min(storeAmount, getMaxVis(is)));
        }
        return leftover;
    }

    // ---------------------------------------------------------------- name / tooltip
    // getItemStackDisplayName lines 117-132 + addInformation lines 134-206.

    @Override
    public Component getName(ItemStack stack) {
        // The original composes "item.Wand.name" = "%CAP %ROD %OBJ"; languages can't do
        // %TOKEN substitution in 1.20.1 components, so the same three keys are composed here
        // in the fixed original order (documented deviation; item.Wand.name kept in lang).
        WandRod rod = getRod(stack);
        String rodKey = rod.getTag();
        int staffIdx = rodKey.indexOf("_staff");
        if (staffIdx >= 0) {
            rodKey = rodKey.substring(0, staffIdx);
        }
        Component obj = isStaff(stack)
                ? Component.translatable("item.Wand.staff.obj")
                : isSceptre(stack)
                ? Component.translatable("item.Wand.sceptre.obj")
                : Component.translatable("item.Wand.wand.obj");
        return Component.empty()
                .append(Component.translatable("item.Wand." + getCap(stack).getTag() + ".cap"))
                .append(" ")
                .append(Component.translatable("item.Wand." + rodKey + ".rod"))
                .append(" ")
                .append(obj);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        int pos = lines.size();
        String tt2 = "";
        if (stack.hasTag()) {
            StringBuilder tt = new StringBuilder();
            int tot = 0;
            int num = 0;
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                if (stack.getTag().contains(aspect.getTag(), Tag.TAG_INT)) {
                    String amount = FORMATTER.format(stack.getTag().getInt(aspect.getTag()) / 100.0F);
                    float mod = getConsumptionModifier(stack, tooltipPlayer(), aspect, false);
                    String consumption = FORMATTER.format(mod * 100.0F);
                    num++;
                    tot = (int) (tot + mod * 100.0F);
                    if (shiftDown()) {
                        // Original line 162-174; focus cost tail (item.Focus.cost1/2) is
                        // skipped while foci are not ported.
                        lines.add(Component.literal(" §" + aspect.getChatcolor() + aspect.getName() + "§r x "
                                + amount + ", ")
                                .append(Component.literal("(" + consumption + "% ").withStyle(ChatFormatting.ITALIC))
                                .append(Component.translatable("tc.vis.cost").withStyle(ChatFormatting.ITALIC))
                                .append(Component.literal(")").withStyle(ChatFormatting.ITALIC)));
                    } else {
                        if (tt.length() > 0) {
                            tt.append(" | ");
                        }
                        // Original line 180: chatcolor of the aspect colours the amount.
                        tt.append("§").append(aspect.getChatcolor()).append(amount).append("§r");
                    }
                }
            }
            if (!shiftDown() && num > 0) {
                lines.add(Component.literal(tt.toString()));
                tot /= num;
                tt2 = " (" + tot + "% ";
            }
        }
        // Original line 192: capacity line inserted first, GOLD, plain average-cost suffix.
        MutableComponent capacity = Component.empty()
                .append(Component.translatable("item.capacity.text").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" " + (getMaxVis(stack) / 100)).withStyle(ChatFormatting.GOLD));
        if (!tt2.isEmpty()) {
            capacity.append(Component.literal(tt2))
                    .append(Component.translatable("tc.vis.costavg"))
                    .append(Component.literal(")"));
        }
        lines.add(pos, capacity);
        // Original focus lines (193-205) are skipped while foci are not ported.
    }

    /** Player used for the gear-discount part of tooltip percentages (client only). */
    @Nullable
    private static Player tooltipPlayer() {
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
    }

    /** Original Thaumcraft.proxy.isShiftKeyDown (tooltip lines 161, 185). */
    private static boolean shiftDown() {
        Boolean held = DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                () -> () -> Screen.hasShiftDown());
        return Boolean.TRUE.equals(held);
    }

    // ---------------------------------------------------------------- use flow

    /**
     * Reach of the original EntityUtils.getMovingObjectPositionFromPlayer lines 211-214:
     * ItemInWorldManager.getBlockReachDistance = creative 5.0 / survival 4.5.
     */
    private static double reach(Player player) {
        return player.isCreative() ? 5.0D : 4.5D;
    }

    /**
     * Ray from the eye against the aura node's interaction box (BlockAiry sets
     * 0.3..0.7 for meta 0 in the original; the modern clip can noCollission blocks,
     * so the node box is tested directly like the original wand ray did — it passed
     * ignoreBlockWithoutBoundingBox=false, see ThaumcraftApiHelper lines 341-342).
     *
     * @return the node position, or null when the player isn't aiming at one
     */
    @Nullable
    public static BlockPos findNodeTarget(Level level, Player player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        double reach = reach(player);
        Vec3 end = eye.add(look.scale(reach));
        for (double t = 0; t <= reach; t += 0.05D) {
            Vec3 point = eye.add(look.scale(t));
            BlockPos pos = new BlockPos((int) Math.floor(point.x), (int) Math.floor(point.y), (int) Math.floor(point.z));
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModNodes.AURA_NODE.get())) {
                continue;
            }
            AABB box = new AABB(pos.getX() + 0.3, pos.getY() + 0.3, pos.getZ() + 0.3,
                    pos.getX() + 0.7, pos.getY() + 0.7, pos.getZ() + 0.7);
            if (box.clip(eye, end).isPresent()) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Port of TileNode#onWandRightClick (lines 152-158): store IIUX/Y/Z in the wand and
     * start the bow-style use animation (original player.setItemInUse(wand, MAX_VALUE)).
     *
     * @return true when a node was grabbed and using started
     */
    private static boolean startNodeTap(Level level, Player player, InteractionHand hand) {
        BlockPos node = findNodeTarget(level, player);
        if (node == null) {
            return false;
        }
        if (!level.isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            stack.getOrCreateTag().putInt(TAG_IIX, node.getX());
            stack.getTag().putInt(TAG_IIY, node.getY());
            stack.getTag().putInt(TAG_IIZ, node.getZ());
        }
        player.startUsingItem(hand);
        return true;
    }

    /** Original getObjectInUse: IWandable at the stored coordinates, or null. */
    @Nullable
    public static BlockPos getObjectInUse(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_IIX, Tag.TAG_INT)) {
            return null;
        }
        return new BlockPos(stack.getTag().getInt(TAG_IIX), stack.getTag().getInt(TAG_IIY),
                stack.getTag().getInt(TAG_IIZ));
    }

    /** Original clearObjectInUse lines 571-576. */
    public static void clearObjectInUse(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().remove(TAG_IIX);
            stack.getTag().remove(TAG_IIY);
            stack.getTag().remove(TAG_IIZ);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (startNodeTap(level, player, hand)) {
            return InteractionResultHolder.consume(stack);
        }
        // Original onItemRightClick falls through to super with no focus: nothing happens.
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        InteractionResult crucible = thaumcraft.common.alchemy.CrucibleBlock.tryConvert(context);
        if (crucible != InteractionResult.PASS) return crucible;
        // 1) Plain-table -> arcane worktable conversion. Original: onItemUseFirst checks
        //    block IWandable first (BlockTable#onWandRightClick, md<=1); the modern mapping
        //    of the plain table is the research table in its lone PART==0 state.
        InteractionResult converted = ArcaneWorkbenchBlock.tryConvertPlainTable(context);
        if (converted != InteractionResult.PASS) {
            return converted;
        }
        // 2) Bookshelf -> thaumonomicon (existing port; original WandTriggerRegistry event 0).
        var pos = context.getClickedPos();
        if (level.getBlockState(pos).is(Blocks.BOOKSHELF)) {
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))
                return InteractionResult.FAIL;
            if (level instanceof ServerLevel server && level.removeBlock(pos, false)) {
                var book = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        new ItemStack(ModItems.THAUMONOMICON.get()));
                book.setDeltaMovement(0, 0, 0);
                book.setDefaultPickUpDelay();
                level.addFreshEntity(book);
                server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        32, 0.3, 0.3, 0.3, 0.2);
                // TC4 sounds.json: the wand event lives in the "master" category
                level.playSound(null, pos, ModSounds.WAND.get(), SoundSource.MASTER, 1, 1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // 3) Node behind / in front of the clicked block (original charged through the
        //    dedicated wand ray; the modern analog keeps it available from useOn so a
        //    wall behind a node doesn't block the tap).
        if (startNodeTap(level, player, context.getHand())) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Original onUsingTick lines 595-607 + TileNode#onUsingWandTick re-ray (lines 377-387). */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        BlockPos pos = getObjectInUse(stack);
        if (pos == null) {
            return;
        }
        if (!pos.equals(findNodeTarget(level, player))) {
            // Looking away stops the charge; releaseUsing then runs onWandStoppedUsing.
            player.stopUsingItem();
            return;
        }
        if (level.getBlockEntity(pos) instanceof IWandable wandable) {
            wandable.onUsingWandTick(stack, player, count);
        }
    }

    /** Original onPlayerStoppedUsing lines 609-623 (focus branch deferred). */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        BlockPos pos = getObjectInUse(stack);
        clearObjectInUse(stack);
        if (pos != null && level.getBlockEntity(pos) instanceof IWandable wandable) {
            wandable.onWandStoppedUsing(stack, level, player, timeLeft);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // Original getMaxItemUseDuration lines 628-630.
        return Integer.MAX_VALUE;
    }

    /** Original onUpdate lines 380-387: rod affinity effects tick server-side. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player) {
            IWandRodOnUpdate onUpdate = getRod(stack).getOnUpdate();
            if (onUpdate != null) {
                onUpdate.onUpdate(stack, player);
            }
        }
    }

    // ---------------------------------------------------------------- creative tabs

    /**
     * The four filled wands of the original getSubItems lines 92-114:
     * iron/wood, gold/greatwood, thaumium/silverwood and a thaumium/silverwood sceptre,
     * each filled to its max vis.
     */
    public static void addCreativeWands(java.util.function.Consumer<ItemStack> output) {
        ItemStack w1 = new ItemStack(ModItems.WAND.get());
        ItemStack w2 = new ItemStack(ModItems.WAND.get());
        ItemStack w3 = new ItemStack(ModItems.WAND.get());
        setCap(w2, ModWandParts.WAND_CAP_GOLD);
        setCap(w3, ModWandParts.WAND_CAP_THAUMIUM);
        setRod(w2, ModWandParts.WAND_ROD_GREATWOOD);
        setRod(w3, ModWandParts.WAND_ROD_SILVERWOOD);
        ItemStack sceptre = new ItemStack(ModItems.WAND.get());
        setCap(sceptre, ModWandParts.WAND_CAP_THAUMIUM);
        setRod(sceptre, ModWandParts.WAND_ROD_SILVERWOOD);
        sceptre.getOrCreateTag().putByte(TAG_SCEPTRE, (byte) 1);
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            addVis(w1, aspect, getMaxVis(w1), true);
            addVis(w2, aspect, getMaxVis(w2), true);
            addVis(w3, aspect, getMaxVis(w3), true);
            addVis(sceptre, aspect, getMaxVis(sceptre), true);
        }
        output.accept(w1);
        output.accept(w2);
        output.accept(w3);
        output.accept(sceptre);
    }

    /** Rarity of the original getRarity: uncommon. */
    public static Properties wandProperties() {
        return new Properties().rarity(Rarity.UNCOMMON);
    }
}

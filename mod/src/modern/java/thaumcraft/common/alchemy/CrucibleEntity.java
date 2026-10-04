package thaumcraft.common.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.RecipeAspects;

public final class CrucibleEntity extends BlockEntity {
    private int water, heat, pollution;
    private long counter = -100;
    private AspectList aspects = new AspectList();
    public CrucibleEntity(BlockPos pos, BlockState state) { super(ModAlchemy.ENTITY.get(), pos, state); }
    public int getWater() { return water; }
    public int getHeat() { return heat; }
    public AspectList getAspects() { return aspects.copy(); }
    public boolean isBoiling() { return heat > 150 && water > 0; }
    public double getFluidHeight() { double base = .3 + .5 * water / 1000.0; return Math.min(.9999, base + aspects.visSize() / 100.0 * (1 - base)); }
    public void fillWater() { water = 1000; changed(); }
    public void empty() {
        if (level != null && level.isClientSide) return;
        int remnants = aspects.visSize();
        if (level instanceof ServerLevel server) {
            // Saved TC4 spillRemnants: one spill attempt per two remaining units.
            for (int i = 0; i < remnants / 2; i++) FluxSpill.attempt(server, worldPosition);
            if (water > 0 || remnants > 0) {
                server.sendParticles(ParticleTypes.WITCH, worldPosition.getX()+.5, worldPosition.getY()+.9,
                        worldPosition.getZ()+.5, 8, .2, .1, .2, .02);
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .5F, 1F);
            }
        }
        pollution += remnants; water = 0; aspects = new AspectList(); changed();
    }
    private void changed() {
        setChanged();
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        int amount = water == 0 ? 0 : Math.min(3, (water + 332) / 333);
        if (state.is(ModAlchemy.CRUCIBLE.get()) && state.getValue(CrucibleBlock.WATER) != amount)
            level.setBlock(worldPosition, state.setValue(CrucibleBlock.WATER, amount), 3);
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }
    public static void tick(Level level, BlockPos pos, BlockState state, CrucibleEntity c) {
        ++c.counter;
        BlockState below = level.getBlockState(pos.below());
        boolean hot = below.is(BlockTags.FIRE) || below.is(Blocks.LAVA)
                || below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT);
        int previous = c.heat;
        c.heat = c.water > 0 && hot ? Math.min(200, c.heat + 1) : Math.max(0, c.heat - 1);
        if (previous != c.heat) c.setChanged();
        if (previous <= 150 && c.heat > 150 || previous > 150 && c.heat <= 150) c.changed();
        if (c.isBoiling()) {
            AABB bowl = new AABB(pos.getX() + .125, pos.getY() + .3, pos.getZ() + .125,
                    pos.getX() + .875, pos.getY() + 1.1, pos.getZ() + .875);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bowl)) c.process(item);
            if (c.counter > 100) {
                c.counter = 0;
                if (c.aspects.size() > 0) {
                    Aspect[] all = c.aspects.getAspects();
                    Aspect aspect = all[level.random.nextInt(all.length)];
                    c.aspects.remove(aspect, 1); c.water = Math.max(0, c.water - 2);
                    if (!aspect.isPrimal()) c.aspects.add(aspect.getComponents()[level.random.nextInt(2)], 1);
                    else { ++c.pollution; FluxSpill.attempt((ServerLevel) level, pos); }
                    c.changed();
                }
            }
        }
        if (c.aspects.visSize() > 100 && c.counter % 5 == 0) {
            Aspect[] all = c.aspects.getAspects();
            c.aspects.remove(all[level.random.nextInt(all.length)], 1); ++c.pollution; c.changed();
            FluxSpill.attempt((ServerLevel) level, pos);
            ((ServerLevel) level).sendParticles(ParticleTypes.WITCH, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 2, .2, .1, .2, 0);
        }
    }
    public void process(ItemEntity entity) {
        if (level == null || level.isClientSide || !isBoiling() || !entity.isAlive()
                || entity.getPersistentData().getBoolean("CrucibleOutput")
                || entity.getPersistentData().getLong("CrucibleRetry") > level.getGameTime()) return;
        ItemStack stack = entity.getItem();
        while (!stack.isEmpty() && water > 0) {
            CrucibleRecipe recipe = level.getRecipeManager().getAllRecipesFor(CrucibleRecipe.TYPE.get()).stream()
                    .filter(r -> r.canCraft(stack, aspects)).sorted(java.util.Comparator.comparing(r -> r.getId().toString())).findFirst().orElse(null);
            if (recipe != null) {
                for (Aspect aspect : recipe.cost().getAspects()) aspects.remove(aspect, recipe.cost().getAmount(aspect));
                water = Math.max(0, water - 50); stack.shrink(1); counter = -250;
                ItemEntity output = new ItemEntity(level, worldPosition.getX() + .5, worldPosition.getY() + 1.1,
                        worldPosition.getZ() + .5, recipe.output());
                output.getPersistentData().putBoolean("CrucibleOutput", true);
                output.setDeltaMovement(0, .1, 0); output.setDefaultPickUpDelay(); level.addFreshEntity(output);
                level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, .5F, 1);
            } else {
                AspectList values = stack.getItem() instanceof thaumcraft.common.items.ItemWispEssence
                        ? thaumcraft.common.items.ItemWispEssence.getAspects(stack) : RecipeAspects.get(stack, level);
                if (values == null || values.size() == 0) {
                    entity.getPersistentData().putLong("CrucibleRetry", level.getGameTime() + 40);
                    entity.setDeltaMovement(.15, .35, .15); break;
                }
                aspects.add(values); stack.shrink(1); counter = -150;
                level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, .2F, 1);
            }
        }
        if (stack.isEmpty()) entity.discard(); else entity.setItem(stack);
        changed();
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Water", water); tag.putInt("Heat", heat); tag.putLong("Counter", counter); tag.putInt("PendingFlux", pollution); aspects.writeToNBT(tag); }
    @Override public void load(CompoundTag tag) { super.load(tag); water = Math.max(0, Math.min(1000, tag.getInt("Water"))); heat = Math.max(0, Math.min(200, tag.getInt("Heat"))); counter = tag.getLong("Counter"); pollution = tag.getInt("PendingFlux"); aspects.readFromNBT(tag); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}

package thaumcraft.common.crafting;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.ModItems;
import thaumcraft.common.items.tools.ItemWand;

/**
 * Port of TC4 {@code thaumcraft.common.lib.crafting.ArcaneWandRecipe} (1.7.10, full
 * source in decompiled/): two equal wand caps on the anti-diagonal (cells (0,2) and
 * (2,0)) with one rod in the centre (1,1), the other six cells empty. The output wand
 * takes the parts' cap/rod; the per-primal cost is dynamic: capCraftCost x rodCraftCost.
 *
 * <p>Documented deviations:
 * <ul>
 *   <li>CAP_* and ROD_* research is enforced by the server workbench menu via ModRecipes.isKnown.</li>
 *   <li>the wood-rod + iron-cap exclusion lives in {@code assemble} exactly where the
 *       original had it (getCraftingResult line 54) — {@code matches} still succeeds for
 *       that grid, so in the arcane workbench the plain {@code wand.json} recipe previews
 *       first (vanilla-first rule) and taking it requires no vis payment, which
 *       is the original behaviour;</li>
 *   <li>item comparison is registry-item equality (modern caps/rods carry no damage/NBT).</li>
 * </ul>
 */
public final class ArcaneWandRecipe implements Recipe<CraftingContainer> {
    /** Per-part research is checked in the server menu. */
    public static final boolean RESEARCH_GATES = true;

    private final ResourceLocation id;

    public ArcaneWandRecipe(ResourceLocation id) {
        this.id = id;
    }

    private static ItemStack cell(CraftingContainer grid, int row, int column) {
        return grid.getItem(row * 3 + column);
    }

    private static boolean layoutMatches(CraftingContainer grid) {
        return cell(grid, 0, 0).isEmpty() && cell(grid, 0, 1).isEmpty()
                && cell(grid, 1, 0).isEmpty() && cell(grid, 1, 2).isEmpty()
                && cell(grid, 2, 1).isEmpty() && cell(grid, 2, 2).isEmpty()
                && !cell(grid, 0, 2).isEmpty() && !cell(grid, 1, 1).isEmpty() && !cell(grid, 2, 0).isEmpty();
    }

    private static WandCap capOf(ItemStack stack) {
        for (WandCap cap : WandCap.caps.values()) {
            if (cap.matches(stack)) {
                return cap;
            }
        }
        return null;
    }

    private static WandRod rodOf(ItemStack stack) {
        for (WandRod rod : WandRod.rods.values()) {
            if (rod.matches(stack)) {
                return rod;
            }
        }
        return null;
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        if (!layoutMatches(grid)) {
            return false;
        }
        ItemStack cap1 = cell(grid, 0, 2);
        ItemStack cap2 = cell(grid, 2, 0);
        if (cap1.getItem() != cap2.getItem()) {
            return false;
        }
        // Original checkMatch lines 132-151 also required isResearchComplete per part.
        return capOf(cap1) != null && rodOf(cell(grid, 1, 1)) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        if (!layoutMatches(grid)) {
            return ItemStack.EMPTY;
        }
        ItemStack cap1 = cell(grid, 0, 2);
        ItemStack cap2 = cell(grid, 2, 0);
        if (cap1.getItem() != cap2.getItem()) {
            return ItemStack.EMPTY;
        }
        WandCap cap = capOf(cap1);
        WandRod rod = rodOf(cell(grid, 1, 1));
        if (cap == null || rod == null) {
            return ItemStack.EMPTY;
        }
        // Original getCraftingResult line 54: the default iron/wood pair is not craftable here.
        if ("wood".equals(rod.getTag()) && "iron".equals(cap.getTag())) {
            return ItemStack.EMPTY;
        }
        ItemStack wand = new ItemStack(ModItems.WAND.get());
        ItemWand.setCap(wand, cap);
        ItemWand.setRod(wand, rod);
        return wand;
    }

    /**
     * Port of the original getAspects(IInventory) lines 69-110: every primal costs
     * capCraftCost x rodCraftCost (no wood/iron exclusion — exactly the original).
     */
    public AspectList getAspects(CraftingContainer grid) {
        AspectList out = new AspectList();
        if (!layoutMatches(grid)) {
            return out;
        }
        ItemStack cap1 = cell(grid, 0, 2);
        ItemStack cap2 = cell(grid, 2, 0);
        if (cap1.getItem() != cap2.getItem()) {
            return out;
        }
        WandCap cap = capOf(cap1);
        WandRod rod = rodOf(cell(grid, 1, 1));
        if (cap == null || rod == null) {
            return out;
        }
        int cost = cap.getCraftCost() * rod.getCraftCost();
        for (Aspect primal : Aspect.getPrimalAspects()) {
            out.add(primal, cost);
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        // Original getRecipeOutput(): null — the result is dynamic.
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        // Not part of the original (it had no ingredients), but the recipe book / toast
        // infrastructure expects something meaningful; list every cap and rod item.
        java.util.List<net.minecraft.world.level.ItemLike> parts = new ArrayList<>();
        for (WandCap cap : WandCap.caps.values()) {
            parts.add(cap.getItem().getItem());
        }
        for (WandRod rod : WandRod.rods.values()) {
            parts.add(rod.getItem().getItem());
        }
        NonNullList<Ingredient> out = NonNullList.withSize(2, Ingredient.EMPTY);
        out.set(0, Ingredient.of(parts.toArray(new net.minecraft.world.level.ItemLike[0])));
        out.set(1, Ingredient.of(parts.toArray(new net.minecraft.world.level.ItemLike[0])));
        return out;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ARCANE_WAND.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ARCANE_WAND_SERIALIZER.get();
    }

    @Override
    public String toString() {
        return "arcane_wand:" + id;
    }

    /** Singleton serializer: the recipe carries no data. */
    public static final class Serializer implements RecipeSerializer<ArcaneWandRecipe> {
        @Override
        public ArcaneWandRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ArcaneWandRecipe(id);
        }

        @Override
        public ArcaneWandRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new ArcaneWandRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ArcaneWandRecipe recipe) {
            // The RecipeManager already wrote the id for the whole recipe.
        }
    }
}

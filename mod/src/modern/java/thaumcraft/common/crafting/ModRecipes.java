package thaumcraft.common.crafting;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;

/**
 * Recipe types and serializers of the port — the two arcane recipe flavours of TC4:
 * static shaped recipes with a vis cost ({@code ThaumcraftApi.addArcaneCraftingRecipe},
 * original interface {@code thaumcraft.api.crafting.IArcaneRecipe}) and the dynamic
 * diagonal cap/rod wand recipe ({@code ArcaneWandRecipe}).
 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Thaumcraft.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Thaumcraft.MODID);

    /** JSON {@code "type": "thaumcraft:arcane"}: shaped pattern + static per-primal cost. */
    public static final RegistryObject<RecipeType<ArcaneRecipe>> ARCANE =
            RECIPE_TYPES.register("arcane", () -> RecipeType.simple(new ResourceLocation(Thaumcraft.MODID, "arcane")));
    /** JSON {@code "type": "thaumcraft:arcane_wand"}: dynamic diagonal cap+rod wand recipe. */
    public static final RegistryObject<RecipeType<ArcaneWandRecipe>> ARCANE_WAND = RECIPE_TYPES.register(
            "arcane_wand", () -> RecipeType.simple(new ResourceLocation(Thaumcraft.MODID, "arcane_wand")));
    public static final RegistryObject<RecipeSerializer<ArcaneRecipe>> ARCANE_SERIALIZER =
            SERIALIZERS.register("arcane", ArcaneRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<ScribingRefillRecipe>> SCRIBING_REFILL =
            SERIALIZERS.register("scribing_refill", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(ScribingRefillRecipe::new));
    public static final RegistryObject<RecipeSerializer<ArcaneWandRecipe>> ARCANE_WAND_SERIALIZER =
            SERIALIZERS.register("arcane_wand", ArcaneWandRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    public static boolean isKnown(net.minecraft.world.entity.player.Player player,
                                 net.minecraft.world.item.crafting.Recipe<?> recipe, CraftingContainer grid) {
        if (recipe instanceof ArcaneRecipe arcane)
            return thaumcraft.common.research.ResearchProgression.has(player,arcane.getResearch());
        if (recipe instanceof ArcaneWandRecipe wandRecipe) {
            var wand=wandRecipe.assemble(grid,player.level().registryAccess());
            if(wand.isEmpty())return false;
            var cap=thaumcraft.common.items.tools.ItemWand.getCap(wand);
            var rod=thaumcraft.common.items.tools.ItemWand.getRod(wand);
            return (cap.getTag().equals("iron") || thaumcraft.common.research.ResearchProgression.has(player,"CAP_"+cap.getTag()))
                    && (rod.getTag().equals("wood") || thaumcraft.common.research.ResearchProgression.has(player,"ROD_"+rod.getTag()));
        }
        return true;
    }

    /**
     * Vanilla-first result — original ContainerArcaneWorkbench#onCraftMatrixChanged line 52
     * (a vanilla recipe always wins the preview over an arcane one).
     */
    public static ItemStack vanillaResult(net.minecraft.world.level.Level level, CraftingContainer grid) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                .map(recipe -> recipe.assemble(grid, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /**
     * First matching arcane recipe. Original order was the linear ThaumcraftApi list with
     * ArcaneWandRecipe registered before the gold/copper/goggles recipes (ConfigRecipes
     * lines 1317 vs 1324+); the shipped grids are disjoint, so the typed lookup order
     * below mirrors that placement.
     */
    public static Optional<RecipeWithType> arcaneMatch(net.minecraft.world.level.Level level, CraftingContainer grid) {
        Optional<ArcaneWandRecipe> wand = level.getRecipeManager().getRecipeFor(ModRecipes.ARCANE_WAND.get(), grid, level);
        if (wand.isPresent()) {
            return Optional.of(new RecipeWithType(wand.get(), true));
        }
        return level.getRecipeManager().getRecipeFor(ModRecipes.ARCANE.get(), grid, level)
                .map(recipe -> new RecipeWithType(recipe, false));
    }

    public static ItemStack arcaneResult(net.minecraft.world.level.Level level, CraftingContainer grid) {
        return arcaneMatch(level, grid).map(match -> match.recipe().assemble(grid, level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /** Port of ThaumcraftCraftingManager#findMatchingArcaneRecipeAspects lines 166-196: the
     *  aspects of the first matching arcane recipe (static cost, or dynamic for the wand
     *  recipe); an empty list when nothing matches. */
    public static AspectList arcaneAspects(net.minecraft.world.level.Level level, CraftingContainer grid) {
        return arcaneMatch(level, grid)
                .map(match -> match.wand()
                        ? ((ArcaneWandRecipe) match.recipe()).getAspects(grid)
                        : ((ArcaneRecipe) match.recipe()).getAspects())
                .orElseGet(AspectList::new);
    }

    /** Carries which flavour matched so callers can dispatch the dynamic cost accessor. */
    public record RecipeWithType(net.minecraft.world.item.crafting.Recipe<CraftingContainer> recipe, boolean wand) {}

    private ModRecipes() {}
}

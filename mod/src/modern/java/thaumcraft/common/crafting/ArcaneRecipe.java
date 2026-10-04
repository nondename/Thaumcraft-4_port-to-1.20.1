package thaumcraft.common.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Port of the static arcane recipes of TC4 ({@code ThaumcraftApi.addArcaneCraftingRecipe}
 * → original {@code thaumcraft.api.crafting.IArcaneRecipe}): a shaped grid pattern plus a
 * vis cost in whole vis applied to every primal (e.g. gold cap = ORDER/FIRE/AIR x3).
 *
 * <p>The pattern half is delegated to the vanilla {@link ShapedRecipe} parsed from the same
 * JSON (unknown fields like {@code cost}/{@code research} are ignored by the vanilla parser).
 *
 * <p>Player research is enforced by the server workbench menu, where the player is available.
 */
public final class ArcaneRecipe implements Recipe<CraftingContainer> {
    /** See class javadoc: Research is enforced by ModRecipes.isKnown in the server menu. */
    public static final boolean RESEARCH_GATES = true;

    private final ResourceLocation id;
    private final ShapedRecipe shape;
    private final AspectList cost;
    private final String research;

    public ArcaneRecipe(ResourceLocation id, ShapedRecipe shape, AspectList cost, String research) {
        this.id = id;
        this.shape = shape;
        this.cost = cost;
        this.research = research;
    }

    /** Static per-primal cost of this recipe (original getAspects()). */
    public AspectList getAspects() {
        return cost;
    }

    /** Original getResearch() — checked by the server menu. */
    public String getResearch() {
        return research;
    }
    public int width() {return shape.getWidth();}
    public int height() {return shape.getHeight();}

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return shape.matches(grid, level);
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        return shape.assemble(grid, access);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return shape.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return shape.getResultItem(access);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return shape.getIngredients();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ARCANE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ARCANE_SERIALIZER.get();
    }

    @Override
    public String toString() {
        return "arcane:" + id;
    }

    public static final class Serializer implements RecipeSerializer<ArcaneRecipe> {
        @Override
        public ArcaneRecipe fromJson(ResourceLocation id, JsonObject json) {
            // Vanilla shaped half: pattern/key/result; cost/research fields are extra.
            ShapedRecipe shape = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            String research = GsonHelper.getAsString(json, "research", "");
            AspectList cost = new AspectList();
            JsonObject costJson = GsonHelper.getAsJsonObject(json, "cost");
            for (var entry : costJson.entrySet()) {
                Aspect aspect = Aspect.aspects.get(entry.getKey());
                if (aspect != null) {
                    cost.add(aspect, entry.getValue().getAsInt());
                }
            }
            return new ArcaneRecipe(id, shape, cost, research);
        }

        @Override
        public ArcaneRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ShapedRecipe shape = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            String research = buf.readUtf();
            AspectList cost = new AspectList();
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                Aspect aspect = Aspect.aspects.get(buf.readUtf());
                int amount = buf.readVarInt();
                if (aspect != null) {
                    cost.add(aspect, amount);
                }
            }
            return new ArcaneRecipe(id, shape, cost, research);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ArcaneRecipe recipe) {
            // The RecipeManager already wrote the id for the whole recipe.
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe.shape);
            buf.writeUtf(recipe.research);
            buf.writeVarInt(recipe.cost.size());
            for (Aspect aspect : recipe.cost.getAspects()) {
                buf.writeUtf(aspect.getTag());
                buf.writeVarInt(recipe.cost.getAmount(aspect));
            }
        }
    }
}

package thaumcraft.common.alchemy;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.api.aspects.*;
import thaumcraft.common.crafting.ModRecipes;

public record CrucibleRecipe(ResourceLocation id, Ingredient catalyst, AspectList cost, ItemStack result, String research) implements Recipe<SimpleContainer> {
    public static final RegistryObject<RecipeType<CrucibleRecipe>> TYPE = ModRecipes.RECIPE_TYPES.register("crucible", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crucible")));
    public static final RegistryObject<RecipeSerializer<CrucibleRecipe>> SERIALIZER = ModRecipes.SERIALIZERS.register("crucible", Serializer::new);
    public static void init() {}
    public boolean canCraft(ItemStack stack, AspectList available) {
        if (!catalyst.test(stack)) return false;
        for (Aspect aspect : cost.getAspects()) if (available.getAmount(aspect) < cost.getAmount(aspect)) return false;
        return true;
    }
    public ItemStack output() { return result.copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public boolean matches(SimpleContainer container, Level level) { return catalyst.test(container.getItem(0)); }
    @Override public ItemStack assemble(SimpleContainer c, RegistryAccess a) { return output(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return false; }
    @Override public ItemStack getResultItem(RegistryAccess a) { return output(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE.get(); }
    public static final class Serializer implements RecipeSerializer<CrucibleRecipe> {
        @Override public CrucibleRecipe fromJson(ResourceLocation id, JsonObject json) {
            AspectList cost = new AspectList();
            json.getAsJsonObject("aspects").entrySet().forEach(entry -> {
                Aspect aspect = Aspect.getAspect(entry.getKey()); int amount = entry.getValue().getAsInt();
                if (aspect == null || amount <= 0) throw new IllegalArgumentException("Invalid crucible aspect " + entry.getKey());
                cost.add(aspect, amount);
            });
            if (cost.size() == 0) throw new IllegalArgumentException("Empty crucible cost");
            return new CrucibleRecipe(id, Ingredient.fromJson(json.get("catalyst")), cost, ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result")), net.minecraft.util.GsonHelper.getAsString(json,"research",""));
        }
        @Override public CrucibleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer); AspectList cost = new AspectList(); cost.readFromNBT(buffer.readNbt());
            return new CrucibleRecipe(id, ingredient, cost, buffer.readItem(),buffer.readUtf(128));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, CrucibleRecipe recipe) {
            recipe.catalyst.toNetwork(buffer); var tag = new net.minecraft.nbt.CompoundTag(); recipe.cost.writeToNBT(tag); buffer.writeNbt(tag); buffer.writeItem(recipe.result); buffer.writeUtf(recipe.research,128);
        }
    }
}

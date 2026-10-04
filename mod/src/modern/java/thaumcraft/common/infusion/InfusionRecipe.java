package thaumcraft.common.infusion;

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
import java.util.*;

public record InfusionRecipe(ResourceLocation id,String research,Ingredient input,List<Ingredient> components,
                             AspectList aspects,ItemStack result,int instability) implements Recipe<SimpleContainer> {
    public static final RegistryObject<RecipeType<InfusionRecipe>> TYPE=ModRecipes.RECIPE_TYPES.register("infusion",() -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath("thaumcraft","infusion")));
    public static final RegistryObject<RecipeSerializer<InfusionRecipe>> SERIALIZER=ModRecipes.SERIALIZERS.register("infusion",Serializer::new);
    public static void init() {}
    public ItemStack output() {return result.copy();}
    @Override public ResourceLocation getId() {return id;}
    @Override public RecipeType<?> getType() {return TYPE.get();}
    @Override public RecipeSerializer<?> getSerializer() {return SERIALIZER.get();}
    @Override public boolean matches(SimpleContainer inventory,Level level) {return input.test(inventory.getItem(0));}
    @Override public boolean canCraftInDimensions(int width,int height) {return false;}
    @Override public ItemStack assemble(SimpleContainer inventory,RegistryAccess access) {return output();}
    @Override public ItemStack getResultItem(RegistryAccess access) {return output();}
    public static final class Serializer implements RecipeSerializer<InfusionRecipe> {
        @Override public InfusionRecipe fromJson(ResourceLocation id,JsonObject json) {
            var list=new ArrayList<Ingredient>();json.getAsJsonArray("components").forEach(i -> list.add(Ingredient.fromJson(i)));
            var cost=new AspectList();json.getAsJsonObject("aspects").entrySet().forEach(e -> {
                Aspect a=Aspect.getAspect(e.getKey());int n=e.getValue().getAsInt();if(a==null || n<=0)throw new IllegalArgumentException("Invalid infusion aspect");cost.add(a,n);
            });
            if(list.isEmpty() || list.size()>16 || cost.size()==0)throw new IllegalArgumentException("Invalid infusion recipe");
            return new InfusionRecipe(id,json.get("research").getAsString(),Ingredient.fromJson(json.get("input")),List.copyOf(list),cost,
                    ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result")),json.get("instability").getAsInt());
        }
        @Override public InfusionRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer) {
            String research=buffer.readUtf(128);var input=Ingredient.fromNetwork(buffer);int n=buffer.readVarInt();
            if(n<1 || n>16)throw new IllegalArgumentException("Invalid infusion component count");
            var list=new ArrayList<Ingredient>();for(int i=0;i<n;i++)list.add(Ingredient.fromNetwork(buffer));
            var aspects=new AspectList();aspects.readFromNBT(buffer.readNbt());return new InfusionRecipe(id,research,input,List.copyOf(list),aspects,buffer.readItem(),buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer,InfusionRecipe recipe) {
            buffer.writeUtf(recipe.research,128);recipe.input.toNetwork(buffer);buffer.writeVarInt(recipe.components.size());recipe.components.forEach(i -> i.toNetwork(buffer));
            var tag=new net.minecraft.nbt.CompoundTag();recipe.aspects.writeToNBT(tag);buffer.writeNbt(tag);buffer.writeItem(recipe.result);buffer.writeVarInt(recipe.instability);
        }
    }
}

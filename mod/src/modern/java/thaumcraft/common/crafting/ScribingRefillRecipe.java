package thaumcraft.common.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.common.research.ModResearch;

/** Shapeless refill preserving the existing tools' custom data. */
public final class ScribingRefillRecipe extends CustomRecipe {
    public ScribingRefillRecipe(ResourceLocation id, CraftingBookCategory category) { super(id,category); }
    @Override public boolean matches(CraftingContainer grid, Level level) {
        int tools=0,ink=0;
        for(int i=0;i<grid.getContainerSize();i++) {
            var stack=grid.getItem(i);if(stack.isEmpty())continue;
            if(stack.is(ModResearch.SCRIBING_TOOLS.get()) && stack.getDamageValue()>0)tools++;
            else if(stack.is(Items.INK_SAC))ink++;
            else return false;
        }
        return tools==1 && ink==1;
    }
    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registry) {
        for(int i=0;i<grid.getContainerSize();i++)if(grid.getItem(i).is(ModResearch.SCRIBING_TOOLS.get())) {
            var result=grid.getItem(i).copy();result.setCount(1);result.setDamageValue(0);return result;
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=2; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.SCRIBING_REFILL.get(); }
}

package thaumcraft.common.config;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.*;
import thaumcraft.common.alchemy.CrucibleRecipe;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.infusion.InfusionRecipe;
import thaumcraft.common.items.tools.ItemWand;
import thaumcraft.common.research.ModResearch;

/** TC4 fallback order: crucible, arcane, infusion, crafting; never recurse through a cycle. */
public final class RecipeAspects {
    private static final Map<RecipeManager,Cache> caches=Collections.synchronizedMap(new WeakHashMap<>());
    private static final class Cache {
        final Map<Item,List<Recipe<?>>> recipes=new HashMap<>();
        final Map<Item,AspectList> results=new HashMap<>();
        Cache(Level level) {
            level.getRecipeManager().getRecipes().stream().filter(r -> priority(r)<4 && !r.isSpecial())
                .sorted(Comparator.comparingInt(RecipeAspects::priority).thenComparing(r -> r.getId().toString()))
                .forEach(r -> {var output=r.getResultItem(level.registryAccess());
                    if(!output.isEmpty())recipes.computeIfAbsent(output.getItem(),k -> new ArrayList<>()).add(r);});
        }
    }
    private static int priority(Recipe<?> r) {return r instanceof CrucibleRecipe?0:r instanceof ArcaneRecipe?1:r instanceof InfusionRecipe?2:r instanceof CraftingRecipe?3:4;}
    public static void clear() {caches.clear();}
    public static AspectList get(ItemStack stack,Level level) {
        if(stack.isEmpty())return null;
        if(stack.is(ModResearch.PHIAL.get()) && stack.hasTag()) {
            var payload=new AspectList();payload.readFromNBT(stack.getTag());
            if(payload.size()==1 && payload.visSize()==8)return payload;
        }
        if(stack.getItem() instanceof thaumcraft.common.items.ItemWispEssence) {
            var payload=new AspectList();if(stack.hasTag())payload.readFromNBT(stack.getTag());return payload;
        }
        if(stack.getItem() instanceof ItemWand) {
            int n=ItemWand.getRod(stack).getCraftCost()+ItemWand.getCap(stack).getCraftCost();
            var base=ThaumcraftApi.getObjectAspects(stack);
            return (base==null?new AspectList():base.copy()).merge(Aspect.MAGIC,n/2).merge(Aspect.TOOL,n/3);
        }
        var direct=ThaumcraftApi.getObjectAspects(stack);
        if(direct!=null || level==null)return direct;
        var cache=caches.computeIfAbsent(level.getRecipeManager(),k -> new Cache(level));
        synchronized(cache) {
            if(!cache.results.containsKey(stack.getItem()))cache.results.put(stack.getItem(),derive(stack,level,cache,new HashSet<>(),new int[]{4096}));
            var value=cache.results.get(stack.getItem());return value==null?null:value.copy();
        }
    }
    private static AspectList derive(ItemStack stack,Level level,Cache cache,Set<Item> visiting,int[] budget) {
        if(--budget[0]<0)return null;
        var direct=ThaumcraftApi.getObjectAspects(stack);if(direct!=null)return direct;
        if(visiting.size()>=16 || !visiting.add(stack.getItem()))return null;
        AspectList best=null;int bestPriority=4;
        try {
            for(var recipe:cache.recipes.getOrDefault(stack.getItem(),List.of())) {
                if(priority(recipe)>bestPriority)break;
                var ingredients=new ArrayList<Ingredient>();var cost=new AspectList();
                boolean crucible=recipe instanceof CrucibleRecipe;
                if(recipe instanceof CrucibleRecipe r) {ingredients.add(r.catalyst());cost=r.cost();}
                else if(recipe instanceof InfusionRecipe r) {ingredients.add(r.input());ingredients.addAll(r.components());cost=r.aspects();}
                else {ingredients.addAll(recipe.getIngredients());if(recipe instanceof ArcaneRecipe r)cost=r.getAspects();}
                var total=new AspectList();boolean complete=true;
                for(var ingredient:ingredients) {
                    if(ingredient.isEmpty())continue;
                    AspectList cheapest=null;
                    for(var candidate:ingredient.getItems()) {
                        var value=derive(candidate,level,cache,visiting,budget);
                        if(value!=null && candidate.hasCraftingRemainingItem()) {
                            var remainder=derive(candidate.getCraftingRemainingItem(),level,cache,visiting,budget);
                            if(remainder!=null) {value=value.copy();for(var a:remainder.getAspects())value.remove(a,Math.min(value.getAmount(a),remainder.getAmount(a)));}
                        }
                        if(value!=null && (cheapest==null || weight(value)<weight(cheapest)))cheapest=value;
                    }
                    // Legacy custom recipes skip untagged inputs; their paid vis/essentia still contributes.
                    if(cheapest==null) {if(priority(recipe)<3)continue;complete=false;break;}total.add(cheapest);
                }
                if(!complete)continue;
                int count=Math.max(1,recipe.getResultItem(level.registryAccess()).getCount());var result=new AspectList();
                for(var a:total.getAspects()) {int n=(int)(total.getAmount(a)*(crucible?1:.75)/count);if(n>0)result.add(a,n);}
                for(var a:cost.getAspects()) {int n=(int)(Math.sqrt(cost.getAmount(a))/count);if(n>0)result.add(a,n);}
                if(result.size()>0 && (best==null || weight(result)<weight(best))) {best=result;bestPriority=priority(recipe);}
            }
            return best;
        } finally {visiting.remove(stack.getItem());}
    }
    private static int weight(AspectList list) {return list.visSize();}
    private RecipeAspects() {}
}

package thaumcraft.common.config;

import java.util.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** TC4 crafting fallback: 75% of ingredient aspects, divided by output count. */
public final class RecipeAspects {
    private static final Map<RecipeManager, Cache> caches = Collections.synchronizedMap(new WeakHashMap<>());
    private static final class Cache {
        final Map<Item, List<CraftingRecipe>> recipes = new HashMap<>();
        final Map<Item, AspectList> results = new HashMap<>();
        Cache(Level level) {
            level.getRecipeManager().getRecipes().stream().filter(r -> r instanceof CraftingRecipe && !r.isSpecial())
                    .sorted(Comparator.comparing(r -> r.getId().toString())).forEach(r -> {
                        var output = r.getResultItem(level.registryAccess());
                        if (!output.isEmpty()) recipes.computeIfAbsent(output.getItem(), k -> new ArrayList<>()).add((CraftingRecipe) r);
                    });
        }
    }
    public static void clear() { caches.clear(); }
    public static AspectList get(ItemStack stack, Level level) {
        AspectList direct = ThaumcraftApi.getObjectAspects(stack);
        if (direct != null || level == null || stack.isEmpty()) return direct;
        Cache cache = caches.computeIfAbsent(level.getRecipeManager(), k -> new Cache(level));
        synchronized (cache) {
            if (!cache.results.containsKey(stack.getItem()))
                cache.results.put(stack.getItem(), derive(stack, level, cache, new HashSet<>(), new int[]{4096}));
            AspectList result = cache.results.get(stack.getItem());
            return result == null ? null : result.copy();
        }
    }
    private static AspectList derive(ItemStack stack, Level level, Cache cache, Set<Item> visiting, int[] budget) {
        if (--budget[0] < 0) return null;
        var direct = ThaumcraftApi.getObjectAspects(stack);
        if (direct != null) return direct;
        Item item = stack.getItem();
        if (visiting.size() >= 16 || !visiting.add(item)) return null;
        AspectList best = null;
        try {
            for (var recipe : cache.recipes.getOrDefault(item, List.of())) {
                AspectList total = new AspectList();
                boolean complete = true;
                for (var ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) continue;
                    AspectList cheapest = null;
                    for (var candidate : ingredient.getItems()) {
                        // Container remainders need separate accounting; do not count a bucket as consumed metal.
                        if (candidate.hasCraftingRemainingItem()) continue;
                        AspectList value = derive(candidate, level, cache, visiting, budget);
                        if (value != null && (cheapest == null || weight(value) < weight(cheapest))) cheapest = value;
                    }
                    if (cheapest == null) { complete = false; break; }
                    for (Aspect aspect : cheapest.getAspects()) total.add(aspect, cheapest.getAmount(aspect));
                }
                if (!complete) continue;
                int count = recipe.getResultItem(level.registryAccess()).getCount();
                AspectList result = new AspectList();
                for (Aspect aspect : total.getAspects()) {
                    int amount = (int)(total.getAmount(aspect) * 0.75F / Math.max(1, count));
                    if (amount > 0) result.add(aspect, amount);
                }
                if (result.size() > 0 && (best == null || weight(result) < weight(best))) best = result;
            }
            return best;
        } finally { visiting.remove(item); }
    }
    private static int weight(AspectList list) {
        int total = 0;
        for (Aspect aspect : list.getAspects()) total += list.getAmount(aspect);
        return total;
    }
    private RecipeAspects() {}
}

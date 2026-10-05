package thaumcraft.common.research;

import net.minecraft.world.item.crafting.Recipe;
import thaumcraft.common.alchemy.CrucibleRecipe;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.infusion.InfusionRecipe;
import java.util.Set;

/** Chapter associations for actual recipes, independent of client rendering. */
public final class BookRecipeCatalog {
    private BookRecipeCatalog() {}
    public static Set<String> chapters(Recipe<?> recipe) {
        String research = recipe instanceof ArcaneRecipe r ? r.getResearch()
                : recipe instanceof CrucibleRecipe r ? r.research()
                : recipe instanceof InfusionRecipe r ? r.research() : "";
        if (!research.isEmpty()) return Set.of(research);
        if (!recipe.getId().getNamespace().equals("thaumcraft")) return Set.of();
        return switch (recipe.getId().getPath()) {
            case "wand_cap_iron", "wand", "thaumometer", "arcane_wand" -> Set.of("BASICTHAUMATURGY");
            case "scribing_tools", "scribing_refill" -> Set.of("RESEARCH");
            case "phial" -> Set.of("RESEARCH", "DISTILESSENTIA");
            case "table" -> Set.of("TABLE");
            case "amber_from_ore", "cinnabar_to_quicksilver" -> Set.of("ORE");
            case "greatwood_planks", "silverwood_planks" -> Set.of("PLANTS");
            case "salis_mundus" -> Set.of("CRUCIBLE");
            case "thaumium_block", "thaumium_ingot_from_nuggets", "thaumium_ingots_from_block", "thaumium_nuggets_from_ingot" -> Set.of("THAUMIUM");
            case "void_ingot_from_nuggets", "void_nuggets_from_ingot" -> Set.of("VOIDMETAL");
            default -> Set.of();
        };
    }
    public static boolean belongsTo(Recipe<?> recipe, String chapter) {
        return chapters(recipe).stream().anyMatch(chapter::equalsIgnoreCase);
    }
}

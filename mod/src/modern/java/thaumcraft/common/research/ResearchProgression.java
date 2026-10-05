package thaumcraft.common.research;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.*;
import thaumcraft.common.lib.capabilities.*;
import thaumcraft.common.lib.network.ModNetwork;
import java.util.*;

/** Server datapack definitions extracted from the saved research registration. */
public final class ResearchProgression {
    private static final Map<net.minecraft.server.packs.resources.ResourceManager,Map<String,Definition>> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    public record Definition(List<String> parents, AspectList aspects, boolean auto, boolean secondary, List<String> hiddenParents, List<String> siblings) {}
    public static Map<String,Definition> definitions(Level level) {
        if (level.getServer()==null) return Map.of();
        var manager=level.getServer().getResourceManager();
        return CACHE.computeIfAbsent(manager,m -> load(level));
    }
    private static Map<String,Definition> load(Level level) {
        var result=new LinkedHashMap<String,Definition>();
        try (var reader=level.getServer().getResourceManager().getResource(
                ResourceLocation.fromNamespaceAndPath("thaumcraft","research/progression.json")).orElseThrow().openAsReader()) {
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry -> {
                var value=entry.getValue().getAsJsonObject(); var cost=new AspectList();
                value.getAsJsonObject("aspects").entrySet().forEach(a -> {
                    Aspect aspect=Aspect.getAspect(a.getKey()); int n=a.getValue().getAsInt();
                    if(aspect==null || n<=0) throw new IllegalArgumentException("Invalid research aspect");
                    cost.add(aspect,n);
                });
                var parents=new ArrayList<String>(); value.getAsJsonArray("parents").forEach(p -> parents.add(p.getAsString()));
                var hidden=new ArrayList<String>(); var siblings=new ArrayList<String>();
                if(value.has("hiddenParents"))value.getAsJsonArray("hiddenParents").forEach(p -> hidden.add(p.getAsString()));
                if(value.has("siblings"))value.getAsJsonArray("siblings").forEach(p -> siblings.add(p.getAsString()));
                result.put(entry.getKey(),new Definition(parents,cost,value.get("auto").getAsBoolean(),value.get("secondary").getAsBoolean(),hidden,siblings));
            });
        } catch(Exception e) { throw new IllegalStateException("Cannot load research progression",e); }
        return result;
    }
    public static IThaumometerKnowledge knowledge(Player player) {
        return player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY).orElse(null);
    }
    public static boolean has(Player player,String key) {
        if(key.isEmpty()) return true;
        var data=knowledge(player); if(data==null) return false;
        if(data.hasResearch(key)) return true;
        var def=definitions(player.level()).get(key.toUpperCase(Locale.ROOT));
        return def!=null && def.auto();
    }
    public static boolean parents(Player player,Definition def) { return def.parents().stream().allMatch(p -> has(player,p)) && def.hiddenParents().stream().allMatch(p -> has(player,p)); }
    public static void request(Player player,String key) {
        if(player.level().isClientSide || !player.getInventory().contains(new ItemStack(thaumcraft.common.items.ModItems.THAUMONOMICON.get()))) return;
        key=key.toUpperCase(Locale.ROOT); var def=definitions(player.level()).get(key); var data=knowledge(player);
        if(def==null || def.auto() || data==null || data.hasResearch(key) || !parents(player,def)) return;
        for(Aspect a:def.aspects().getAspects()) if(!data.hasDiscoveredAspect(a)) {player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.progress.need_aspects"),false);return;}
        if(def.secondary()) {
            if(data.spendAspects(def.aspects())) {finish(player,key); ModNetwork.syncThaumometerKnowledge(player);}
            else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.progress.need_points"),false);
            return;
        }
        if(def.aspects().size()==0)return; // Event discoveries are never bought with blank paper.
        var inventory=player.getInventory(); int paper=-1,ink=-1;
        for(int i=0;i<inventory.getContainerSize();i++) {
            var stack=inventory.getItem(i);
            if(stack.is(ModResearch.NOTES.get()) && key.equals(stack.getOrCreateTag().getString("Research"))) return;
            if(stack.is(Items.PAPER)) paper=i;
            if(stack.is(ModResearch.SCRIBING_TOOLS.get()) && stack.getDamageValue()<stack.getMaxDamage()) ink=i;
        }
        if(paper<0 || ink<0) {player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.progress.need_materials"),false);return;}
        ItemStack notes=ResearchNotes.create(key,def);
        inventory.getItem(paper).shrink(1); ScribingTools.consumeInk(inventory.getItem(ink));
        if(!inventory.add(notes)) player.drop(notes,false);
        inventory.setChanged();
    }
    public static void finish(Player player,String key) {
        var data=knowledge(player);if(data!=null && data.grantResearch(key)) {
            int amount=warp.getOrDefault(key.toUpperCase(Locale.ROOT),0);
            if(amount>0)data.addWarp((amount+1)/2,amount/2,0);
            var def=definitions(player.level()).get(key.toUpperCase(Locale.ROOT));
            if(def!=null)for(String sibling:def.siblings()) {
                var related=definitions(player.level()).get(sibling);
                if(related!=null && parents(player,related))finish(player,sibling);
            }
        }
    }
    private static final Map<String,Integer> warp=Map.ofEntries(
        Map.entry("RESEARCHER2",1),Map.entry("CRIMSON",3),Map.entry("SINSTONE",2),Map.entry("INFERNALFURNACE",2),
        Map.entry("JARBRAIN",3),Map.entry("MASKANGRYGHOST",1),Map.entry("MASKSIPPINGFIEND",1),Map.entry("LIQUIDDEATH",3),
        Map.entry("BOTTLETAINT",2),Map.entry("GOLEMFLESH",3),Map.entry("OCULUS",6),Map.entry("PRIMNODE",1),
        Map.entry("CAP_VOID",1),Map.entry("FOCUSPRIMAL",2),Map.entry("ROD_PRIMAL_STAFF",3),Map.entry("FOCUSHELLBAT",2),
        Map.entry("ROD_BONE",1),Map.entry("ROD_BONE_STAFF",1));
    private ResearchProgression() {}

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
    public static final class Reload {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void reload(net.minecraftforge.event.AddReloadListenerEvent event) {
            event.addListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<Void>() {
                @Override protected Void prepare(net.minecraft.server.packs.resources.ResourceManager manager,net.minecraft.util.profiling.ProfilerFiller profiler) {return null;}
                @Override protected void apply(Void data,net.minecraft.server.packs.resources.ResourceManager manager,net.minecraft.util.profiling.ProfilerFiller profiler) {CACHE.clear();}
            });
        }
    }
}

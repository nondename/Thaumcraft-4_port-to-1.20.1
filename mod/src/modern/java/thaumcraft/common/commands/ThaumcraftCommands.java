package thaumcraft.common.commands;

import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.exceptions.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.research.ResearchProgression;
import thaumcraft.common.lib.network.ModNetwork;
import java.util.*;
import static net.minecraft.commands.Commands.*;

/** Original TC4 administration syntax, adapted to Brigadier and per-player capabilities. */
public final class ThaumcraftCommands {
    private static final DynamicCommandExceptionType UNKNOWN_RESEARCH=new DynamicCommandExceptionType(k -> Component.translatable("tc.command.unknown_research",k));
    private static final DynamicCommandExceptionType UNKNOWN_ASPECT=new DynamicCommandExceptionType(k -> Component.translatable("tc.command.unknown_aspect",k));
    private static final SimpleCommandExceptionType NO_KNOWLEDGE=new SimpleCommandExceptionType(Component.translatable("tc.command.no_knowledge"));
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root=dispatcher.register(literal("thaumcraft").requires(s -> s.hasPermission(2)).executes(c -> help(c.getSource()))
                .then(literal("help").executes(c -> help(c.getSource())))
                .then(literal("research")
                        .then(literal("list").executes(c -> list(c.getSource())))
                        .then(argument("player",EntityArgument.player()).then(argument("key",StringArgumentType.word())
                                .suggests((c,b) -> SharedSuggestionProvider.suggest(researchKeys(c.getSource()),b))
                                .executes(c -> research(c.getSource(),EntityArgument.getPlayer(c,"player"),StringArgumentType.getString(c,"key"))))))
                .then(literal("aspect").then(argument("player",EntityArgument.player()).then(argument("aspect",StringArgumentType.word())
                        .suggests((c,b) -> SharedSuggestionProvider.suggest(aspectKeys(),b))
                        .then(argument("amount",IntegerArgumentType.integer(1)).executes(c -> aspect(c.getSource(),EntityArgument.getPlayer(c,"player"),
                                StringArgumentType.getString(c,"aspect"),IntegerArgumentType.getInteger(c,"amount")))))))
                .then(literal("warp").then(argument("player",EntityArgument.player())
                        .then(warpBranch("set",0,Integer.MAX_VALUE)).then(warpBranch("add",-100,100)))));
        dispatcher.register(literal("tc").requires(s -> s.hasPermission(2)).executes(c -> help(c.getSource())).redirect(root));
        dispatcher.register(literal("thaum").requires(s -> s.hasPermission(2)).executes(c -> help(c.getSource())).redirect(root));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> warpBranch(String operation,int min,int max) {
        return literal(operation).then(argument("amount",IntegerArgumentType.integer(min,max))
                .executes(c -> warp(c.getSource(),EntityArgument.getPlayer(c,"player"),operation,IntegerArgumentType.getInteger(c,"amount"),""))
                .then(argument("kind",StringArgumentType.word()).suggests((c,b) -> SharedSuggestionProvider.suggest(List.of("PERM","TEMP"),b))
                        .executes(c -> warp(c.getSource(),EntityArgument.getPlayer(c,"player"),operation,IntegerArgumentType.getInteger(c,"amount"),StringArgumentType.getString(c,"kind")))));
    }
    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("tc.command.help"),false);
        for(String syntax:List.of("/thaumcraft research list","/thaumcraft research <player> <all|reset|research>",
                "/thaumcraft aspect <player> <aspect|all> <amount>","/thaumcraft warp <player> <add|set> <amount> [PERM|TEMP]"))
            source.sendSuccess(() -> Component.literal(syntax),false);
        return 1;
    }
    private static List<String> researchKeys(CommandSourceStack source) {
        var keys=new ArrayList<>(ResearchProgression.definitions(source.getLevel()).keySet());Collections.sort(keys);keys.add("all");keys.add("reset");return keys;
    }
    private static List<String> aspectKeys() {var keys=new ArrayList<>(Aspect.aspects.keySet());keys.add("all");return keys;}
    private static int list(CommandSourceStack source) {
        var keys=ResearchProgression.definitions(source.getLevel()).keySet().stream().sorted().toList();
        for(String key:keys) source.sendSuccess(() -> Component.literal(key),false);
        return keys.size();
    }
    private static int research(CommandSourceStack source,ServerPlayer player,String key) throws CommandSyntaxException {
        changeResearch(player,key);sync(source,player,"research "+key);return 1;
    }
    /** Grant prerequisites including hidden links/siblings; reset affects only research. */
    public static void changeResearch(Player player,String requested) throws CommandSyntaxException {
        var data=ResearchProgression.knowledge(player);if(data==null)throw NO_KNOWLEDGE.create();
        var definitions=ResearchProgression.definitions(player.level());
        if(requested.equalsIgnoreCase("reset")) {
            data.clearResearch();definitions.forEach((key,def) -> {if(def.auto())ResearchProgression.finish(player,key);});return;
        }
        if(requested.equalsIgnoreCase("all")) {definitions.keySet().forEach(key -> ResearchProgression.finish(player,key));return;}
        String key=canonical(definitions.keySet(),requested);if(key==null)throw UNKNOWN_RESEARCH.create(requested);
        Map<String,List<String>> links=new HashMap<>();
        try(var reader=player.level().getServer().getResourceManager().getResource(new ResourceLocation("thaumcraft","commands/research_links.json")).orElseThrow().openAsReader()) {
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(e -> {
                var values=new ArrayList<String>();e.getValue().getAsJsonArray().forEach(v -> values.add(v.getAsString()));links.put(e.getKey(),values);
            });
        } catch(Exception ex) {throw new SimpleCommandExceptionType(Component.translatable("tc.command.links_error")).create();}
        grant(player,key,definitions,links,new HashSet<>());
    }
    private static void grant(Player player,String key,Map<String,ResearchProgression.Definition> definitions,Map<String,List<String>> links,Set<String> visited) {
        key=canonical(definitions.keySet(),key);if(key==null || !visited.add(key))return;
        ResearchProgression.finish(player,key);
        for(String parent:definitions.get(key).parents())grant(player,parent,definitions,links,visited);
        for(String related:links.getOrDefault(key,List.of()))grant(player,related,definitions,links,visited);
    }
    private static String canonical(Set<String> keys,String key) {return keys.stream().filter(k -> k.equalsIgnoreCase(key)).findFirst().orElse(null);}
    private static int aspect(CommandSourceStack source,ServerPlayer player,String key,int amount) throws CommandSyntaxException {
        giveAspects(player,key,amount);sync(source,player,"aspect "+key+" +"+amount);return 1;
    }
    public static void giveAspects(Player player,String key,int amount) throws CommandSyntaxException {
        if(amount<1)throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().create(amount,1);
        var data=ResearchProgression.knowledge(player);if(data==null)throw NO_KNOWLEDGE.create();
        if(key.equalsIgnoreCase("all")) {for(var a:Aspect.aspects.values())data.addAspectPool(a,amount);return;}
        var aspect=Aspect.getAspect(key.toLowerCase(Locale.ROOT));if(aspect==null)throw UNKNOWN_ASPECT.create(key);
        data.addAspectPool(aspect,amount);
    }
    private static int warp(CommandSourceStack source,ServerPlayer player,String operation,int amount,String kind) throws CommandSyntaxException {
        changeWarp(player,operation,amount,kind);sync(source,player,"warp "+operation+" "+amount+" "+kind);return 1;
    }
    public static void changeWarp(Player player,String operation,int amount,String kind) throws CommandSyntaxException {
        if(!operation.equals("set") && !operation.equals("add"))throw new SimpleCommandExceptionType(Component.translatable("tc.command.invalid_operation")).create();
        if(operation.equals("set") && amount<0)throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().create(amount,0);
        if(operation.equals("add") && amount< -100)throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().create(amount,-100);
        if(operation.equals("add") && amount>100)throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().create(amount,100);
        var data=ResearchProgression.knowledge(player);if(data==null)throw NO_KNOWLEDGE.create();
        int index=kind.equalsIgnoreCase("PERM")?0:kind.equalsIgnoreCase("TEMP")?2:1;
        int[] values={data.getPermanentWarp(),data.getNormalWarp(),data.getTemporaryWarp()};
        values[index]=operation.equals("set")?Math.max(0,amount):(int)Math.max(0,Math.min(Integer.MAX_VALUE,(long)values[index]+amount));
        data.setWarp(values[0],values[1],values[2]);
    }
    private static void sync(CommandSourceStack source,ServerPlayer player,String action) {
        ModNetwork.syncThaumometerKnowledge(player);
        var message=Component.translatable("tc.command.done",player.getGameProfile().getName(),action);
        source.sendSuccess(() -> message,true);if(source.getEntity()!=player)player.sendSystemMessage(message);
    }
    private ThaumcraftCommands() {}
}

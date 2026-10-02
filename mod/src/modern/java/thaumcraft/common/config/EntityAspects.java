package thaumcraft.common.config;

import java.util.Map;
import java.util.HashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Vanilla entity values from the maintained TC4 reference. */
public final class EntityAspects {
    private static final Map<String, AspectList> tags = new HashMap<>();
    static {
        tags.put("zombie", new AspectList().add(Aspect.UNDEAD, 2).add(Aspect.MAN, 1).add(Aspect.EARTH, 1));
        tags.put("giant", new AspectList().add(Aspect.UNDEAD, 4).add(Aspect.MAN, 3).add(Aspect.EARTH, 3));
        tags.put("skeleton", new AspectList().add(Aspect.UNDEAD, 3).add(Aspect.MAN, 1).add(Aspect.EARTH, 1));
        tags.put("wither_skeleton", new AspectList().add(Aspect.UNDEAD, 4).add(Aspect.MAN, 1).add(Aspect.FIRE, 2));
        tags.put("creeper", new AspectList().add(Aspect.PLANT, 2).add(Aspect.FIRE, 2));
        tags.put("horse", new AspectList().add(Aspect.BEAST, 4).add(Aspect.EARTH, 1).add(Aspect.AIR, 1));
        tags.put("pig", new AspectList().add(Aspect.BEAST, 2).add(Aspect.EARTH, 2));
        tags.put("experience_orb", new AspectList().add(Aspect.MIND, 5));
        tags.put("sheep", new AspectList().add(Aspect.BEAST, 2).add(Aspect.EARTH, 2));
        tags.put("cow", new AspectList().add(Aspect.BEAST, 3).add(Aspect.EARTH, 3));
        tags.put("mooshroom", new AspectList().add(Aspect.BEAST, 3).add(Aspect.PLANT, 1).add(Aspect.EARTH, 2));
        tags.put("snow_golem", new AspectList().add(Aspect.COLD, 3).add(Aspect.WATER, 1));
        tags.put("ocelot", new AspectList().add(Aspect.BEAST, 3).add(Aspect.ENTROPY, 3));
        tags.put("chicken", new AspectList().add(Aspect.BEAST, 2).add(Aspect.FLIGHT, 2).add(Aspect.AIR, 1));
        tags.put("squid", new AspectList().add(Aspect.BEAST, 2).add(Aspect.WATER, 2));
        tags.put("wolf", new AspectList().add(Aspect.BEAST, 3).add(Aspect.EARTH, 3));
        tags.put("bat", new AspectList().add(Aspect.BEAST, 1).add(Aspect.FLIGHT, 1).add(Aspect.AIR, 1));
        tags.put("boat", new AspectList().add(Aspect.MECHANISM, 2).add(Aspect.WATER, 2));
        tags.put("spider", new AspectList().add(Aspect.BEAST, 3).add(Aspect.ENTROPY, 2));
        tags.put("slime", new AspectList().add(Aspect.SLIME, 2).add(Aspect.WATER, 2));
        tags.put("ghast", new AspectList().add(Aspect.UNDEAD, 3).add(Aspect.FIRE, 2));
        tags.put("zombified_piglin", new AspectList().add(Aspect.UNDEAD, 4).add(Aspect.FIRE, 2));
        tags.put("enderman", new AspectList().add(Aspect.ELDRITCH, 4).add(Aspect.TRAVEL, 2).add(Aspect.AIR, 2));
        tags.put("cave_spider", new AspectList().add(Aspect.BEAST, 2).add(Aspect.POISON, 2).add(Aspect.EARTH, 1));
        tags.put("silverfish", new AspectList().add(Aspect.BEAST, 1).add(Aspect.EARTH, 1));
        tags.put("blaze", new AspectList().add(Aspect.ELDRITCH, 4).add(Aspect.FIRE, 1));
        tags.put("magma_cube", new AspectList().add(Aspect.SLIME, 3).add(Aspect.FIRE, 2));
        tags.put("ender_dragon", new AspectList().add(Aspect.ELDRITCH, 20).add(Aspect.BEAST, 20).add(Aspect.ENTROPY, 20));
        tags.put("wither", new AspectList().add(Aspect.UNDEAD, 20).add(Aspect.ENTROPY, 20).add(Aspect.FIRE, 15));
        tags.put("witch", new AspectList().add(Aspect.MAN, 3).add(Aspect.MAGIC, 2).add(Aspect.FIRE, 1));
        tags.put("villager", new AspectList().add(Aspect.MAN, 3).add(Aspect.AIR, 2));
        tags.put("iron_golem", new AspectList().add(Aspect.METAL, 4).add(Aspect.EARTH, 3));
        tags.put("minecart", new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.AIR, 2));
        tags.put("chest_minecart", new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.AIR, 1).add(Aspect.VOID, 1));
        tags.put("furnace_minecart", new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.AIR, 1).add(Aspect.FIRE, 1));
        tags.put("tnt_minecart", new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.AIR, 1).add(Aspect.FIRE, 1));
        tags.put("hopper_minecart", new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.AIR, 1).add(Aspect.EXCHANGE, 1));
        tags.put("spawner_minecart", new AspectList().add(Aspect.MECHANISM, 3).add(Aspect.AIR, 1).add(Aspect.MAGIC, 1));
        tags.put("end_crystal", new AspectList().add(Aspect.ELDRITCH, 3).add(Aspect.MAGIC, 3).add(Aspect.HEAL, 3));
        tags.put("item_frame", new AspectList().add(Aspect.SENSES, 3).add(Aspect.CLOTH, 1));
        tags.put("painting", new AspectList().add(Aspect.SENSES, 5).add(Aspect.CLOTH, 3));
    }
    public static String variant(Entity entity) {
        return entity instanceof Creeper creeper && creeper.isPowered() ? "/charged" : "";
    }
    public static AspectList get(Entity entity) {
        if (entity instanceof Creeper creeper && creeper.isPowered())
            return new AspectList().add(Aspect.PLANT, 3).add(Aspect.FIRE, 3).add(Aspect.ENERGY, 3);
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        AspectList result = id.getNamespace().equals("minecraft") ? tags.get(id.getPath()) : null;
        return result == null ? null : result.copy();
    }
    private EntityAspects() {}
}

package thaumcraft.common.alchemy;

import net.minecraft.world.entity.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class ModAlchemyEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"thaumcraft");
    public static final RegistryObject<EntityType<AlumentumProjectile>> ALUMENTUM=ENTITIES.register("alumentum",() ->
            EntityType.Builder.<AlumentumProjectile>of(AlumentumProjectile::new,MobCategory.MISC).sized(.25F,.25F)
                    .clientTrackingRange(4).updateInterval(10).build("thaumcraft:alumentum"));
    public static void register(IEventBus bus) {ENTITIES.register(bus);}
    private ModAlchemyEntities() {}
}

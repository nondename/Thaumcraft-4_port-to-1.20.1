package thaumcraft.common.sounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;

/** Sound events used by the Thaumcraft scanner. */
public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Thaumcraft.MODID);

    public static final RegistryObject<SoundEvent> CAMERA_TICKS = register("cameraticks");
    public static final RegistryObject<SoundEvent> CAMERA_CLACK = register("cameraclack");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}

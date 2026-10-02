package thaumcraft;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Forge 1.20.1 bootstrap entry point for the Thaumcraft 4 port.
 *
 * The legacy 1.12.2 implementation remains under src/main and is used as the
 * behavioural reference while systems are migrated into src/modern.
 */
@Mod(Thaumcraft.MODID)
public final class Thaumcraft {
    public static final String MODID = "thaumcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Thaumcraft() {
        LOGGER.info("Thaumcraft 4 port bootstrap loaded on Minecraft 1.20.1");
    }
}

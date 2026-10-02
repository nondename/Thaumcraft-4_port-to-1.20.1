package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;

/** Applies server-authoritative scan data to the local player's capability. */
public final class ThaumometerKnowledgeClientHandler {
    private ThaumometerKnowledgeClientHandler() {
    }

    public static void apply(CompoundTag data) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.getCapability(ThaumometerKnowledgeProvider.CAPABILITY)
                .ifPresent(knowledge -> knowledge.deserializeNBT(data));
    }
}

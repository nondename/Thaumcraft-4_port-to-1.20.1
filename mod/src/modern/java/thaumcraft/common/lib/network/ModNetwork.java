package thaumcraft.common.lib.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.capabilities.IThaumometerKnowledge;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import thaumcraft.common.lib.network.message.SyncThaumometerKnowledgeMessage;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                0,
                SyncThaumometerKnowledgeMessage.class,
                SyncThaumometerKnowledgeMessage::encode,
                SyncThaumometerKnowledgeMessage::decode,
                SyncThaumometerKnowledgeMessage::handle
        );
    }

    public static void syncThaumometerKnowledge(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        IThaumometerKnowledge knowledge = serverPlayer
                .getCapability(ThaumometerKnowledgeProvider.CAPABILITY)
                .orElse(null);
        if (knowledge != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncThaumometerKnowledgeMessage(knowledge.serializeNBT()));
        }
    }
}

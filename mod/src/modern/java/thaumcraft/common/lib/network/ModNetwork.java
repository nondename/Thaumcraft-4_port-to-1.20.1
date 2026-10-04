package thaumcraft.common.lib.network;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.capabilities.IThaumometerKnowledge;
import thaumcraft.common.lib.capabilities.ThaumometerKnowledgeProvider;
import thaumcraft.common.lib.network.message.AuraNodeZapMessage;
import thaumcraft.common.lib.network.message.SyncThaumometerKnowledgeMessage;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(2, thaumcraft.common.lib.network.message.ResearchRequestMessage.class,
                thaumcraft.common.lib.network.message.ResearchRequestMessage::encode,
                thaumcraft.common.lib.network.message.ResearchRequestMessage::decode,
                thaumcraft.common.lib.network.message.ResearchRequestMessage::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(
                0,
                SyncThaumometerKnowledgeMessage.class,
                SyncThaumometerKnowledgeMessage::encode,
                SyncThaumometerKnowledgeMessage::decode,
                SyncThaumometerKnowledgeMessage::handle
        );
        CHANNEL.registerMessage(
                1,
                AuraNodeZapMessage.class,
                AuraNodeZapMessage::encode,
                AuraNodeZapMessage::decode,
                AuraNodeZapMessage::handle
        );
    }

    public static void requestResearch(String key) {
        CHANNEL.sendToServer(new thaumcraft.common.lib.network.message.ResearchRequestMessage(key));
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

    /** Mirrors TC4 PacketFXBlockZap delivery: only nearby clients need the visual arc. */
    public static void sendAuraNodeZap(ServerLevel level, BlockPos from, BlockPos to) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        to.getX() + 0.5D,
                        to.getY() + 0.5D,
                        to.getZ() + 0.5D,
                        32.0D,
                        level.dimension())),
                new AuraNodeZapMessage(from, to));
    }
}

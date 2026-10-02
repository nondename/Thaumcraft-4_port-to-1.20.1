package thaumcraft.common.lib.network.message;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.client.ThaumometerKnowledgeClientHandler;

import java.util.function.Supplier;

public final class SyncThaumometerKnowledgeMessage {
    private final CompoundTag data;

    public SyncThaumometerKnowledgeMessage(CompoundTag data) {
        this.data = data.copy();
    }

    public static void encode(SyncThaumometerKnowledgeMessage message, FriendlyByteBuf buffer) {
        buffer.writeNbt(message.data);
    }

    public static SyncThaumometerKnowledgeMessage decode(FriendlyByteBuf buffer) {
        CompoundTag data = buffer.readNbt();
        return new SyncThaumometerKnowledgeMessage(data == null ? new CompoundTag() : data);
    }

    public static void handle(SyncThaumometerKnowledgeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ThaumometerKnowledgeClientHandler.apply(message.data)));
        context.setPacketHandled(true);
    }
}

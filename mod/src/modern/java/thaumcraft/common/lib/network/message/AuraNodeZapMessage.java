package thaumcraft.common.lib.network.message;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.client.NodeZapClientHandler;

import java.util.function.Supplier;

/** S2C visual packet for TC4-style aura-node discharge. */
public final class AuraNodeZapMessage {
    private final BlockPos from;
    private final BlockPos to;

    public AuraNodeZapMessage(BlockPos from, BlockPos to) {
        this.from = from.immutable();
        this.to = to.immutable();
    }

    public static void encode(AuraNodeZapMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.from);
        buffer.writeBlockPos(message.to);
    }

    public static AuraNodeZapMessage decode(FriendlyByteBuf buffer) {
        return new AuraNodeZapMessage(buffer.readBlockPos(), buffer.readBlockPos());
    }

    public static void handle(AuraNodeZapMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> NodeZapClientHandler.spawn(message.from, message.to)));
        context.setPacketHandled(true);
    }
}

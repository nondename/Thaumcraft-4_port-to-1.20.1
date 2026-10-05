package thaumcraft.common.lib.network.message;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.common.research.ResearchMenu;

/** Board action IDs exceed the vanilla container-button packet's signed byte. */
public record ResearchActionMessage(int containerId,int action) {
    public static void encode(ResearchActionMessage message,FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.containerId);buffer.writeVarInt(message.action);
    }
    public static ResearchActionMessage decode(FriendlyByteBuf buffer) {
        return new ResearchActionMessage(buffer.readVarInt(),buffer.readVarInt());
    }
    public boolean apply(Player player) {
        if(player==null || !(player.containerMenu instanceof ResearchMenu menu) || menu.containerId!=containerId || !menu.stillValid(player))return false;
        boolean changed=menu.clickMenuButton(player,action);
        if(changed)menu.broadcastChanges();
        return changed;
    }
    public static void handle(ResearchActionMessage message,Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();context.enqueueWork(() -> message.apply(context.getSender()));context.setPacketHandled(true);
    }
}

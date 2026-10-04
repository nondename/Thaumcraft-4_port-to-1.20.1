package thaumcraft.common.lib.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.common.research.ResearchProgression;
import java.util.function.Supplier;

public record ResearchRequestMessage(String key) {
    public static void encode(ResearchRequestMessage message,FriendlyByteBuf buffer) {buffer.writeUtf(message.key,128);}
    public static ResearchRequestMessage decode(FriendlyByteBuf buffer) {return new ResearchRequestMessage(buffer.readUtf(128));}
    public static void handle(ResearchRequestMessage message,Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get(); context.enqueueWork(() -> {
            var player=context.getSender(); if(player!=null) ResearchProgression.request(player,message.key);
        }); context.setPacketHandled(true);
    }
}

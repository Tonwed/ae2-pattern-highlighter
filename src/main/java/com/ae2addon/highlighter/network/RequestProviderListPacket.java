package com.ae2addon.highlighter.network;

import appeng.api.stacks.AEKey;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.ae2addon.highlighter.server.PatternProviderLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * 客户端发送到服务器的请求包 - 请求查找样板接口位置列表（不触发高亮）
 */
public class RequestProviderListPacket {
    @Nullable
    private final AEKey what;
    private final int cpuSerial;
    
    public RequestProviderListPacket(@Nullable AEKey what, int cpuSerial) {
        this.what = what;
        this.cpuSerial = cpuSerial;
    }
    
    public static void encode(RequestProviderListPacket msg, FriendlyByteBuf buf) {
        AEKey.writeOptionalKey(buf, msg.what);
        buf.writeVarInt(msg.cpuSerial);
    }
    
    public static RequestProviderListPacket decode(FriendlyByteBuf buf) {
        AEKey what = AEKey.readOptionalKey(buf);
        int cpuSerial = buf.readVarInt();
        return new RequestProviderListPacket(what, cpuSerial);
    }
    
    public static void handle(RequestProviderListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            Ae2PatternHighlighter.LOGGER.info(
                "Received provider list request for item: {} on CPU {}", 
                msg.what != null ? msg.what.getDisplayName().getString() : "null", 
                msg.cpuSerial);
            
            if (msg.what == null) {
                Ae2PatternHighlighter.LOGGER.warn("Received null AEKey in provider list request");
                return;
            }
            
            // 查找对应的样板接口位置
            List<BlockPos> positions = PatternProviderLocator.findProviderPositions(
                    player, msg.what, msg.cpuSerial);
            
            // 发送provider列表（不触发高亮）
            ModNetworkHandler.sendToPlayer(
                    new ProviderListPacket(positions, msg.what), player);
            
            if (positions.isEmpty()) {
                Ae2PatternHighlighter.LOGGER.info("No providers found for item: {}", 
                    msg.what.getDisplayName().getString());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

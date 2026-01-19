package com.ae2addon.highlighter.network;

import appeng.api.stacks.AEKey;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.ae2addon.highlighter.server.PatternProviderLocator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 客户端发送到服务器的请求包 - 请求查找处理特定物品的样板接口位置
 */
public class RequestHighlightPacket {
    @Nullable
    private final AEKey what;
    private final int cpuSerial;
    
    public RequestHighlightPacket(@Nullable AEKey what, int cpuSerial) {
        this.what = what;
        this.cpuSerial = cpuSerial;
    }
    
    public static void encode(RequestHighlightPacket msg, FriendlyByteBuf buf) {
        AEKey.writeOptionalKey(buf, msg.what);
        buf.writeVarInt(msg.cpuSerial);
    }
    
    public static RequestHighlightPacket decode(FriendlyByteBuf buf) {
        AEKey what = AEKey.readOptionalKey(buf);
        int cpuSerial = buf.readVarInt();
        return new RequestHighlightPacket(what, cpuSerial);
    }
    
    public static void handle(RequestHighlightPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            Ae2PatternHighlighter.LOGGER.info(
                "Received highlight request for item: {} on CPU {}", 
                msg.what != null ? msg.what.getDisplayName().getString() : "null", 
                msg.cpuSerial);
            
            if (msg.what == null) {
                Ae2PatternHighlighter.LOGGER.warn("Received null AEKey in highlight request");
                return;
            }
            
            // 查找对应的样板接口位置
            var positions = PatternProviderLocator.findProviderPositions(
                    player, msg.what, msg.cpuSerial);
            
            // 始终发送响应（包括空列表，让客户端知道搜索完成）
            ModNetworkHandler.sendToPlayer(
                    HighlightPositionsPacket.fromPositions(positions), player);
            
            if (positions.isEmpty()) {
                Ae2PatternHighlighter.LOGGER.info("No providers found for item: {}", 
                    msg.what.getDisplayName().getString());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

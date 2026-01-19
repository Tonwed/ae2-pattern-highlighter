package com.ae2addon.highlighter.network;

import com.ae2addon.highlighter.Ae2PatternHighlighter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 处理客户端与服务器之间的网络通信
 */
public class ModNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Ae2PatternHighlighter.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    private static int nextId() {
        return packetId++;
    }
    
    public static void register() {
        // 客户端 -> 服务器: 请求高亮某个物品对应的样板接口
        CHANNEL.messageBuilder(RequestHighlightPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestHighlightPacket::encode)
                .decoder(RequestHighlightPacket::decode)
                .consumerMainThread(RequestHighlightPacket::handle)
                .add();
        
        // 服务器 -> 客户端: 返回需要高亮的位置列表
        CHANNEL.messageBuilder(HighlightPositionsPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HighlightPositionsPacket::encode)
                .decoder(HighlightPositionsPacket::decode)
                .consumerMainThread(HighlightPositionsPacket::handle)
                .add();
        
        // 客户端 -> 服务器: 请求provider列表（不触发高亮）
        CHANNEL.messageBuilder(RequestProviderListPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestProviderListPacket::encode)
                .decoder(RequestProviderListPacket::decode)
                .consumerMainThread(RequestProviderListPacket::handle)
                .add();
        
        // 服务器 -> 客户端: 返回provider列表并在聊天显示
        CHANNEL.messageBuilder(ProviderListPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProviderListPacket::encode)
                .decoder(ProviderListPacket::decode)
                .consumerMainThread(ProviderListPacket::handle)
                .add();
    }
    
    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }
    
    public static void sendToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}

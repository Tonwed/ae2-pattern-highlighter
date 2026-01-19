package com.ae2addon.highlighter.client;

import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 拦截客户端聊天消息来处理自定义命令
 */
public class ChatCommandInterceptor {
    
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String message = event.getMessage();
        
        // 检查是否是我们的命令
        if (ClientCommandHandler.handleChatCommand(message)) {
            // 取消发送到服务器
            event.setCanceled(true);
        }
    }
}

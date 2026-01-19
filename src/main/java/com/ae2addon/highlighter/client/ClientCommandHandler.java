package com.ae2addon.highlighter.client;

import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.ae2addon.highlighter.client.camera.FreeCameraController;
import com.ae2addon.highlighter.network.HighlightInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理客户端命令 - 通过解析聊天消息实现
 */
public class ClientCommandHandler {
    
    /**
     * 检查并处理客户端命令
     * 返回true表示命令已处理，应取消发送到服务器
     */
    public static boolean handleChatCommand(String message) {
        // 检查是否是我们的命令
        if (message.startsWith("/ae2highlight ")) {
            try {
                int index = Integer.parseInt(message.substring("/ae2highlight ".length()).trim());
                handleHighlight(index);
                return true;
            } catch (NumberFormatException e) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c无效的索引格式"));
                return true;
            }
        }
        
        if (message.startsWith("/ae2camera ")) {
            try {
                int index = Integer.parseInt(message.substring("/ae2camera ".length()).trim());
                handleCamera(index);
                return true;
            } catch (NumberFormatException e) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c无效的索引格式"));
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 处理高亮命令
     */
    private static void handleHighlight(int index) {
        BlockPos pos = ClientProviderCache.getPosition(index);
        if (pos == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§c无效的索引: " + index));
            }
            return;
        }
        
        Ae2PatternHighlighter.LOGGER.info("Highlighting provider at: {}", pos);
        
        // 高亮单个provider
        List<HighlightInfo> highlights = new ArrayList<>();
        highlights.add(new HighlightInfo(pos, false));
        HighlightRenderer.addHighlights(highlights);
        
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("§a已高亮位置: " + pos.toShortString()));
    }
    
    /**
     * 处理相机命令
     */
    private static void handleCamera(int index) {
        BlockPos pos = ClientProviderCache.getPosition(index);
        if (pos == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§c无效的索引: " + index));
            }
            return;
        }
        
        Ae2PatternHighlighter.LOGGER.info("Starting camera view at: {}", pos);
        
        // 启动相机观察模式
        FreeCameraController.getInstance().startObservation(pos);
        
        Minecraft.getInstance().player.sendSystemMessage(Component.literal("§a进入相机观察模式"));
    }
}

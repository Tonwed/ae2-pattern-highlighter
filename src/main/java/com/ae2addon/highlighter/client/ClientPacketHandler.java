package com.ae2addon.highlighter.client;

import appeng.api.stacks.AEKey;
import com.ae2addon.highlighter.client.camera.FreeCameraController;
import com.ae2addon.highlighter.network.HighlightInfo;
import com.ae2addon.highlighter.network.HighlightPositionsPacket;
import com.ae2addon.highlighter.network.ProviderListPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端侧的包处理逻辑，隔离客户端类以防止服务端崩溃
 */
public class ClientPacketHandler {
    
    public static void handleProviderList(ProviderListPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // 检查是否有待处理的相机动作
        if (PendingCameraAction.hasPending()) {
            // 触发相机观察
            if (!msg.getPositions().isEmpty()) {
                FreeCameraController.getInstance()
                    .startObservation(msg.getPositions());
            } else {
                mc.player.sendSystemMessage(Component.literal("§c未找到样板接口"));
            }
            PendingCameraAction.clear();
        } else {
            // 在聊天中显示provider列表，包含可点击的按钮
            ChatMessageBuilder.sendProviderListMessage(mc.player, msg.getPositions(), msg.getItemWhat());
        }
    }
    
    public static void handleHighlightPositions(HighlightPositionsPacket msg) {
        HighlightRenderer.addHighlights(msg.getHighlights());
    }
}

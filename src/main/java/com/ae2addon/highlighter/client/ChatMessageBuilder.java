package com.ae2addon.highlighter.client;

import appeng.api.stacks.AEKey;
import com.ae2addon.highlighter.client.camera.FreeCameraController;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 构建聊天消息组件，包含可点击的高亮和相机按钮
 */
public class ChatMessageBuilder {
    
    /**
     * 发送provider列表到聊天，包含可点击的功能按钮
     */
    public static void sendProviderListMessage(Player player, List<BlockPos> positions, AEKey itemWhat) {
        if (positions.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                "§6[AE2 Highlighter] §c未找到相关样板接口，可能使用了缺失合成功能或该物品是合成的中间产物"));
            return;
        }
        
        String itemName = itemWhat.getDisplayName().getString();
        player.sendSystemMessage(Component.literal(
            String.format("§6[AE2 Highlighter] §f找到 §e%s §f的 §b%d §f个样板接口:", 
                itemName, positions.size())));
        
        // 存储positions到客户端缓存，供命令使用
        ClientProviderCache.setPositions(positions);
        
        int index = 1;
        for (BlockPos pos : positions) {
            // 创建坐标文本 [序号] (x, y, z)
            String coordinateText = String.format("§e[%d] §b(%d, %d, %d)", 
                index, pos.getX(), pos.getY(), pos.getZ());
            
            String tpCommand = String.format("/tp @s %d %d %d", pos.getX(), pos.getY(), pos.getZ());
            
            // 基础坐标组件（可点击填充TP命令）
            Component coordComponent = Component.literal(coordinateText)
                    .setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpCommand))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                    Component.literal("§7点击填充传送命令\n" + tpCommand))));
            
            // [高亮接口] 按钮
            Component highlightButton = Component.literal(" §a[高亮接口]")
                    .setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                                    "/ae2highlight " + index))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("§7点击高亮此接口"))));
            
            // [到底在哪？] 按钮
            Component cameraButton = Component.literal(" §d[到底在哪？]")
                    .setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                                    "/ae2camera " + index))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("§7点击进入相机观察模式\n§7自动旋转查看接口位置\n§7鼠标滚轮调整距离"))));
            
            // 组合消息
            Component fullMessage = Component.empty()
                    .append(coordComponent)
                    .append(highlightButton)
                    .append(cameraButton);
            
            player.sendSystemMessage(fullMessage);
            index++;
        }
    }
}

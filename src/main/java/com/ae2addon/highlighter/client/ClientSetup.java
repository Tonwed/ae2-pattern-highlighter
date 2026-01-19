package com.ae2addon.highlighter.client;

import com.ae2addon.highlighter.Ae2PatternHighlighter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 客户端初始化
 */
public class ClientSetup {
    
    public static void init() {
        // 注册世界渲染事件
        MinecraftForge.EVENT_BUS.register(HighlightRenderer.class);
        // 注册屏幕点击事件
        MinecraftForge.EVENT_BUS.register(ScreenClickHandler.class);
        
        Ae2PatternHighlighter.LOGGER.info("Client setup complete - registered event handlers");
    }
}

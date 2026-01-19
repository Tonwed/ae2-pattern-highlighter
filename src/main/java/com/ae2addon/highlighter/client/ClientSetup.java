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
        // 注册聊天命令拦截器
        MinecraftForge.EVENT_BUS.register(ChatCommandInterceptor.class);
        // 注册相机渲染器
        MinecraftForge.EVENT_BUS.register(com.ae2addon.highlighter.client.camera.FreeCameraRenderer.class);
        // 注册相机输入处理
        MinecraftForge.EVENT_BUS.register(com.ae2addon.highlighter.client.camera.CameraInputHandler.class);
        
        Ae2PatternHighlighter.LOGGER.info("Client setup complete - registered event handlers and camera system");
    }
}

package com.ae2addon.highlighter.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 处理相机模式下的输入事件
 */
public class CameraInputHandler {
    
    /**
     * 处理鼠标滚轮 - 调整相机距离
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        FreeCameraController controller = FreeCameraController.getInstance();
        if (!controller.isActive()) return;
        
        double delta = event.getScrollDelta();
        controller.adjustDistance(delta);
        
        // 取消事件，防止其他处理
        event.setCanceled(true);
    }
    
    /**
     * 每tick更新相机状态
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        FreeCameraController controller = FreeCameraController.getInstance();
        if (controller.isActive()) {
            controller.update();
        }
    }
}

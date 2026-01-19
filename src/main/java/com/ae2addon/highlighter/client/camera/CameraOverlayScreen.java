package com.ae2addon.highlighter.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 相机观察模式的覆盖层界面 - 显示退出按钮
 */
public class CameraOverlayScreen extends Screen {
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    
    public CameraOverlayScreen() {
        super(Component.literal("Camera Observation"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // 在屏幕底部中央添加退出按钮
        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int buttonY = this.height - 40;
        
        this.addRenderableWidget(Button.builder(
                Component.literal("§c退出观察 [ESC]"),
                button -> exitCamera())
                .bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不渲染背景（完全透明）
        
        // 渲染按钮
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 可选：在顶部显示提示文本
        String hint = "§e鼠标滚轮 §7调整距离 | §e自动旋转 §7观察接口";
        int textWidth = this.font.width(hint);
        guiGraphics.drawString(this.font, hint, (this.width - textWidth) / 2, 10, 0xFFFFFF, true);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC键退出
        if (keyCode == 256) { // GLFW.GLFW_KEY_ESCAPE
            exitCamera();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }
    
    /**
     * 退出相机模式
     */
    private void exitCamera() {
        FreeCameraController.getInstance().exitObservation();
        Minecraft.getInstance().setScreen(null);
    }
}

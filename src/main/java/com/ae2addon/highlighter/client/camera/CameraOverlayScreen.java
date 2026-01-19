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
    
    private boolean originalHideGui;
    
    @Override
    protected void init() {
        super.init();
        
        // 保存并设置隐藏GUI
        this.originalHideGui = this.minecraft.options.hideGui;
        this.minecraft.options.hideGui = true;
        
        // 在屏幕底部中央添加退出按钮
        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int buttonY = this.height - 40;
        
        this.addRenderableWidget(Button.builder(
                Component.literal("§c退出观察 [ESC]"),
                button -> exitCamera())
                .bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        FreeCameraController controller = FreeCameraController.getInstance();
        if (controller.hasMultipleTargets()) {
            // 上一个
            this.addRenderableWidget(Button.builder(Component.literal("<"), 
                btn -> controller.prevTarget())
                .bounds(buttonX - 25, buttonY, 20, 20)
                .build());

            // 下一个
            this.addRenderableWidget(Button.builder(Component.literal(">"), 
                btn -> controller.nextTarget())
                .bounds(buttonX + BUTTON_WIDTH + 5, buttonY, 20, 20)
                .build());
        }
    }
    
    @Override
    public void removed() {
        super.removed();
        // 恢复GUI显示状态
        if (this.minecraft != null) {
            this.minecraft.options.hideGui = this.originalHideGui;
        }
    }
    
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // 不渲染背景，保持透明
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        FreeCameraController.getInstance().adjustDistance(delta);
        return true;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 不渲染背景（完全透明）但需调用super渲染buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 在顶部显示提示文本
        String hint = "§e鼠标滚轮 §7调整距离 | §e自动旋转 §7观察接口";
        FreeCameraController controller = FreeCameraController.getInstance();
        if (controller.hasMultipleTargets()) {
             hint += String.format(" | §b目标 §f%d/%d", controller.getCurrentTargetIndex() + 1, controller.getTargetCount());
        }
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

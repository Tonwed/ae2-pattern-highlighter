package com.ae2addon.highlighter.client;

import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.ae2addon.highlighter.client.camera.FreeCameraController;
import com.ae2addon.highlighter.network.ModNetworkHandler;
import com.ae2addon.highlighter.network.RequestProviderListPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 眼镜按钮 - 触发相机观察功能
 */
public class CameraButton extends Button {
    private final CraftingStatusEntry entry;
    
    public CameraButton(int x, int y, int width, int height, CraftingStatusEntry entry) {
        super(x, y, width, height, Component.literal("👁"), 
              button -> {}, 
              DEFAULT_NARRATION);
        this.entry = entry;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (this.visible && this.active) {
            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() && 
                            mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            
            // 眼镜图标背景色（青色）
            int color = hovered ? 0xFF00FFFF : 0xFF00AAAA;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
            
            // 绘制边框
            int borderColor = hovered ? 0xFFFFFFFF : 0xFF888888;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
            guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
            guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);
            
            // 绘制眼镜符号 (简化版)
            int centerX = this.getX() + this.width / 2;
            int centerY = this.getY() + this.height / 2;
            int eyeColor = 0xFF000000;
            
            // 左眼
            guiGraphics.fill(centerX - 3, centerY - 1, centerX - 1, centerY + 1, eyeColor);
            // 右眼
            guiGraphics.fill(centerX + 1, centerY - 1, centerX + 3, centerY + 1, eyeColor);
        }
    }
    
    /**
     * 公开的点击方法，供外部调用
     */
    public void onClick() {
        doClick();
    }
    
    private void doClick() {
        if (entry != null && entry.getWhat() != null) {
            Ae2PatternHighlighter.LOGGER.info("CameraButton clicked! Item: {}", 
                entry.getWhat().getDisplayName().getString());
            
            // 发送provider列表请求，带回调触发相机
            ModNetworkHandler.sendToServer(new RequestProviderListPacket(entry.getWhat(), 0));
            
            // 标记需要触发相机（由ProviderListPacket处理）
            PendingCameraAction.set(entry.getWhat());
            
            // 关闭当前GUI
            Minecraft.getInstance().setScreen(null);
        } else {
            Ae2PatternHighlighter.LOGGER.warn("CameraButton clicked but entry or what is null");
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean isOver = mouseX >= (double)this.getX() && mouseY >= (double)this.getY() && 
                                mouseX < (double)(this.getX() + this.width) && mouseY < (double)(this.getY() + this.height);
                if (isOver) {
                    this.onClick();
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        doClick();
    }
}

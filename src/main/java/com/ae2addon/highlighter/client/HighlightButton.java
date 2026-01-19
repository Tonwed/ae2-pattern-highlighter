package com.ae2addon.highlighter.client;

import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.ae2addon.highlighter.network.ModNetworkHandler;
import com.ae2addon.highlighter.network.RequestHighlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 真正的Minecraft按钮Widget，可以正确处理点击事件
 */
public class HighlightButton extends Button {
    
    private CraftingStatusEntry entry;
    
    public HighlightButton(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("?"), HighlightButton::onClick, DEFAULT_NARRATION);
    }
    
    public void setEntry(CraftingStatusEntry entry) {
        this.entry = entry;
    }
    
    public CraftingStatusEntry getEntry() {
        return entry;
    }
    
    private static void onClick(Button button) {
        if (button instanceof HighlightButton highlightButton) {
            highlightButton.doClick();
        }
    }
    
    /**
     * 手动触发按钮点击
     */
    public void onClick(double mouseX, double mouseY) {
        doClick();
    }
    
    private void doClick() {
        if (entry != null && entry.getWhat() != null) {
            Ae2PatternHighlighter.LOGGER.info("HighlightButton clicked! Item: {}", 
                entry.getWhat().getDisplayName().getString());
            
            // 发送网络请求，包含物品信息
            ModNetworkHandler.sendToServer(new RequestHighlightPacket(entry.getWhat(), 0));
            
            // 关闭当前GUI
            Minecraft.getInstance().setScreen(null);
        } else {
            Ae2PatternHighlighter.LOGGER.warn("HighlightButton clicked but entry or what is null");
        }
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || entry == null) return;
        
        boolean hovered = this.isHovered();
        
        // 使用高z-index
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 300);
        
        // 背景
        int bgColor = hovered ? 0xFFFF8800 : 0xCC444444;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        
        // 边框
        int borderColor = hovered ? 0xFFFFCC00 : 0xFF888888;
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
        
        // 问号文字
        int textColor = hovered ? 0xFF000000 : 0xFFFFFFFF;
        guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                "?",
                getX() + width / 2,
                getY() + (height - 8) / 2,
                textColor);
        
        pose.popPose();
    }
    
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.visible || entry == null) return false;
        return mouseX >= this.getX() && mouseX < this.getX() + this.width 
            && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }
    
    /**
     * 重写mouseClicked以确保我们的按钮能处理点击
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.visible && this.active && entry != null) {
            if (this.isMouseOver(mouseX, mouseY)) {
                Ae2PatternHighlighter.LOGGER.info("HighlightButton.mouseClicked! Entry serial: {}", entry.getSerial());
                this.doClick();
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }
        }
        return false;
    }
}

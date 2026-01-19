package com.ae2addon.highlighter.client;

import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.ae2addon.highlighter.network.ModNetworkHandler;
import com.ae2addon.highlighter.network.RequestHighlightPacket;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * 处理高亮按钮的渲染和点击
 */
public class HighlightButtonHandler {
    
    // 表格配置
    private static final int TABLE_X = 9;
    private static final int TABLE_Y = 19;
    private static final int CELL_WIDTH = 67;
    private static final int CELL_HEIGHT = 22;
    private static final int CELL_BORDER = 1;
    private static final int COLS = 3;
    private static final int ROWS = 6;
    
    // 按钮配置
    private static final int BUTTON_SIZE = 9;
    private static final int BUTTON_OFFSET_X = 2;
    private static final int BUTTON_OFFSET_Y = 2;
    
    // 缓存当前悬停的条目
    private static CraftingStatusEntry hoveredEntry = null;
    private static int hoveredButtonX = 0;
    private static int hoveredButtonY = 0;
    
    /**
     * 渲染所有高亮按钮
     */
    public static void renderButtons(GuiGraphics guiGraphics, List<CraftingStatusEntry> entries, 
                                     int scrollOffset, int mouseX, int mouseY) {
        hoveredEntry = null;
        
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = (row + scrollOffset) * COLS + col;
                if (index >= entries.size()) break;
                
                CraftingStatusEntry entry = entries.get(index);
                
                // 只为有活跃合成的条目显示按钮
                if (entry.getActiveAmount() <= 0) continue;
                
                int cellX = TABLE_X + col * (CELL_WIDTH + CELL_BORDER);
                int cellY = TABLE_Y + row * (CELL_HEIGHT + CELL_BORDER);
                
                int buttonX = cellX + BUTTON_OFFSET_X;
                int buttonY = cellY + BUTTON_OFFSET_Y;
                
                // 检查鼠标是否悬停
                boolean hovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE
                        && mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
                
                if (hovered) {
                    hoveredEntry = entry;
                    hoveredButtonX = buttonX;
                    hoveredButtonY = buttonY;
                }
                
                // 绘制按钮
                drawButton(guiGraphics, buttonX, buttonY, hovered);
            }
        }
    }
    
    /**
     * 绘制单个按钮
     */
    private static void drawButton(GuiGraphics guiGraphics, int x, int y, boolean hovered) {
        // 背景
        int bgColor = hovered ? 0xFFFF8800 : 0xAA333333;
        guiGraphics.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, bgColor);
        
        // 边框
        int borderColor = hovered ? 0xFFFFCC00 : 0xFF666666;
        guiGraphics.fill(x, y, x + BUTTON_SIZE, y + 1, borderColor);
        guiGraphics.fill(x, y + BUTTON_SIZE - 1, x + BUTTON_SIZE, y + BUTTON_SIZE, borderColor);
        guiGraphics.fill(x, y, x + 1, y + BUTTON_SIZE, borderColor);
        guiGraphics.fill(x + BUTTON_SIZE - 1, y, x + BUTTON_SIZE, y + BUTTON_SIZE, borderColor);
        
        // 问号图标
        int textColor = hovered ? 0xFF000000 : 0xFFFFFFFF;
        guiGraphics.drawString(
                net.minecraft.client.Minecraft.getInstance().font,
                "?",
                x + 2,
                y + 1,
                textColor,
                false);
    }
    
    /**
     * 处理鼠标点击
     */
    public static boolean handleClick(int mouseX, int mouseY, int button) {
        if (button != 0 || hoveredEntry == null || hoveredEntry.getWhat() == null) {
            return false;
        }
        
        // 验证点击位置
        if (mouseX >= hoveredButtonX && mouseX < hoveredButtonX + BUTTON_SIZE
                && mouseY >= hoveredButtonY && mouseY < hoveredButtonY + BUTTON_SIZE) {
            
            Ae2PatternHighlighter.LOGGER.info("Highlight button clicked! Item: {}", 
                hoveredEntry.getWhat().getDisplayName().getString());
            
            // 发送网络请求
            ModNetworkHandler.sendToServer(new RequestHighlightPacket(hoveredEntry.getWhat(), 0));
            
            return true;
        }
        
        return false;
    }
}

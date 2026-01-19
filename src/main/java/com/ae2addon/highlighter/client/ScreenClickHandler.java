package com.ae2addon.highlighter.client;

import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.menu.me.crafting.CraftingStatusEntry;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ScreenClickHandler {
    
    // 表格配置
    private static final int TABLE_X = 9;
    private static final int TABLE_Y = 19;
    private static final int CELL_WIDTH = 67;
    private static final int CELL_HEIGHT = 22;
    private static final int CELL_BORDER = 1;
    private static final int COLS = 3;
    private static final int ROWS = 6;
    private static final int BUTTON_SIZE = 10;
    private static final int BUTTON_OFFSET_X = 1;
    private static final int BUTTON_OFFSET_Y = 1;
    private static final int BUTTON_SPACING = 2; // 按钮之间的间距
    
    // 缓存每个Screen的按钮
    private static final WeakHashMap<Screen, List<HighlightButton>> highlightButtons = new WeakHashMap<>();
    private static final WeakHashMap<Screen, List<CameraButton>> cameraButtons = new WeakHashMap<>();
    private static final WeakHashMap<Screen, Integer> lastScrollOffset = new WeakHashMap<>();
    private static final WeakHashMap<Screen, Integer> lastEntryCount = new WeakHashMap<>();
    
    /**
     * 供Mixin调用的按钮点击处理方法
     * @return true 如果点击被我们的按钮处理
     */
    public static boolean handleButtonClick(Screen screen, double mouseX, double mouseY) {
        // 先检查问号按钮
        List<HighlightButton> hButtons = highlightButtons.get(screen);
        if (hButtons != null) {
            for (int i = 0; i < hButtons.size(); i++) {
                HighlightButton btn = hButtons.get(i);
                if (btn.visible && btn.isMouseOver(mouseX, mouseY)) {
                    Ae2PatternHighlighter.LOGGER.info("HighlightButton {} clicked", i);
                    btn.onClick(mouseX, mouseY);
                    return true;
                }
            }
        }
        
        // 再检查眼镜按钮
        List<CameraButton> cButtons = cameraButtons.get(screen);
        if (cButtons != null) {
            for (int i = 0; i < cButtons.size(); i++) {
                CameraButton btn = cButtons.get(i);
                if (btn.visible && btn.isMouseOver(mouseX, mouseY)) {
                    Ae2PatternHighlighter.LOGGER.info("CameraButton {} clicked", i);
                    btn.onClick(mouseX, mouseY);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查鼠标是否在我们的按钮上
     */
    public static boolean isOverButton(Screen screen, double mouseX, double mouseY) {
        List<HighlightButton> hButtons = highlightButtons.get(screen);
        if (hButtons != null) {
            for (HighlightButton btn : hButtons) {
                if (btn.visible && btn.isMouseOver(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        
        List<CameraButton> cButtons = cameraButtons.get(screen);
        if (cButtons != null) {
            for (CameraButton btn : cButtons) {
                if (btn.visible && btn.isMouseOver(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 在Screen初始化后添加按钮
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        
        if (screen instanceof CraftingCPUScreen<?> cpuScreen) {
            createButtons(cpuScreen, event);
            Ae2PatternHighlighter.LOGGER.info("Added highlight buttons to CraftingCPUScreen");
        }
    }
    
    /**
     * 在渲染前更新按钮状态
     */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        
        if (screen instanceof CraftingCPUScreen<?> cpuScreen) {
            updateButtons(cpuScreen);
        }
    }
    
    /**
     * 使用InputEvent.MouseButton.Pre - 这是GLFW层面的事件，比ScreenEvent更早触发
     * 在任何模组或Screen处理之前拦截
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRawMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        
        // 只处理左键按下
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        
        if (!(screen instanceof CraftingCPUScreen<?> cpuScreen)) {
            return;
        }
        
        // 获取鼠标位置（需要从GLFW坐标转换）
        double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
        
        Ae2PatternHighlighter.LOGGER.info("InputEvent.MouseButton.Pre at ({}, {})", mouseX, mouseY);
        
        // 强制更新按钮位置
        updateButtons(cpuScreen);
        
        // 检查并处理按钮点击
        if (handleButtonClick(screen, mouseX, mouseY)) {
            event.setCanceled(true);
            Ae2PatternHighlighter.LOGGER.info("InputEvent: button click handled, event canceled!");
        }
    }
    
    /**
     * ScreenEvent作为备用（已经被InputEvent处理的不会到达这里）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        
        if (screen instanceof CraftingCPUScreen<?> cpuScreen && event.getButton() == 0) {
            double mouseX = event.getMouseX();
            double mouseY = event.getMouseY();
            
            updateButtons(cpuScreen);
            
            if (handleButtonClick(screen, mouseX, mouseY)) {
                event.setCanceled(true);
                Ae2PatternHighlighter.LOGGER.info("ScreenEvent: button click handled!");
            }
        }
    }
    
    private static void createButtons(CraftingCPUScreen<?> screen, ScreenEvent.Init.Post event) {
        List<HighlightButton> hButtons = new ArrayList<>();
        List<CameraButton> cButtons = new ArrayList<>();
        
        int guiLeft = getGuiLeft(screen);
        int guiTop = getGuiTop(screen);
        
        // 为每个可见单元格位置创建两个按钮（问号和眼镜）
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int cellX = TABLE_X + col * (CELL_WIDTH + CELL_BORDER);
                int cellY = TABLE_Y + row * (CELL_HEIGHT + CELL_BORDER);
                
                // 问号按钮在左上
                int highlightButtonX = guiLeft + cellX + BUTTON_OFFSET_X;
                int highlightButtonY = guiTop + cellY + BUTTON_OFFSET_Y;
                HighlightButton highlightButton = new HighlightButton(highlightButtonX, highlightButtonY, BUTTON_SIZE, BUTTON_SIZE);
                highlightButton.visible = false;
                hButtons.add(highlightButton);
                event.addListener(highlightButton);
                
                // 眼镜按钮在问号按钮下方
                int cameraButtonX = guiLeft + cellX + BUTTON_OFFSET_X;
                int cameraButtonY = guiTop + cellY + BUTTON_OFFSET_Y + BUTTON_SIZE + BUTTON_SPACING;
                CameraButton cameraButton = new CameraButton(cameraButtonX, cameraButtonY, BUTTON_SIZE, BUTTON_SIZE, null);
                cameraButton.visible = false;
                cButtons.add(cameraButton);
                event.addListener(cameraButton);
            }
        }
        
        highlightButtons.put(screen, hButtons);
        cameraButtons.put(screen, cButtons);
        lastScrollOffset.put(screen, -1);
        lastEntryCount.put(screen, -1);
    }
    
    private static void updateButtons(CraftingCPUScreen<?> screen) {
        List<HighlightButton> hButtons = highlightButtons.get(screen);
        List<CameraButton> cButtons = cameraButtons.get(screen);
        if (hButtons == null || cButtons == null) return;
        
        List<CraftingStatusEntry> entries = getEntries(screen);
        int scrollOffset = getScroll(screen);
        
        // 获取当前GUI位置（每帧都可能变化）
        int guiLeft = getGuiLeft(screen);
        int guiTop = getGuiTop(screen);
        
        Integer lastScroll = lastScrollOffset.get(screen);
        Integer lastCount = lastEntryCount.get(screen);
        
        lastScrollOffset.put(screen, scrollOffset);
        lastEntryCount.put(screen, entries.size());
        
        int visibleCount = 0;
        int buttonIndex = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int entryIndex = (row + scrollOffset) * COLS + col;
                
                if (buttonIndex < hButtons.size() && buttonIndex < cButtons.size()) {
                    // 更新问号按钮
                    HighlightButton hButton = hButtons.get(buttonIndex);
                    CameraButton cButton = cButtons.get(buttonIndex);
                    
                    // 更新位置
                    int cellX = TABLE_X + col * (CELL_WIDTH + CELL_BORDER);
                    int cellY = TABLE_Y + row * (CELL_HEIGHT + CELL_BORDER);
                    
                    hButton.setX(guiLeft + cellX + BUTTON_OFFSET_X);
                    hButton.setY(guiTop + cellY + BUTTON_OFFSET_Y);
                    
                    cButton.setX(guiLeft + cellX + BUTTON_OFFSET_X);
                    cButton.setY(guiTop + cellY + BUTTON_OFFSET_Y + BUTTON_SIZE + BUTTON_SPACING);
                    
                    if (entryIndex < entries.size()) {
                        CraftingStatusEntry entry = entries.get(entryIndex);
                        hButton.setEntry(entry);
                        hButton.visible = true;
                        hButton.active = true;
                        
                        // 通过反射设置CameraButton的entry
                        try {
                            java.lang.reflect.Field entryField = CameraButton.class.getDeclaredField("entry");
                            entryField.setAccessible(true);
                            entryField.set(cButton, entry);
                        } catch (Exception e) {
                            // Ignore
                        }
                        cButton.visible = true;
                        cButton.active = true;
                        
                        visibleCount++;
                    } else {
                        hButton.setEntry(null);
                        hButton.visible = false;
                        cButton.visible = false;
                    }
                }
                buttonIndex++;
            }
        }
        
        // 调试日志
        if (visibleCount > 0 && (lastCount == null || lastCount != entries.size())) {
            Ae2PatternHighlighter.LOGGER.info("Updated buttons: {} entries, {} visible, scroll={}, guiPos=({},{})", 
                entries.size(), visibleCount, scrollOffset, guiLeft, guiTop);
        }
    }
    
    private static int getGuiLeft(Screen screen) {
        if (screen instanceof appeng.client.gui.AEBaseScreen<?> aeScreen) {
            return aeScreen.getGuiLeft();
        }
        return 0;
    }
    
    private static int getGuiTop(Screen screen) {
        if (screen instanceof appeng.client.gui.AEBaseScreen<?> aeScreen) {
            return aeScreen.getGuiTop();
        }
        return 0;
    }
    
    private static List<CraftingStatusEntry> getEntries(CraftingCPUScreen<?> screen) {
        try {
            var statusField = findField(screen.getClass(), "status");
            if (statusField != null) {
                statusField.setAccessible(true);
                Object status = statusField.get(screen);
                if (status != null) {
                    var getEntriesMethod = status.getClass().getMethod("getEntries");
                    @SuppressWarnings("unchecked")
                    List<CraftingStatusEntry> entries = (List<CraftingStatusEntry>) getEntriesMethod.invoke(status);
                    return entries != null ? entries : Collections.emptyList();
                }
            }
        } catch (Exception e) {}
        return Collections.emptyList();
    }
    
    private static int getScroll(CraftingCPUScreen<?> screen) {
        try {
            var scrollbarField = findField(screen.getClass(), "scrollbar");
            if (scrollbarField != null) {
                scrollbarField.setAccessible(true);
                Object scrollbar = scrollbarField.get(screen);
                if (scrollbar != null) {
                    var getCurrentScrollMethod = scrollbar.getClass().getMethod("getCurrentScroll");
                    return (int) getCurrentScrollMethod.invoke(scrollbar);
                }
            }
        } catch (Exception e) {}
        return 0;
    }
    
    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}

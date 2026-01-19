package com.ae2addon.highlighter.mixin;

import appeng.client.Point;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.ae2addon.highlighter.client.HighlightButton;
import com.ae2addon.highlighter.client.ScreenClickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin注入WidgetContainer.onMouseDown,在AE2表格处理之前拦截点击
 */
@Mixin(value = WidgetContainer.class, remap = false)
public abstract class WidgetContainerMixin {
    
    /**
     * 在WidgetContainer处理点击之前检查我们的按钮
     */
    @Inject(method = "onMouseDown", at = @At("HEAD"), cancellable = true)
    private void onMouseDownHead(Point mousePos, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        
        Screen currentScreen = Minecraft.getInstance().screen;
        if (!(currentScreen instanceof CraftingCPUScreen<?>)) {
            return;
        }
        
        Ae2PatternHighlighter.LOGGER.info("WidgetContainer.onMouseDown intercepted! mousePos=({},{})", 
            mousePos.getX(), mousePos.getY());
        
        // 获取GUI偏移
        int guiLeft = ((appeng.client.gui.AEBaseScreen<?>) currentScreen).getGuiLeft();
        int guiTop = ((appeng.client.gui.AEBaseScreen<?>) currentScreen).getGuiTop();
        
        // 转换为屏幕坐标
        double screenMouseX = mousePos.getX() + guiLeft;
        double screenMouseY = mousePos.getY() + guiTop;
        
        // 检查是否点击了我们的按钮
        if (ScreenClickHandler.handleButtonClick(currentScreen, screenMouseX, screenMouseY)) {
            Ae2PatternHighlighter.LOGGER.info("WidgetContainer click intercepted by our button!");
            cir.setReturnValue(true);
        }
    }
}

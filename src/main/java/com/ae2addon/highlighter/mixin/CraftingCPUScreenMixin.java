package com.ae2addon.highlighter.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.StackWithBounds;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.ae2addon.highlighter.client.ScreenClickHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin注入AEBaseScreen来拦截getStackUnderMouse
 * 当鼠标在我们的按钮上时返回null阻止JEI获取物品
 */
@Mixin(value = AEBaseScreen.class)
public abstract class CraftingCPUScreenMixin {
    
    /**
     * 拦截getStackUnderMouse - 当点击我们的按钮时返回null
     */
    @Inject(method = "getStackUnderMouse", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetStackUnderMouse(double mouseX, double mouseY, CallbackInfoReturnable<StackWithBounds> cir) {
        if (!(((Object) this) instanceof CraftingCPUScreen<?> self)) {
            return;
        }
        
        // 检查是否在我们的按钮上
        if (ScreenClickHandler.isOverButton(self, mouseX, mouseY)) {
            // 返回null阻止JEI获取物品
            cir.setReturnValue(null);
        }
    }
    
    /**
     * 拦截mouseClicked
     */
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        
        if (!(((Object) this) instanceof CraftingCPUScreen<?> self)) {
            return;
        }
        
        // 检查是否点击了我们的按钮
        if (ScreenClickHandler.handleButtonClick(self, mouseX, mouseY)) {
            Ae2PatternHighlighter.LOGGER.info("Mixin intercepted button click!");
            cir.setReturnValue(true);
        }
    }
}

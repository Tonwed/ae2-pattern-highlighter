package com.ae2addon.highlighter.mixin;

import com.ae2addon.highlighter.client.camera.FreeCameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to override camera position during free camera mode
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    
    @Shadow
    protected abstract void setPosition(double x, double y, double z);
    
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);
    
    /**
     * 在相机设置时注入，覆盖位置和角度
     */
    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        FreeCameraController controller = FreeCameraController.getInstance();
        if (!controller.isActive()) return;
        
        // 使用控制器计算的位置和角度
        Vec3 cameraPos = controller.getCurrentCameraPosition();
        float yaw = controller.getCurrentYaw();
        float pitch = controller.getCurrentPitch();
        
        // 设置相机位置和角度
        this.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        this.setRotation(yaw, pitch);
    }
}

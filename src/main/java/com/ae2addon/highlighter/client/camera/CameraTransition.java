package com.ae2addon.highlighter.client.camera;

import net.minecraft.world.phys.Vec3;

/**
 * 相机平滑过渡动画
 */
public class CameraTransition {
    private final Vec3 startPos;
    private final Vec3 endPos;
    private final float startYaw;
    private final float endYaw;
    private final float startPitch;
    private final float endPitch;
    private final int duration; // 过渡时长（ticks）
    private int currentTick = 0;
    private boolean finished = false;
    
    public CameraTransition(Vec3 startPos, Vec3 endPos, float startYaw, float endYaw, 
                           float startPitch, float endPitch, int duration) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.startYaw = startYaw;
        this.endYaw = endYaw;
        this.startPitch = startPitch;
        this.endPitch = endPitch;
        this.duration = duration;
    }
    
    /**
     * 更新动画进度
     */
    public void update() {
        if (!finished) {
            currentTick++;
            if (currentTick >= duration) {
                currentTick = duration;
                finished = true;
            }
        }
    }
    
    /**
     * 获取当前位置
     */
    public Vec3 getCurrentPosition() {
        float progress = getProgress();
        return lerp(startPos, endPos, smoothstep(progress));
    }
    
    /**
     * 获取当前Yaw
     */
    public float getCurrentYaw() {
        float progress = getProgress();
        return lerpAngle(startYaw, endYaw, smoothstep(progress));
    }
    
    /**
     * 获取当前Pitch
     */
    public float getCurrentPitch() {
        float progress = getProgress();
        return lerpAngle(startPitch, endPitch, smoothstep(progress));
    }
    
    /**
     * 获取进度（0.0 - 1.0）
     */
    public float getProgress() {
        return (float) currentTick / duration;
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    /**
     * 线性插值
     */
    private Vec3 lerp(Vec3 from, Vec3 to, float t) {
        return new Vec3(
            from.x + (to.x - from.x) * t,
            from.y + (to.y - from.y) * t,
            from.z + (to.z - from.z) * t
        );
    }
    
    /**
     * 角度插值（处理角度环绕问题）
     */
    private float lerpAngle(float from, float to, float t) {
        // 处理角度差超过180度的情况
        float diff = to - from;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return from + diff * t;
    }
    
    /**
     * 平滑步进函数（ease-in-out）
     */
    private float smoothstep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }
}

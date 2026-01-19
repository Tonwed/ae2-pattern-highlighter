package com.ae2addon.highlighter.client.camera;

import com.ae2addon.highlighter.Ae2PatternHighlighter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 自由相机控制器 - 管理相机观察模式的状态和动画
 */
public class FreeCameraController {
    private static final FreeCameraController INSTANCE = new FreeCameraController();
    
    private boolean active = false;
    private BlockPos targetPos = null;
    private Vec3 originalPlayerPos = null;
    private float originalYaw = 0;
    private float originalPitch = 0;
    
    private CameraTransition transition = null;
    private float cameraDistance = 5.0f; // 相机距离目标的距离
    private float rotationAngle = 0.0f; // 轨道旋转角度
    private float elevation = 30.0f; // 仰角（度）
    
    private static final float MIN_DISTANCE = 2.0f;
    private static final float MAX_DISTANCE = 20.0f;
    private static final float ROTATION_SPEED = 0.5f; // 度/tick - 降低旋转速度让其更丝滑
    private static final int TRANSITION_DURATION = 60; // 3秒 - 增加过渡时间让其更丝滑
    
    private List<BlockPos> targets = new ArrayList<>();
    private int currentTargetIndex = 0;
    
    private FreeCameraController() {}
    
    public static FreeCameraController getInstance() {
        return INSTANCE;
    }
    
    /**
     * 开始观察模式 (单个目标)
     */
    public void startObservation(BlockPos target) {
        List<BlockPos> list = new ArrayList<>();
        list.add(target);
        startObservation(list);
    }

    /**
     * 开始观察模式 (多个目标)
     */
    public void startObservation(List<BlockPos> targets) {
        if (targets == null || targets.isEmpty()) return;
        
        this.targets = targets;
        this.currentTargetIndex = 0;
        startObservationInternal(this.targets.get(0));
    }
    
    private void startObservationInternal(BlockPos target) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // 如果已经在观察模式，从当前位置开始过渡
        Vec3 startPos;
        float startYaw, startPitch;
        
        if (active) {
            startPos = getCurrentCameraPosition();
            startYaw = getCurrentYaw();
            startPitch = getCurrentPitch();
        } else {
            // 保存原始位置和视角
            originalPlayerPos = player.position();
            originalYaw = player.getYRot();
            originalPitch = player.getXRot();
            
            startPos = originalPlayerPos.add(0, player.getEyeHeight(), 0);
            startYaw = originalYaw;
            startPitch = originalPitch;
        }
        
        targetPos = target;
        active = true;
        // 保持当前的旋转角度，或者是0
        if (!active) rotationAngle = 0;
        
        // 计算初始相机位置（在目标上方斜视）
        Vec3 initialCameraPos = calculateCameraPosition();
        
        // 计算朝向目标的角度
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        Vec3 direction = targetCenter.subtract(initialCameraPos).normalize();
        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float targetPitch = (float) -Math.toDegrees(Math.asin(direction.y));
        
        // 创建过渡动画
        transition = new CameraTransition(
            startPos,
            initialCameraPos,
            startYaw,
            targetYaw,
            startPitch,
            targetPitch,
            TRANSITION_DURATION
        );
        
        Ae2PatternHighlighter.LOGGER.info("Started or switched camera observation to {}", targetPos);
        
        // 显示overlay UI (如果还没显示)
        if (!(mc.screen instanceof CameraOverlayScreen)) {
            mc.setScreen(new CameraOverlayScreen());
        }
    }
    
    /**
     * 切换到下一个目标
     */
    public void nextTarget() {
        if (targets.isEmpty()) return;
        currentTargetIndex = (currentTargetIndex + 1) % targets.size();
        startObservationInternal(targets.get(currentTargetIndex));
    }
    
    /**
     * 切换到上一个目标
     */
    public void prevTarget() {
        if (targets.isEmpty()) return;
        currentTargetIndex = (currentTargetIndex - 1 + targets.size()) % targets.size();
        startObservationInternal(targets.get(currentTargetIndex));
    }
    
    public boolean hasMultipleTargets() {
        return targets != null && targets.size() > 1;
    }
    
    public int getCurrentTargetIndex() {
        return currentTargetIndex;
    }
    
    public int getTargetCount() {
        return targets != null ? targets.size() : 0;
    }

    /**
     * 退出观察模式
     */
    public void exitObservation() {
        if (!active) return;
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // 创建返回过渡
        Vec3 currentCameraPos = getCurrentCameraPosition();
        float currentYaw = getCurrentYaw();
        float currentPitch = getCurrentPitch();
        
        transition = new CameraTransition(
            currentCameraPos,
            originalPlayerPos.add(0, player.getEyeHeight(), 0),
            currentYaw,
            originalYaw,
            currentPitch,
            originalPitch,
            TRANSITION_DURATION
        );
        
        // 标记为退出中（过渡完成后才真正退出）
        active = false;
        targets.clear();
        
        Ae2PatternHighlighter.LOGGER.info("Exiting camera observation");
    }
    
    /**
     * 每帧更新
     */
    public void update() {
        if (transition != null) {
            transition.update();
            
            // 过渡完成后的处理
            if (transition.isFinished()) {
                if (!active) {
                    // 退出过渡完成，清理状态
                    transition = null;
                    targetPos = null;
                } else {
                    // 进入过渡完成，清除过渡对象
                    transition = null;
                }
            }
        }
        
        // 在观察模式且过渡完成后，更新旋转角度
        if (active && transition == null && targetPos != null) {
            rotationAngle += ROTATION_SPEED;
            if (rotationAngle >= 360) {
                rotationAngle -= 360;
            }
        }
    }
    
    /**
     * 调整相机距离（鼠标滚轮）
     */
    public void adjustDistance(double delta) {
        cameraDistance -= (float) delta * 0.5f;
        cameraDistance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, cameraDistance));
    }
    
    /**
     * 计算当前相机位置
     */
    private Vec3 calculateCameraPosition() {
        if (targetPos == null) return Vec3.ZERO;
        
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        
        // 轨道位置计算：围绕目标旋转
        double radians = Math.toRadians(rotationAngle);
        double elevationRadians = Math.toRadians(elevation);
        
        double offsetX = Math.cos(radians) * Math.cos(elevationRadians) * cameraDistance;
        double offsetY = Math.sin(elevationRadians) * cameraDistance;
        double offsetZ = Math.sin(radians) * Math.cos(elevationRadians) * cameraDistance;
        
        return targetCenter.add(offsetX, offsetY, offsetZ);
    }
    
    /**
     * 获取当前相机位置（考虑过渡）
     */
    public Vec3 getCurrentCameraPosition() {
        if (transition != null) {
            return transition.getCurrentPosition();
        }
        return calculateCameraPosition();
    }
    
    /**
     * 获取当前Yaw（考虑过渡）
     */
    public float getCurrentYaw() {
        if (transition != null) {
            return transition.getCurrentYaw();
        }
        
        // 计算朝向目标的角度
        if (targetPos == null) return 0;
        
        Vec3 cameraPos = getCurrentCameraPosition();
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        Vec3 direction = targetCenter.subtract(cameraPos).normalize();
        
        return (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
    }
    
    /**
     * 获取当前Pitch（考虑过渡）
     */
    public float getCurrentPitch() {
        if (transition != null) {
            return transition.getCurrentPitch();
        }
        
        // 计算朝向目标的角度
        if (targetPos == null) return 0;
        
        Vec3 cameraPos = getCurrentCameraPosition();
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        Vec3 direction = targetCenter.subtract(cameraPos).normalize();
        
        return (float) -Math.toDegrees(Math.asin(direction.y));
    }
    
    public boolean isActive() {
        return active || (transition != null && !active); // 退出过渡期间也算激活
    }
    
    public BlockPos getTargetPos() {
        return targetPos;
    }
}

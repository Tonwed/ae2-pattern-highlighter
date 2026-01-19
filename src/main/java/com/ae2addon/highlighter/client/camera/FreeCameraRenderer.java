package com.ae2addon.highlighter.client.camera;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 自由相机渲染器 - 渲染目标方块的脉冲高亮
 * 相机位置和角度通过 CameraMixin 处理
 */
public class FreeCameraRenderer {
    
    /**
     * 渲染目标方块的脉冲高亮
     */
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        
        FreeCameraController controller = FreeCameraController.getInstance();
        if (!controller.isActive()) return;
        
        BlockPos targetPos = controller.getTargetPos();
        if (targetPos == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        // 渲染脉冲效果的方块边框
        renderPulsingBox(poseStack, buffers, targetPos, System.currentTimeMillis());
        
        poseStack.popPose();
        buffers.endBatch();
    }
    
    /**
     * 渲染脉冲边框
     */
    private static void renderPulsingBox(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                        BlockPos pos, long time) {
        // 脉冲效果：alpha在0.5-1.0之间变化
        float pulse = 0.5f + 0.5f * (float) Math.sin((time % 1000) / 1000.0 * Math.PI * 2);
        
        // 使用透视线条RenderType
        VertexConsumer buffer = buffers.getBuffer(RenderType.lines());
        
        // 略微放大边框
        double expand = 0.01;
        double x1 = pos.getX() - expand;
        double y1 = pos.getY() - expand;
        double z1 = pos.getZ() - expand;
        double x2 = pos.getX() + 1 + expand;
        double y2 = pos.getY() + 1 + expand;
        double z2 = pos.getZ() + 1 + expand;
        
        // 使用黄色高亮
        float r = 1.0f;
        float g = 1.0f;
        float b = 0.0f;
        float alpha = pulse;
        
        // 使用Minecraft的renderLineBox
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(
            poseStack, buffer,
            x1, y1, z1, x2, y2, z2,
            r, g, b, alpha
        );
    }
}

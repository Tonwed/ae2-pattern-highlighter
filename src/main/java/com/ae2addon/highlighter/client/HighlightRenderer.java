package com.ae2addon.highlighter.client;

import com.ae2addon.highlighter.Ae2PatternHighlighter;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端渲染器 - 在世界中绘制透视的彩虹流光边框
 */
public class HighlightRenderer {
    
    private static final Map<BlockPos, Long> highlightedPositions = new ConcurrentHashMap<>();
    private static final long HIGHLIGHT_DURATION_MS = 15000; // 15秒
    
    // 自定义RenderType - 透视线条（禁用深度测试）
    private static final RenderType XRAY_LINES = RenderType.create(
        "ae2_highlight_xray_lines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader))
            .setLayeringState(new RenderStateShard.LayeringStateShard(
                "view_offset_z_layering",
                () -> {
                    PoseStack poseStack = RenderSystem.getModelViewStack();
                    poseStack.pushPose();
                    poseStack.scale(0.99F, 0.99F, 0.99F);
                    RenderSystem.applyModelViewMatrix();
                },
                () -> {
                    PoseStack poseStack = RenderSystem.getModelViewStack();
                    poseStack.popPose();
                    RenderSystem.applyModelViewMatrix();
                }
            ))
            .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                "translucent_transparency",
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                },
                () -> {
                    RenderSystem.disableBlend();
                }
            ))
            .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519)) // GL_ALWAYS = 519
            .setCullState(new RenderStateShard.CullStateShard(false))
            .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false)) // 不写入深度
            .createCompositeState(false)
    );
    
    // 自定义RenderType - 透视四边形（禁用深度测试）
    private static final RenderType XRAY_QUADS = RenderType.create(
        "ae2_highlight_xray_quads",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
            .setLayeringState(new RenderStateShard.LayeringStateShard(
                "view_offset_z_layering",
                () -> {
                    PoseStack poseStack = RenderSystem.getModelViewStack();
                    poseStack.pushPose();
                    poseStack.scale(0.98F, 0.98F, 0.98F);
                    RenderSystem.applyModelViewMatrix();
                },
                () -> {
                    PoseStack poseStack = RenderSystem.getModelViewStack();
                    poseStack.popPose();
                    RenderSystem.applyModelViewMatrix();
                }
            ))
            .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                "translucent_transparency",
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                },
                () -> {
                    RenderSystem.disableBlend();
                }
            ))
            .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519)) // GL_ALWAYS = 519
            .setCullState(new RenderStateShard.CullStateShard(false))
            .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false)) // 不写入深度
            .createCompositeState(false)
    );
    
    /**
     * 添加需要高亮的位置并在聊天栏显示可点击坐标
     */
    public static void addHighlights(Collection<com.ae2addon.highlighter.network.HighlightInfo> highlights) {
        Minecraft mc = Minecraft.getInstance();
        
        // 处理空结果
        if (highlights.isEmpty()) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(
                    "§6[AE2 Highlighter] §c未找到相关样板接口，可能使用了缺失合成功能或该物品是合成的中间产物"));
            }
            return;
        }
        
        long now = System.currentTimeMillis();
        
        for (var info : highlights) {
            highlightedPositions.put(info.getPosition().immutable(), now);
        }
        
        // 在聊天栏显示可点击坐标
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(
                String.format("§6[AE2 Highlighter] §f找到 %d 个样板接口:", highlights.size())));
            
            int index = 1;
            for (var info : highlights) {
                BlockPos pos = info.getPosition();
                String tpCommand = String.format("/tp @s %d %d %d", pos.getX(), pos.getY(), pos.getZ());
                
                Component coordText = Component.literal(
                        String.format("§e[%d] §b(%d, %d, %d)", index, pos.getX(), pos.getY(), pos.getZ()))
                        .setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tpCommand))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                        Component.literal("§7点击填充传送命令\n" + tpCommand))));
                
                mc.player.sendSystemMessage(coordText);
                index++;
            }
        }
        
        Ae2PatternHighlighter.LOGGER.info("Added {} highlight positions", highlights.size());
    }
    
    /**
     * 清除所有高亮
     */
    public static void clearHighlights() {
        highlightedPositions.clear();
    }
    
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        //  在粒子之后渲染，确保始终可见
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        
        if (highlightedPositions.isEmpty()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // 移除过期的高亮
        highlightedPositions.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > HIGHLIGHT_DURATION_MS);
        
        if (highlightedPositions.isEmpty()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        
        // 启用透视设置
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); // 关键：禁用深度测试
        RenderSystem.depthMask(false); // 不写入深度缓冲
        
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        for (Map.Entry<BlockPos, Long> entry : highlightedPositions.entrySet()) {
            BlockPos pos = entry.getKey();
            long startTime = entry.getValue();
            long elapsed = currentTime - startTime;
            
            // 计算渐隐效果 (最后3秒开始渐隐)
            float fadeAlpha = 1.0f;
            if (elapsed > HIGHLIGHT_DURATION_MS - 3000) {
                fadeAlpha = (HIGHLIGHT_DURATION_MS - elapsed) / 3000.0f;
            }
            
            // 先渲染半透明填充面
            VertexConsumer quadBuffer = buffers.getBuffer(XRAY_QUADS);
            renderRainbowFaces(poseStack, quadBuffer, pos, currentTime, fadeAlpha * 0.3f);
            
            // 再渲染线条边框
            VertexConsumer lineBuffer = buffers.getBuffer(XRAY_LINES);
            renderRainbowBox(poseStack, lineBuffer, pos, currentTime, fadeAlpha);
        }
        
        poseStack.popPose();
        buffers.endBatch();
        
        // 恢复渲染状态
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }
    
    /**
     * 渲染半透明彩虹面
     */
    private static void renderRainbowFaces(PoseStack poseStack, VertexConsumer buffer,
                                          BlockPos pos, long time, float alpha) {
        double expand = 0.005;
        double x1 = pos.getX() - expand;
        double y1 = pos.getY() - expand;
        double z1 = pos.getZ() - expand;
        double x2 = pos.getX() + 1 + expand;
        double y2 = pos.getY() + 1 + expand;
        double z2 = pos.getZ() + 1 + expand;
        
        var matrix = poseStack.last().pose();
        float offset = ((time % 3000) / 3000.0f);
        
        // 绘制6个面，每个面不同颜色
        int faceIndex = 0;
        
        // 下面 (Y-)
        drawQuad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, faceIndex++, offset, alpha);
        // 上面 (Y+)
        drawQuad(buffer, matrix, x1, y2, z1, x1, y2, z2,x2, y2, z2, x2, y2, z1, faceIndex++, offset, alpha);
        // 北面 (Z-)
        drawQuad(buffer, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, faceIndex++, offset, alpha);
        // 南面 (Z+)
        drawQuad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, faceIndex++, offset, alpha);
        // 西面 (X-)
        drawQuad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, faceIndex++, offset, alpha);
        // 东面 (X+)
        drawQuad(buffer, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, faceIndex++, offset, alpha);
    }
    
    /**
     * 绘制彩虹四边形
     */
    private static void drawQuad(VertexConsumer buffer, org.joml.Matrix4f matrix,
                                 double x1, double y1, double z1, double x2, double y2, double z2,
                                 double x3, double y3, double z3, double x4, double y4, double z4,
                                 int faceIndex, float offset, float alpha) {
        float hue = (faceIndex / 6.0f + offset) % 1.0f;
        float[] rgb = hsvToRgb(hue, 0.8f, 1.0f);
        
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(matrix, (float)x3, (float)y3, (float)z3).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(matrix, (float)x4, (float)y4, (float)z4).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
    }
    
    /**
     * 渲染彩虹流光边框
     */
    private static void renderRainbowBox(PoseStack poseStack, VertexConsumer buffer, 
                                        BlockPos pos, long time, float fadeAlpha) {
        // 绘制3层来增加可见度
        for (int layer = 0; layer < 3; layer++) {
            double expand = 0.002 + layer * 0.001; // 每层略微扩展
            double x1 = pos.getX() - expand;
            double y1 = pos.getY() - expand;
            double z1 = pos.getZ() - expand;
            double x2 = pos.getX() + 1 + expand;
            double y2 = pos.getY() + 1 + expand;
            double z2 = pos.getZ() + 1 + expand;
            
            var matrix = poseStack.last().pose();
            var normal = poseStack.last().normal();
            
            // 流光偏移量（0-1循环），每层稍微不同
            float offset = ((time % 2000) / 2000.0f) + (layer * 0.1f);
            float layerAlpha = fadeAlpha * (1.0f - layer * 0.2f); // 外层逐渐透明
            
            // 绘制12条边，每条边使用不同的彩虹色和流光效果
            int edgeIndex = 0;
            
            // 底面4条边
            drawRainbowLine(buffer, matrix, normal, x1, y1, z1, x2, y1, z1, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x2, y1, z1, x2, y1, z2, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x2, y1, z2, x1, y1, z2, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x1, y1, z2, x1, y1, z1, edgeIndex++, offset, layerAlpha);
            
            // 顶面4条边
            drawRainbowLine(buffer, matrix, normal, x1, y2, z1, x2, y2, z1, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x2, y2, z1, x2, y2, z2, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x2, y2, z2, x1, y2, z2, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x1, y2, z2, x1, y2, z1, edgeIndex++, offset, layerAlpha);
            
            // 垂直4条边
            drawRainbowLine(buffer, matrix, normal, x1, y1, z1, x1, y2, z1, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x2, y1, z1, x2, y2, z1, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x2, y1, z2, x2, y2, z2, edgeIndex++, offset, layerAlpha);
            drawRainbowLine(buffer, matrix, normal, x1, y1, z2, x1, y2, z2, edgeIndex++, offset, layerAlpha);
        }
    }
    
    /**
     * 绘制彩虹色线条
     */
    private static void drawRainbowLine(VertexConsumer buffer, org.joml.Matrix4f matrix, org.joml.Matrix3f normal,
                                       double x1, double y1, double z1, double x2, double y2, double z2,
                                       int edgeIndex, float flowOffset, float fadeAlpha) {
        // 计算法线
        float nx = (float)(x2 - x1);
        float ny = (float)(y2 - y1);
        float nz = (float)(z2 - z1);
        float len = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len > 0) {
            nx /= len; ny /= len; nz /= len;
        }
        
        // 每条边的基础色相偏移
        float hueOffset = edgeIndex / 12.0f;
        
        // 起点和终点使用流光渐变
        float hue1 = (hueOffset + flowOffset) % 1.0f;
        float hue2 = (hueOffset + flowOffset + 0.3f) % 1.0f; // 终点偏移30%
        
        // 使用更高的饱和度和亮度
        float[] rgb1 = hsvToRgb(hue1, 1.0f, 1.0f);
        float[] rgb2 = hsvToRgb(hue2, 1.0f, 1.0f);
        
        // 提高alpha值以增加可见度
        float alpha = 0.95f * fadeAlpha;
        
        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1)
              .color(rgb1[0], rgb1[1], rgb1[2], alpha)
              .normal(normal, nx, ny, nz)
              .endVertex();
        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2)
              .color(rgb2[0], rgb2[1], rgb2[2], alpha)
              .normal(normal, nx, ny, nz)
              .endVertex();
    }
    
    /**
     * HSV转RGB
     */
    private static float[] hsvToRgb(float h, float s, float v) {
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        
        return switch (i % 6) {
            case 0 -> new float[]{v, t, p};
            case 1 -> new float[]{q, v, p};
            case 2 -> new float[]{p, v, t};
            case 3 -> new float[]{p, q, v};
            case 4 -> new float[]{t, p, v};
            case 5 -> new float[]{v, p, q};
            default -> new float[]{1, 1, 1};
        };
    }
}

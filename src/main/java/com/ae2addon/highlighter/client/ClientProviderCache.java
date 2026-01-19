package com.ae2addon.highlighter.client;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端缓存provider位置，供命令使用
 */
public class ClientProviderCache {
    private static List<BlockPos> cachedPositions = new ArrayList<>();
    
    public static void setPositions(List<BlockPos> positions) {
        cachedPositions = new ArrayList<>(positions);
    }
    
    public static List<BlockPos> getPositions() {
        return new ArrayList<>(cachedPositions);
    }
    
    public static BlockPos getPosition(int index) {
        if (index < 1 || index > cachedPositions.size()) {
            return null;
        }
        return cachedPositions.get(index - 1); // 转换为0-based索引
    }
    
    public static void clear() {
        cachedPositions.clear();
    }
}

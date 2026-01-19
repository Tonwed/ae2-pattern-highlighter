package com.ae2addon.highlighter.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 高亮信息 - 包含位置和是否正在使用
 */
public class HighlightInfo {
    private final BlockPos position;
    private final boolean isActive;  // 是否正在使用
    
    public HighlightInfo(BlockPos position, boolean isActive) {
        this.position = position;
        this.isActive = isActive;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(position);
        buf.writeBoolean(isActive);
    }
    
    public static HighlightInfo read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean active = buf.readBoolean();
        return new HighlightInfo(pos, active);
    }
}

package com.ae2addon.highlighter.network;

import com.ae2addon.highlighter.client.HighlightRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务器发送到客户端的响应包 - 包含需要高亮的方块位置列表及使用状态  
 */
public class HighlightPositionsPacket {
    private final List<HighlightInfo> highlights;
    
    public HighlightPositionsPacket(List<HighlightInfo> highlights) {
        this.highlights = highlights;
    }
    
    // 便捷构造函数 - 兼容旧代码
    public static HighlightPositionsPacket fromPositions(List<BlockPos> positions) {
        List<HighlightInfo> highlights = new ArrayList<>();
        for (BlockPos pos : positions) {
            highlights.add(new HighlightInfo(pos, false));
        }
        return new HighlightPositionsPacket(highlights);
    }
    
    public static void encode(HighlightPositionsPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.highlights.size());
        for (HighlightInfo info : msg.highlights) {
            info.write(buf);
        }
    }
    
    public static HighlightPositionsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<HighlightInfo> highlights = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            highlights.add(HighlightInfo.read(buf));
        }
        return new HighlightPositionsPacket(highlights);
    }
    
    public static void handle(HighlightPositionsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端添加高亮
            HighlightRenderer.addHighlights(msg.highlights);
        });
        ctx.get().setPacketHandled(true);
    }
}

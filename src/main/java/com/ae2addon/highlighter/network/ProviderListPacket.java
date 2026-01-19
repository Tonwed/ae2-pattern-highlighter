package com.ae2addon.highlighter.network;

import appeng.api.stacks.AEKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务器发送到客户端的响应包 - 包含provider位置列表，在聊天中显示可点击按钮
 */
public class ProviderListPacket {
    private final List<BlockPos> positions;
    private final AEKey itemWhat;
    
    public ProviderListPacket(List<BlockPos> positions, AEKey itemWhat) {
        this.positions = positions;
        this.itemWhat = itemWhat;
    }
    
    public static void encode(ProviderListPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.positions.size());
        for (BlockPos pos : msg.positions) {
            buf.writeBlockPos(pos);
        }
        AEKey.writeKey(buf, msg.itemWhat);
    }
    
    public static ProviderListPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        AEKey itemWhat = AEKey.readKey(buf);
        return new ProviderListPacket(positions, itemWhat);
    }
    
    public List<BlockPos> getPositions() {
        return positions;
    }
    
    public AEKey getItemWhat() {
        return itemWhat;
    }
    
    public static void handle(ProviderListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 使用DistExecutor确保只在客户端执行客户端代码
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
                com.ae2addon.highlighter.client.ClientPacketHandler.handleProviderList(msg);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}

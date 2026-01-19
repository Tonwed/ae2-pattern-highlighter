package com.ae2addon.highlighter.network;

import appeng.api.stacks.AEKey;
import com.ae2addon.highlighter.client.ChatMessageBuilder;
import com.ae2addon.highlighter.client.PendingCameraAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
    
    public static void handle(ProviderListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            
            // 检查是否有待处理的相机动作
            if (PendingCameraAction.hasPending()) {
                // 触发相机观察第一个provider
                if (!msg.positions.isEmpty()) {
                    com.ae2addon.highlighter.client.camera.FreeCameraController.getInstance()
                        .startObservation(msg.positions.get(0));
                } else {
                    mc.player.sendSystemMessage(Component.literal("§c未找到样板接口"));
                }
                PendingCameraAction.clear();
            } else {
                // 在聊天中显示provider列表，包含可点击的按钮
                ChatMessageBuilder.sendProviderListMessage(mc.player, msg.positions, msg.itemWhat);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

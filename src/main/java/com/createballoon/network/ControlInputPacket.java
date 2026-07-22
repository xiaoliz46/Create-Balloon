package com.createballoon.network;

import com.createballoon.CreateBalloon;
import com.createballoon.block.ControlConsoleBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

public record ControlInputPacket(BlockPos pos, boolean ascend, boolean descend, boolean exit,
                                   boolean forward, boolean back, boolean left, boolean right) implements CustomPacketPayload {
    public static final Type<ControlInputPacket> TYPE = new Type<>(CreateBalloon.id("control_input"));

    public static final StreamCodec<ByteBuf, ControlInputPacket> CODEC = new StreamCodec<>() {
        @Override public ControlInputPacket decode(ByteBuf buf) {
            return new ControlInputPacket(
                    BlockPos.STREAM_CODEC.decode(buf),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
        }
        @Override public void encode(ByteBuf buf, ControlInputPacket pkt) {
            BlockPos.STREAM_CODEC.encode(buf, pkt.pos);
            buf.writeBoolean(pkt.ascend);
            buf.writeBoolean(pkt.descend);
            buf.writeBoolean(pkt.exit);
            buf.writeBoolean(pkt.forward);
            buf.writeBoolean(pkt.back);
            buf.writeBoolean(pkt.left);
            buf.writeBoolean(pkt.right);
        }
    };

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar r) {
        r.playToServer(TYPE, CODEC, ControlInputPacket::handle);
    }

    private static void handle(ControlInputPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                if (sp.level().getBlockEntity(pkt.pos()) instanceof ControlConsoleBlockEntity cce
                        && cce.isControlling(sp)) {
                    if (pkt.exit()) {
                        cce.stopControl(sp);
                    } else {
                        cce.setInput(pkt.ascend(), pkt.descend(),
                                pkt.forward(), pkt.back(), pkt.left(), pkt.right());
                    }
                }
            }
        });
    }
}

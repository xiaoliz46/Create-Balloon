package com.createballoon.network;

import com.createballoon.CreateBalloon;
import com.createballoon.client.ControlInputHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

public record ControlSyncPacket(BlockPos pos, boolean controlling) implements CustomPacketPayload {
    public static final Type<ControlSyncPacket> TYPE = new Type<>(CreateBalloon.id("control_sync"));
    public static final StreamCodec<FriendlyByteBuf, ControlSyncPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ControlSyncPacket::pos,
            ByteBufCodecs.BOOL, ControlSyncPacket::controlling,
            ControlSyncPacket::new);

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar r) {
        r.playToClient(TYPE, CODEC, ControlSyncPacket::handle);
    }

    private static void handle(ControlSyncPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (pkt.controlling()) ControlInputHandler.start(pkt.pos());
            else ControlInputHandler.stop();
        });
    }
}

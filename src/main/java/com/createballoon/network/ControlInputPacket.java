package com.createballoon.network;

import com.createballoon.CreateBalloon;
import com.createballoon.block.ControlConsoleBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

public record ControlInputPacket(BlockPos pos, boolean ascend, boolean descend, boolean exit) implements CustomPacketPayload {
    public static final Type<ControlInputPacket> TYPE = new Type<>(CreateBalloon.id("control_input"));
    public static final StreamCodec<FriendlyByteBuf, ControlInputPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ControlInputPacket::pos,
            ByteBufCodecs.BOOL, ControlInputPacket::ascend,
            ByteBufCodecs.BOOL, ControlInputPacket::descend,
            ByteBufCodecs.BOOL, ControlInputPacket::exit,
            ControlInputPacket::new);

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
                        cce.setInput(pkt.ascend(), pkt.descend());
                    }
                }
            }
        });
    }
}

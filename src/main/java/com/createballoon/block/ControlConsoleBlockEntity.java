package com.createballoon.block;

import com.createballoon.CreateBalloon;
import com.createballoon.network.ControlSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ControlConsoleBlockEntity extends BlockEntity {
    private static final int SCAN_RADIUS = 32;
    private static final int SCAN_INTERVAL = 20;

    @Nullable private UUID controller;
    private boolean ascend;
    private boolean descend;
    private int scanTimer = SCAN_INTERVAL;
    private final List<BlockPos> balloons = new ArrayList<>();

    public ControlConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(CreateBalloon.CONTROL_CONSOLE_ENTITY.get(), pos, state);
    }

    public void tickServer() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;

        if (--scanTimer <= 0) {
            scanTimer = SCAN_INTERVAL;
            scan();
        }

        if (isBeingControlled()) {
            if (balloons.isEmpty()) {
                stopControl(null);
                return;
            }
            if (level instanceof ServerLevel sl) {
                ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(controller);
                if (sp == null || sp.distanceToSqr(getBlockPos().getCenter()) > 256) {
                    stopControl(sp);
                    return;
                }
            }
            applyToBalloons();
        } else {
            checkRedstone();
        }
    }

    public boolean isControlling(ServerPlayer player) {
        return controller != null && controller.equals(player.getUUID());
    }

    public boolean isBeingControlled() {
        return controller != null;
    }

    @Nullable public UUID getController() { return controller; }

    public void startControl(ServerPlayer player) {
        scan();
        if (balloons.isEmpty()) {
            player.displayClientMessage(Component.translatable("msg.create_balloon.console.noballoon"), true);
            return;
        }
        controller = player.getUUID();
        ascend = false;
        descend = false;
        setChanged();
        PacketDistributor.sendToPlayer(player, new ControlSyncPacket(getBlockPos(), true));
        player.displayClientMessage(Component.translatable("msg.create_balloon.console.hint"), true);
    }

    public void stopControl(@Nullable ServerPlayer player) {
        if (controller != null && level instanceof ServerLevel sl && player == null) {
            player = sl.getServer().getPlayerList().getPlayer(controller);
        }
        if (player != null) {
            PacketDistributor.sendToPlayer(player, new ControlSyncPacket(getBlockPos(), false));
        }
        resetBalloons();
        controller = null;
        ascend = false;
        descend = false;
        setChanged();
    }

    public void setInput(boolean a, boolean d) {
        this.ascend = a;
        this.descend = d;
        applyToBalloons();
        setChanged();
    }

    public void checkRedstone() {
        if (level == null || level.isClientSide) return;
        BlockPos p = getBlockPos();
        int west = level.getSignal(p.west(), Direction.WEST);
        int east = level.getSignal(p.east(), Direction.EAST);
        if (west > 0 && east > 0) return;
        if (west > 0) { ascend = true; descend = false; }
        else if (east > 0) { ascend = false; descend = true; }
        else { ascend = false; descend = false; }
        applyToBalloons();
        setChanged();
    }

    void onBlockRemoved() {
        if (isBeingControlled()) {
            if (level instanceof ServerLevel sl) {
                ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(controller);
                if (sp != null) {
                    PacketDistributor.sendToPlayer(sp, new ControlSyncPacket(getBlockPos(), false));
                }
            }
        }
        for (BlockPos p : balloons) {
            if (level != null && level.getBlockEntity(p) instanceof HotAirBalloonBlockEntity bbe) {
                bbe.setLiftActive(false);
                bbe.setDescendActive(false);
                bbe.setHoverActive(false);
                bbe.setLiftPrimary(false);
                bbe.setStructureBalloonCount(1);
            }
        }
    }

    private void scan() {
        balloons.clear();
        if (level == null) return;
        BlockPos c = getBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos p = c.offset(x, y, z);
                    if (level.getBlockState(p).getBlock() instanceof HotAirBalloonBlock) {
                        balloons.add(p.immutable());
                    }
                }
            }
        }
    }

    private void applyToBalloons() {
        int count = balloons.size();
        for (int i = 0; i < balloons.size(); i++) {
            BlockPos p = balloons.get(i);
            if (level != null && level.getBlockEntity(p) instanceof HotAirBalloonBlockEntity bbe) {
                bbe.setLiftActive(ascend);
                bbe.setHoverActive(!ascend && !descend);
                bbe.setDescendActive(descend);
                bbe.setStructureBalloonCount(count);
                bbe.setLiftPrimary(i == 0);
            }
        }
    }

    private void resetBalloons() {
        for (BlockPos p : balloons) {
            if (level != null && level.getBlockEntity(p) instanceof HotAirBalloonBlockEntity bbe) {
                bbe.setLiftActive(false);
                bbe.setDescendActive(false);
                bbe.setHoverActive(true);
                bbe.setStructureBalloonCount(1);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (controller != null) tag.putUUID("Ctrl", controller);
        tag.putBoolean("Asc", ascend);
        tag.putBoolean("Desc", descend);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        controller = tag.hasUUID("Ctrl") ? tag.getUUID("Ctrl") : null;
        ascend = tag.getBoolean("Asc");
        descend = tag.getBoolean("Desc");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider reg) { return saveWithoutMetadata(reg); }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

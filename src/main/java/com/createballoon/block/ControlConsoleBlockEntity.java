package com.createballoon.block;

import com.createballoon.CreateBalloon;
import com.createballoon.DebugLog;
import com.createballoon.ModConfigs;
import com.createballoon.network.ControlSyncPacket;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
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
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import java.util.UUID;

public final class ControlConsoleBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    @Nullable private UUID controller;
    private boolean ascend;
    private boolean descend;
    private boolean forward, back, turnLeft, turnRight;
    private boolean wasActivated;
    private int scanTimer = 20;
    private int landTicks;
    private final List<BlockPos> balloons = new ArrayList<>();

    public ControlConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(CreateBalloon.CONTROL_CONSOLE_ENTITY.get(), pos, state);
    }

    public void tickServer() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;
        if (--scanTimer <= 0) { scanTimer = 20; scan(); }
        if (isBeingControlled()) {
            if (balloons.isEmpty()) { stopControl(null); return; }
            if (level instanceof ServerLevel sl) {
                ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(controller);
                if (sp == null || sp.distanceToSqr(getBlockPos().getCenter()) > 256) { stopControl(sp); return; }
            }
            if (descend) {
                boolean allOnGround = !balloons.isEmpty();
                for (BlockPos p : balloons) {
                    if (level != null && level.getBlockEntity(p) instanceof LiftProviderBE bbe) {
                        if (!bbe.isOnGround()) { allOnGround = false; break; }
                    }
                }
                if (allOnGround) {
                    landTicks++;
                    if (landTicks >= 20) {
                        stopControl(null); return;
                    }
                } else { landTicks = 0; }
            } else { landTicks = 0; }
            applyToBalloons();
        }
    }

    public boolean isControlling(ServerPlayer p) { return controller != null && controller.equals(p.getUUID()); }
    public boolean isBeingControlled() { return controller != null; }

    public void startControl(ServerPlayer player) {
        scan();
        if (balloons.isEmpty()) { player.displayClientMessage(Component.translatable("msg.create_balloon.console.noballoon"), true); return; }
        if (checkOverloaded()) return;
        controller = player.getUUID(); ascend = false; descend = false; setChanged();
        PacketDistributor.sendToPlayer(player, new ControlSyncPacket(getBlockPos(), true));
        player.displayClientMessage(Component.translatable("msg.create_balloon.console.hint"), true);
    }

    public void stopControl(@Nullable ServerPlayer player) {
        if (controller != null && level instanceof ServerLevel sl && player == null)
            player = sl.getServer().getPlayerList().getPlayer(controller);
        if (player != null) PacketDistributor.sendToPlayer(player, new ControlSyncPacket(getBlockPos(), false));
        ascend = false; descend = false; forward = false; back = false;
        turnLeft = false; turnRight = false; wasActivated = false;
        resetBalloons(); controller = null; setChanged();
    }

    public void setInput(boolean a, boolean d, boolean fwd, boolean bwd, boolean lt, boolean rt) {
        if (checkOverloaded()) return;
        this.ascend = a; this.descend = d;
        this.forward = fwd; this.back = bwd; this.turnLeft = lt; this.turnRight = rt;
        this.wasActivated = true;
        if (ModConfigs.LOG_LEVEL.get() != ModConfigs.LogLevel.OFF) {
            DebugLog.log("CTRL ascend=%b descend=%b fwd=%b back=%b left=%b right=%b balloons=%d",
                    a, d, fwd, bwd, lt, rt, balloons.size());
        }
        applyToBalloons(); setChanged();
    }

    public void checkRedstone() {
        if (level == null || level.isClientSide || isBeingControlled()) return;
        BlockState state = level.getBlockState(getBlockPos());
        Direction facing = state.getValue(ControlConsoleBlock.FACING);
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        BlockPos p = getBlockPos();
        int leftSignal = level.getSignal(p.relative(left), left);
        int rightSignal = level.getSignal(p.relative(right), right);

        try {
            var signalClass = Class.forName("edn.stratodonut.drivebywire.compat.ControllerSignalStore");
            var getSignals = signalClass.getMethod("getSignals", Level.class, BlockPos.class);
            Object wireSignals = getSignals.invoke(null, level, p);
            if (wireSignals instanceof java.util.Map<?, ?> wm) {
                for (var e : wm.entrySet()) {
                    int v = ((Number) e.getValue()).intValue();
                    if ("ascend".equals(e.getKey())) leftSignal = Math.max(leftSignal, v);
                    if ("descend".equals(e.getKey())) rightSignal = Math.max(rightSignal, v);
                }
            }
        } catch (Throwable ignored) {}

        if (leftSignal > 0 && rightSignal > 0) return;
        if (leftSignal > 0) { ascend = false; descend = true; }
        else if (rightSignal > 0) { ascend = true; descend = false; }
        else { ascend = false; descend = false; }
        if (checkOverloaded()) return;
        wasActivated = true;
        applyToBalloons(); setChanged();
    }

    void onBlockRemoved() {
        if (isBeingControlled() && level instanceof ServerLevel sl) {
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(controller);
            if (sp != null) PacketDistributor.sendToPlayer(sp, new ControlSyncPacket(getBlockPos(), false));
        }
        for (BlockPos p : balloons) {
            if (level != null && level.getBlockEntity(p) instanceof LiftProviderBE bbe) {
                bbe.setLiftActive(false); bbe.setDescendActive(false); bbe.setHoverActive(false);
                bbe.setStructureBalloonCount(1);
            }
        }
    }

    private void scan() {
        balloons.clear();
        if (level == null) return;
        if (Sable.HELPER.getContaining(level, getBlockPos()) instanceof ServerSubLevel ssl
                && ssl.getPlot() instanceof ServerLevelPlot sp) {
            for (var ctx : sp.getLiftProviders()) {
                if (ctx.state().getBlock() instanceof HotAirBalloonBlock
                        || ctx.state().getBlock() instanceof InflatedWoolBlock) {
                    balloons.add(ctx.pos().immutable());
                }
            }
        }
    }

    private void applyToBalloons() {
        if (!wasActivated) return;
        int count = balloons.size();
        Direction facing = level != null ? level.getBlockState(getBlockPos()).getValue(ControlConsoleBlock.FACING) : Direction.NORTH;
        for (int i = 0; i < balloons.size(); i++) {
            BlockPos p = balloons.get(i);
            if (level != null && level.getBlockEntity(p) instanceof LiftProviderBE bbe) {
                bbe.setLiftActive(ascend); bbe.setHoverActive(!ascend && !descend);
                bbe.setDescendActive(descend); bbe.setStructureBalloonCount(count); bbe.setLiftPrimary(i == 0);
                bbe.setMoveForward(forward); bbe.setMoveBack(back);
                bbe.setTurnLeft(turnLeft); bbe.setTurnRight(turnRight);
                bbe.setForwardDir(facing);
            }
        }
    }

    private boolean checkOverloaded() {
        if (level == null || balloons.isEmpty()) return false;
        if (!(Sable.HELPER.getContaining(level, getBlockPos()) instanceof ServerSubLevel ssl)) return false;
        double mass = ssl.getMassTracker().getMass();
        int count = balloons.size();
        double capacity = ModConfigs.LIFT_FORCE.get() * count;
        if (mass > capacity) {
            if (level instanceof ServerLevel sl && controller != null) {
                var sp = sl.getServer().getPlayerList().getPlayer(controller);
                if (sp != null) sp.displayClientMessage(
                    Component.translatable("msg.create_balloon.console.overloaded"), true);
            }
            return true;
        }
        return false;
    }

    private void resetBalloons() {
        int count = balloons.size();
        for (BlockPos p : balloons) {
            if (level != null && level.getBlockEntity(p) instanceof LiftProviderBE bbe) {
                bbe.setLiftActive(false); bbe.setDescendActive(false); bbe.setHoverActive(true);
                bbe.setMoveForward(false); bbe.setMoveBack(false);
                bbe.setTurnLeft(false); bbe.setTurnRight(false);
                bbe.setStructureBalloonCount(count);
            }
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (controller != null) tag.putUUID("Ctrl", controller);
        tag.putBoolean("Asc", ascend); tag.putBoolean("Desc", descend); tag.putBoolean("Act", wasActivated);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        controller = tag.hasUUID("Ctrl") ? tag.getUUID("Ctrl") : null;
        ascend = tag.getBoolean("Asc"); descend = tag.getBoolean("Desc");
        wasActivated = tag.getBoolean("Act");
        scanTimer = 0;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider reg) { return saveWithoutMetadata(reg); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (level == null || level.isClientSide) return;
        double pt = dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.getCurrentlySteppingSystem().getPartialPhysicsTick();
        GyroController.of(subLevel).tick(pt, body, subLevel, dt);
    }
}

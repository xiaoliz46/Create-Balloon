package com.createballoon.block;

import com.createballoon.CreateBalloon;
import com.createballoon.DebugLog;
import com.createballoon.ModConfigs;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class HotAirBalloonBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, LiftProviderBE {
    public static final double MAX_LIFT_KPG = 100.0;
    private static final double MAX_GAS = 2000.0;
    private static final double LERP_SPEED = 0.1;

    private double gasAmount;
    private double gasTarget;
    private boolean liftActive;
    private int structureBalloonCount = 1;
    private boolean isLiftPrimary;
    private boolean hoverActive;
    private boolean descendActive;
    private boolean onGround;
    private double smoothFactor;
    private double hoverIntegral;         // transient PI integral for zero-drift hover
    private double yawAccum;                // accumulated yaw angle for propulsion direction
    private boolean moveForward, moveBack, turnLeft, turnRight;
    private Direction forwardDir = Direction.NORTH;
    private double currentKpg = 0.2597;
    private double currentThrust;
    private byte gyroPercent;
    private byte gyroScalePercent;
    private byte gyroBalloonCount;
    private byte gyroNeedBalloons;
    private long lastPhysicsTick = -1;

    public HotAirBalloonBlockEntity(BlockPos pos, BlockState state) {
        super(CreateBalloon.HOT_AIR_BALLOON_ENTITY.get(), pos, state);
    }

    public void tickServer() {
        Level level = this.level;
        if (level == null || level.isClientSide) return;
        if (Math.abs(gasAmount - gasTarget) > 0.01) {
            gasAmount += (gasTarget - gasAmount) * LERP_SPEED;
            setChanged();
        }
        double target = liftActive ? 1.0 : 0.0;
        if (Math.abs(smoothFactor - target) > 0.001) {
            smoothFactor += (target - smoothFactor) * 0.03;
            setChanged();
        }
    }

    public double getLiftMultiplier() {
        if (!liftActive) return 0.0;
        double y = getBlockPos().getY();
        double altitudeFactor = Math.exp(-(y - 62.0) / 180.0);
        altitudeFactor = Math.max(0.15, altitudeFactor);
        double gasFactor = 0.5 + 0.5 * (gasAmount / Math.max(1.0, MAX_GAS));
        return gasFactor * altitudeFactor;
    }

    public double getSmoothFactor() { return smoothFactor; }

    public boolean isLiftActive() { return liftActive; }
    public void setLiftActive(boolean active) { this.liftActive = active; setChanged(); }

    public int getStructureBalloonCount() { return structureBalloonCount; }
    public void setStructureBalloonCount(int count) { this.structureBalloonCount = Math.max(1, count); setChanged(); }

    public boolean isLiftPrimary() { return isLiftPrimary; }
    public void setLiftPrimary(boolean p) { this.isLiftPrimary = p; setChanged(); }

    public boolean isHoverActive() { return hoverActive; }
    public void setHoverActive(boolean h) {
        if (h != this.hoverActive && ModConfigs.LOG_LEVEL.get() == ModConfigs.LogLevel.DETAILED) {
            DebugLog.log("BALLOON pos=(%d,%d,%d) hoverActive %b→%b lift=%b primary=%b",
                    getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(),
                    this.hoverActive, h, liftActive, isLiftPrimary);
        }
        this.hoverActive = h;
        setChanged();
    }

    public boolean isDescendActive() { return descendActive; }
    public void setDescendActive(boolean d) { this.descendActive = d; setChanged(); }
    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean g) { this.onGround = g; }

    public boolean isMoveForward() { return moveForward; }
    public void setMoveForward(boolean f) { this.moveForward = f; }
    public boolean isMoveBack() { return moveBack; }
    public void setMoveBack(boolean b) { this.moveBack = b; }
    public boolean isTurnLeft() { return turnLeft; }
    public void setTurnLeft(boolean l) { this.turnLeft = l; }
    public boolean isTurnRight() { return turnRight; }
    public void setTurnRight(boolean r) { this.turnRight = r; }

    public Direction getForwardDir() { return forwardDir; }
    public void setForwardDir(Direction d) { this.forwardDir = d; }

    public double getCurrentKpg() { return currentKpg; }
    public void setCurrentKpg(double k) { this.currentKpg = k; }

    public double getCurrentThrust() { return currentThrust; }
    public void setCurrentThrust(double t) { this.currentThrust = t; }

    public int getGyroPercent() { return Byte.toUnsignedInt(gyroPercent); }
    public void setGyroPercent(int p) { this.gyroPercent = (byte) Math.max(0, Math.min(100, p)); }

    public boolean isFrameSkipped() {
        long gameTime = level.getGameTime();
        if (gameTime == lastPhysicsTick) return true;
        lastPhysicsTick = gameTime;
        return false;
    }

    public int getGyroScalePercent() { return Byte.toUnsignedInt(gyroScalePercent); }
    public int getGyroBalloonCount() { return Byte.toUnsignedInt(gyroBalloonCount); }
    public int getGyroNeedBalloons() { return Byte.toUnsignedInt(gyroNeedBalloons); }
    public void setGyroScale(int scalePct, int balloonCount, int needBalloons) {
        this.gyroScalePercent = (byte) Math.max(0, Math.min(100, scalePct));
        this.gyroBalloonCount = (byte) Math.max(0, Math.min(255, balloonCount));
        this.gyroNeedBalloons = (byte) Math.max(0, Math.min(255, needBalloons));
    }

    public double getGasAmount() { return gasAmount; }
    public double getMaxGas() { return MAX_GAS; }

    public double getHoverIntegral() { return hoverIntegral; }
    public void setHoverIntegral(double v) { this.hoverIntegral = v; }
    public double getYawAccum() { return yawAccum; }
    public void setYawAccum(double v) { this.yawAccum = v; }

    public void setGasTarget(double target) {
        gasTarget = Math.max(0, Math.min(MAX_GAS, target));
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putDouble("GasAmount", gasAmount);
        tag.putDouble("GasTarget", gasTarget);
        tag.putBoolean("LiftActive", liftActive);
        tag.putBoolean("HoverActive", hoverActive);
        tag.putBoolean("LiftPrimary", isLiftPrimary);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        gasAmount = tag.getDouble("GasAmount");
        gasTarget = tag.getDouble("GasTarget");
        liftActive = tag.getBoolean("LiftActive");
        if (tag.contains("HoverActive")) hoverActive = tag.getBoolean("HoverActive");
        if (tag.contains("LiftPrimary")) isLiftPrimary = tag.getBoolean("LiftPrimary");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider reg) { return saveWithoutMetadata(reg); }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle rigidBody, double dt) {
        if (level == null || level.isClientSide) return;
        if (!isLiftPrimary) return;
        HotAirBalloonBlock.physicsTick(this, subLevel, rigidBody, dt);
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

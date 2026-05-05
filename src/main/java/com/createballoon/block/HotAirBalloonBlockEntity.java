package com.createballoon.block;

import com.createballoon.CreateBalloon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class HotAirBalloonBlockEntity extends BlockEntity {
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
    private double smoothFactor;
    private double hoverIntegral;

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
            smoothFactor += (target - smoothFactor) * 0.05;
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
        if (h && !this.hoverActive) this.hoverIntegral = 0;
        this.hoverActive = h;
        setChanged();
    }
    public boolean isDescendActive() { return descendActive; }
    public void setDescendActive(boolean d) { this.descendActive = d; setChanged(); }

    public double getHoverIntegral() { return hoverIntegral; }
    public void addHoverIntegral(double v) { this.hoverIntegral += v; setChanged(); }

    public double getGasAmount() { return gasAmount; }
    public double getMaxGas() { return MAX_GAS; }

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        gasAmount = tag.getDouble("GasAmount");
        gasTarget = tag.getDouble("GasTarget");
        liftActive = tag.getBoolean("LiftActive");
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider reg) { return saveWithoutMetadata(reg); }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

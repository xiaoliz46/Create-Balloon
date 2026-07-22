package com.createballoon.block;

import com.createballoon.CreateBalloon;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class InflatedWoolBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, LiftProviderBE {
    private boolean liftActive, hoverActive = true, descendActive;
    private boolean moveForward, moveBack, turnLeft, turnRight;
    private Direction forwardDir = Direction.NORTH;
    private int balloonCount;
    private boolean liftPrimary;
    private double hoverIntegral;
    private double currentThrust;
    private boolean onGround;
    private long frameMark;

    public InflatedWoolBlockEntity(BlockPos pos, BlockState state) {
        super(CreateBalloon.INFLATED_WOOL_ENTITY.get(), pos, state);
    }

    public boolean isLiftActive() { return liftActive; }
    public void setLiftActive(boolean v) { liftActive = v; }
    public boolean isHoverActive() { return hoverActive; }
    public void setHoverActive(boolean v) { hoverActive = v; }
    public boolean isDescendActive() { return descendActive; }
    public void setDescendActive(boolean v) { descendActive = v; }
    public boolean isMoveForward() { return moveForward; }
    public void setMoveForward(boolean v) { moveForward = v; }
    public boolean isMoveBack() { return moveBack; }
    public void setMoveBack(boolean v) { moveBack = v; }
    public boolean isTurnLeft() { return turnLeft; }
    public void setTurnLeft(boolean v) { turnLeft = v; }
    public boolean isTurnRight() { return turnRight; }
    public void setTurnRight(boolean v) { turnRight = v; }
    public Direction getForwardDir() { return forwardDir; }
    public void setForwardDir(Direction d) { forwardDir = d; }
    public int getStructureBalloonCount() { return balloonCount; }
    public void setStructureBalloonCount(int c) { balloonCount = c; }
    public void setLiftPrimary(boolean v) { liftPrimary = v; }
    public boolean isLiftPrimary() { return liftPrimary; }
    public double getHoverIntegral() { return hoverIntegral; }
    public void setHoverIntegral(double v) { hoverIntegral = v; }
    public double getCurrentThrust() { return currentThrust; }
    public void setCurrentThrust(double v) { currentThrust = v; }
    public boolean isFrameSkipped() {
        long t = level != null ? level.getGameTime() : 0;
        if (t == frameMark) return true;
        frameMark = t; return false;
    }
    public boolean isOnGround() { return onGround; }
    public void setOnGround(boolean v) { onGround = v; }

    @Override protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) { super.saveAdditional(t, r); }
    @Override protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) { super.loadAdditional(t, r); }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (level == null || level.isClientSide) return;
        if (!liftPrimary) return;
        HotAirBalloonBlock.physicsTickInline(this, subLevel, body, dt);
    }
}

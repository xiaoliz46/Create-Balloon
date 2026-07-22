package com.createballoon.block;

import net.minecraft.core.Direction;

public interface LiftProviderBE {
    boolean isLiftActive(); void setLiftActive(boolean v);
    boolean isHoverActive(); void setHoverActive(boolean v);
    boolean isDescendActive(); void setDescendActive(boolean v);
    boolean isMoveForward(); void setMoveForward(boolean v);
    boolean isMoveBack(); void setMoveBack(boolean v);
    boolean isTurnLeft(); void setTurnLeft(boolean v);
    boolean isTurnRight(); void setTurnRight(boolean v);
    Direction getForwardDir(); void setForwardDir(Direction d);
    int getStructureBalloonCount(); void setStructureBalloonCount(int c);
    boolean isFrameSkipped();
    double getHoverIntegral(); void setHoverIntegral(double v);
    double getCurrentThrust(); void setCurrentThrust(double v);
    boolean isOnGround(); void setOnGround(boolean v);
    boolean isLiftPrimary();
    void setLiftPrimary(boolean v);
}

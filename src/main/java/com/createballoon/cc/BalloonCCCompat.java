package com.createballoon.cc;

import com.createballoon.CreateBalloon;
import com.createballoon.DebugLog;
import com.createballoon.ModConfigs;
import com.createballoon.block.ControlConsoleBlockEntity;
import com.createballoon.block.HotAirBalloonBlock;
import com.createballoon.block.HotAirBalloonBlockEntity;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BalloonCCCompat {

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> {
            var cap = PeripheralCapability.get();
            event.registerBlockEntity(cap,
                    CreateBalloon.HOT_AIR_BALLOON_ENTITY.get(),
                    (HotAirBalloonBlockEntity be, net.minecraft.core.Direction side) -> new BalloonIPeripheral(be));
            event.registerBlockEntity(cap,
                    CreateBalloon.CONTROL_CONSOLE_ENTITY.get(),
                    (ControlConsoleBlockEntity cce, net.minecraft.core.Direction side) ->
                            new BalloonIPeripheral(cce.getLevel(), cce.getBlockPos()));
        });
        ComputerCraftAPI.registerAPIFactory(BalloonAPI::new);
    }

    private static List<BlockPos> scanSableBalloons(Level level, BlockPos pos) {
        var list = new ArrayList<BlockPos>();
        if (Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel ssl
                && ssl.getPlot() instanceof ServerLevelPlot sp) {
            for (var ctx : sp.getLiftProviders()) {
                if (ctx.state().getBlock() instanceof HotAirBalloonBlock) {
                    list.add(ctx.pos().immutable());
                }
            }
        }
        return list;
    }

    private static void applyToBalloons(Level level, List<BlockPos> balloons,
                                         boolean ascend, boolean descend, boolean hover,
                                         boolean forward, boolean back, boolean turnLeft, boolean turnRight) {
        if (level == null || balloons.isEmpty()) return;
        int count = balloons.size();
        for (int i = 0; i < count; i++) {
            var be = level.getBlockEntity(balloons.get(i));
            if (be instanceof HotAirBalloonBlockEntity bbe) {
                bbe.setLiftActive(ascend);
                bbe.setDescendActive(descend);
                bbe.setHoverActive(hover);
                bbe.setMoveForward(forward);
                bbe.setMoveBack(back);
                bbe.setTurnLeft(turnLeft);
                bbe.setTurnRight(turnRight);
                bbe.setStructureBalloonCount(count);
                bbe.setLiftPrimary(i == 0);
            }
        }
    }

    private static boolean anyActive(Level level, List<BlockPos> balloons) {
        for (var p : balloons) {
            var be = level.getBlockEntity(p);
            if (be instanceof HotAirBalloonBlockEntity bbe
                    && (bbe.isLiftActive() || bbe.isDescendActive() || bbe.isHoverActive()
                    || bbe.isMoveForward() || bbe.isMoveBack()
                    || bbe.isTurnLeft() || bbe.isTurnRight()))
                return true;
        }
        return false;
    }

    private static boolean isOverloaded(Level level, BlockPos pos, List<BlockPos> balloons) {
        if (level == null || balloons.isEmpty()) return false;
        if (!(Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel ssl)) return false;
        double mass = ssl.getMassTracker().getMass();
        double capacity = ModConfigs.LIFT_FORCE.get() * balloons.size();
        return mass > capacity;
    }

    // ===== Legacy IPeripheral (backward compatible) =====

    public static final class BalloonIPeripheral implements IPeripheral {
        private final HotAirBalloonBlockEntity self;

        BalloonIPeripheral(HotAirBalloonBlockEntity self) {
            this.self = self;
        }

        BalloonIPeripheral(Level level, BlockPos pos) {
            var be = level.getBlockEntity(pos);
            this.self = be instanceof HotAirBalloonBlockEntity bbe ? bbe : null;
        }

        @Override
        public String getType() {
            return "balloon";
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return other instanceof BalloonIPeripheral bp && self == bp.self;
        }

        @Override
        public void attach(IComputerAccess computer) {}

        @Override
        public void detach(IComputerAccess computer) {}

        private List<BlockPos> scan() {
            if (self == null) return Collections.emptyList();
            var level = self.getLevel();
            if (level == null) return Collections.emptyList();
            return scanSableBalloons(level, self.getBlockPos());
        }

        private void requireLift() throws LuaException {
            if (self != null && isOverloaded(self.getLevel(), self.getBlockPos(), scan()))
                throw new LuaException("Overloaded! Insufficient lift");
        }

        @LuaFunction(mainThread = true)
        public final int count() {
            return scan().size();
        }

        @LuaFunction(mainThread = true)
        public final void ascend() throws LuaException {
            requireLift();
            applyToBalloons(self != null ? self.getLevel() : null, scan(), true, false, false, false, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void descend() throws LuaException {
            requireLift();
            applyToBalloons(self != null ? self.getLevel() : null, scan(), false, true, false, false, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void hover() throws LuaException {
            requireLift();
            applyToBalloons(self != null ? self.getLevel() : null, scan(), false, false, true, false, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void stop() {
            applyToBalloons(self != null ? self.getLevel() : null, scan(), false, false, false, false, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void forward() throws LuaException {
            requireLift();
            applyToBalloons(self != null ? self.getLevel() : null, scan(), false, false, false, true, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void back() throws LuaException {
            requireLift();
            applyToBalloons(self != null ? self.getLevel() : null, scan(), false, false, false, false, true, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void turnLeft() throws LuaException {
            requireLift();
            applyToBalloons(self != null ? self.getLevel() : null, scan(), false, false, false, false, false, true, false);
        }

        @LuaFunction(mainThread = true)
        public final void turnRight() throws LuaException {
            requireLift();
            applyToBalloons(self != null ? self.getLevel() : null, scan(), false, false, false, false, false, false, true);
        }

        @LuaFunction(mainThread = true)
        public final boolean isActive() {
            if (self == null) return false;
            return anyActive(self.getLevel(), scan());
        }

        @LuaFunction(mainThread = true)
        public final java.util.Map<String, Object> getGyro() {
            var map = new java.util.HashMap<String, Object>();
            if (self == null) {
                map.put("stabilized", 0);
                map.put("scale", 0);
                map.put("balloons", 0);
                map.put("needBalloons", 0);
            } else {
                map.put("stabilized", self.getGyroPercent());
                map.put("scale", self.getGyroScalePercent());
                map.put("balloons", self.getGyroBalloonCount());
                map.put("needBalloons", self.getGyroNeedBalloons());
            }
            return map;
        }
    }

    // ===== Native ILuaAPI (new global API) =====

    public static final class BalloonAPI implements ILuaAPI {
        private final IComputerSystem computer;
        private List<BlockPos> cachedBalloons = Collections.emptyList();
        private int rescanTimer;

        BalloonAPI(IComputerSystem computer) {
            this.computer = computer;
        }

        @Override
        public String[] getNames() {
            return new String[]{"balloon"};
        }

        @Override
        public void startup() {}

        @Override
        public void shutdown() {}

        @Override
        public void update() {}

        private List<BlockPos> getBalloons() {
            if (--rescanTimer <= 0) {
                rescanTimer = 20;
                var level = computer.getLevel();
                if (level == null || level.isClientSide) {
                    cachedBalloons = Collections.emptyList();
                } else {
                    cachedBalloons = scanSableBalloons(level, computer.getPosition());
                }
            }
            return cachedBalloons;
        }

        private void log(String format, Object... args) {
            if (ModConfigs.LOG_LEVEL.get() != ModConfigs.LogLevel.OFF) {
                DebugLog.log("[CC:" + computer.getID() + "] " + format, args);
            }
        }

        private void requireLift() throws LuaException {
            if (isOverloaded(computer.getLevel(), computer.getPosition(), getBalloons()))
                throw new LuaException("Overloaded! Insufficient lift");
        }

        @LuaFunction(mainThread = true)
        public final int count() {
            int n = getBalloons().size();
            log("count() = %d", n);
            return n;
        }

        @LuaFunction(mainThread = true)
        public final void ascend() throws LuaException {
            requireLift();
            log("ascend()");
            applyToBalloons(computer.getLevel(), getBalloons(), true, false, false, false, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void descend() throws LuaException {
            requireLift();
            log("descend()");
            applyToBalloons(computer.getLevel(), getBalloons(), false, true, false, false, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void hover() throws LuaException {
            requireLift();
            log("hover()");
            applyToBalloons(computer.getLevel(), getBalloons(), false, false, true, false, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void stop() {
            log("stop()");
            applyToBalloons(computer.getLevel(), getBalloons(), false, false, false, false, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void forward() throws LuaException {
            requireLift();
            log("forward()");
            applyToBalloons(computer.getLevel(), getBalloons(), false, false, false, true, false, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void back() throws LuaException {
            requireLift();
            log("back()");
            applyToBalloons(computer.getLevel(), getBalloons(), false, false, false, false, true, false, false);
        }

        @LuaFunction(mainThread = true)
        public final void turnLeft() throws LuaException {
            requireLift();
            log("turnLeft()");
            applyToBalloons(computer.getLevel(), getBalloons(), false, false, false, false, false, true, false);
        }

        @LuaFunction(mainThread = true)
        public final void turnRight() throws LuaException {
            requireLift();
            log("turnRight()");
            applyToBalloons(computer.getLevel(), getBalloons(), false, false, false, false, false, false, true);
        }

        @LuaFunction(mainThread = true)
        public final boolean isActive() {
            return anyActive(computer.getLevel(), getBalloons());
        }

        @LuaFunction(mainThread = true)
        public final int getHeight() {
            var b = getBalloons();
            return b.isEmpty() ? computer.getPosition().getY() : b.get(0).getY();
        }

        @LuaFunction(mainThread = true)
        public final java.util.Map<String, Object> getGyro() {
            var map = new java.util.HashMap<String, Object>();
            var balloons = getBalloons();
            if (balloons.isEmpty()) {
                map.put("stabilized", 0);
                map.put("scale", 0);
                map.put("balloons", 0);
                map.put("needBalloons", 0);
            } else {
                var be = computer.getLevel().getBlockEntity(balloons.get(0));
                if (be instanceof HotAirBalloonBlockEntity bbe) {
                    map.put("stabilized", bbe.getGyroPercent());
                    map.put("scale", bbe.getGyroScalePercent());
                    map.put("balloons", bbe.getGyroBalloonCount());
                    map.put("needBalloons", bbe.getGyroNeedBalloons());
                } else {
                    map.put("stabilized", 0);
                    map.put("scale", 0);
                    map.put("balloons", 0);
                    map.put("needBalloons", 0);
                }
            }
            return map;
        }
    }
}

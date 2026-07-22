package com.createballoon.block;

import com.createballoon.CreateBalloon;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class GyroscopeBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    public GyroscopeBlockEntity(BlockPos pos, BlockState state) {
        super(CreateBalloon.GYROSCOPE_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) { super.saveAdditional(t, r); }
    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) { super.loadAdditional(t, r); }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (level == null || level.isClientSide) return;
        double pt = SubLevelPhysicsSystem.getCurrentlySteppingSystem().getPartialPhysicsTick();
        GyroController.of(subLevel).tick(pt, body, subLevel, dt);
    }
}

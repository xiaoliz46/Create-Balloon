package com.createballoon.block;

import com.createballoon.CreateBalloon;
import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public final class HotAirBalloonBlock extends BaseEntityBlock implements BlockSubLevelLiftProvider {
    public static final MapCodec<HotAirBalloonBlock> CODEC = simpleCodec(HotAirBalloonBlock::new);
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 14, 14);
    private static final double KPG_PER_BALLOON = 100.0;
    private static final double LIFT_FORCE = 1.0;
    private static final double HOVER_P = 0.5;
    private static final double HOVER_I = 0.5;
    private static final double HOVER_DAMPING = 0.5;
    private static final double DESCEND_FORCE = -0.3;
    private static final float DRAG = 6.0F;
    private static final float ANGULAR_DRAG = 50.0F;
    private static final ForceGroup BALLOON_GROUP = new ForceGroup(
            Component.literal("Create Balloon Lift"), Component.empty(), 0xFFFFAA, false);

    public HotAirBalloonBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override protected VoxelShape getBlockSupportShape(BlockState s, BlockGetter l, BlockPos p) { return Shapes.block(); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HotAirBalloonBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, CreateBalloon.HOT_AIR_BALLOON_ENTITY.get(),
                (l, p, s, be) -> ((HotAirBalloonBlockEntity) be).tickServer());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.create_balloon.hot_air_balloon.1"));
        tooltip.add(Component.translatable("tooltip.create_balloon.hot_air_balloon.2"));
        if (flag.isAdvanced()) {
            tooltip.add(Component.translatable("tooltip.create_balloon.hot_air_balloon.3"));
            tooltip.add(Component.translatable("tooltip.create_balloon.hot_air_balloon.4"));
        }
    }

    @Override public Direction sable$getNormal(BlockState s) { return Direction.UP; }
    @Override public float sable$getLiftScalar() { return 0.0F; }
    @Override public float sable$getParallelDragScalar() { return 0.0F; }
    @Override public float sable$getDirectionlessDragScalar() { return ANGULAR_DRAG; }

    private static Vector3d worldToLocal(ServerSubLevel subLevel, double x, double y, double z) {
        Vector3d v = new Vector3d(x, y, z);
        if (subLevel != null) {
            new Quaterniond(subLevel.logicalPose().orientation()).invert().transform(v);
        }
        return v;
    }

    @Override
    public void sable$contributeLiftAndDrag(LiftProviderContext ctx, ServerSubLevel subLevel,
                                             Pose3d pose, double scale, Vector3dc vel, Vector3dc angVel,
                                             Vector3d liftForce, Vector3d dragForce, LiftProviderGroup group) {
        if (subLevel == null) return;
        Level level = subLevel.getLevel();
        if (level == null) return;
        BlockEntity be = level.getBlockEntity(ctx.pos());
        if (!(be instanceof HotAirBalloonBlockEntity bbe)) return;
        if (!bbe.isLiftPrimary()) return;

        double mass = subLevel.getMassTracker().getMass();
        ForceTotal ft = subLevel.getOrCreateQueuedForceGroup(BALLOON_GROUP).getForceTotal();

        if (bbe.isLiftActive()) {
            double smooth = bbe.getSmoothFactor();
            if (smooth > 0.001) {
                int count = bbe.getStructureBalloonCount();
                double totalKpg = count * KPG_PER_BALLOON;
                if (totalKpg > mass) {
                    ft.applyLinearImpulse(worldToLocal(subLevel, 0, LIFT_FORCE * smooth * count, 0));
                }
            }
        }

        if (bbe.isHoverActive()) {
            double velY = vel.y();
            double damping = -velY * mass * HOVER_DAMPING;
            double error = 0.0 - velY;
            if (Math.abs(error) > 0.02) {
                bbe.addHoverIntegral(error * 0.05);
            } else {
                double cur = bbe.getHoverIntegral();
                if (Math.abs(cur) > 0.001) {
                    bbe.addHoverIntegral(-Math.signum(cur) * 0.01);
                }
            }
            double integral = Math.max(-3.0, Math.min(3.0, bbe.getHoverIntegral()));
            double hoverForce = damping + (error * HOVER_P + integral * HOVER_I) * mass;
            ft.applyLinearImpulse(worldToLocal(subLevel, 0, hoverForce, 0));
        }

        if (bbe.isDescendActive()) {
            ft.applyLinearImpulse(worldToLocal(subLevel, 0, mass * DESCEND_FORCE, 0));
        }

        double speed = vel.length();
        if (speed > 1e-4) {
            ft.applyLinearImpulse(worldToLocal(subLevel,
                    -scale * DRAG * speed * vel.x(),
                    -scale * DRAG * speed * vel.y(),
                    -scale * DRAG * speed * vel.z()));
        }
    }
}

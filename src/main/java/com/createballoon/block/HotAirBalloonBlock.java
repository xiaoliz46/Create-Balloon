package com.createballoon.block;

import com.createballoon.CreateBalloon;
import com.createballoon.DebugLog;
import com.createballoon.ModConfigs;
import com.createballoon.item.ColoredBalloonBlockItem;
import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Quaterniondc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HotAirBalloonBlock extends BaseEntityBlock implements BlockSubLevelLiftProvider {
    public static final MapCodec<HotAirBalloonBlock> CODEC = simpleCodec(HotAirBalloonBlock::new);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    private static final VoxelShape SHAPE = Shapes.block();

    public HotAirBalloonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.WHITE));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override protected VoxelShape getBlockSupportShape(BlockState s, BlockGetter l, BlockPos p) { return Shapes.block(); }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        DyeColor color = state.getValue(COLOR);
        for (var s : CreateBalloon.COLORED_BALLOON_ITEMS) {
            var item = s.get();
            if (item instanceof ColoredBalloonBlockItem cbi && cbi.getColor() == color)
                return new ItemStack(item);
        }
        return new ItemStack(CreateBalloon.HOT_AIR_BALLOON_ITEM.get());
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HotAirBalloonBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, CreateBalloon.HOT_AIR_BALLOON_ENTITY.get(),
                (l, p, s, be) -> ((HotAirBalloonBlockEntity) be).tickServer());
    }

    private static final Map<UUID, DyeClickState> dyeClicks = new ConcurrentHashMap<>();

    private static class DyeClickState {
        long lastTime; int count; DyeColor color; BlockPos pos;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        DyeColor newColor = dye.getDyeColor();
        DyeColor current = state.getValue(COLOR);
        if (newColor == current) return ItemInteractionResult.SUCCESS;

        UUID id = player.getUUID();
        long now = System.currentTimeMillis();
        DyeClickState cs = dyeClicks.computeIfAbsent(id, k -> new DyeClickState());
        if (now - cs.lastTime > 800 || cs.color != newColor || !pos.equals(cs.pos)) cs.count = 0;
        cs.lastTime = now; cs.color = newColor; cs.pos = pos; cs.count++;

        int radius = cs.count >= 3 ? 8 : cs.count >= 2 ? 1 : 0;
        Iterable<BlockPos> targets = BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius));
        for (BlockPos p : targets) {
            BlockState bs = level.getBlockState(p);
            if (bs.getBlock() instanceof HotAirBalloonBlock) {
                level.setBlock(p, bs.setValue(COLOR, newColor), 3);
            }
        }
        if (!player.isCreative()) stack.shrink(1);
        return ItemInteractionResult.SUCCESS;
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
    @Override public float sable$getDirectionlessDragScalar() { return 500.0F; }

    @Override
    public void sable$contributeLiftAndDrag(BlockSubLevelLiftProvider.LiftProviderContext ctx, ServerSubLevel subLevel,
                                             Pose3d pose, double scale, Vector3dc vel, Vector3dc angVel,
                                             Vector3d liftForce, Vector3d dragForce,
                                             BlockSubLevelLiftProvider.LiftProviderGroup group) {
        // Forces applied in HotAirBalloonBlockEntity.sable$physicsTick via RigidBodyHandle
    }

    static void physicsTickInline(Object be, ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        if (be instanceof LiftProviderBE lbe) physicsTick(lbe, subLevel, handle, dt);
    }

    static void physicsTick(LiftProviderBE bbe, ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        if (subLevel == null || handle == null) return;
        Level level = subLevel.getLevel();
        if (level == null) return;
        try {
            boolean liftEngaged = bbe.isLiftActive() || bbe.isHoverActive() || bbe.isDescendActive();
            if (!liftEngaged) return;

            MassData massData = subLevel.getMassTracker();
            if (massData == null || massData.isInvalid()) return;
            double mass = massData.getMass();
            int balloons = bbe.getStructureBalloonCount();
            if (mass > ModConfigs.LIFT_FORCE.get() * balloons) return;

            boolean firstSubstep = !bbe.isFrameSkipped();
            ModConfigs.LogLevel logLevel = ModConfigs.LOG_LEVEL.get();
            boolean log = firstSubstep && logLevel != ModConfigs.LogLevel.OFF;

            Vector3d vel = handle.getLinearVelocity(new Vector3d());

            // Kill all forces when nearly inverted — let gyro recover
            if (subLevel.logicalPose().orientation().transform(new Vector3d(0, 1, 0), new Vector3d()).y() < -0.85) return;

            double gy;
            try { gy = DimensionPhysicsData.getGravity(level).y(); } catch (Throwable t) { gy = -11.0; }

            double targetVy = 0.0;
            if (bbe.isLiftActive())         targetVy = ModConfigs.ASCEND_SPEED.get();
            else if (bbe.isDescendActive()) targetVy = -ModConfigs.DESCEND_SPEED.get();

            final double DAMPER  = 25.0;
            double balloonLift   = ModConfigs.LIFT_FORCE.get() * balloons;
            double currentY      = subLevel.logicalPose().position().y();

            double prevY   = bbe.getHoverIntegral();
            double actualVy = (Math.abs(prevY) > 1e-6) ? (currentY - prevY) / dt : 0.0;
            if (firstSubstep) bbe.setHoverIntegral(currentY);

            double errorVy = targetVy - actualVy;
            double brake   = mass * errorVy * DAMPER;
            double smoothMass = bbe.getCurrentThrust();
            if (smoothMass < 1.0 || Math.abs(smoothMass - mass) > mass * 0.2) smoothMass = mass;
            smoothMass += (mass - smoothMass) * 0.05;
            bbe.setCurrentThrust(smoothMass);
            double gyComp  = smoothMass * (-gy);
            if (targetVy == 0 && Math.abs(actualVy) < 0.001) brake = 0;
            double force = brake + gyComp;
            if (force < 0)           force = 0;
            if (force > balloonLift) force = balloonLift;
            double impulseY = force * dt;

            Quaterniondc ori = subLevel.logicalPose().orientation();

            double impulseX = 0;
            double impulseZ = 0;

            // --- Propulsion (W/S) ---
            if (bbe.isMoveForward() != bbe.isMoveBack()) {
                Direction dir = bbe.getForwardDir();
                Vector3d facingLocal = new Vector3d(dir.getStepX(), 0, dir.getStepZ());
                Vector3d facingWorld = ori.transform(facingLocal, new Vector3d());
                double targetSpd = bbe.isMoveForward() ? -ModConfigs.FORWARD_SPEED.get()
                                                   : ModConfigs.BACKWARD_SPEED.get();
                double curSpd = vel.x() * facingWorld.x + vel.z() * facingWorld.z;
                double err = targetSpd - curSpd;
                double absTgt = Math.abs(targetSpd);
                err = Math.max(-absTgt, Math.min(absTgt, err));
                double prop = mass * err * 10.0 * dt;
                double maxProp = mass * absTgt * 2.0 * dt;
                prop = Math.max(-maxProp, Math.min(maxProp, prop));
                impulseX += facingWorld.x * prop;
                impulseZ += facingWorld.z * prop;
            }

            // --- Turning (A/D) ---
            double angImpulse = 0;
            if (bbe.isTurnLeft() != bbe.isTurnRight()) {
                double targetAngVel = Math.toRadians(ModConfigs.TURN_SPEED.get());
                if (bbe.isTurnRight()) targetAngVel = -targetAngVel;
                double curYaw = handle.getAngularVelocity(new Vector3d()).y();
                double angErr = targetAngVel - curYaw;
                double avgInertia = (massData.getInertiaTensor().m00() + massData.getInertiaTensor().m11() + massData.getInertiaTensor().m22()) / 3.0;
                angImpulse = avgInertia * angErr * 5.0 * dt;
                // Clamp: at most 50% yaw reduction, minimum from target speed
                double maxTurn = Math.max(Math.abs(curYaw) * avgInertia * 0.5, avgInertia * Math.abs(targetAngVel) * 0.5);
                if (angImpulse > maxTurn) angImpulse = maxTurn;
                else if (angImpulse < -maxTurn) angImpulse = -maxTurn;
            }

            if (bbe.isDescendActive() && firstSubstep) bbe.setOnGround(Math.abs(vel.y()) < 0.05);

            Vector3d worldImpulse = new Vector3d(impulseX, impulseY, impulseZ);
            Vector3d localImpulse = ori.transformInverse(worldImpulse, new Vector3d());
            handle.applyLinearAndAngularImpulse(localImpulse, new Vector3d(), true);
            if (angImpulse != 0) handle.applyAngularImpulse(new Vector3d(0, angImpulse, 0));

            if (log && (logLevel == ModConfigs.LogLevel.DETAILED || logLevel == ModConfigs.LogLevel.DIAGNOSTIC)) {
                DebugLog.log("LIFT vx=%.3f vz=%.3f vy=%.3f tgt=%.2f imp=%.3f brake=%.1f gy=%.2f n=%d L=%b H=%b D=%b",
                        vel.x(), vel.z(), actualVy, targetVy, impulseY, brake, gy, balloons,
                        bbe.isLiftActive(), bbe.isHoverActive(), bbe.isDescendActive());
            } else if (log && logLevel == ModConfigs.LogLevel.SIMPLE) {
                DebugLog.log("LIFT mass=%.1f vy=%.3f tgt=%.2f imp=%.3f y=%.2f gy=%.2f", mass, actualVy, targetVy, impulseY, currentY, gy);
            }
        } catch (Exception e) {
            DebugLog.log("PHYSICS ERROR: %s", e.toString());
        }
    }
}

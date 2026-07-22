package com.createballoon.block;

import com.createballoon.CreateBalloon;
import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class InflatedWoolBlock extends BaseEntityBlock implements BlockSubLevelLiftProvider {
    public static final MapCodec<InflatedWoolBlock> CODEC = simpleCodec(InflatedWoolBlock::new);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public InflatedWoolBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(COLOR, DyeColor.WHITE));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) { b.add(COLOR); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InflatedWoolBlockEntity(pos, state); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        DyeColor current = state.getValue(COLOR);
        DyeColor to = dye.getDyeColor();
        if (current == to) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(COLOR, to), 3);
            if (!player.isCreative()) stack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override public Direction sable$getNormal(BlockState s) { return Direction.UP; }
    @Override public float sable$getLiftScalar() { return 0.0F; }
    @Override public float sable$getParallelDragScalar() { return 0.0F; }
    @Override public float sable$getDirectionlessDragScalar() { return 500.0F; }
    @Override public void sable$contributeLiftAndDrag(BlockSubLevelLiftProvider.LiftProviderContext ctx, ServerSubLevel sl, Pose3d pose, double scale, Vector3dc vel, Vector3dc angVel, Vector3d lift, Vector3d drag, BlockSubLevelLiftProvider.LiftProviderGroup group) {}
}

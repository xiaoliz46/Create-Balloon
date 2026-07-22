package com.createballoon.item;

import com.createballoon.block.InflatedWoolBlock;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ColoredInflatedWoolBlockItem extends net.minecraft.world.item.BlockItem {
    private final DyeColor color;

    public ColoredInflatedWoolBlockItem(Block block, DyeColor color) {
        super(block, new Properties());
        this.color = color;
    }

    public DyeColor getColor() { return color; }

    @Nullable @Override
    protected BlockState getPlacementState(BlockPlaceContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state == null ? null : state.setValue(InflatedWoolBlock.COLOR, color);
    }

    @Override
    public String getDescriptionId() {
        return "block.create_balloon.inflated_wool_" + color.getSerializedName();
    }
}

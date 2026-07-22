package com.createballoon.item;

import com.createballoon.block.HotAirBalloonBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ColoredBalloonBlockItem extends BlockItem {
    private final DyeColor color;

    public ColoredBalloonBlockItem(Block block, DyeColor color, Item.Properties props) {
        super(block, props);
        this.color = color;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        return state == null ? null : state.setValue(HotAirBalloonBlock.COLOR, color);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return "block.create_balloon.hot_air_balloon_" + color.getSerializedName();
    }

    public DyeColor getColor() {
        return color;
    }
}

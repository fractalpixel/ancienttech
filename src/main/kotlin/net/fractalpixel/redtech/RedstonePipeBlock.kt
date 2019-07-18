package net.fractalpixel.redtech

import net.minecraft.block.AbstractRedstoneGateBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.entity.EntityContext
import net.minecraft.state.StateFactory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.ViewableWorld


class RedstonePipeBlock(settings: Settings): AbstractRedstoneGateBlock(settings) {

    override fun getOutlineShape(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, entityContext: EntityContext): VoxelShape {
        return shape
    }

    override fun canPlaceAt(blockState_1: BlockState?, viewableWorld_1: ViewableWorld?, blockPos_1: BlockPos?): Boolean {
        return true
    }

    override fun getUpdateDelayInternal(blockState: BlockState): Int {
        return 2
    }

    override fun appendProperties(builder: StateFactory.Builder<Block, BlockState>) {
        builder.add(HorizontalFacingBlock.FACING, AbstractRedstoneGateBlock.POWERED)
    }


    companion object {
        val shape = Block.createCuboidShape(0.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    }
}
package net.fractalpixel.ancienttech.utils

import net.fractalpixel.ancienttech.api.nets.redstonepipe.RedstonePipeNetManager
import net.fractalpixel.ancienttech.blocks.RedstoneGateBlock
import net.fractalpixel.ancienttech.blocks.RedstonePipeBlock
import net.minecraft.block.*
import net.minecraft.util.BooleanBiFunction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.World

/*
    Minecraft related utility functions.
 */

/**
 * Check the neighbor in the specified direction from the given position, and returns true if it appears that it may emit a redstone signal.
 */
fun neighbourMayEmitRedstone(world: World, blockPos: BlockPos, direction: Direction): Boolean {
    val neighborPos = blockPos.offset(direction)
    val blockState = world.getBlockState(neighborPos)
    val block = blockState.block

    return when (block) {
        is RedstoneGateBlock -> blockState.get(FacingBlock.FACING) == direction
        is RedstonePipeBlock -> block.isConnectedTowards(world, neighborPos, blockState, block, direction.opposite, RedstonePipeNetManager.REDSTONE_PIPE_NET_TYPE)
        is AbstractRedstoneGateBlock -> blockState.get(AbstractRedstoneGateBlock.FACING) == direction
        is ObserverBlock -> blockState.get(ObserverBlock.FACING) == direction
        else -> blockState.emitsRedstonePower()
    }
}

/**
 * Check the neighbor in the specified direction from the given position, and returns true if it currently emits a redstone signal stronger than zero.
 */
fun neighbourCurrentlyEmitsRedstone(world: World, blockPos: BlockPos, direction: Direction): Boolean {
    return neighbourRedstoneStrength(world, blockPos, direction) > 0
}

/**
 * Check the redstone strength of the neighbor in the specified direction from the given position.
 */
fun neighbourRedstoneStrength(world: World, blockPos: BlockPos, direction: Direction): Int {
    return world.getEmittedRedstonePower(blockPos.offset(direction), direction)
}

/**
 * Creates a cuboid extending from the specified edge of a block towards its center.
 */
fun createEdgeCuboidShape(direction: Direction, diam: Int, length: Int = 8 - diam/2): VoxelShape {
    val c1 = 8.0 - diam / 2.0
    val c2 = 8.0 + diam / 2.0
    val l1 = 0.0 + length
    val l2 = 16.0 - length
    val x1 = if (direction.offsetX < 0) 0.0 else if (direction.offsetX > 0) l2 else c1
    val y1 = if (direction.offsetY < 0) 0.0 else if (direction.offsetY > 0) l2 else c1
    val z1 = if (direction.offsetZ < 0) 0.0 else if (direction.offsetZ > 0) l2 else c1
    val x2 = if (direction.offsetX > 0) 16.0 else if (direction.offsetX < 0) l1 else c2
    val y2 = if (direction.offsetY > 0) 16.0 else if (direction.offsetY < 0) l1 else c2
    val z2 = if (direction.offsetZ > 0) 16.0 else if (direction.offsetZ < 0) l1 else c2
    return Block.createCuboidShape(x1, y1, z1, x2, y2, z2)
}

/**
 * Create a cube of the specified size centered in a block.
 */
fun createCenteredCuboidShape(sizeX: Int, sizeY: Int = sizeX, sizeZ: Int = sizeX): VoxelShape {
    val x1 = 8.0 - sizeX/2
    val y1 = 8.0 - sizeY/2
    val z1 = 8.0 - sizeZ/2
    return Block.createCuboidShape(
            x1,
            y1,
            z1,
            x1 + sizeX,
            y1 + sizeY,
            z1 + sizeZ)
}

/**
 * Returns new VoxelShape that is the combination of this shape and the specified other shape.
 * Optionally specify the logical function to use when combining the voxels in the shapes.
 * If [simplify] is true (the default), the resulting shape is simplified before returned.
 */
fun VoxelShape.combine(other: VoxelShape,
                       cominationFunction: BooleanBiFunction = BooleanBiFunction.OR,
                       simplify: Boolean = true): VoxelShape {
    val combinedShape = VoxelShapes.combine(this, other, cominationFunction)
    return if (simplify) combinedShape.simplify()
    else return combinedShape
}


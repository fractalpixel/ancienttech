package net.fractalpixel.ancienttech.utils

import net.fractalpixel.ancienttech.RedstoneGateBlock
import net.minecraft.block.*
import net.minecraft.state.property.Property
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.ViewableWorld
import net.minecraft.world.World

/*
    Minecraft related utility functions.
 */

/**
 * Check the neighbor in the specified direction from the given position, and returns true if it appears that it may emit a redstone signal.
 */
fun neighbourMayEmitRedstone(viewableWorld: ViewableWorld, blockPos: BlockPos, direction: Direction): Boolean {
    val neighborPos = blockPos.offset(direction)
    val blockState = viewableWorld.getBlockState(neighborPos)
    val block = blockState.block

    return when (block) {
        is RedstoneGateBlock -> blockState.get(FacingBlock.FACING) == direction
        is AbstractRedstoneGateBlock -> blockState.get(AbstractRedstoneGateBlock.FACING) == direction
        is ObserverBlock -> blockState.get(ObserverBlock.FACING) == direction
        else -> blockState.emitsRedstonePower()
    }
}

/**
 * Check the neighbor in the specified direction from the given position, and returns true if it currently emits a redstone signal stronger than zero.
 */
fun neighbourCurrentlyEmitsRedstone(world: World, blockPos: BlockPos, direction: Direction): Boolean {
    val neighborPos = blockPos.offset(direction)
    val blockState = world.getBlockState(neighborPos)
    val block= blockState.block
    return if (block === Blocks.REDSTONE_BLOCK) {
        true
    } else {
        if (block === Blocks.REDSTONE_WIRE) {
            blockState.get(RedstoneWireBlock.POWER) > 0
        } else {
            world.getEmittedRedstonePower(neighborPos, direction) > 0
        }
    }
}



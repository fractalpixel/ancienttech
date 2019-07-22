package net.fractalpixel.ancienttech.api.redstoneports

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * Interface for blocks that provide input, output, or bi-directional redstone connections in different directions.
 */
interface RedstoneConnectionBlock {

    /**
     * Whether the block outputs redstone in the specified direction, reads redstone signals from that direction, or
     * does both or neither.
     */
    fun getRedstonePortDirection(side: Direction, world: World, pos: BlockPos, blockState: BlockState): PortDirection

    /**
     * Instruct block to connect in the specified direction, if possible.
     * Returns the updated block state if connected, null if not.
     * The world is also updated with the new state.
     */
    fun connectTowards(direction: Direction, world: World, pos: BlockPos, blockState: BlockState): BlockState?

}
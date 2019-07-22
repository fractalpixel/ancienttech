package net.fractalpixel.ancienttech.api.redstoneports

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * Exposes redstone ports of some block.
 * Register implementations of this with the [RedstonePortRegistry] to provide better interoperability with your custom
 * redstone using blocks and the AncientTech mod.
 */
interface RedstonePortExposer<T: Block> {

    /**
     * Whether the block outputs redstone in the specified direction, reads redstone signals from that direction, or
     * does both or neither.
     */
    fun getRedstonePortDirection(side: Direction, world: World, blockPos: BlockPos, blockState: BlockState, block: T): PortDirection

    /**
     * Instruct block to connect in the specified direction, if possible.
     * Returns the updated block state if connected, null if not.
     * The world is also updated with the new state.
     */
    fun connectTowards(direction: Direction, world: World, pos: BlockPos, blockState: BlockState, block: T): BlockState?

}
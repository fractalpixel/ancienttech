package net.fractalpixel.ancienttech.api.nets

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * Interface that blocks that can be part of a [Net] should implement, to tell in which directions they are connected.
 */
interface NetworkedBlock {

    /**
     * Should return true if the specified [netType] in the specified block is connected in the specified direction.
     * The netType could be used in blocks that connect many nets to distinguish between them.
     *
     * The blocks on both sides need to return true with this method for there to be a connection between them.
     */
    fun isConnectedTowards(world: World, pos: BlockPos, blockState: BlockState, block: Block, neighborDirection: Direction, netType: String): Boolean

}
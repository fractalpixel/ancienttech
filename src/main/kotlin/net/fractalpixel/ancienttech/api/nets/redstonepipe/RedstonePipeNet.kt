package net.fractalpixel.ancienttech.api.nets.redstonepipe

import net.fractalpixel.ancienttech.api.nets.Net
import net.fractalpixel.ancienttech.blocks.RedstonePipeBlock
import net.fractalpixel.ancienttech.utils.max
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.max

class RedstonePipeNet: Net(RedstonePipeNetManager) {

    /**
     * The redstone power level of this net.
     */
    var redstonePower: Int = 0
        private set

    /**
     * Update all member blocks with the new power level
     */
    fun setPower(newPowerLevel: Int) {
        pipePowerUpdateOngoing = true
        redstonePower = newPowerLevel
        forEachMember { world, blockPos, blockPosAsLong ->
            val blockState = world.getBlockState(blockPos)
            val block = blockState.block
            if (block is RedstonePipeBlock) {
                block.setPower(newPowerLevel, world, blockPos, blockState)
            }
        }
        pipePowerUpdateOngoing = false
    }

    /**
     * Calculate the maximum outside power level of all blocks in the net.
     */
    fun calculateMaxOutsidePower(): Int {
        var maxPower = 0
        forEachMember { world, blockPos, blockPosAsLong ->
            val blockState = world.getBlockState(blockPos)
            val block = blockState.block
            if (block is RedstonePipeBlock) {
                maxPower = maxPower max block.calculateOutsideRedstoneStrength(world, blockPos, blockState)
            }
        }
        return maxPower
    }

    override fun onNetMerged(other: Net) {
        val newPower = max(redstonePower, (other as RedstonePipeNet).redstonePower)
        if (newPower != redstonePower) {
            setPower(newPower)
        }
    }

    override fun onMemberAdded(world: World, pos: Long) {
        // Determine power of the added member
        val blockPos = BlockPos.fromLong(pos)
        val blockState = world.getBlockState(blockPos)
        val block = blockState.block
        if (block is RedstonePipeBlock) {
            val addedBlockPower = block.calculateOutsideRedstoneStrength(world, blockPos, blockState)

            // Compare with current power, and update the party that needs it
            if (addedBlockPower < redstonePower) {
                // Update the block
                block.setPower(redstonePower, world, blockPos, blockState)
            }
            else if (addedBlockPower > redstonePower) {
                // Update network
                setPower(addedBlockPower)
            }
        }
    }

    companion object {
        // Used to manage updates to power levels of adjacent pipes without cascading updates
        var pipePowerUpdateOngoing = false
            private set
    }
}
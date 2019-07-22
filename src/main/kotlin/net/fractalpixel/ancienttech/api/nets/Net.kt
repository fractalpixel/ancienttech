package net.fractalpixel.ancienttech.api.nets

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * A net represents a set of connected blocks, and the data/state they share with the connection.
 * Used for connected redstone bamboo pipes to propagate signals immediately to all connected pipes.
 *
 * Could also be used for other purposes such power or information transfer.
 *
 * Currently the nets are not persisted, they are discovered on the fly if a block that needs a net does not have it,
 * by searching for connected neighboring blocks.  If a block that is part of a net is broken, the net is also removed,
 * and rediscovered by searching neighbors the next time a block needs to use it.
 */
abstract class Net(val netManager: NetManager<*>) {

    /**
     * Contains locations of all the blocks that are members of this net (for each world/dimension)
     * Do not edit this directly from calling code.
     */
    val memberBlocks = HashMap<World, LongSet>()

    /**
     * True if this net contains the specified block.
     */
    fun contains(world: World, pos: BlockPos): Boolean {
        return contains(world, pos.asLong())
    }

    /**
     * True if this net contains the specified block (location given as a long pos, as acquired with pos.asLong()).
     */
    fun contains(world: World, longPos: Long): Boolean {
        return memberBlocks[world]?.contains(longPos) ?: false
    }

    /**
     * Number of members in the net
     */
    fun getSize(): Int {
        return memberBlocks.values.sumBy { it.size }
    }

    /**
     * Call the provided visitor for each block in this Net.
     * Note that the BlockPos is reused, do not store direct references to it!  If you need to store it, take a copy of it.
     */
    inline fun forEachMember(visitor: (World, BlockPos, blockPosAsLong: Long) -> Unit) {
        val tempPos = BlockPos.Mutable()
        for (entry in memberBlocks) {
            val world = entry.key
            for (posAsLong in entry.value.stream()) {
                tempPos.setFromLong(posAsLong)
                visitor(world, tempPos, posAsLong)
            }
        }
    }

    /**
     * Include a block at the specified position (expressed as a long) in the specified world.
     */
    open fun addMember(world: World, longPos: Long) {
        getWorldSpecificMemberSet(world).add(longPos)
        onMemberAdded(world, longPos)
    }

    /**
     * Gets or creates a world -specific member set.
     * The set contains the positions of the blocks in the world in long format.
     */
    fun getWorldSpecificMemberSet(world: World): LongSet {
        return memberBlocks.getOrPut(world) { LongOpenHashSet() }
    }

    /**
     * Removes all members from the net.
     */
    fun clear() {
        memberBlocks.clear()
    }

    /**
     * Add the members of another net to this net
     */
    open fun <T: Net> addNet(otherNet: T) {
        for (entry in otherNet.memberBlocks) {
            getWorldSpecificMemberSet(entry.key).addAll(entry.value)
        }
    }

    /**
     * Called when a new block is added to this net.
     */
    open fun onMemberAdded(world: World, pos: Long) {}

    /**
     * Called when the [other] net is about to be merged into this net.
     */
    open fun onNetMergeStarting(other: Net) {}

    /**
     * Called when the [other] net has been merged into this net.
     * The other net still retains a list of the blocks it contained, but they have also already been added to this net.
     */
    open fun onNetMerged(other: Net) {}

    /**
     * Called when this net is about to be invalidated due to a removed block
     */
    open fun onNetInvalidated(worldOfRemovedBlock: World, removedBlockPos: BlockPos)  {}


}
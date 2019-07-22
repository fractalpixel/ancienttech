package net.fractalpixel.ancienttech.api.nets

import it.unimi.dsi.fastutil.longs.*
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.lang.IllegalStateException

/**
 * Manages the nets for some type of blocks that can be connected together.
 *
 * This allows fast queries for all blocks connected to a specific block through a specific network.
 *
 * Blocks in a net need to always call [invalidateNetAt] when they are broken or otherwise removed.
 */
abstract class NetManager<T: Net> {

    protected val cache = HashMap<World, Long2ObjectMap<T>>()

    // Used temporarily when building a net.  Kept as fields to avoid re-allocating the memory for them again and again.
    private var unhandledPositions: LongSet = LongOpenHashSet()
    private var newUnhandledPositions: LongSet = LongOpenHashSet()
    private var netsToMerge: MutableSet<T> = ObjectOpenHashSet()
    private var neighborNets: MutableSet<T> = ObjectOpenHashSet()

    /**
     * Returns an id for the type of networks managed.  E.g. "redstonePipeNetwork"
     */
    abstract val netType: String

    /**
     * Gets the net that the specified block is connected to.
     * If it is unconnected to any other blocks, a net containing only itself is returned.
     * Note that this assumes the block at the specified position is a block with this kind of net.
     */
    open fun getNet(world: World, pos: BlockPos): T {
        // Get world/dimension specific cache
        val worldSpecificCache = getWorldSpecificCache(world)

        // Get net at position from cache
        return worldSpecificCache.getOrPut(pos.asLong()) {
            // Net not found, build it and add it to the map
            buildNet(world, pos)
        }
    }

    /**
     * Called by new blocks that have been recently added to the world.
     * Adds the block to the net, checks if it merges two nets, and updates things.
     * Returns the net it belongs to after all updates.
     */
    open fun addToNet(world: World, pos: BlockPos): T {
        // Find any neighboring nets
        val blockState = world.getBlockState(pos)
        val block = blockState.block
        neighborNets.clear()
        for (direction in Direction.values()) {
            if (hasConnectionToNeighbor(world, pos, blockState, block, direction, netType)) {
                neighborNets.add(getNet(world, pos.offset(direction)))
            }
        }

        val netAddedTo = when (neighborNets.size) {
            1 -> {
                // Add to one existing net, straightforward case
                val net = neighborNets.first()
                val longPos = pos.asLong()
                getWorldSpecificCache(world)[longPos] = net
                net.addMember(world, longPos)
                net
            }
            else -> {
                // Connect several nets together or create a new net for a lone block, use getNet to build the network
                getNet(world, pos)
            }
        }

        neighborNets.clear()

        return netAddedTo
    }

    /**
     * (re)build the net, starting from the specified position, by following links in this network.
     */
    open fun buildNet(world: World, pos: BlockPos): T {
        // Create net
        val net = createNet()

        // As there can be no neighbor blocks in other dimensions, all members of the net discovered by this method will be in just one world
        val worldSpecificCache = getWorldSpecificCache(world)

        // Reuse datastructures to avoid memory trashing
        unhandledPositions.clear()

        // Add startpos, and set the net for it
        val longPos = pos.asLong()
        unhandledPositions.add(longPos)

        // Expand outwards, include all connected blocks
        val tempPos = BlockPos.Mutable()
        netsToMerge.clear()
        while (unhandledPositions.isNotEmpty()) {
            // Collect new positions to handle in this
            newUnhandledPositions.clear()

            // Loop current unhandled positions
            unhandledPositions.stream().forEach {

                // Check if this is already connected to a net
                val existingNet = worldSpecificCache[it]
                if (existingNet != null) {
                    // Already connected to another net, so merge that net and the new net at the end
                    netsToMerge.add(existingNet)
                } else {
                    // Include this block in the new net
                    worldSpecificCache[it] = net
                    net.addMember(world, it)

                    // Handle connected neighbours next round
                    tempPos.setFromLong(it)
                    val blockState = world.getBlockState(tempPos)
                    val block = blockState.block
                    for (direction in Direction.values()) {
                        if (hasConnectionToNeighbor(world, tempPos, blockState, block, direction, netType)) {
                            newUnhandledPositions.add(tempPos.offset(direction).asLong())
                        }
                    }
                }
            }

            // Swap buffers
            val t = unhandledPositions
            unhandledPositions = newUnhandledPositions
            newUnhandledPositions = t
        }

        unhandledPositions.clear()
        newUnhandledPositions.clear()

        // Merge all connected nets if necessary
        return if (netsToMerge.isNotEmpty()) {
            netsToMerge.add(net)
            mergeNets(netsToMerge)
        } else {
            net
        }
    }

    open fun mergeNets(nets: Set<T>): T {
        // Add the blocks in the smaller nets to the largest net
        val largestNet = nets.maxBy { it.getSize() } ?: throw IllegalStateException("Could not find largest net, provided number of nets: ${nets.size}.  This should not happen.")
        for (net in nets) {
            if (net != largestNet) {
                // Notify target net about the beginning merge
                largestNet.onNetMergeStarting(net)

                // Update net associations
                net.forEachMember { world, blockPos, blockPosAsLong ->
                    getWorldSpecificCache(world)[blockPosAsLong] = largestNet
                }

                // Update net
                largestNet.addNet(net)

                // Notify target net about the finished merge
                largestNet.onNetMerged(net)
            }
        }
        return largestNet
    }

    /**
     * Create a new, empty instance of the Net type that this manager manages.
     */
    abstract fun createNet(): T


    /**
     * Returns true if the specified block has a mutual connection to the neighbor in the specified direction
     */
    open fun hasConnectionToNeighbor(world: World, pos: BlockPos, blockState: BlockState, block: Block, direction: Direction, netType: String): Boolean {
        val neighborPos = pos.offset(direction)
        val neighborState = world.getBlockState(neighborPos)
        val neighborBlock = neighborState.block
        return isConnectedTowards(world, pos, blockState, block, direction, netType) &&
               isConnectedTowards(world, neighborPos, neighborState, neighborBlock, direction.opposite, netType)
    }

    /**
     * Returns true if the block at the specified position is connected towards the specified direction for the specified [netType].
     * Override in implementing classes if necessary.  By default checks if the block is of type [NetworkedBlock], and then follows those links.
     * Should return false if there is no block at the position, or it is otherwise an invalid type of block.
     */
    open fun isConnectedTowards(world: World, pos: BlockPos, blockState: BlockState, block: Block, direction: Direction, netType: String): Boolean {
        return if (block is NetworkedBlock) block.isConnectedTowards(world, pos, blockState, block, direction, netType)
        else false
    }

    /**
     * Notify that the block at the specified position is no longer connected to the net, possibly
     * cutting it into pieces.  Call this when a networked block is broken or otherwise removed from
     * the network.
     *
     * The network at that position will be removed, and next time it is needed it will be rebuilt.
     */
    open fun invalidateNetAt(world: World, pos: BlockPos) {
        // Get world/dimension specific cache
        val worldSpecificCache = getWorldSpecificCache(world)

        // Get net at position from cache
        val combinedPos = pos.asLong()
        val net = worldSpecificCache.get(combinedPos)
        if (net != null) {
            // Notify net about the invalidation
            net.onNetInvalidated(world, pos)

            // Remove all blocks that are part of the net
            net.forEachMember { memberWorld, memberPos, memberLongPos ->
                cache[memberWorld]?.remove(memberLongPos)
            }
            net.clear()
        }
    }

    open fun getWorldSpecificCache(world: World): Long2ObjectMap<T> {
        return cache.getOrPut(world, { Long2ObjectOpenHashMap() })
    }

    /**
     * Gets the net that the specified block is connected to, or null if there is currently no up-to-date net at the location.
     * This is not recommended for normal use as the cache may have been invalidated, instead use getNet() to always get an up to date net.
     */
    open fun getNetOrNull(world: World, pos: BlockPos): T? {
        // Get world/dimension specific cache
        val worldSpecificCache = cache[world] ?: return null

        // Get net at position from cache
        return worldSpecificCache.get(pos.asLong())
    }


}
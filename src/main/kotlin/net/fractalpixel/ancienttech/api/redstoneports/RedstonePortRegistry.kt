package net.fractalpixel.ancienttech.api.redstoneports

import net.minecraft.block.AbstractRedstoneGateBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ObserverBlock
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

/**
 * Utility for retrieving information about redstone input or output on different sides of
 * blocks in the world, for connecting neighboring redstone pipes to them.
 *
 * You can either register a [RedstonePortExposer] for a block type, or implement
 * the [RedstoneConnectionBlock] interface for the blocks you want to make compatible with AncientTech.
 */
object RedstonePortRegistry {

    private val registry = LinkedHashMap<Identifier, RedstonePortExposer<*>>()

    init {
        registerDefaultBlocks()
    }

    /**
     * Register a [RedstonePortExposer] for a block type, it is used to query which sides of the block
     * normally emit or accept redstone signals, for automatically connecting neighboring pipes.
     * Alternatively make the block implement the [RedstoneConnectionBlock] interface.
     */
    @JvmStatic
    fun <T: Block>registerPortExposer(blockType: Identifier, portExposer: RedstonePortExposer<T>) {
        registry[blockType] = portExposer
    }

    /**
     * Retrieve the port exposer for the specified blocktype, returns null if none registered.
     *
     * Usually you'd use the [getRedstonePortDirection], [getRedstoneOutputPower] and similar methods
     * in this object instead of using this method.
     */
    @JvmStatic
    fun getPortExposer(blockType: Identifier): RedstonePortExposer<*>? = registry[blockType]

    /**
     * Retrieve the port exposer for the specified block, or null if not available.
     *
     * This method is mostly intended for internal use of this object.
     */
    @JvmStatic
    fun getPortExposer(block: Block): RedstonePortExposer<*>? = getPortExposer(Registry.BLOCK.getId(block))


    /**
     * Whether the block outputs redstone in the specified direction, reads redstone signals from that direction, or
     * does both or neither.
     */
    @JvmStatic
    @JvmOverloads
    fun getRedstonePortDirection(side: Direction, world: World, blockPos: BlockPos, blockState: BlockState = world.getBlockState(blockPos)): PortDirection {
        val block = blockState.block

        // Look for a registered handler first, to allow overrides of default behaviour
        val portExposer = getPortExposer(block) as? RedstonePortExposer<Block>
        if (portExposer != null) {
            return portExposer.getRedstonePortDirection(side, world, blockPos, blockState, block)
        }

        // Check block type
        return when (block) {
            is RedstoneConnectionBlock -> block.getRedstonePortDirection(side, world, blockPos, blockState)
            is AbstractRedstoneGateBlock -> getRedstoneGateBlockPortDirection(side, blockState)
            else -> PortDirection.NONE  // No information found
        }
    }

    /**
     * Instruct block to connect in the specified direction, if possible.
     * Returns the updated block state if connected, null if not.
     */
    @JvmStatic
    @JvmOverloads
    fun connectTowards(direction: Direction, world: World, blockPos: BlockPos, blockState: BlockState = world.getBlockState(blockPos)): BlockState? {
        val block = blockState.block

        // Look for a registered handler first, to allow overrides of default behaviour
        val portExposer = getPortExposer(block) as? RedstonePortExposer<Block>
        if (portExposer != null) {
            return portExposer.connectTowards(direction, world, blockPos, blockState, block)
        }

        // Check block type
        return when (block) {
            is RedstoneConnectionBlock -> block.connectTowards(direction, world, blockPos, blockState)
            else -> null // No information found
        }
    }

    /**
     * Checks for connections to or from the specified block that have an active connection on one side, and an inactive but
     * potential matching connection on the other, and connects those.  Returns the updated blockstate if the state was changed, null if not.
     * The world is also updated with the new state.
     */
    fun connectHalfConnections(world: World, blockPos: BlockPos, blockState: BlockState = world.getBlockState(blockPos)): BlockState? {
        var updatedState = blockState
        var changesMade = false
        for (direction in Direction.values()) {
            val portTypeTowardsNeighbor = getRedstonePortDirection(direction, world, blockPos, updatedState)

            if (portTypeTowardsNeighbor != PortDirection.NONE) {
                val neighborPos = blockPos.offset(direction)
                val neighborState = world.getBlockState(neighborPos)
                val portTypeFromNeighbor = getRedstonePortDirection(direction.opposite, world, neighborPos, neighborState)

                if (portTypeFromNeighbor != PortDirection.NONE &&
                    portTypeFromNeighbor.active != portTypeTowardsNeighbor.active &&
                    portTypeTowardsNeighbor.canConnectTo(portTypeFromNeighbor)) {
                    // We should create a connection
                    val newState = if (!portTypeTowardsNeighbor.active) {
                        // Connect to neighbor
                        connectTowards(direction, world, blockPos, updatedState)
                    } else {
                        // Connect from neighbor
                        connectTowards(direction.opposite, world, neighborPos, neighborState)
                    }

                    if (newState != null) {
                        changesMade = true
                        updatedState = newState
                    }
                }
            }
        }

        return if (changesMade) updatedState else null
    }

    /**
     * Register various vanilla blocks that provide signals in specific directions.
     * Automatically called when this object is initialized.
     */
    @JvmStatic
    fun registerDefaultBlocks() {
        val minecraftNamespace = "minecraft"

        // The observer block has a clear output direction, if pipes are placed behind it one could expect them to want to be connected.
        registerPortExposer(Identifier(minecraftNamespace, "observer"), object : RedstonePortExposer<ObserverBlock> {
            override fun getRedstonePortDirection(side: Direction, world: World, blockPos: BlockPos, blockState: BlockState, block: ObserverBlock): PortDirection {
                return if (side == blockState[ObserverBlock.FACING].opposite) PortDirection.OUT else PortDirection.NONE
            }

            override fun connectTowards(direction: Direction, world: World, pos: BlockPos, blockState: BlockState, block: ObserverBlock): BlockState? {
                return null
            }
        })

        // All other vanilla blocks (except gates that are handled below) activate many neighboring blocks, or accept activation from many neighboring blocks,
        // so we don't want pipes to automatically connect to them all.
    }

    /**
     * Determine the type of port a redstone gate has in the specified direction.
     * Used internally, probably no need to call this function directly.
     *
     * Assumes the front end is output and the back end is input, and the other faces are not interested in connecting to pipes,
     * as the inputs from the sides require other redstone gates or redstone wire / blocks.
     */
    @JvmStatic
    fun getRedstoneGateBlockPortDirection(side: Direction, blockState: BlockState): PortDirection {
        return when (blockState[AbstractRedstoneGateBlock.FACING]) {
            side -> PortDirection.OUT
            side.opposite -> PortDirection.IN
            else -> PortDirection.NONE
        }
    }



}
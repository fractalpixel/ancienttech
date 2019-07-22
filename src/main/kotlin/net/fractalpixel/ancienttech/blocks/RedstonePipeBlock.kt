package net.fractalpixel.ancienttech.blocks

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fractalpixel.ancienttech.api.nets.NetworkedBlock
import net.fractalpixel.ancienttech.api.nets.redstonepipe.RedstonePipeNet
import net.fractalpixel.ancienttech.api.nets.redstonepipe.RedstonePipeNetManager
import net.fractalpixel.ancienttech.api.nets.redstonepipe.RedstonePipeNetManager.REDSTONE_PIPE_NET_TYPE
import net.fractalpixel.ancienttech.api.redstoneports.PortDirection
import net.fractalpixel.ancienttech.api.redstoneports.RedstoneConnectionBlock
import net.fractalpixel.ancienttech.api.redstoneports.RedstonePortRegistry
import net.fractalpixel.ancienttech.utils.*
import net.minecraft.block.Block
import net.minecraft.block.BlockPlacementEnvironment
import net.minecraft.block.BlockState
import net.minecraft.block.FacingBlock
import net.minecraft.entity.EntityContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.particle.DustParticleEffect
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateFactory
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.IWorld
import net.minecraft.world.World
import java.util.*

class RedstonePipeBlock(settings: Settings): Block(settings), RedstoneConnectionBlock, NetworkedBlock {

    // Called on client and server
    override fun getPlacementState(itemPlacementContext: ItemPlacementContext): BlockState {
        // Connect to the direction placed
        val direction = itemPlacementContext.side.opposite
        val state = defaultState
                .with(POWER, 0)
                .with(CONNECT_DOWN, direction == Direction.DOWN)
                .with(CONNECT_UP, direction == Direction.UP)
                .with(CONNECT_WEST, direction == Direction.WEST)
                .with(CONNECT_EAST, direction == Direction.EAST)
                .with(CONNECT_NORTH, direction == Direction.NORTH)
                .with(CONNECT_SOUTH, direction == Direction.SOUTH)

        // Determine current strength
        val power = calculateOutsideRedstoneStrength(itemPlacementContext.world, itemPlacementContext.blockPos, state)
        return state.with(POWER, power)
    }

    override fun onPlaced(world: World, blockPos: BlockPos, blockState: BlockState, livingEntity_1: LivingEntity?, itemStack_1: ItemStack?) {
        // Add to network
        RedstonePipeNetManager.addToNet(world, blockPos)

        // Connect
        val updatedState = RedstonePortRegistry.connectHalfConnections(world, blockPos, blockState) ?: blockState
    }

    // Called after block added(?) on server
    override fun onBlockAdded(blockState: BlockState, world: World, blockPos: BlockPos, blockState_2: BlockState?, boolean_1: Boolean) {
        // Add to network
//        RedstonePipeNetManager.addToNet(world, blockPos)
    }

    override fun onBlockRemoved(blockState: BlockState, world: World, blockPos: BlockPos, blockState_2: BlockState, boolean_1: Boolean) {
        // Does this method get called when a block of this type is removed?  If so, invalidate any net we have at this position:
        RedstonePipeNetManager.invalidateNetAt(world, blockPos)
    }


    override fun neighborUpdate(blockState: BlockState, world: World, blockPos: BlockPos, block: Block, blockPos2: BlockPos, boolean_1: Boolean) {
        // If some other pipe started an update, let them carry it out
        if (RedstonePipeNet.pipePowerUpdateOngoing) return

        // Update input connections from neighbours if necessary
        val updatedState = RedstonePortRegistry.connectHalfConnections(world, blockPos, blockState) ?: blockState
        //val (state, connectionsChanged) = updateNeighborConnections(world, blockPos, blockState)

        // Check if powered state changed
        val previousPower = updatedState[POWER]
        val newPower = calculateOutsideRedstoneStrength(world, blockPos, updatedState)
        if (previousPower != newPower) {
            // Power changed, update connected pipes
            val net = RedstonePipeNetManager.getNet(world, blockPos)
            if (newPower > previousPower) {
                // We can just raise power for everyone
                net.setPower(newPower)
            } else {
                // We need to check the input power to all pipe blocks in the network and take the max, update if lower
                val maxOutsidePower = net.calculateMaxOutsidePower()
                if (maxOutsidePower != previousPower) {
                    // We need to adjust the power level for all pipes in the net
                    net.setPower(maxOutsidePower)
                }
            }
        }
    }

    override fun hasSidedTransparency(blockState: BlockState): Boolean {
        return true
    }

    override fun canPlaceAtSide(blockState: BlockState, blockView_1: BlockView, blockPos_1: BlockPos, blockPlacementEnvironment_1: BlockPlacementEnvironment): Boolean {
        return false
    }

    override fun isSimpleFullBlock(blockState: BlockState, blockView: BlockView, blockPos: BlockPos): Boolean {
        return false
    }

    override fun rotate(blockState: BlockState, blockRotation: BlockRotation): BlockState {
        return blockState.with(FacingBlock.FACING, blockRotation.rotate(blockState[FacingBlock.FACING]))
    }

    override fun mirror(blockState: BlockState, blockMirror: BlockMirror): BlockState {
        return blockState.with(FacingBlock.FACING, blockMirror.apply(blockState[FacingBlock.FACING]))
    }

    override fun getStrongRedstonePower(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, direction: Direction): Int {
        return getWeakRedstonePower(blockState, blockView, blockPos, direction)
    }

    override fun getWeakRedstonePower(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, direction: Direction): Int {
        return if (blockState[connectProperty(direction.opposite)]) blockState[POWER]
               else 0
    }

    override fun emitsRedstonePower(blockState: BlockState): Boolean {
        return true
    }

    override fun isConnectedTowards(world: World, pos: BlockPos, blockState: BlockState, block: Block, neighborDirection: Direction, netType: String): Boolean {
        // Check if we are connected in the specified direction, and the netType is correct
        return netType == REDSTONE_PIPE_NET_TYPE &&
               blockState[connectProperty(neighborDirection)]
    }

    override fun getRedstonePortDirection(side: Direction, world: World, pos: BlockPos, blockState: BlockState): PortDirection {
        return if (blockState[connectProperty(side)]) PortDirection.IN_OUT else PortDirection.POTENTIAL_IN_OUT
    }

    override fun connectTowards(direction: Direction, world: World, pos: BlockPos, blockState: BlockState): BlockState? {
        val connectProperty = connectProperty(direction)
        return if (!blockState[connectProperty]) {
            val newState = blockState.with(connectProperty, true)
            world.setBlockState(pos, newState)
            newState
        }
        else null
    }

    /**
     * Sets power level for this pipe block.  Called from the net it belongs to when it changes power.
     */
    fun setPower(newPower: Int, world: World, blockPos: BlockPos, blockState: BlockState) {
        val currentPower = blockState[POWER]
        if (currentPower != newPower) {
            world.setBlockState(blockPos, blockState.with(POWER, newPower))
        }
    }

    /*
    /**
     * Determine the directions that there should be pipes to.
     * Returns a pair with the new block state and a boolean if there was any changes
     * (null if the block broke due to no connections.)
     */
    private fun updateNeighborConnections(world: World, blockPos: BlockPos, blockState: BlockState): Pair<BlockState?, Boolean> {

        // Check each direction
        var changed = false
        var connections = 0
        var result = blockState
        for (direction in Direction.values()) {
            val connectDirectionProperty = connectProperty(direction)
            val isConnected = blockState[connectDirectionProperty]
            val neighborPos = blockPos.offset(direction)
            if (!isConnected) {
                // If there is an active port from the neighbour here, we want to connect to it
                if (RedstonePortRegistry.getRedstonePortDirection(direction.opposite, world, neighborPos).active) {
                    result = result.with(connectDirectionProperty, true)
                    connections++
                    changed = true
                }
            } else {
                // Remove connection if it leads to air
                if (world.isAir(neighborPos)) {
                    result = result.with(connectDirectionProperty, false)
                    changed = true
                } else {
                    // Count the connection
                    connections++
                }
            }
        }

        // If last connection removed, break the block
        return if (connections <= 0) {
            // Remember to tell the net it is no longer valid
            RedstonePipeNetManager.invalidateNetAt(world, blockPos)

            // Break block
            world.breakBlock(blockPos, false)

            Pair(null, true)
        }
        else {
            Pair(result, changed)
        }
    }

     */

    /**
     * The redstone strength affecting this block from neighbors that are not other redstone pipes.
     */
    fun calculateOutsideRedstoneStrength(world: World, blockPos: BlockPos, blockState: BlockState): Int {
        // Get highest power of any non-pipe neighbour
        var strength = 0
        for (direction in Direction.values()) {
            if (blockState[connectProperty(direction)]) {
                val neighborPos = blockPos.offset(direction)
                val neighborState = world.getBlockState(neighborPos)
                if (neighborState.block !is RedstonePipeBlock) {
                    strength = strength max world.getEmittedRedstonePower(neighborPos, direction)
                }
            }
        }
        return strength
    }


    private fun connectProperty(direction: Direction): BooleanProperty {
        return when(direction) {
            Direction.UP -> CONNECT_UP
            Direction.DOWN -> CONNECT_DOWN
            Direction.NORTH -> CONNECT_NORTH
            Direction.SOUTH -> CONNECT_SOUTH
            Direction.WEST -> CONNECT_WEST
            Direction.EAST -> CONNECT_EAST
        }
    }

    override fun getOutlineShape(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, entityContext: EntityContext): VoxelShape {

        // TODO: Cache or pre-generate these

        var shape = SHAPE_CENTER
        if (blockState[CONNECT_UP]) shape = shape.combine(SHAPE_FACING_UP)
        if (blockState[CONNECT_DOWN]) shape = shape.combine(SHAPE_FACING_DOWN)
        if (blockState[CONNECT_NORTH]) shape = shape.combine(SHAPE_FACING_NORTH)
        if (blockState[CONNECT_SOUTH]) shape = shape.combine(SHAPE_FACING_SOUTH)
        if (blockState[CONNECT_WEST]) shape = shape.combine(SHAPE_FACING_WEST)
        if (blockState[CONNECT_EAST]) shape = shape.combine(SHAPE_FACING_EAST)
        return shape
    }


    override fun appendProperties(builder: StateFactory.Builder<Block, BlockState>) {
        builder.add(
                POWER,
                CONNECT_UP,
                CONNECT_DOWN,
                CONNECT_WEST,
                CONNECT_EAST,
                CONNECT_NORTH,
                CONNECT_SOUTH)
    }

    companion object {
        // Properties of this block
        val POWER = Properties.POWER
        val CONNECT_NORTH: BooleanProperty = BooleanProperty.of("connect_north")
        val CONNECT_SOUTH: BooleanProperty = BooleanProperty.of("connect_south")
        val CONNECT_WEST: BooleanProperty = BooleanProperty.of("connect_west")
        val CONNECT_EAST: BooleanProperty = BooleanProperty.of("connect_east")
        val CONNECT_UP: BooleanProperty = BooleanProperty.of("connect_up")
        val CONNECT_DOWN: BooleanProperty = BooleanProperty.of("connect_down")

        // Shapes for center and each connected direction
        private const val centerDiam = 4
        private const val pipeDiam = 2
        private const val pipeLen = 8 - centerDiam / 2
        val SHAPE_CENTER       = createCenteredCuboidShape(centerDiam)
        val SHAPE_FACING_WEST  = createEdgeCuboidShape(Direction.WEST, pipeDiam, pipeLen)
        val SHAPE_FACING_EAST  = createEdgeCuboidShape(Direction.EAST, pipeDiam, pipeLen)
        val SHAPE_FACING_NORTH = createEdgeCuboidShape(Direction.NORTH, pipeDiam, pipeLen)
        val SHAPE_FACING_SOUTH = createEdgeCuboidShape(Direction.SOUTH, pipeDiam, pipeLen)
        val SHAPE_FACING_UP    = createEdgeCuboidShape(Direction.UP, pipeDiam, pipeLen)
        val SHAPE_FACING_DOWN  = createEdgeCuboidShape(Direction.DOWN, pipeDiam, pipeLen)

        /**
         * Amount of generated particles when output is on.  0 = none, 1 = max possible.
         */
        val particleAmount = 0.15

    }


    /**
     * Show redstone sparklies on the client side.
     */
    @Environment(EnvType.CLIENT)
    override fun randomDisplayTick(blockState: BlockState, world: World, blockPos: BlockPos, random: Random) {
        // Spawn some particles occasionally when powered
        val power = blockState[POWER]
        if (power > 0 && random.nextBoolean(particleAmount * power / 15.0)) {

            // Position sparkling over center
            var x = blockPos.x + 0.5
            var y = blockPos.y + 0.5
            var z = blockPos.z + 0.5

            // Randomize particle position
            val spread = 0.5
            x += random.nextDouble(-spread, spread)
            y += random.nextDouble(-spread, spread)
            z += random.nextDouble(-spread, spread)

            // Spawn particle
            world.addParticle(DustParticleEffect.RED, x, y, z, 0.0, 0.0, 0.0)
        }
    }


}
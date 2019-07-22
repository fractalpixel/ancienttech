package net.fractalpixel.ancienttech.blocks

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fractalpixel.ancienttech.api.redstoneports.PortDirection
import net.fractalpixel.ancienttech.api.redstoneports.RedstoneConnectionBlock
import net.fractalpixel.ancienttech.api.redstoneports.RedstonePortRegistry
import net.fractalpixel.ancienttech.utils.*
import net.minecraft.block.*
import net.minecraft.entity.EntityContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.particle.DustParticleEffect
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateFactory
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.BlockMirror
import net.minecraft.util.BlockRotation
import net.minecraft.util.Hand
import net.minecraft.util.TaskPriority
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.World
import java.util.*

/**
 * A redstone logic block that has one output and inputs from any adjacent blocks that may usually produce redstone
 * signals.  It has a configurable logic gate that can switch between OR, TWO, AND, and XOR, see [RedstoneGateLogic],
 * by touching the gate block near its center.  Its output can also be inverted, this is toggled by touching the
 * block near its output end.  The block indicates whether it is outputting a redstone signal by having a glowing output end.
 */
class RedstoneGateBlock(settings: Settings): FacingBlock(settings), RedstoneConnectionBlock {

    override fun getPlacementState(itemPlacementContext: ItemPlacementContext): BlockState {
        val state = defaultState
                .with(POWERED, false)
                .with(FACING, itemPlacementContext.side.opposite)
                .with(GATE, RedstoneGateLogic.OR)
                .with(INVERT_OUTPUT, false)
                .with(INPUT_DOWN, false)
                .with(INPUT_UP, false)
                .with(INPUT_WEST, false)
                .with(INPUT_EAST, false)
                .with(INPUT_NORTH, false)
                .with(INPUT_SOUTH, false)
        return state
    }

    // This is called before the block is placed in world
    override fun onPlaced(world: World, blockPos: BlockPos, blockState: BlockState, livingEntity_1: LivingEntity?, itemStack_1: ItemStack?) {
        /*
        val updatedState = calculateDynamicState(world, blockPos, blockState)
        if (updatedState[POWERED] != blockState[POWERED]) {
            // Schedule update tick if necessary
            world.blockTickScheduler.schedule(blockPos, this, 1)
        }
        */

        // Update input connections from and to neighbours if necessary
        val updatedState = RedstonePortRegistry.connectHalfConnections(world, blockPos, blockState) ?: blockState

        // Check if we should power up
        if (calculateOutput(world, blockPos, updatedState)) {
            // Schedule update tick for powerup
            world.blockTickScheduler.schedule(blockPos, this, 1)
        }

    }

    /*
    // Called whenever the block is added to the world (by anything)
    override fun onBlockAdded(blockState: BlockState, world: World, blockPos: BlockPos, blockState_2: BlockState?, boolean_1: Boolean) {
        // Copied from AbstractRedstoneGate - no idea what this is supposed to do
        this.updateTarget(world, blockPos, updatedState)
    }

    override fun onBlockRemoved(blockState: BlockState, world: World, blockPos: BlockPos, blockState_2: BlockState, boolean_1: Boolean) {
        // Copied from AbstractRedstoneGate - no idea what this is supposed to do
        if (!boolean_1 && blockState.block !== blockState_2.block) {
            super.onBlockRemoved(blockState, world, blockPos, blockState_2, boolean_1)
            this.updateTarget(world, blockPos, blockState)
        }
    }

     */

    /*
    private fun updateTarget(world: World, blockPos: BlockPos, blockState: BlockState) {
        // Copied from AbstractRedstoneGate - no idea what this is supposed to do
        val direction = blockState[FACING]
        val neighborPos = blockPos.offset(direction)
        world.updateNeighbor(neighborPos, this, blockPos)
        world.updateNeighborsExcept(neighborPos, this, direction.opposite)
    }

     */


    override fun neighborUpdate(blockState: BlockState, world: World, blockPos: BlockPos, block: Block, blockPos2: BlockPos, boolean_1: Boolean) {
        // Update input connections from and to neighbours if necessary
        val updatedState = RedstonePortRegistry.connectHalfConnections(world, blockPos, blockState) ?: blockState

        // Check if we should schedule an update to set the powered state, do not update directly, as that way lies infinite loops
        val wasPowered = updatedState[POWERED]
        val shouldPower = calculateOutput(world, blockPos, updatedState)
        if (wasPowered != shouldPower) {
            // Schedule an update tick
            world.blockTickScheduler.schedule(blockPos, this, updateDelayTicks, TaskPriority.HIGH)
        }
    }

    override fun activate(blockState: BlockState, world: World, blockPos: BlockPos, playerEntity: PlayerEntity, hand: Hand, blockHitResult: BlockHitResult): Boolean {
        return if (!playerEntity.abilities.allowModifyWorld) {
            false
        } else {

            var state = blockState

            // Detect if player touched the front or back part
            val frontBackTurnWheelBorder = 0.5
            val x = blockHitResult.pos.x.modPositive(1.0) * 2 - 1
            val y = blockHitResult.pos.y.modPositive(1.0) * 2 - 1
            val z = blockHitResult.pos.z.modPositive(1.0) * 2 - 1
            val facing = blockState[FACING]
            val part = facing.offsetX * x + facing.offsetY * y + facing.offsetZ * z
            if (part > frontBackTurnWheelBorder) {
                // We hit the front part, switch output negation
                state = state.with(INVERT_OUTPUT, !state[INVERT_OUTPUT])
                playerEntity.playSound(SoundEvents.BLOCK_BAMBOO_BREAK, 1.0F, 1.0F);
            } else {
                // We hit the back part, switch gate
                state = state.with(GATE, state[GATE].nextGate())
                playerEntity.playSound(SoundEvents.BLOCK_BAMBOO_STEP, 1.0F, 1.0F);
            }

            // Update output powered state
            state = state.with(POWERED, calculateOutput(world, blockPos, state))

            // Update state for block
            world.setBlockState(blockPos, state)

            true
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
        return blockState.with(FACING, blockRotation.rotate(blockState[FACING]))
    }

    override fun mirror(blockState: BlockState, blockMirror: BlockMirror): BlockState {
        return blockState.with(FACING, blockMirror.apply(blockState[FACING]))
    }

    override fun getStrongRedstonePower(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, direction: Direction): Int {
        return getWeakRedstonePower(blockState, blockView, blockPos, direction)
    }

    override fun getWeakRedstonePower(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, direction: Direction): Int {
        return if (blockState[POWERED] && direction.opposite == blockState[FACING]) 15 else 0
    }

    override fun emitsRedstonePower(blockState: BlockState): Boolean {
        return true
    }

    override fun getRedstonePortDirection(side: Direction, world: World, pos: BlockPos, blockState: BlockState): PortDirection {
        return when {
            blockState[FACING] == side -> PortDirection.OUT
            blockState[inputProperty(side)] -> PortDirection.IN
            else -> PortDirection.POTENTIAL_IN
        }
    }

    override fun connectTowards(direction: Direction, world: World, pos: BlockPos, blockState: BlockState): BlockState? {
        val inputProperty = inputProperty(direction)
        return if (blockState[FACING] != direction && !blockState[inputProperty]) {
            // Add input
            var state = blockState.with(inputProperty, true)

            // Update output powered state
            state = state.with(POWERED, calculateOutput(world, pos, state))

            // Update world
            world.setBlockState(pos, state)

            /*
            // Just in case, schedule update tick as well
            // TODO: Is this necessary?
            world.blockTickScheduler.schedule(pos, this, updateDelayTicks, TaskPriority.HIGH)
             */
            state
        }
        else null
    }

    override fun onScheduledTick(blockState: BlockState, world: World, blockPos: BlockPos, random: Random) {
        val wasPowered = blockState[POWERED]
        val shouldPower = calculateOutput(world, blockPos, blockState)
        if (wasPowered != shouldPower) {
            world.setBlockState(blockPos, blockState.with(POWERED, shouldPower))
        }
    }

    /* *
     * Determine the directions that there should be input pipes from
     */
    /*
    private fun updateNeighborConnections(world: World, blockPos: BlockPos, blockState: BlockState): BlockState {

        // Check each direction, if it outputs a redstone signal in this direction (and is not the output direction), add input pipe from there
        val facing = blockState[FACING]
        var result = blockState
        var inputsFound = false
        for (direction in Direction.values()) {
            val inputProperty = inputProperty(direction)
            val hasInput = blockState[inputProperty]
            val shouldInput = neighbourMayEmitRedstone(world, blockPos, direction) && direction != facing
            if (hasInput != shouldInput) {
                result = result.with(inputProperty, shouldInput)
            }
            if (shouldInput) inputsFound = true
        }

        /*
        // If no inputs found, create one opposite of the output
        if (!inputsFound) {
            result = result.with(inputProperty(facing.opposite), true)
        }
         */

        return result
    }
    */

    private fun calculateOutput(world: World, blockPos: BlockPos, blockState: BlockState): Boolean {
        // Determine number of input gates, and number of them that have active redstone signals coming in
        var numInputs = 0
        var activeInputs = 0
        for (direction in Direction.values()) {
            if (blockState[inputProperty(direction)]) {
                numInputs++
                if (neighbourCurrentlyEmitsRedstone(world, blockPos, direction)) activeInputs++
            }
        }

        // Determine output based on gate type and number of inputs active from total number of inputs
        val result = blockState[GATE].calculate(activeInputs, numInputs)

        // Negate result if required
        return if (blockState[INVERT_OUTPUT]) !result else result
    }


    private fun inputProperty(direction: Direction): BooleanProperty {
        return when(direction) {
            Direction.UP -> INPUT_UP
            Direction.DOWN -> INPUT_DOWN
            Direction.NORTH -> INPUT_NORTH
            Direction.SOUTH -> INPUT_SOUTH
            Direction.WEST -> INPUT_WEST
            Direction.EAST -> INPUT_EAST
        }
    }

    override fun getOutlineShape(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, entityContext: EntityContext): VoxelShape {
        // TODO: Cache or pre-generate these?

        // Gate itself
        var shape = when (blockState[FACING]) {
            Direction.UP -> SHAPE_FACING_UP
            Direction.DOWN -> SHAPE_FACING_DOWN
            Direction.NORTH -> SHAPE_FACING_NORTH
            Direction.SOUTH -> SHAPE_FACING_SOUTH
            Direction.WEST -> SHAPE_FACING_WEST
            Direction.EAST -> SHAPE_FACING_EAST
            else -> SHAPE_FACING_DOWN
        }

        // Inputs to it
        if (blockState[INPUT_UP]) shape = shape.combine(PIPE_FACING_UP)
        if (blockState[INPUT_DOWN]) shape = shape.combine(PIPE_FACING_DOWN)
        if (blockState[INPUT_NORTH]) shape = shape.combine(PIPE_FACING_NORTH)
        if (blockState[INPUT_SOUTH]) shape = shape.combine(PIPE_FACING_SOUTH)
        if (blockState[INPUT_WEST]) shape = shape.combine(PIPE_FACING_WEST)
        if (blockState[INPUT_EAST]) shape = shape.combine(PIPE_FACING_EAST)

        return shape
    }


    override fun appendProperties(builder: StateFactory.Builder<Block, BlockState>) {
        builder.add(
                POWERED,
                FACING,
                GATE,
                INVERT_OUTPUT,
                INPUT_UP,
                INPUT_DOWN,
                INPUT_WEST,
                INPUT_EAST,
                INPUT_NORTH,
                INPUT_SOUTH)
    }

    companion object {

        private const val pipeDiam = 2
        private const val pipeLength = 6
        private const val gateDiam = 4
        private const val gateLength = 10
        val SHAPE_FACING_WEST  = createEdgeCuboidShape(Direction.WEST, gateDiam, gateLength)
        val SHAPE_FACING_EAST  = createEdgeCuboidShape(Direction.EAST, gateDiam, gateLength)
        val SHAPE_FACING_NORTH = createEdgeCuboidShape(Direction.NORTH, gateDiam, gateLength)
        val SHAPE_FACING_SOUTH = createEdgeCuboidShape(Direction.SOUTH, gateDiam, gateLength)
        val SHAPE_FACING_UP = createEdgeCuboidShape(Direction.UP, gateDiam, gateLength)
        val SHAPE_FACING_DOWN  = createEdgeCuboidShape(Direction.DOWN, gateDiam, gateLength)
        val PIPE_FACING_WEST  = createEdgeCuboidShape(Direction.WEST, pipeDiam, pipeLength)
        val PIPE_FACING_EAST  = createEdgeCuboidShape(Direction.EAST, pipeDiam, pipeLength)
        val PIPE_FACING_NORTH = createEdgeCuboidShape(Direction.NORTH, pipeDiam, pipeLength)
        val PIPE_FACING_SOUTH = createEdgeCuboidShape(Direction.SOUTH, pipeDiam, pipeLength)
        val PIPE_FACING_UP = createEdgeCuboidShape(Direction.UP, pipeDiam, pipeLength)
        val PIPE_FACING_DOWN  = createEdgeCuboidShape(Direction.DOWN, pipeDiam, pipeLength)

        val INPUT_NORTH: BooleanProperty = BooleanProperty.of("input_north")
        val INPUT_SOUTH: BooleanProperty = BooleanProperty.of("input_south")
        val INPUT_WEST: BooleanProperty = BooleanProperty.of("input_west")
        val INPUT_EAST: BooleanProperty = BooleanProperty.of("input_east")
        val INPUT_UP: BooleanProperty = BooleanProperty.of("input_up")
        val INPUT_DOWN: BooleanProperty = BooleanProperty.of("input_down")

        val POWERED = Properties.POWERED

        val GATE: EnumProperty<RedstoneGateLogic> = EnumProperty.of("gate", RedstoneGateLogic::class.java)
        val INVERT_OUTPUT: BooleanProperty = BooleanProperty.of("invert_output")

        /**
         * Delay before updating outputs when an input has changed.
         * Setting this to higher than 1 results in missed pulses if it is driven by something with fast pulses.
         */
        val updateDelayTicks = 1

        /**
         * Amount of generated particles when output is on.  0 = none, 1 = max possible.
         */
        val particleAmount = 0.2
    }


    /**
     * Show redstone sparklies on the client side.
     */
    @Environment(EnvType.CLIENT)
    override fun randomDisplayTick(blockState: BlockState, world: World, blockPos: BlockPos, random: Random) {
        // Spawn some particles occasionally when output powered
        if (blockState[POWERED] && random.nextBoolean(particleAmount)) {

            // Position sparkling over output end
            val facing = blockState[FACING]
            val outputOffset = 7.5/8.0
            var x = blockPos.x + 0.5 + outputOffset * facing.offsetX * 0.5
            var y = blockPos.y + 0.5
            var z = blockPos.z + 0.5 + outputOffset * facing.offsetZ * 0.5

            // Randomize particle position
            val spread = 0.15
            x += random.nextDouble(-spread, spread)
            y += random.nextDouble(-spread, spread)
            z += random.nextDouble(-spread, spread)

            // Spawn particle
            world.addParticle(DustParticleEffect.RED, x, y, z, 0.0, 0.0, 0.0)
        }
    }

}
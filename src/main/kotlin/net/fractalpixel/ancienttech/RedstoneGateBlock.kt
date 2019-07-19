package net.fractalpixel.ancienttech

import modPositive
import net.minecraft.block.*
import net.minecraft.entity.EntityContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
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
import net.minecraft.world.ViewableWorld
import net.minecraft.world.World
import java.util.*


class RedstoneGateBlock(settings: Settings): FacingBlock(settings) {

    /**
     * Setting this to higher than 1 results in messed pulses if it is driven by something with fast pulses.
     */
    val updateDelayTicks = 1

    override fun getOutlineShape(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, entityContext: EntityContext): VoxelShape {
        return when (blockState.get(FACING)) {
            Direction.UP -> SHAPE_FACING_UP
            Direction.DOWN -> SHAPE_FACING_DOWN
            Direction.NORTH -> SHAPE_FACING_NORTH
            Direction.SOUTH -> SHAPE_FACING_SOUTH
            Direction.WEST -> SHAPE_FACING_WEST
            Direction.EAST -> SHAPE_FACING_EAST
            else -> SHAPE_FACING_DOWN
        }
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


    override fun isSimpleFullBlock(blockState: BlockState, blockView: BlockView, blockPos: BlockPos): Boolean {
        return false
    }


    override fun getPlacementState(itemPlacementContext: ItemPlacementContext): BlockState {
        val state = defaultState
                .with(POWERED, false)
                .with(FACING, itemPlacementContext.side)
                .with(GATE, AncientTechGate.OR)
                .with(INVERT_OUTPUT, false)
                .with(INPUT_DOWN, false)
                .with(INPUT_UP, false)
                .with(INPUT_WEST, false)
                .with(INPUT_EAST, false)
                .with(INPUT_NORTH, false)
                .with(INPUT_SOUTH, false)

        return calculateDynamicState(itemPlacementContext.world, itemPlacementContext.blockPos, state)
    }

    private fun calculateDynamicState(world: World, blockPos: BlockPos, state: BlockState): BlockState {
        var updatedState = state
        updatedState = updateNeighborConnections(world, blockPos, updatedState)
        updatedState = updatedState.with(POWERED, calculateOutput(world, blockPos, updatedState))
        return updatedState
    }

    override fun onPlaced(world: World?, blockPos: BlockPos?, blockState: BlockState?, livingEntity_1: LivingEntity?, itemStack_1: ItemStack?) {
        if (world != null && blockPos != null && blockState != null) {
            val updatedState = calculateDynamicState(world, blockPos, blockState)
            if (updatedState.get(POWERED) != blockState.get(POWERED)) {
                // Schedule update tick if necessary
                world.blockTickScheduler.schedule(blockPos, this, 1)
            }
        }
    }

    override fun onBlockAdded(blockState: BlockState, world: World, blockPos: BlockPos, blockState_2: BlockState?, boolean_1: Boolean) {
        // Copied from AbstractRedstoneGate - no idea what this is supposed to do
        this.updateTarget(world, blockPos, blockState)
    }

    override fun onBlockRemoved(blockState: BlockState, world: World, blockPos: BlockPos, blockState_2: BlockState, boolean_1: Boolean) {
        // Copied from AbstractRedstoneGate - no idea what this is supposed to do
        if (!boolean_1 && blockState.block !== blockState_2.block) {
            super.onBlockRemoved(blockState, world, blockPos, blockState_2, boolean_1)
            this.updateTarget(world, blockPos, blockState)
        }
    }

    private fun updateTarget(world: World, blockPos: BlockPos, blockState: BlockState) {
        // Copied from AbstractRedstoneGate - no idea what this is supposed to do
        val direction = blockState.get(FACING)
        val neighborPos = blockPos.offset(direction.opposite)
        world.updateNeighbor(neighborPos, this, blockPos)
        world.updateNeighborsExcept(neighborPos, this, direction)
    }

    override fun neighborUpdate(blockState: BlockState, world: World, blockPos: BlockPos, block: Block, blockPos2: BlockPos, boolean_1: Boolean) {
        val state = updateNeighborConnections(world, blockPos, blockState)
        world.setBlockState(blockPos, state)

        // Just check if we should schedule an update, do not update directly, as that way lies infinite loops
        checkForUpdateNeed(state, world, blockPos)
    }

    override fun hasSidedTransparency(blockState: BlockState): Boolean {
        return true
    }

    override fun canPlaceAtSide(blockState: BlockState, blockView_1: BlockView, blockPos_1: BlockPos, blockPlacementEnvironment_1: BlockPlacementEnvironment): Boolean {
        return false
    }


    override fun activate(blockState: BlockState, world: World, blockPos: BlockPos, playerEntity: PlayerEntity, hand: Hand, blockHitResult: BlockHitResult): Boolean {
        return if (!playerEntity.abilities.allowModifyWorld) {
            false
        } else {

            /*
            // Do not switch state if holding a pipe (makes it easier to place a lot of pipes)
            val mainHandContent = playerEntity.getEquippedStack(EquipmentSlot.MAINHAND)
            if (!mainHandContent.isEmpty && mainHandContent.name == AncientTechMod.redstone_gate_ITEM.name) return false
            */

            var state = blockState

            // Detect if player touched the front or back part
            val frontBackTurnWheelBorder = -0.5
            val x = blockHitResult.pos.x.modPositive(1.0) * 2 - 1
            val y = blockHitResult.pos.y.modPositive(1.0) * 2 - 1
            val z = blockHitResult.pos.z.modPositive(1.0) * 2 - 1
            val facing = blockState.get(FACING)
            val part = facing.offsetX * x + facing.offsetY * y + facing.offsetZ * z
            if (part < frontBackTurnWheelBorder) {
                // We hit the front part, switch output negation
                state = state.with(INVERT_OUTPUT, !state.get(INVERT_OUTPUT))
                playerEntity.playSound(SoundEvents.BLOCK_BAMBOO_BREAK, 1.0F, 1.0F);
            } else {
                // We hit the back part, switch gate
                state = nextGateState(state)
                playerEntity.playSound(SoundEvents.BLOCK_BAMBOO_STEP, 1.0F, 1.0F);
            }

            // Update output powered state
            state = state.with(POWERED, calculateOutput(world, blockPos, state))

            // Update state for block
            world.setBlockState(blockPos, state)

            true
        }
    }

    private fun checkForUpdateNeed(blockState: BlockState, world: World, blockPos: BlockPos) {
        val wasPowered = blockState.get(POWERED)
        val shouldPower = calculateOutput(world, blockPos, blockState)
        if (wasPowered != shouldPower) {
            // Schedule an update tick
            scheduleUpdateTick(world, blockPos)
        }
    }

    override fun rotate(blockState: BlockState, blockRotation: BlockRotation): BlockState {
        return blockState.with(FACING, blockRotation.rotate(blockState.get(FACING)))
    }

    override fun mirror(blockState: BlockState, blockMirror: BlockMirror): BlockState {
        return blockState.with(FACING, blockMirror.apply(blockState.get(FACING)))
    }

    override fun getStrongRedstonePower(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, direction: Direction): Int {
        return getWeakRedstonePower(blockState, blockView, blockPos, direction)
    }

    override fun getWeakRedstonePower(blockState: BlockState, blockView: BlockView, blockPos: BlockPos, direction: Direction): Int {
        return if (blockState.get(POWERED) && direction == blockState.get(FACING)) 15 else 0
    }

    override fun emitsRedstonePower(blockState: BlockState): Boolean {
        return true
    }

    override fun onScheduledTick(blockState: BlockState, world: World, blockPos: BlockPos, random: Random) {
        val wasPowered = blockState.get(POWERED)
        val shouldPower = calculateOutput(world, blockPos, blockState)
        if (wasPowered != shouldPower) {
            //val updateFlag = 2  // Notify clients, but not neighbours?
            world.setBlockState(blockPos, blockState.with(POWERED, shouldPower))
        }
        /*
        else if (!wasPowered) {
            // A short on-tick requested (e.g. from observer block)
            world.setBlockState(blockPos, blockState.with(POWERED, true))
            if (!shouldPower) scheduleUpdateTick(world, blockPos)
        }
        */

    }

    private fun scheduleUpdateTick(world: World, blockPos: BlockPos) {
        world.blockTickScheduler.schedule(blockPos, this, updateDelayTicks, TaskPriority.HIGH)
    }


    /**
     * Toggle to next logic gate in sequence
     */
    private fun nextGateState(blockState: BlockState): BlockState {
        // Find index of existing gate, and increase to next, rolling over if past size
        val currentGate = blockState.get(GATE)
        val gateValues = AncientTechGate.values()
        val nextIndex = (gateValues.indexOf(currentGate) + 1) % gateValues.size
        val nextGate = gateValues[nextIndex]
        return blockState.with(GATE, nextGate)
    }

    /**
     * Determine the directions that there should be input pipes from
     */
    private fun updateNeighborConnections(viewableWorld: ViewableWorld, blockPos: BlockPos, blockState: BlockState): BlockState {

        // Check each direction, if it outputs a redstone signal in this direction, add input pipe from there
        val facing = blockState.get(FACING)
        var result = blockState
        var inputsFound = false
        for (direction in Direction.values()) {
            val inputProperty = inputProperty(direction)
            val hasInput = blockState.get(inputProperty)
            val shouldInput = neighbourMayEmitRedstone(viewableWorld, blockPos, direction) && direction != facing.opposite
            if (hasInput != shouldInput) {
                result = result.with(inputProperty, shouldInput)
            }
            if (shouldInput) inputsFound = true
        }

        // If no inputs found, create one opposite of the output
        if (!inputsFound) {
            result = result.with(inputProperty(facing), true)
        }

        return result
    }

    private fun calculateOutput(world: World, blockPos: BlockPos, blockState: BlockState): Boolean {
        // Determine number of input gates, and number of them that have active redstone signals coming in
        var numInputs = 0
        var activeInputs = 0
        for (direction in Direction.values()) {
            if (blockState.get(inputProperty(direction))) {
                numInputs++
                if (neighbourCurrentlyEmitsRedstone(world, blockPos, direction)) activeInputs++
            }
        }

        // Determine output based on gate type and number of inputs active from total number of inputs
        val result = blockState.get(GATE).calculate(activeInputs, numInputs)

        // Negate result if required
        return if (blockState.get(INVERT_OUTPUT)) !result else result
    }

    private fun neighbourMayEmitRedstone(viewableWorld: ViewableWorld, blockPos: BlockPos, direction: Direction): Boolean {
        val neighborPos = blockPos.offset(direction)
        val blockState = viewableWorld.getBlockState(neighborPos)
        val block = blockState.block

        return when (block) {
            is RedstoneGateBlock -> blockState.get(FACING) == direction
            is AbstractRedstoneGateBlock -> blockState.get(AbstractRedstoneGateBlock.FACING) == direction
            is ObserverBlock -> blockState.get(ObserverBlock.FACING) == direction
            else -> blockState.emitsRedstonePower()
        }
    }

    private fun neighbourCurrentlyEmitsRedstone(world: World, blockPos: BlockPos, direction: Direction): Boolean {
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


    companion object {
        val SHAPE_FACING_WEST: VoxelShape  = createCuboidShape(6.0, 6.0, 6.0, 16.0, 10.0, 10.0)
        val SHAPE_FACING_EAST: VoxelShape  = createCuboidShape(0.0, 6.0, 6.0, 10.0, 10.0, 10.0)
        val SHAPE_FACING_NORTH: VoxelShape = createCuboidShape(6.0, 6.0, 6.0, 10.0, 10.0, 16.0)
        val SHAPE_FACING_SOUTH: VoxelShape = createCuboidShape(6.0, 6.0, 0.0, 10.0, 10.0, 10.0)
        val SHAPE_FACING_UP: VoxelShape    = createCuboidShape(6.0, 0.0, 6.0, 10.0, 10.0, 10.0)
        val SHAPE_FACING_DOWN: VoxelShape  = createCuboidShape(6.0, 6.0, 6.0, 10.0, 16.0, 10.0)

        val INPUT_NORTH: BooleanProperty = BooleanProperty.of("input_north")
        val INPUT_SOUTH: BooleanProperty = BooleanProperty.of("input_south")
        val INPUT_WEST: BooleanProperty = BooleanProperty.of("input_west")
        val INPUT_EAST: BooleanProperty = BooleanProperty.of("input_east")
        val INPUT_UP: BooleanProperty = BooleanProperty.of("input_up")
        val INPUT_DOWN: BooleanProperty = BooleanProperty.of("input_down")

        val POWERED = Properties.POWERED

        val GATE: EnumProperty<AncientTechGate> = EnumProperty.of("gate", AncientTechGate::class.java)
        val INVERT_OUTPUT: BooleanProperty = BooleanProperty.of("invert_output")
    }
}
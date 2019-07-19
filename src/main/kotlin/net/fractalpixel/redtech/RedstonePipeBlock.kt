package net.fractalpixel.redtech

import net.minecraft.block.*
import net.minecraft.entity.EntityContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateFactory
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
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


class RedstonePipeBlock(settings: Settings): Block(settings) {

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
                REQUIRE_ALL,
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
        var state = defaultState
                .with(POWERED, false)
                .with(FACING, itemPlacementContext.side)
                .with(REQUIRE_ALL, false)
                .with(INVERT_OUTPUT, false)
                .with(INPUT_DOWN, false)
                .with(INPUT_UP, false)
                .with(INPUT_WEST, false)
                .with(INPUT_EAST, false)
                .with(INPUT_NORTH, false)
                .with(INPUT_SOUTH, false)

        state = updateNeighborConnections(itemPlacementContext.world, itemPlacementContext.blockPos, state)

        state = state.with(POWERED, calculateOutput(itemPlacementContext.world, itemPlacementContext.blockPos, state))

        return state
    }

    override fun neighborUpdate(blockState: BlockState, world: World, blockPos: BlockPos, block: Block, blockPos2: BlockPos, boolean_1: Boolean) {
        val state = updateNeighborConnections(world, blockPos, blockState)
        world.setBlockState(blockPos, state)

        // Just check if we should schedule an update, do not update directly, as that way lies infinite loops?
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
            if (!mainHandContent.isEmpty && mainHandContent.name == RedTechMod.REDSTONE_PIPE_ITEM.name) return false
            */

            // TODO: Detect if we hit the front or back part
            /*
            println("activate")
            println(blockHitResult.pos)
            */

            // Switch state
            var state = nextGateState(blockState)
            state = state.with(POWERED, calculateOutput(world, blockPos, state))
            world.setBlockState(blockPos, state)
            playerEntity.playSound(SoundEvents.BLOCK_BAMBOO_PLACE, 1.0F, 1.0F);
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
        } else if (!wasPowered) {
            // A short on-tick requested (e.g. from observer block)
            world.setBlockState(blockPos, blockState.with(POWERED, true))
            if (!shouldPower) scheduleUpdateTick(world, blockPos)
        }

    }

    private fun scheduleUpdateTick(world: World, blockPos: BlockPos) {
        world.blockTickScheduler.schedule(blockPos, this, updateDelayTicks, TaskPriority.HIGH)
    }


    /**
     * Toggle to next logic gate in sequence
     */
    private fun nextGateState(blockState: BlockState): BlockState {
        var reqAll = blockState.get(REQUIRE_ALL)
        var invertOut = blockState.get(INVERT_OUTPUT)

        reqAll = !reqAll
        if (!reqAll) invertOut = !invertOut

        return blockState.with(INVERT_OUTPUT, invertOut).with(REQUIRE_ALL, reqAll)
    }

    /**
     * Determine the directions that there should be input pipes from
     */
    private fun updateNeighborConnections(viewableWorld: ViewableWorld, blockPos: BlockPos, blockState: BlockState): BlockState {
        val facing = blockState.get(FACING)
        var result = blockState
        for (direction in Direction.values()) {
            val inputProperty = inputProperty(direction)
            val hasInput = blockState.get(inputProperty)
            val shouldInput = neighbourMayEmitRedstone(viewableWorld, blockPos, direction) && direction != facing.opposite
            if (hasInput != shouldInput) {
                result = result.with(inputProperty, shouldInput)
            }
        }
        return result
    }

    private fun calculateOutput(viewableWorld: ViewableWorld, blockPos: BlockPos, blockState: BlockState): Boolean {
        var numInputs = 0
        var activeInputs = 0
        for (direction in Direction.values()) {
            if (blockState.get(inputProperty(direction))) {
                numInputs++
                if (neighbourCurrentlyEmitsRedstone(viewableWorld, blockPos, direction)) activeInputs++
            }
        }

        // Do we need all inputs to be on or only one?
        var result = if (blockState.get(REQUIRE_ALL)) {
            activeInputs > 0 && activeInputs == numInputs
        }
        else {
            activeInputs > 0
        }

        // Negate if required
        if (blockState.get(INVERT_OUTPUT)) {
            result = !result
        }

        return result
    }

    private fun neighbourMayEmitRedstone(viewableWorld: ViewableWorld, blockPos: BlockPos, direction: Direction): Boolean {
        val neighborPos = blockPos.offset(direction)
        val blockState = viewableWorld.getBlockState(neighborPos)
        val block = blockState.block

        return if (block is RedstonePipeBlock) blockState.get(FACING) == direction
               else if (block is AbstractRedstoneGateBlock) blockState.get(AbstractRedstoneGateBlock.FACING) == direction
               else if (block is ObserverBlock) blockState.get(ObserverBlock.FACING) == direction
               else blockState.emitsRedstonePower()
    }

    private fun neighbourCurrentlyEmitsRedstone(viewableWorld: ViewableWorld, blockPos: BlockPos, direction: Direction): Boolean {
        val neighborPos = blockPos.offset(direction)
        val blockState = viewableWorld.getBlockState(neighborPos)
        return if (blockState.emitsRedstonePower()) {
            val block= blockState.block
            if (block === Blocks.REDSTONE_BLOCK) {
                true
            } else {
                if (block === Blocks.REDSTONE_WIRE) {
                    blockState.get(RedstoneWireBlock.POWER) > 0
                } else {
                    viewableWorld.getEmittedStrongRedstonePower(neighborPos, direction) > 0
                }
            }
        } else {
            false
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
        val FACING = Properties.FACING

        val INVERT_OUTPUT: BooleanProperty = BooleanProperty.of("invert_output")
        val REQUIRE_ALL: BooleanProperty = BooleanProperty.of("require_all")

    }
}
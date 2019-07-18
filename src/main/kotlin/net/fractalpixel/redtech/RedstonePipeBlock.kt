package net.fractalpixel.redtech

import net.minecraft.block.*
import net.minecraft.block.RepeaterBlock.DELAY
import net.minecraft.entity.EntityContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateFactory
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.World


class RedstonePipeBlock(settings: Settings): Block(settings) {

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
        println("issimplefullblock")
        return false
    }


    override fun getPlacementState(itemPlacementContext: ItemPlacementContext): BlockState {
        println("getplacementstate")
        val outDown = itemPlacementContext.side == Direction.DOWN
        val outUp = itemPlacementContext.side == Direction.UP
        val outWest = itemPlacementContext.side == Direction.WEST
        val outEast = itemPlacementContext.side == Direction.EAST
        val outNorth = itemPlacementContext.side == Direction.NORTH
        val outSouth = itemPlacementContext.side == Direction.SOUTH

        return defaultState
                .with(POWERED, false)
                .with(FACING, itemPlacementContext.side)
                .with(REQUIRE_ALL, false)
                .with(INVERT_OUTPUT, false)
                .with(INPUT_DOWN, true)
                .with(INPUT_UP, false)
                .with(INPUT_WEST, true)
                .with(INPUT_EAST, false)
                .with(INPUT_NORTH, false)
                .with(INPUT_SOUTH, true)
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
            world.setBlockState(blockPos, nextGateState(blockState))
            playerEntity.playSound(SoundEvents.BLOCK_BAMBOO_PLACE, 1.0F, 1.0F);
            true
        }
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
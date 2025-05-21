package honziggi.branik_mod.energy.blocks

import honziggi.branik_mod.fluids.BranikFluids
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidUtil
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.network.NetworkHooks
import java.util.Optional

class BranikTap : Block(Properties.copy(Blocks.IRON_BLOCK).strength(2.0f)), EntityBlock {

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
    }
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }


    fun getBeerFluidStack(stack: ItemStack): FluidStack? {
        val maybeFluid: Optional<FluidStack> = FluidUtil.getFluidContained(stack)
        if (maybeFluid.isPresent) {
            val fluidStack = maybeFluid.get()

            val isBeer = BranikFluids.beerBuckets.any { it.get() == stack.item }
            if (isBeer) {
                return fluidStack
            }
        }
        return null
    }

    override fun use(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult
    ): InteractionResult {
        val stack = player.getItemInHand(hand)
        val blockEntity = level.getBlockEntity(pos)

        if (blockEntity is BranikTapEntity) {

            val beerFluid = getBeerFluidStack(stack)
            if (beerFluid != null){
                val tank = blockEntity.beerTank
                val currentFluid = tank.fluid
                val canAccept = tank.isEmpty || FluidStack(currentFluid, 1000).isFluidEqual(beerFluid)

                if (canAccept) {
                    val filled = tank.fill(beerFluid.copy(), IFluidHandler.FluidAction.EXECUTE)
                    if (filled > 0 && !player.isCreative) {
                        player.setItemInHand(hand, ItemStack(Items.BUCKET))
                    }
                }

                return InteractionResult.SUCCESS
            }

            // Open GUI if not holding buckets
            if (!level.isClientSide) {
                NetworkHooks.openScreen(player as ServerPlayer, blockEntity) { buf ->
                    buf.writeBlockPos(pos)
                }
            }
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }


    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return BranikTapEntity(pos,state)
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide) {
            BlockEntityTicker { lvl, pos, st, be ->
                if (be is BranikTapEntity) {
                    be.tickServer()
                }
            }
        } else null
    }
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (state.block != newState.block) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is BranikTapEntity) {
                for (i in 0 until blockEntity.itemHandler.slots) {
                    val stack = blockEntity.itemHandler.getStackInSlot(i)
                    if (!stack.isEmpty) {
                        Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                    }
                }
                level.updateNeighbourForOutputSignal(pos, this)
            }

            super.onRemove(state, level, pos, newState, isMoving)
        }
    }
}

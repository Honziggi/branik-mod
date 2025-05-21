package honziggi.branik_mod.energy.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.network.NetworkHooks
import org.spongepowered.asm.launch.platform.container.IContainerHandle

class MaltingMachine: Block(Properties.copy(Blocks.IRON_BLOCK)
), EntityBlock {

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


    override fun use(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pPlayer: Player,
        pHand: InteractionHand,
        pHit: BlockHitResult
    ): InteractionResult {
        if (!pLevel.isClientSide) {
            val blockEntity = pLevel.getBlockEntity(pPos)
            println("Block Entity at $pPos: $blockEntity")
            if (blockEntity is MenuProvider) {
                NetworkHooks.openScreen(pPlayer as ServerPlayer, blockEntity) {
                        buf -> buf.writeBlockPos(pPos)
                }
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return MaltingMachineEntity(pos, state)
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
            if (blockEntity is MaltingMachineEntity) {
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


    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide) {
            BlockEntityTicker { lvl, pos, st, be ->
                if (be is MaltingMachineEntity) {
                    be.tickServer()
                }
            }
        } else null
    }
}
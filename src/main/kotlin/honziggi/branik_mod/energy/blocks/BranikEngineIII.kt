package honziggi.branik_mod.energy.blocks

import honziggi.branik_mod.registry.BlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.Containers
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class BranikEngineIII : AbstractBranikEngine() {

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide && type == BlockEntities.BRANIK_ENGINE_III_ENTITY.get()) {
            BlockEntityTicker { lvl, pos, st, be ->
                (be as? BranikEngineIIIEntity)?.tickServer()
            }
        } else null
    }

    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState
    ): BlockEntity {
        return BranikEngineIIIEntity(pos, state)
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
            if (blockEntity is BranikEngineIIIEntity) {
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
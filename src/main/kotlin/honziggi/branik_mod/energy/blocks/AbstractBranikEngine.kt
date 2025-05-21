package honziggi.branik_mod.energy.blocks

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
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
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.network.NetworkHooks

abstract class AbstractBranikEngine(): Block(
    Properties.copy(Blocks.IRON_BLOCK)
        .strength(3.5f)
        .noOcclusion()
        .lightLevel { se -> if (se.getValue(LIT)) 13 else 0 }
), EntityBlock {

    init {
        registerDefaultState(this.defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false))
    }
    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
        val LIT: BooleanProperty = BlockStateProperties.LIT
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is MenuProvider && player is ServerPlayer) {
                NetworkHooks.openScreen(player, blockEntity) { buf ->
                    buf.writeBlockPos(pos)
                    if (blockEntity is AbstractBranikEngineEntity) {
                        buf.writeInt(blockEntity.energyStorage.energyStored)
                    } else {
                        buf.writeInt(0)
                    }
                }
            }
        }
        return InteractionResult.SUCCESS
    }

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource
    ) {
        if (state.getValue(LIT)) {
            val x = pos.x + 0.5
            val y = pos.y + 1.0
            val z = pos.z + 0.5

            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.05, 0.0)
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.02, 0.0)

            if (random.nextDouble() < 0.1) {
                level.playLocalSound(
                    x, y, z,
                    SoundEvents.FURNACE_FIRE_CRACKLE,
                    SoundSource.BLOCKS,
                    1.0f,             // volume
                    1.0f + random.nextFloat() * 0.2f // pitch
                    , false
                )
            }
        }
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, LIT)
    }
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(LIT, false)
    }

    abstract override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity
    abstract override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>?
}

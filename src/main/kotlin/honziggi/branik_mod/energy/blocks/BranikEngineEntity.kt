package honziggi.branik_mod.energy.blocks

import honziggi.branik_mod.energy.menu.BranikEngineMenu
import honziggi.branik_mod.registry.BlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockState

class BranikEngineEntity(
    pos: BlockPos,
    state: BlockState
) : AbstractBranikEngineEntity(
    BlockEntities.BRANIK_ENGINE_ENTITY.get(),
    pos,
    state
) {
    init {
        initEnergyStorage(cap = 50000, maxIn = 10, maxOut = 20)
    }

    override val validItems = listOf(
        "branik_mod:branik_11", "branik_mod:branik2l_11"
    )

    override fun getEnergyPerTickFor(item: Item): Int = when (BuiltInRegistries.ITEM.getKey(item).toString()) {
        "branik_mod:branik_11", "branik_mod:branik2l_11" -> 10
        else -> 0
    }

    override fun getItemEnergy(item: Item): Int = when (BuiltInRegistries.ITEM.getKey(item).toString()) {
        "branik_mod:branik_11" -> 500
        "branik_mod:branik2l_11"-> 2000
        else -> 0
    }

    override fun getDisplayName(): Component {
        return Component.translatable("block.branik_mod.branik_engine")
    }
    override fun createMenu(
        id: Int,
        inventory: Inventory,
        player: Player
    ): BranikEngineMenu {
        return BranikEngineMenu (id, inventory, this, dataSlot)
    }
}
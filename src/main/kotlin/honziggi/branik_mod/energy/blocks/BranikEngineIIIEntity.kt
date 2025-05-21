package honziggi.branik_mod.energy.blocks

import honziggi.branik_mod.energy.menu.BranikEngineIIIMenu
import honziggi.branik_mod.registry.BlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.BlockState

class BranikEngineIIIEntity(
    pos: BlockPos,
    state: BlockState
) : AbstractBranikEngineEntity(
    BlockEntities.BRANIK_ENGINE_III_ENTITY.get(),
    pos,
    state
) {
    init {
        initEnergyStorage(cap = 1000000, maxIn = 30, maxOut = 60)
    }

    override val validItems = listOf(
        "branik_mod:branik_11", "branik_mod:branik2l_11",
        "branik_mod:branik_12", "branik_mod:branik2l_12",
        "branik_mod:branik_14", "branik_mod:branik2l_14",
        "branik_mod:branik_18", "branik_mod:branik2l_18"
    )

    override fun getEnergyPerTickFor(item: Item): Int = when (BuiltInRegistries.ITEM.getKey(item).toString()) {
        "branik_mod:branik_11", "branik_mod:branik2l_11" -> 10
        "branik_mod:branik_12", "branik_mod:branik2l_12" -> 15
        "branik_mod:branik_14", "branik_mod:branik2l_14" -> 20
        "branik_mod:branik_18", "branik_mod:branik2l_18" -> 30
        else -> 0
    }

    override fun getItemEnergy(item: Item): Int = when (BuiltInRegistries.ITEM.getKey(item).toString()) {
        "branik_mod:branik_11" -> 500
        "branik_mod:branik_12" -> 1000
        "branik_mod:branik2l_11", "branik_mod:branik_14" -> 2000
        "branik_mod:branik2l_12", "branik_mod:branik_18" -> 4000
        "branik_mod:branik2l_14" -> 8000
        "branik_mod:branik2l_18" -> 16000
        else -> 0
    }

    override fun getDisplayName(): Component {
        return Component.translatable("block.branik_mod.branik_engine_iii")
    }
    override fun createMenu(
        id: Int,
        inventory: Inventory,
        player: Player
    ): BranikEngineIIIMenu {
        return BranikEngineIIIMenu (id, inventory, this, dataSlot)
    }
}




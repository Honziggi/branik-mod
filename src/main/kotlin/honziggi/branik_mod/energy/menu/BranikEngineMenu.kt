package honziggi.branik_mod.energy.menu

import honziggi.branik_mod.energy.blocks.BranikEngineEntity
import honziggi.branik_mod.registry.BranikMenus
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerData

class BranikEngineMenu(
    windowId: Int,
    playerInventory: Inventory,
    blockEntity: BranikEngineEntity,
    data: ContainerData
): AbstractBranikEngineMenu<BranikEngineEntity>(
    BranikMenus.BRANIK_ENGINE_MENU.get(),
    windowId,
    playerInventory,
    blockEntity,
    data
) {
    companion object {
        fun fromNetwork(id: Int, inv: Inventory, extraData: FriendlyByteBuf?): BranikEngineMenu {
            requireNotNull(extraData) {"Missing extraData for BranikEngineMenu"}

            val pos = extraData.readBlockPos()
            val energyStored = extraData.readInt()

            val be = inv.player.level().getBlockEntity(pos) as? BranikEngineEntity
                ?: throw IllegalStateException("Missing or wrong block entity at: $pos")

            return BranikEngineMenu(id, inv, be, be.dataSlot).apply {
                this.clientEnergyStored = energyStored
            }
        }
    }
}
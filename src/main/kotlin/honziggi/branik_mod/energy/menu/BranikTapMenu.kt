package honziggi.branik_mod.energy.menu

import honziggi.branik_mod.energy.blocks.BranikTapEntity
import honziggi.branik_mod.registry.BranikMenus
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.SlotItemHandler

class BranikTapMenu(
    id: Int,
    playerInventory: Inventory,
    val blockEntity: BranikTapEntity,
    val data: ContainerData
) : AbstractContainerMenu(BranikMenus.BRANIK_TAP_MENU.get(), id) {

    private val itemHandler: IItemHandler = blockEntity.itemHandler
    private val access = ContainerLevelAccess.create(playerInventory.player.level(), blockEntity.blockPos)

    init {
        addDataSlots(data)
        // Input slot (empty bottle)
        addSlot(SlotItemHandler(itemHandler, BranikTapEntity.INPUT_SLOT, 62, 34))

        // Output slot (filled beer bottle)
        addSlot(object : SlotItemHandler(itemHandler, BranikTapEntity.OUTPUT_SLOT, 135, 34) {
            override fun mayPlace(stack: ItemStack): Boolean = false
        })

        // Player inventory (3 rows of 9)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }

        // Hotbar
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    val fluidAmount: Int get() = data.get(0)
    val fluidId: Int get() = data.get(1)
    val maxFluidAmount: Int get() = 16000
    override fun stillValid(player: Player): Boolean = stillValid(access, player, blockEntity.blockState.block)

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY

        val stack = slot.item
        val copy = stack.copy()

        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size, true)) return ItemStack.EMPTY
        } else {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY
        }

        if (stack.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        return copy
    }

    companion object {
        fun fromNetwork(id: Int, inv: Inventory, extraData: FriendlyByteBuf?): BranikTapMenu {
            requireNotNull(extraData) { "Missing extraData for BranikTapMenu!" }
            val pos = extraData.readBlockPos()
            val be = inv.player.level().getBlockEntity(pos) as? BranikTapEntity
                ?: throw IllegalStateException("Wrong block entity at $pos")

            val data = be.dataSlot
            return BranikTapMenu(id, inv, be, data)
        }
    }
}

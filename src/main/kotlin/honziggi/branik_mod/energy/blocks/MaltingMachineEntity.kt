package honziggi.branik_mod.energy.blocks

import honziggi.branik_mod.energy.menu.MaltingMachineMenu
import honziggi.branik_mod.items.BranikItems
import honziggi.branik_mod.registry.BlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler

class MaltingMachineEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(BlockEntities.MALTING_MACHINE_ENTITY.get(), pos, state), MenuProvider {

    companion object {
        const val FUEL_SLOT = 0
        const val INPUT_SLOT1 = 1
        const val INPUT_SLOT2 = 2
        const val INPUT_SLOT3 = 3
        const val INPUT_SLOT4 = 4
        const val OUTPUT_SLOT1 = 5
        const val OUTPUT_SLOT2 = 6
        const val OUTPUT_SLOT3 = 7
        const val OUTPUT_SLOT4 = 8
    }

    val itemHandler = object : ItemStackHandler(9) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            return when (slot) {
                FUEL_SLOT -> ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0
                INPUT_SLOT1, INPUT_SLOT2, INPUT_SLOT3, INPUT_SLOT4 -> stack.item == BranikItems.GRAINS.get()
                OUTPUT_SLOT1, OUTPUT_SLOT2, OUTPUT_SLOT3, OUTPUT_SLOT4 -> false // no inserting
                else -> false
            }
        }
    }

    private val itemCap = LazyOptional.of { itemHandler }

    var burnTime = 0
    var burnDuration = 0
    var temperature = 20 // starts at 20°C
    var maltingProgress = 0
    var maltingTotalTime = 200

    var dataSlot: ContainerData = object : ContainerData {
        override fun get(index: Int): Int {
            return when (index) {
                0 -> burnTime
                1 -> burnDuration
                2 -> temperature
                3 -> maltingProgress
                4 -> maltingTotalTime
                else -> 0
            }
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                0 -> burnTime = value
                1 -> burnDuration = value
                2 -> temperature = value
                3 -> maltingProgress = value
                4 -> maltingTotalTime = value
            }
        }

        override fun getCount(): Int = 5
    }

    fun tickServer() {
        if (burnTime > 0) {
            burnTime--
        }

        if (burnTime == 0 && temperature <= 80) {
            tryConsumeFuel()
        }

        if (level!!.gameTime % 20L == 0L) {
            when {
                burnTime > 0 -> {
                    // 🔥 increase temp while burning
                    temperature = (temperature + 2).coerceAtMost(120)
                }
                burnTime == 0 -> {
                    // ❄️ cooldown when not burning
                    temperature = (temperature - 1).coerceAtLeast(20)
                }
            }
        }

        // --- Malting logic ---
        if (canMalt()) {
            maltingProgress++
            if (maltingProgress >= maltingTotalTime) {
                finishMalting()
                maltingProgress = 0
            }
        } else {
            maltingProgress = 0
        }

        setChanged()
    }


    private fun tryConsumeFuel() {
        val fuelStack = itemHandler.getStackInSlot(FUEL_SLOT)
        if (!fuelStack.isEmpty) {
            val fuelValue = ForgeHooks.getBurnTime(fuelStack, RecipeType.SMELTING)

            if (fuelValue > 0) {
                burnDuration = fuelValue
                burnTime = fuelValue
                fuelStack.shrink(1)
                setChanged()
            }
        }
    }

    private fun canMalt(): Boolean {
        val inputSlots = listOf(INPUT_SLOT1, INPUT_SLOT2, INPUT_SLOT3, INPUT_SLOT4)
        val inputs = inputSlots.map { itemHandler.getStackInSlot(it) }

        val outputSlots = listOf(OUTPUT_SLOT1, OUTPUT_SLOT2, OUTPUT_SLOT3, OUTPUT_SLOT4)
        val outputs = outputSlots.map { itemHandler.getStackInSlot(it) }

        val hasAllGrains = inputs.all { it.item == BranikItems.GRAINS.get() }
        val hasRoomForMalt = outputs.any { it.isEmpty || (it.item == BranikItems.MALT.get() && it.count < it.maxStackSize) }
        val isHotEnough = temperature > 80

        return hasAllGrains && hasRoomForMalt && isHotEnough
    }

    private fun finishMalting() {
        val inputSlots = listOf(INPUT_SLOT1, INPUT_SLOT2, INPUT_SLOT3, INPUT_SLOT4)
        val outputSlots = listOf(OUTPUT_SLOT1, OUTPUT_SLOT2, OUTPUT_SLOT3, OUTPUT_SLOT4)

        for (i in inputSlots.indices) {
            val input = itemHandler.getStackInSlot(inputSlots[i])
            val output = itemHandler.getStackInSlot(outputSlots[i])

            if (!input.isEmpty && (output.isEmpty || (output.item == BranikItems.MALT.get() && output.count < output.maxStackSize))) {
                if (output.isEmpty) {
                    itemHandler.setStackInSlot(outputSlots[i], ItemStack(BranikItems.MALT.get()))
                } else {
                    output.grow(1)
                }
                input.shrink(1)
            }
        }

    }

    override fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        val itemCapType = CapabilityManager.get(object : CapabilityToken<IItemHandler>() {})
        return when (cap) {
            itemCapType -> itemCap.cast()
            else -> super.getCapability(cap, side)
        }
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        itemCap.invalidate()
    }

    override fun getDisplayName() = Component.translatable("block.branik_mod.malting_machine")

    override fun createMenu(id: Int, inventory: Inventory, player: Player): AbstractContainerMenu {
        return MaltingMachineMenu(id, inventory, this, dataSlot)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.put("Items", itemHandler.serializeNBT())
        tag.putInt("BurnTime", burnTime)
        tag.putInt("BurnDuration", burnDuration)
        tag.putInt("Temperature", temperature)
    }
    override fun load(tag: CompoundTag) {
        super.load(tag)
        itemHandler.deserializeNBT(tag.getCompound("Items"))
        burnTime = tag.getInt("BurnTime")
        burnDuration = tag.getInt("BurnDuration")
        temperature = tag.getInt("Temperature")
    }
}

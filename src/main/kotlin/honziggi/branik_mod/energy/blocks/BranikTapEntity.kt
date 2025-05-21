package honziggi.branik_mod.energy.blocks

import honziggi.branik_mod.energy.menu.BranikTapMenu
import honziggi.branik_mod.fluids.BranikFluids
import honziggi.branik_mod.registry.BlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.templates.FluidTank
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler

class BranikTapEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(BlockEntities.BRANIK_TAP_ENTITY.get(), pos, state), MenuProvider {

    companion object {
        const val INPUT_SLOT = 0
        const val OUTPUT_SLOT = 1
    }

    val itemHandler = object : ItemStackHandler(2) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            val id = BuiltInRegistries.ITEM.getKey(stack.item).toString()
            return slot == INPUT_SLOT && (id == "branik_mod:branik_bottle" || id == "branik_mod:two_elco_bottle")
        }
    }

    val dataSlot: ContainerData = object : ContainerData {
        override fun get(index: Int): Int {
            return when (index) {
                0 -> beerTank.fluid.amount
                1 -> BuiltInRegistries.FLUID.getId(beerTank.fluid.fluid)
                2 -> if (maxFillTime > 0) (fillProgress * 100 / maxFillTime) else 0 // % progress
                else -> 0
            }
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                0 -> beerTank.fluid = FluidStack(beerTank.fluid.fluid, value)
                1 -> {
                    val fluid = BuiltInRegistries.FLUID.byId(value)
                    beerTank.fluid = FluidStack(fluid, beerTank.fluid.amount)
                }
            }
        }

        override fun getCount(): Int = 3
    }


    val beerTank = object : FluidTank(16000) {
        override fun isFluidValid(stack: FluidStack): Boolean {
            return stack.fluid in listOf(
                BranikFluids.BRANIK11.source.get(),
                BranikFluids.BRANIK12.source.get(),
                BranikFluids.BRANIK14.source.get(),
                BranikFluids.BRANIK18.source.get()
            )
        }
    }

    private val itemCap = LazyOptional.of { itemHandler }
    private val fluidCap = LazyOptional.of { beerTank }
    var fillProgress = 0
    var maxFillTime = 0

    fun tickServer() {
        val input = itemHandler.getStackInSlot(INPUT_SLOT)
        val output = itemHandler.getStackInSlot(OUTPUT_SLOT)
        val inputId = BuiltInRegistries.ITEM.getKey(input.item).toString()
        val isSmall = inputId == "branik_mod:branik_bottle"
        val isLarge = inputId == "branik_mod:two_elco_bottle"

        if (!isSmall && !isLarge) {
            fillProgress = 0
            return
        }

        val fluid = beerTank.fluid.fluid
        val outputItemId = when (fluid) {
            BranikFluids.BRANIK11.source.get() -> if (isSmall) "branik_mod:branik_11" else "branik_mod:branik2l_11"
            BranikFluids.BRANIK12.source.get() -> if (isSmall) "branik_mod:branik_12" else "branik_mod:branik2l_12"
            BranikFluids.BRANIK14.source.get() -> if (isSmall) "branik_mod:branik_14" else "branik_mod:branik2l_14"
            BranikFluids.BRANIK18.source.get() -> if (isSmall) "branik_mod:branik_18" else "branik_mod:branik2l_18"
            else -> {
                fillProgress = 0
                return
            }
        }

        val outputItem = BuiltInRegistries.ITEM.get(ResourceLocation(outputItemId)) ?: return

        if (!output.isEmpty && (!ItemStack.isSameItemSameTags(output, ItemStack(outputItem)) || output.count >= output.maxStackSize)) {
            fillProgress = 0
            return
        }

        val amount = if (isSmall) 500 else 2000
        if (beerTank.fluid.amount < amount) {
            fillProgress = 0
            return
        }

        if (maxFillTime == 0) {
            maxFillTime = if (isSmall) 40 else 160
        }

        fillProgress++
        if (fillProgress >= maxFillTime) {
            val outputItem = BuiltInRegistries.ITEM.get(ResourceLocation(outputItemId)) ?: return
            beerTank.drain(FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE)
            if (output.isEmpty) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack(outputItem))
            } else {
                output.grow(1)
            }
            input.shrink(1)
            fillProgress = 0
            maxFillTime = 0
            setChanged()
        }
    }

    override fun getDisplayName(): Component = Component.translatable("block.branik_mod.tap")

    override fun createMenu(
        id: Int,
        inventory: Inventory,
        player: Player
    ): AbstractContainerMenu {
        return BranikTapMenu(id, inventory, this, dataSlot)
    }

    override fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        val itemCapType = CapabilityManager.get(object : CapabilityToken<IItemHandler>() {})
        val fluidCapType = CapabilityManager.get(object : CapabilityToken<IFluidHandler>() {})
        return when (cap) {
            itemCapType -> itemCap.cast()
            fluidCapType -> fluidCap.cast()
            else -> super.getCapability(cap, side)
        }
    }
    override fun invalidateCaps() {
        itemCap.invalidate()
        fluidCap.invalidate()
    }

    override fun saveAdditional(tag: CompoundTag) {
        tag.put("Items", itemHandler.serializeNBT())
        tag.put("BeerTank", beerTank.writeToNBT(CompoundTag()))
        tag.putInt("FillProgress", fillProgress)
        tag.putInt("MaxFillTime", maxFillTime)
    }
    override fun load(tag: CompoundTag) {
        itemHandler.deserializeNBT(tag.getCompound("Items"))
        beerTank.readFromNBT(tag.getCompound("BeerTank"))
        fillProgress = tag.getInt("FillProgress")
        maxFillTime = tag.getInt("MaxFillTime")
    }
}

package honziggi.branik_mod.energy.blocks

import honziggi.branik_mod.energy.data.BranikEnergyStorage
import honziggi.branik_mod.energy.menu.AbstractBranikEngineMenu
import honziggi.branik_mod.energy.sync.SyncEngineEnergyPacket
import honziggi.branik_mod.network.NetworkHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.EnergyStorage
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.network.PacketDistributor

abstract class AbstractBranikEngineEntity(
    entityType: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(entityType, pos, state), MenuProvider {

    lateinit var energyStorage: BranikEnergyStorage
    fun initEnergyStorage(cap: Int, maxIn: Int, maxOut: Int) {
        energyStorage = BranikEnergyStorage(cap, maxIn, maxOut)
    }

    var clientEnergyStored: Int = 0
    protected var burnTime = 0
    protected var maxBurnTime = 0
    protected var lastEnergyPerTick = 0
    protected var lastFuelItem: Item? = null
    private var syncEnergyStored = 0
    private val energyCap = LazyOptional.of { energyStorage }
    private val itemCap = LazyOptional.of { itemHandler }

    val itemHandler = object : ItemStackHandler(5) {
        override fun onContentsChanged(slot: Int) = setChanged()

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            if (slot != 0) return false

            val id = BuiltInRegistries.ITEM.getKey(stack.item).toString()
            return validItems.contains(id)
        }
    }

    val dataSlot: ContainerData = object : ContainerData {
        override fun get(p0: Int) = when (p0) {
            0 -> burnTime
            1 -> energyStorage.energyStored
            2 -> getCurrentGeneration()
            3 -> maxBurnTime
            else -> 0
        }

        override fun set(p0: Int, p1: Int) {
            when (p0) {
                0 -> burnTime = p1
                1 -> clientEnergyStored = p1
                2 -> lastEnergyPerTick = p1
                3 -> maxBurnTime = p1
            }
        }

        override fun getCount() = 4
    }

    fun tickServer() {
        val fuel = itemHandler.getStackInSlot(0)
        if (burnTime > 0) {
            if (energyStorage.energyStored < energyStorage.maxEnergyStored){
                burnTime--
                energyStorage.receiveEnergy(lastEnergyPerTick, false)

                dataSlot.set(0, burnTime)
                dataSlot.set(1, energyStorage.energyStored)
                syncEnergyStored = energyStorage.energyStored

                if (burnTime == 0) {
                    lastEnergyPerTick = 0
                }

                setChanged()
            } else {
                print("Energy Full: ${energyStorage.energyStored}/${energyStorage.maxEnergyStored}")
            }
        }

        if (burnTime <= 0) {
            val stack = itemHandler.getStackInSlot(0)
            if (!stack.isEmpty) {
                val item = stack.item
                val energyPerTick = getEnergyPerTickFor(item)
                val totalEnergy = getItemEnergy(item)
                val duration = totalEnergy / energyPerTick

                if (energyPerTick > 0 && duration > 0) {
                    burnTime = duration
                    maxBurnTime = duration
                    lastEnergyPerTick = energyPerTick

                    lastFuelItem = item
                    stack.shrink(1)
                    tryOutputBeerBottle()

                    dataSlot.set(0, burnTime)
                    val scaled = (energyStorage.energyStored * 10000) / energyStorage.maxEnergyStored
                    dataSlot.set(1, scaled)
                    dataSlot.set(2, lastEnergyPerTick)
                    dataSlot.set(3, maxBurnTime)

                    setChanged()
                }
            }
        }
        updateLitState()
        sendEnergyUpdate()
    }

    protected fun updateLitState() {
        val lit = burnTime > 0
        val state = level?.getBlockState(blockPos) ?: return
        if (
            state.block is AbstractBranikEngine &&
            state.hasProperty(AbstractBranikEngine.Companion.LIT)
        ) {
            val currentLit = state.getValue(AbstractBranikEngine.Companion.LIT)
            if (currentLit != lit) {
                level?.setBlock(blockPos, state.setValue(AbstractBranikEngine.Companion.LIT, lit), 3)
            }
        }
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        itemCap.invalidate()
        energyCap.invalidate()
    }
    override fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        val itemCapType = CapabilityManager.get(object : CapabilityToken<IItemHandler>() {})
        val energyCapType = CapabilityManager.get(object : CapabilityToken<IEnergyStorage>() {})

        return when (cap) {
            itemCapType -> itemCap.cast()
            energyCapType -> energyCap.cast()
            else -> super.getCapability(cap, side)
        }
    }

    private fun sendEnergyUpdate() {
        val packet = SyncEngineEnergyPacket(blockPos, energyStorage.energyStored)
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with { level!!.getChunkAt(blockPos) }, packet)
    }

    open fun tryOutputBeerBottle() {
        val inputKey = BuiltInRegistries.ITEM.getKey(lastFuelItem ?: return)
        val outputId = when (inputKey.path) {
            "branik2l_11","branik2l_12","branik2l_14","branik2l_18" -> "two_elco_bottle"
            "branik_11", "branik_12","branik_14","branik_18" -> "branik_bottle"
            else -> null
        }
        val outputItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation("branik_mod", outputId)).orElse(null)
        if (outputItem != null) {
            val output = ItemStack(outputItem)
            for (slot in 1..4) {
                val current = itemHandler.getStackInSlot(slot)
                if (current.isEmpty) {
                    itemHandler.setStackInSlot(slot, output.copy())
                    break
                } else if (ItemStack.isSameItemSameTags(current, output) && current.count < current.maxStackSize) {
                    current.grow(1)
                    break
                }
            }
        }
    }

    open fun getCurrentGeneration(): Int = if (burnTime > 0) lastEnergyPerTick else 0

    abstract fun getEnergyPerTickFor(item: Item): Int
    abstract fun getItemEnergy(item: Item): Int
    abstract override fun getDisplayName(): Component
    abstract override fun createMenu(id: Int, inventory: Inventory, player: Player): AbstractBranikEngineMenu<*>
    abstract val validItems: List<String>

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putInt("Energy", energyStorage.energyStored)
        tag.put("Inventory", itemHandler.serializeNBT())
        tag.putInt("BurnTime", burnTime)
        tag.putInt("MaxBurnTime", maxBurnTime)
        tag.putInt("LastEnergyPerTick", lastEnergyPerTick)
    }
    override fun load(tag: CompoundTag) {
        super.load(tag)
        tag.getInt("Energy").also { energyStorage.setEnergyStored(it) }
        itemHandler.deserializeNBT(tag.getCompound("Inventory"))
        burnTime = tag.getInt("BurnTime")
        maxBurnTime = tag.getInt("MaxBurnTime")
        lastEnergyPerTick = tag.getInt("LastEnergyPerTick")
    }

}

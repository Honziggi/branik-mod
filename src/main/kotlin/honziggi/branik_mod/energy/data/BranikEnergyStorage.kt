package honziggi.branik_mod.energy.data

import net.minecraftforge.energy.EnergyStorage

class BranikEnergyStorage(capacity: Int, maxInput: Int, maxOutput: Int
) : EnergyStorage(capacity, maxInput, maxOutput)
{
    fun setEnergyStored(value: Int) {
        this.energy = value.coerceIn(0, this.capacity)
    }
}
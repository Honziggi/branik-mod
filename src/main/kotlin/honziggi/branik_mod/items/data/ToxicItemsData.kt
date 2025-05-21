package honziggi.branik_mod.items.data

import honziggi.branik_mod.items.ToxicItems
import net.minecraft.world.item.Item
import net.minecraftforge.registries.RegistryObject

object ToxicItemsData {

    private val volumeMap = mapOf(
        "branik" to 500,
        "branik2l" to 2000
    )

    private val percentMap = mapOf(
        "11" to 3.0,
        "12" to 5.0,
        "14" to 8.0,
        "18" to 12.0
    )

    private val alcoholMap: Map<Item, ToxicItemsInfo> = ToxicItems.ITEMS.entries
        .filterIsInstance<RegistryObject<Item>>() // ensure it's a registry object
        .mapNotNull { obj ->
            val id = obj.id.path // e.g. "branik2l_14"
            val item = obj.get()

            val parts = id.split("_")
            if (parts.size != 2) return@mapNotNull null

            val prefix = parts[0]     // "branik" or "branik2l"
            val strength = parts[1]   // "11", "12", ...

            val volume = volumeMap[prefix] ?: return@mapNotNull null
            val percent = percentMap[strength] ?: return@mapNotNull null

            item to ToxicItemsInfo(volume, percent)
        }
        .toMap()

    fun getInfo(item: Item): ToxicItemsInfo? = alcoholMap[item]
}

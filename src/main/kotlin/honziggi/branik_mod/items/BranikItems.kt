package honziggi.branik_mod.items

import honziggi.branik_mod.blocks.BranikBlocks
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.effect.MobEffects.BLINDNESS
import net.minecraft.world.effect.MobEffects.CONFUSION
import net.minecraft.world.effect.MobEffects.DAMAGE_BOOST
import net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE
import net.minecraft.world.effect.MobEffects.REGENERATION
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemNameBlockItem
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import java.util.UUID

object BranikItems {
    val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(ForgeRegistries.ITEMS, "branik_mod")

    private fun craftItem(name: String): RegistryObject<Item> =
        ITEMS.register(name) { Item(Item.Properties()) }

    val BRANIK_BOTTLE = craftItem("branik_bottle")
    val TWO_ELCO_BOTTLE = craftItem("two_elco_bottle")
    val HOPS = craftItem("hops")
    val GRAINS = craftItem("grains")
    val MALT = craftItem("malt")
    val BLOOD_SPIT = craftItem("blood_spit")
    val HOPS_SEEDS = ITEMS.register("hops_seeds") {
        ItemNameBlockItem(BranikBlocks.HOPS_CROP.get(), Item.Properties())
    }


    val ALL_BRANIK_ITEMS = listOf(
        BRANIK_BOTTLE, TWO_ELCO_BOTTLE,
        HOPS, HOPS_SEEDS, GRAINS, MALT, BLOOD_SPIT
    )

    fun registerItems(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }
}
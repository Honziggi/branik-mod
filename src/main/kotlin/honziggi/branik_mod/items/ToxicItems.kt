package honziggi.branik_mod.items

import honziggi.branik_mod.BranikMod
import honziggi.branik_mod.items.data.ToxicItemsData
import honziggi.branik_mod.items.sync.SyncEbacPacket
import honziggi.branik_mod.network.NetworkHandler
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import java.util.*

class ToxicItems(
    private val effects: List<MobEffectInstance>
) : Item(Properties().stacksTo(16).food(createFood(effects))) {

    fun calculateEBAC(ethanolMass: Double): Double {
        val vD = 710.0 // 710 dL, 100 * 0.71 * 10, steve = 100 kg

        val ebac = (ethanolMass / vD) * 10// ‰
        return ebac.coerceAtLeast(0.0)
    }

    companion object {
        private fun createFood(effects: List<MobEffectInstance>): FoodProperties {
            val builder = FoodProperties.Builder()
                .nutrition(2 + effects.size)
                .saturationMod(0.5f + effects.size * 0.1f)
                .alwaysEat()

            effects.forEach { builder.effect({ it }, 1.0f) }
            return builder.build()
        }
        val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, BranikMod.ID)

        private fun registerBeer(name: String, vararg effects: MobEffectInstance): RegistryObject<Item> {
            return ITEMS.register(name) { ToxicItems(effects.toList()) }
        }

        val BRANIK_11 = registerBeer("branik_11",
            MobEffectInstance(MobEffects.DAMAGE_BOOST, 200)
        )
        val BRANIK_12 = registerBeer("branik_12",
            MobEffectInstance(MobEffects.DAMAGE_BOOST, 200),
            MobEffectInstance(MobEffects.REGENERATION, 100)
        )
        val BRANIK_14 = registerBeer("branik_14",
            MobEffectInstance(MobEffects.DAMAGE_BOOST, 400),
            MobEffectInstance(MobEffects.REGENERATION, 200),
            MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200)
        )
        val BRANIK_18 = registerBeer("branik_18",
            MobEffectInstance(MobEffects.DAMAGE_BOOST, 600),
            MobEffectInstance(MobEffects.REGENERATION, 400),
            MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400)
            )

        val BRANIK2L_11 = registerBeer("branik2l_11",
            MobEffectInstance(MobEffects.DAMAGE_BOOST, 400),
            MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200),
            MobEffectInstance(MobEffects.REGENERATION, 200)
        )
        val BRANIK2L_12 = registerBeer("branik2l_12",
            MobEffectInstance(MobEffects.DAMAGE_BOOST, 800),
            MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400),
            MobEffectInstance(MobEffects.REGENERATION, 400)
        )
        val BRANIK2L_14 = registerBeer("branik2l_14",
            MobEffectInstance(MobEffects.DAMAGE_BOOST, 800, 1),
            MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 800, 1),
            MobEffectInstance(MobEffects.REGENERATION, 200, 1)
        )
        val BRANIK2L_18 = registerBeer("branik2l_18",
            MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1),
            MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1),
            MobEffectInstance(MobEffects.REGENERATION, 400, 2)
        )

        val TOXIC_ITEMS = listOf(
            BRANIK_11, BRANIK_12, BRANIK_14, BRANIK_18,
            BRANIK2L_11, BRANIK2L_12, BRANIK2L_14, BRANIK2L_18
        )
        fun register(eventBus: IEventBus) {
            ITEMS.register(eventBus)
        }
    }
    override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
        if (entity is Player && !level.isClientSide) {

            val info = ToxicItemsData.getInfo(this)
            if (info == null) {
                BranikMod.LOGGER.warn("No alcohol data for item: $this")
                return super.finishUsingItem(stack, level, entity)
            }

            val ethanolVolume = info.volume * (info.percent / 100.0)// mB or mL
            val ethanolMass = ethanolVolume * 0.789// g

            val tag = entity.persistentData
            val prevEbac = tag.getDouble("branik_ebac")
            val newEbac = prevEbac + calculateEBAC(ethanolMass)
            tag.putDouble("branik_ebac", newEbac)

            NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with { entity as ServerPlayer },
                SyncEbacPacket(newEbac)
            )
        }
        return super.finishUsingItem(stack, level, entity)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.DRINK
}

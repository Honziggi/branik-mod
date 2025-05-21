package honziggi.branik_mod.items.data

import honziggi.branik_mod.items.BranikItems
import honziggi.branik_mod.items.sync.SyncEbacPacket
import honziggi.branik_mod.network.NetworkHandler
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.network.PacketDistributor
import java.util.*

@Mod.EventBusSubscriber(modid = "branik_mod")
object EbacTicker {
    private val scheduledVomits = mutableMapOf<UUID, Long>()

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return

        val player = event.player
        val tag = player.persistentData

        val ebac = tag.getDouble("branik_ebac")
        val decayPerTick = 0.0003 // 0.006 per second
        val decayedEbac = (ebac - decayPerTick).coerceAtLeast(0.0)
        tag.putDouble("branik_ebac", decayedEbac)

        applyEbacEffects(player, decayedEbac)

        val scheduledTime = scheduledVomits[player.uuid]
        if (scheduledTime != null && player.level().gameTime >= scheduledTime) {
            vomitEvent(player)
            scheduledVomits.remove(player.uuid)
        }

        if (player.tickCount % 20 == 0) { // every 20 ticks = 1 second
            if (player is ServerPlayer) {
                NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with { player },
                    SyncEbacPacket(decayedEbac)
                )
            }
        }
    }

    private fun applyEbacEffects(player: Player, ebac: Double) {
        val tag = player.persistentData

        //clearTypes.forEach { player.removeEffect(it) }
        if (ebac >= 4.0 && ebac < 7.5 && player.level().gameTime % 20L == 0L && player.random.nextInt(30) == 0) {
            scheduledVomits[player.uuid] = player.level().gameTime + 40L // 2 seconds later
        }
        if (ebac >= 7.5 && player.level().gameTime % 20L == 0L && player.random.nextInt(15) == 0) {
            scheduledVomits[player.uuid] = player.level().gameTime + 40L
        }

        // === Warning 1: 6 ≤ EBAC < 8 ===
        if (ebac in 6.0..7.999 && !tag.getBoolean("branik_warn_1")) {
            player.sendSystemMessage(Component.literal("Your liver is starting to fail."))
            tag.putBoolean("branik_warn_1", true)
        }
        if (ebac < 6.0 && tag.getBoolean("branik_warn_1")) {
            tag.putBoolean("branik_warn_1", false)
        }

        // === Warning 2: 8 ≤ EBAC < 10 ===
        if (ebac in 8.0..9.999 && !tag.getBoolean("branik_warn_2")) {
            player.sendSystemMessage(Component.literal("""
        |You're seriously intoxicated. I recommend to:
        |Get some rest
        |Seek medical attention
        |Stop Branik intake
        """.trimIndent()))
            tag.putBoolean("branik_warn_2", true)
        }
        if (ebac < 8.0 && tag.getBoolean("branik_warn_2")) {
            tag.putBoolean("branik_warn_2", false)
        }

        // === Warning 2: 10 ≤ EBAC < 11 ===
        if (ebac in 10.0..10.999 && !tag.getBoolean("branik_warn_3")) {
            player.sendSystemMessage(Component.literal("""
        |You're depressed.
        |Branik took everything enjoyable out of your life.
        """.trimIndent()))
            tag.putBoolean("branik_warn_3", true)
        }
        if (ebac < 10.0 && tag.getBoolean("branik_warn_3")) {
            tag.putBoolean("branik_warn_3", false)
        }

        // === Warning 4: 11 ≤ EBAC < 12 ===
        if (ebac in 11.0..11.999 && !tag.getBoolean("branik_warn_4")) {
            player.sendSystemMessage(Component.literal("""
        |You're dying. Your liver can’t take it anymore.
        |If you don’t die, it’ll be a miracle.
        """.trimIndent()))
            tag.putBoolean("branik_warn_4", true)
        }
        if (ebac < 11.0 && tag.getBoolean("branik_warn_4")) {
            tag.putBoolean("branik_warn_4", false)
        }

        when {
            ebac < 0.5 -> return
            ebac < 0.8 -> {
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("Let's get this party started!"))
                }
            }
            ebac < 1.2 -> {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("You giggle for no reason. Everything seems nice."))
                }
            }
            ebac < 2.5 -> {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("I should probably call my ex."))
                }
            }
            ebac < 4.0 -> {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("You're starting to feel sick."))
                }
            }
            ebac < 6.0 -> {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("*burp*" ))
                }
                if (player.level().gameTime % 40 == 0L && player.random.nextFloat() < 0.25f) {
                    player.jumpFromGround() // simulate accidental jump
                }
            }
            ebac < 8.0 -> {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                player.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 10))
                if (player.level().gameTime % 40 == 0L && player.random.nextFloat() < 0.25f) {
                    player.jumpFromGround() // simulate accidental jump
                }
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("IwbnfouevcmsiehwvubqvGovenrment."))
                }
            }
            ebac < 10.0 -> {

                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                player.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 10))
                player.addEffect(MobEffectInstance(MobEffects.CONFUSION, 20))
                player.isShiftKeyDown = true
                player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE,  // use a muffled deep sound
                    SoundSource.PLAYERS,
                    0.3f,
                    0.5f
                )
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("*you spit blood on the ground*"))
                    bloodSpit(player)
                }
            }
            ebac < 11.0 -> {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2))
                player.addEffect(MobEffectInstance(MobEffects.POISON, 40))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                player.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 10))
                player.addEffect(MobEffectInstance(MobEffects.CONFUSION, 20))
                if (player.level().gameTime % 40L < 20L) {
                    player.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 20))
                }
                player.isShiftKeyDown = true
                player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE,  // use a muffled deep sound
                    SoundSource.PLAYERS,
                    0.3f,
                    0.5f
                )
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("*your nose bleeds*"))
                    bloodSpit(player)
                }
            }
            ebac < 12 -> {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2))
                player.addEffect(MobEffectInstance(MobEffects.WITHER, 40))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                player.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 10))
                player.addEffect(MobEffectInstance(MobEffects.CONFUSION, 20))
                player.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 20))
                player.isShiftKeyDown = true
                player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 0.5f, 0.3f)
                player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE,  // use a muffled deep sound
                    SoundSource.PLAYERS,
                    0.3f,
                    0.5f
                )
                if (player.tickCount % 600 == 0) {
                    player.sendSystemMessage(Component.literal("*your body starts shaking uncontrollably*"))
                    bloodSpit(player)
                }
            }
            else -> {
                player.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2))
                player.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 10))
                player.addEffect(MobEffectInstance(MobEffects.WITHER, 40, 2))
                player.addEffect(MobEffectInstance(MobEffects.POISON, 40, 2))
                player.addEffect(MobEffectInstance(MobEffects.HUNGER, 10))
                player.addEffect(MobEffectInstance(MobEffects.CONFUSION, 20))
                player.addEffect(MobEffectInstance(MobEffects.BLINDNESS, 20))
                player.isShiftKeyDown = true
                player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 0.5f, 0.3f)
                player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE,  // use a muffled deep sound
                    SoundSource.PLAYERS,
                    0.3f,
                    0.5f
                )
            }
        }
    }

    private fun bloodSpit(player: Player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1f, 0.8f)
        val dropStack = ItemStack(BranikItems.BLOOD_SPIT.get())
        player.drop(dropStack, false) // false = no offset from player
    }
    private fun vomitEvent(player: Player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1f, 0.8f)
        val dropStack = ItemStack(BranikItems.MALT.get())
        player.drop(dropStack, false) // false = no offset from player
    }

}

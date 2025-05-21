package honziggi.branik_mod.network

import honziggi.branik_mod.items.sync.SyncEbacPacket
import honziggi.branik_mod.energy.sync.SyncEngineEnergyPacket
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel
import java.util.Optional

object NetworkHandler {
    private const val PROTOCOL_VERSION = "1.0"

    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation("branik_mod", "main"),
        { PROTOCOL_VERSION },
        { it == PROTOCOL_VERSION },
        { it == PROTOCOL_VERSION }
    )

    private var id = 0
    private fun nextId() = id++

    fun register() {
        CHANNEL.registerMessage(
            nextId(),
            SyncEngineEnergyPacket::class.java,
            SyncEngineEnergyPacket::encode,
            SyncEngineEnergyPacket::decode,
            SyncEngineEnergyPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        )
        CHANNEL.registerMessage(
            nextId(),
            SyncEbacPacket::class.java,
            SyncEbacPacket::encode,
            SyncEbacPacket.Companion::decode,
            SyncEbacPacket.Companion::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        )
    }
}
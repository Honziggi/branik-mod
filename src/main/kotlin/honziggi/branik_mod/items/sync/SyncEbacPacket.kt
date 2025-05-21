package honziggi.branik_mod.items.sync

import honziggi.branik_mod.items.ToxicItems
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class SyncEbacPacket(val ebac: Double) {
    companion object {
        fun encode(packet: SyncEbacPacket, buffer: FriendlyByteBuf) {
            buffer.writeDouble(packet.ebac)
        }

        fun decode(buffer: FriendlyByteBuf): SyncEbacPacket {
            return SyncEbacPacket(buffer.readDouble())
        }

        fun handle(packet: SyncEbacPacket, context: Supplier<NetworkEvent.Context>) {
            context.get().enqueueWork {
                val player = Minecraft.getInstance().player ?: return@enqueueWork

                // Update player persistent data
                player.persistentData.putDouble("branik_ebac", packet.ebac)
            }
            context.get().packetHandled = true
        }
    }
}

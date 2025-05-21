package honziggi.branik_mod.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg
import honziggi.branik_mod.items.sync.SyncEbacPacket
import honziggi.branik_mod.network.NetworkHandler
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkDirection

object BranikCommands {

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("ebac")
                .requires { it.hasPermission(2) } // OP level
                .then(Commands.literal("set")
                    .then(Commands.argument("value", doubleArg(0.0, 20.0))
                        .executes { ctx ->
                            val source = ctx.source
                            val value = DoubleArgumentType.getDouble(ctx, "value")
                            val player = source.playerOrException

                            // Server-side: store in persistentData
                            player.persistentData.putDouble("branik_ebac", value)

                            // Send to client
                            NetworkHandler.CHANNEL.sendTo(
                                SyncEbacPacket(value),
                                (player as ServerPlayer).connection.connection,
                                NetworkDirection.PLAY_TO_CLIENT
                            )

                            source.sendSuccess({ Component.literal("EBAC set to %.2f‰".format(value)) }, true)
                            1
                        }
                    )
                )
        )
    }
}

package honziggi.branik_mod

import com.honziggi.branik_mod.BranikCreativeTab
import honziggi.branik_mod.registry.BranikMenus
import honziggi.branik_mod.blocks.BranikBlocks
import honziggi.branik_mod.commands.BranikCommands
import honziggi.branik_mod.energy.gui.BranikEngineIIIScreen
import honziggi.branik_mod.energy.gui.BranikEngineIIScreen
import honziggi.branik_mod.energy.gui.BranikEngineScreen
import honziggi.branik_mod.energy.gui.BranikTapScreen
import honziggi.branik_mod.energy.gui.MaltingMachineScreen
import honziggi.branik_mod.energy.gui.FermentationKegScreen
import honziggi.branik_mod.fluids.BranikFluids
import honziggi.branik_mod.registry.BlockEntities
import honziggi.branik_mod.items.BranikItems
import honziggi.branik_mod.items.ToxicItems
import honziggi.branik_mod.network.NetworkHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraftforge.event.LootTableLoadEvent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.runForDist

/**
 * Main mod class. Should be an `object` declaration annotated with `@Mod`.
 * The modid should be declared in this object and should match the modId entry
 * in mods.toml.
 *
 * An example for blocks is in the `blocks` package of this mod.
 */
@Mod(BranikMod.ID)
object BranikMod {
    const val ID = "branik_mod"

    // the logger for our mod
    val LOGGER: Logger = LogManager.getLogger(ID)

    init {
        LOGGER.log(Level.INFO, "Hello world!")

        // Register the KDeferredRegister to the mod-specific event bus
        BranikFluids.register(MOD_BUS)
        BranikBlocks.register(MOD_BUS)
        BranikItems.registerItems(MOD_BUS)
        ToxicItems.register(MOD_BUS)
        BranikMenus.register(MOD_BUS)
        BlockEntities.register(MOD_BUS)
        BranikCreativeTab.register(MOD_BUS)


        LOGGER.log(Level.INFO, "All items, block, and menus registered")

        val obj = runForDist(
            clientTarget = {
                MOD_BUS.addListener(::onClientSetup)
                Minecraft.getInstance()
            },
            serverTarget = {
                MOD_BUS.addListener(::onServerSetup)
                "test"
            })

        println(obj)
    }

    /**
     * This is used for initializing client specific
     * things such as renderers and keymaps
     * Fired on the mod specific event bus.
     */

    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Initializing client...")
        event.enqueueWork {
            MenuScreens.register(BranikMenus.BRANIK_ENGINE_MENU.get(), ::BranikEngineScreen)
            MenuScreens.register(BranikMenus.BRANIK_ENGINE_II_MENU.get(), ::BranikEngineIIScreen)
            MenuScreens.register(BranikMenus.BRANIK_ENGINE_III_MENU.get(), ::BranikEngineIIIScreen)
            MenuScreens.register(BranikMenus.MALTING_MACHINE_MENU.get(), ::MaltingMachineScreen)
            MenuScreens.register(BranikMenus.FERMENTATION_KEG_MENU.get(), ::FermentationKegScreen)
            MenuScreens.register(BranikMenus.BRANIK_TAP_MENU.get(), ::BranikTapScreen)
            NetworkHandler.register()
        }
    }

    /**
     * Fired on the global Forge bus.
     */
    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.log(Level.INFO, "Server starting...")
    }
}
@Mod.EventBusSubscriber(modid = BranikMod.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object BranikLootInjector {

    @SubscribeEvent
    fun onLootTableLoad(event: LootTableLoadEvent) {
        if (event.name == ResourceLocation("minecraft", "blocks/grass")) {
            val entry = LootItem.lootTableItem(BranikItems.HOPS_SEEDS.get())
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1f)))
                .`when` (LootItemRandomChanceCondition.randomChance(0.2f))

            val pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1f))
                .add(entry)
                .build()

            event.table.addPool(pool)
            BranikMod.LOGGER.info("✔ Injected HOPS_SEEDS into grass drops")
        }
    }
}

@Mod.EventBusSubscriber(modid = BranikMod.ID)
object BranikCommandsEvens {
    @SubscribeEvent
    fun onRegisterCommands (event: RegisterCommandsEvent){
        BranikCommands.register(event.dispatcher)
    }
}
package honziggi.branik_mod.energy.gui

import honziggi.branik_mod.energy.menu.BranikEngineMenu
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class BranikEngineScreen(
    menu: BranikEngineMenu,
    inventory: Inventory,
    title: Component,
) : AbstractBranikEngineScreen<BranikEngineMenu>(menu, inventory, title) {
    override val texture = ResourceLocation("branik_mod", "textures/gui/branik_engine_gui.png")
}
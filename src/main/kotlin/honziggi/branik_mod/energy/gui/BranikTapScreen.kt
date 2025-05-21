package honziggi.branik_mod.energy.gui

import honziggi.branik_mod.BranikMod
import honziggi.branik_mod.energy.menu.BranikTapMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions

class BranikTapScreen(
    menu: BranikTapMenu,
    inventory: Inventory,
    title: Component
) : AbstractContainerScreen<BranikTapMenu>(menu, inventory, title) {

    private val texture = ResourceLocation(BranikMod.ID, "textures/gui/branik_tap_gui.png")

    init {
        imageWidth = 176
        imageHeight = 166
    }
    fun getBrewingFrame(): Int {
        val t = (System.currentTimeMillis() / 100) % 7
        return t.toInt()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderBackground(graphics)
        super.render(graphics, mouseX, mouseY, partialTicks)
        renderTooltip(graphics, mouseX, mouseY)

        // Optional: render fluid amount as text
        val fluidId = menu.data.get(1)
        val amount = menu.data.get(0)
        val progress = menu.data.get(2)
        val fluid = BuiltInRegistries.FLUID.byId(fluidId)
        val fluidName = fluid.fluidType.description.string


        // Tooltip for energy bar
        val tankX = leftPos + 26
        val tankY = topPos + 11
        val tankWidth = 16
        val tankHeight = 64

        if (mouseX in tankX..(tankX + tankWidth) && mouseY in tankY..(tankY + tankHeight)) {
            graphics.renderTooltip(font, Component.literal("Stored: $amount of $fluidName"), mouseX, mouseY)
        }
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight)

        val fluid = BuiltInRegistries.FLUID.byId(menu.data.get(1))
        val tint = IClientFluidTypeExtensions.of(fluid.fluidType).getTintColor()
        val fluidAmount = menu.data.get(0)
        val progress = menu.data.get(2)

        // Tank position and size
        val tankX = leftPos + 26
        val tankY = topPos + 11
        val tankWidth = 16
        val tankHeight = 64
        val fluidHeight = (fluidAmount / 16000f * tankHeight).toInt()

        if (fluidHeight > 0) {
            graphics.fill(
                tankX, tankY + tankHeight - fluidHeight,
                tankX + tankWidth, tankY + tankHeight,
                0xFF000000.toInt() or tint
            )
        }

        val filling = menu.data.get(2) > 0
        if (filling) {
            val brewFrame = (System.currentTimeMillis() / 100 % 7).toInt()
            val bubbleHeights = listOf(29, 24, 20, 16, 11, 6, 0)
            val h = bubbleHeights[brewFrame]

            val x = leftPos + 102  // start x position
            val y = topPos + 44 + (29 - h)   // bottom position

            graphics.blit(
                texture,
                x, y,
                0,
                176f, (20 -h).toFloat(), //texture coordinates
                12, h,
                256, 256
            )
        }

    }

    override fun renderLabels(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {}
}

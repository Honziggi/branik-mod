package honziggi.branik_mod.client

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderGuiOverlayEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = "branik_mod", value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.FORGE)
object ClientHudEvents {

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGuiOverlayEvent.Post) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val ebac = player.persistentData.getDouble("branik_ebac")
        val ebacStr = "EBAC: %.2f‰".format(ebac)

        val font = mc.font
        val guiGraphics = event.guiGraphics
        val poseStack = guiGraphics.pose()
        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight

        guiGraphics.drawString(font, Component.literal(ebacStr), 5, 5, 0xFFFFFF, true)

        if (ebac < 2) return
        // green tint / random overlay
        if (2 < ebac && ebac < 6) {
            val alpha = (((ebac / 6.0) * 0.01 + 0.01).coerceIn(0.01, 0.02)).toFloat()


            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShader { GameRenderer.getPositionColorShader() }

            val tesselator = Tesselator.getInstance()
            val buffer = tesselator.builder
            poseStack.pushPose()
            val matrix = poseStack.last().pose()

            val baseR = 0.1f
            val baseG = 0.6f
            val baseB = 0.1f
            val brightness = (1.0 - (ebac / 6.0)).coerceIn(0.2, 1.0).toFloat()

            val r = baseR * brightness
            val g = baseG * brightness
            val b = baseB * brightness

            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
            buffer.vertex(matrix, 0f, 0f, 0f).color(r, g, b, alpha).endVertex()
            buffer.vertex(matrix, 0f, height.toFloat(), 0f).color(r, g, b, alpha).endVertex()
            buffer.vertex(matrix, width.toFloat(), height.toFloat(), 0f).color(r, g, b, alpha).endVertex()
            buffer.vertex(matrix, width.toFloat(), 0f, 0f).color(r, g, b, alpha).endVertex()
            tesselator.end()
            poseStack.popPose()
            RenderSystem.disableBlend()

        } else if (ebac >= 6) {
            val alpha = ((((ebac - 8.0) / 6.0) * 0.01 + 0.01).coerceIn(0.02, 0.05)).toFloat()
            val texture = ResourceLocation("branik_mod", "textures/gui/random_overlay.png")

            RenderSystem.enableBlend()
            RenderSystem.setShader { GameRenderer.getPositionTexShader() }
            RenderSystem.setShaderTexture(0, texture)
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha)

            poseStack.pushPose()
            val scrollX = (mc.level!!.gameTime % 256).toInt()
            val scrollY = ((mc.level!!.gameTime * 0.5) % 256).toInt()
            guiGraphics.blit(texture, -scrollX, -scrollY, 0f, 0f, width + 256, height + 256, 256, 256)
            poseStack.popPose()

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.disableBlend()
        }
    }
}

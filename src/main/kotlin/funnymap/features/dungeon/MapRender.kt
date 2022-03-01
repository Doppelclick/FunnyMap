package funnymap.features.dungeon

import funnymap.FunnyMap.Companion.config
import funnymap.FunnyMap.Companion.inDungeons
import funnymap.FunnyMap.Companion.mc
import funnymap.core.*
import funnymap.utils.MapUtils
import funnymap.utils.MapUtils.roomSize
import funnymap.utils.RenderUtils
import funnymap.utils.Utils.equalsOneOf
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object MapRender {

    private val neuGreen = ResourceLocation("funnymap", "neu/green_check.png")
    private val neuWhite = ResourceLocation("funnymap", "neu/white_check.png")
    private val neuCross = ResourceLocation("funnymap", "neu/cross.png")
    private val defaultGreen = ResourceLocation("funnymap", "default/green_check.png")
    private val defaultWhite = ResourceLocation("funnymap", "default/white_check.png")
    private val defaultCross = ResourceLocation("funnymap", "default/cross.png")

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !inDungeons || !config.mapEnabled) return
        if (config.mapHideInBoss && Dungeon.inBoss || !Dungeon.hasScanned) return

        GlStateManager.pushMatrix()
        GlStateManager.translate(config.mapX.toFloat(), config.mapY.toFloat(), 0f)
        GlStateManager.scale(config.mapScale, config.mapScale, 1f)

        RenderUtils.renderRect(
            0.0,
            0.0,
            128.0,
            if (config.mapShowRunInformation) 138.0 else 128.0,
            config.mapBackground
        )

        RenderUtils.renderRectBorder(
            0.0,
            0.0,
            128.0,
            if (config.mapShowRunInformation) 138.0 else 128.0,
            config.mapBorderWidth.toDouble(),
            config.mapBorder
        )

        GlStateManager.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        renderRooms()
        if (mc.currentScreen !is MoveMapGui) {
            renderText()
            renderPlayerHeads()
            if (config.mapShowRunInformation) {
                renderRunInformation()
            }
        }
        GlStateManager.popMatrix()
    }

    private fun renderRooms() {
        val connectorSize = roomSize shr 2

        for (y in 0..10) {
            for (x in 0..10) {
                val tile = Dungeon.dungeonList[y * 11 + x]
                if (tile is Door && tile.type == DoorType.NONE) continue

                val xOffset = (x shr 1) * (roomSize + connectorSize)
                val yOffset = (y shr 1) * (roomSize + connectorSize)

                val xEven = x and 1 == 0
                val yEven = y and 1 == 0

                val color = if (config.mapDarkenUndiscovered && tile.state == RoomState.UNDISCOVERED) {
                    tile.getColor().run {
                        Color(
                            (red * (1 - config.mapDarkenPercent)).toInt(),
                            (green * (1 - config.mapDarkenPercent)).toInt(),
                            (blue * (1 - config.mapDarkenPercent)).toInt(),
                            alpha
                        )
                    }
                } else tile.getColor()

                when {
                    xEven && yEven -> if (tile is Room) {
                        RenderUtils.renderRect(
                            xOffset.toDouble(),
                            yOffset.toDouble(),
                            roomSize.toDouble(),
                            roomSize.toDouble(),
                            color
                        )
                    }
                    !xEven && !yEven -> {
                        RenderUtils.renderRect(
                            xOffset.toDouble(),
                            yOffset.toDouble(),
                            (roomSize + connectorSize).toDouble(),
                            (roomSize + connectorSize).toDouble(),
                            color
                        )
                    }
                    else -> drawRoomConnector(
                        xOffset,
                        yOffset,
                        connectorSize,
                        tile is Door,
                        !xEven,
                        color
                    )
                }
            }
        }

    }

    private fun renderText() {
        val connectorSize = roomSize shr 2

        for (y in 0..10 step 2) {
            for (x in 0..10 step 2) {

                val tile = Dungeon.dungeonList[y * 11 + x]

                if (tile is Room && tile in Dungeon.uniqueRooms) {

                    val xOffset = (x shr 1) * (roomSize + connectorSize)
                    val yOffset = (y shr 1) * (roomSize + connectorSize)

                    if (config.mapCheckmark != 0 && config.mapRoomSecrets != 2) {
                        getCheckmark(tile.state, config.mapCheckmark)?.let {
                            GlStateManager.enableAlpha()
                            GlStateManager.color(255f, 255f, 255f, 255f)
                            mc.textureManager.bindTexture(it)
                            Gui.drawModalRectWithCustomSizedTexture(
                                xOffset + 2, yOffset + 2,
                                0f, 0f, roomSize - 4, roomSize - 4, roomSize - 4f, roomSize - 4f
                            )
                            GlStateManager.disableAlpha()
                        }
                    }

                    val name = mutableListOf<String>()

                    if (config.mapRoomNames != 0 && tile.data.type.equalsOneOf(RoomType.PUZZLE, RoomType.TRAP) ||
                        config.mapRoomNames == 2 && tile.data.type.equalsOneOf(
                            RoomType.NORMAL,
                            RoomType.RARE,
                            RoomType.CHAMPION
                        )
                    ) {
                        name.addAll(tile.data.name.split(" "))
                    }
                    if (tile.data.type == RoomType.NORMAL && config.mapRoomSecrets == 1) {
                        name.add(tile.data.secrets.toString())
                    }

                    val color = if (config.mapColorText) when (tile.state) {
                        RoomState.GREEN -> 0x55ff55
                        RoomState.CLEARED, RoomState.FAILED -> 0xffffff
                        else -> 0xaaaaaa
                    } else 0xffffff

                    // Offset + half of roomsize
                    RenderUtils.renderCenteredText(name, xOffset + (roomSize shr 1), yOffset + (roomSize shr 1), color)
                }
            }
        }
    }

    private fun getCheckmark(state: RoomState, type: Int): ResourceLocation? {
        return when (type) {
            1 -> when (state) {
                RoomState.CLEARED -> defaultWhite
                RoomState.GREEN -> defaultGreen
                RoomState.FAILED -> defaultCross
                else -> null
            }
            2 -> when (state) {
                RoomState.CLEARED -> neuWhite
                RoomState.GREEN -> neuGreen
                RoomState.FAILED -> neuCross
                else -> null
            }
            else -> null
        }
    }

    private fun renderPlayerHeads() {
        val multiplier = (roomSize + 4.0) / Dungeon.roomSize
        for (player in Dungeon.dungeonTeamates) {
            RenderUtils.drawPlayerHead(player, multiplier)
        }
    }

    private fun drawRoomConnector(
        x: Int,
        y: Int,
        doorWidth: Int,
        doorway: Boolean,
        vertical: Boolean,
        color: Color
    ) {
        val doorwayOffset = if (roomSize == 16) 5 else 6
        val width = if (doorway) 6 else roomSize
        var x1 = if (vertical) x + roomSize else x
        var y1 = if (vertical) y else y + roomSize
        if (doorway) {
            if (vertical) y1 += doorwayOffset else x1 += doorwayOffset
        }
        RenderUtils.renderRect(
            x1.toDouble(), y1.toDouble(),
            (if (vertical) doorWidth else width).toDouble(),
            (if (vertical) width else doorWidth).toDouble(),
            color
        )
    }

    private fun renderRunInformation() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(0f - MapUtils.startCorner.first, 128f - MapUtils.startCorner.second, 0f)
        GlStateManager.scale(0.66, 0.66, 1.0)
        mc.fontRendererObj.drawString("Secrets: ${RunInformation.secretCount}/${Dungeon.secretCount}", 5, 0, 0xffffff)
        mc.fontRendererObj.drawString("Crypts: ${RunInformation.cryptsCount}", 85, 0, 0xffffff)
        mc.fontRendererObj.drawString("Deaths: ${RunInformation.deathCount}", 140, 0, 0xffffff)
        GlStateManager.popMatrix()
    }
}

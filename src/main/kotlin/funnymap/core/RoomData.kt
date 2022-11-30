package funnymap.core

import funnymap.core.map.RoomType

data class RoomData(
    val name: String,
    val type: RoomType,
    val secrets: Int,
    val cores: List<Int>,
    val trappedChests: Int,
)

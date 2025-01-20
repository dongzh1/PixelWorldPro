package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.world.Config
import com.dongzh1.pixelworldpro.world.Level
import com.dongzh1.pixelworldpro.world.WorldImpl
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import top.shadowpixel.shadowlevels.api.events.PlayerLevelsModifiedEvent
import java.util.*

class ShadowLevels : Listener {
    @EventHandler
    fun shadowLevelUp(e: PlayerLevelsModifiedEvent) {
        if (Level.config.getBoolean("shadowLevels.enable")) {
            val player = e.player
            val level = e.levelSystem.getLevelData(player).levels
            val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return
            worldData.worldLevel = level.toString()
            PixelWorldPro.databaseApi.setWorldData(player.uniqueId, worldData)
            val dimensionData = Config.getWorldDimensionData(worldData.worldName)
            val dimensionlist = dimensionData.createlist
            for (dimension in dimensionlist) {
                val worlds = Bukkit.getWorld(worldData.worldName.lowercase(Locale.getDefault()) + "/" + dimension)
                if (worlds != null) {
                    //世界边界更新
                    WorldImpl.setWorldBorder(worlds, level.toString())
                }
            }
        }
    }
}
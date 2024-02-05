package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import me.casperge.realisticseasons.api.SeasonChangeEvent
import me.casperge.realisticseasons.api.SeasonsAPI
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent

class RealisticSeasons : Listener {
    private val seasons = SeasonsAPI.getInstance()
    @EventHandler
    fun seasonChange(event: SeasonChangeEvent){
        val world = event.world
        if (WorldProtect.getWorldNameUUID(world.name) == null){
            return
        }
        val mainWorld = Bukkit.getWorld(PixelWorldPro.instance.advancedWorldSettings.getString("expansion.RealisticSeasons.world")!!)
        val mainSeason = seasons.getSeason(mainWorld)
        if (event.newSeason != mainSeason) {
            seasons.setSeason(world, mainSeason)
        }
    }

    @EventHandler
    fun worldLoad(event: WorldLoadEvent) {
        val world = event.world
        if (WorldProtect.getWorldNameUUID(world.name) == null){
            return
        }
        val mainWorld = Bukkit.getWorld(PixelWorldPro.instance.advancedWorldSettings.getString("expansion.RealisticSeasons.world")!!)
        val mainSeason = seasons.getSeason(mainWorld)
        seasons.setSeason(world, mainSeason)
    }
}
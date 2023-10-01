package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import org.bukkit.Bukkit
import java.io.File
import java.util.*

object WorldFile {
    //检查世界文件是否损坏
    fun isBreak(file: File): String {
        return "ok"

       }
    private fun lang(string: String): String{
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
    fun saveWorld(localWorldList: MutableList<UUID>){
        for (uuid in localWorldList) {
            val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
            if (worldData != null) {
                val dimensionData = Config.getWorldDimensionData(worldData.worldName)
                for (dimension in dimensionData.createlist) {
                    Bukkit.getWorld("${worldData.worldName}/$dimension")?.save()
                }
            }
        }
    }
}
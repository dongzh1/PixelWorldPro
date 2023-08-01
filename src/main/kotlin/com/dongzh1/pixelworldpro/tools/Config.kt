package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.database.WorldDimensionData
import com.dongzh1.pixelworldpro.tools.Dimension.getDimensionList
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object Config {
    private fun getWorldDimensionconfig(worldname: String): YamlConfiguration {
        val config = File("./$worldname", "world.yml")
        val data = YamlConfiguration()
        if (!config.exists()) {
            config.createNewFile()
        }
        data.load(config)
        data.set("version", 1)
        val dimensionlist = getDimensionList()
        for (dimension in dimensionlist) {
            if(!data.isSet(dimension)){
                data.set(dimension, false)
            }
        }
        data.save(config)
        return data
    }
    fun getWorldDimensionData(worldname: String): WorldDimensionData {
        val data = getWorldDimensionconfig(worldname)
        val dimensionlist = getDimensionList()
        val createlist = ArrayList<String>()
        val discreatelist = ArrayList<String>()
        for (dimension in dimensionlist) {
            val back = data.getBoolean(dimension)
            if (back) {
                createlist.add(dimension)
            } else {
                discreatelist.add(dimension)
            }
        }
        createlist.add("world")
        return WorldDimensionData(
            createlist,
            discreatelist
        )
    }

    fun setWorldDimensionData(worldname: String, dimension: String, finish: Any){
        val config = File("./$worldname", "world.yml")
        val data = getWorldDimensionconfig(worldname)
        data.set(dimension, finish)
        data.save(config)
    }
}
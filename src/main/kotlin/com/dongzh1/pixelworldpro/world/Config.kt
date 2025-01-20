package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.WorldDimensionData
import com.dongzh1.pixelworldpro.world.Dimension.getDimensionList
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object Config {
    private fun getWorldDimensionConfig(worldName: String): YamlConfiguration {
        val config = File("${PixelWorldPro.instance.config.getString("WorldPath")}/$worldName", "world.yml")
        val data = YamlConfiguration()
        if (!config.exists()) {
            config.createNewFile()
        }
        data.load(config)
        when (data.getInt("version")) {
            1 -> {
                data.set("version", 2)
                data.set("seed", "0")
            }

            2 -> {

            }

            else -> {
                data.set("version", 2)
                data.set("seed", "0")
            }
        }
        val dimensionlist = getDimensionList()
        for (dimension in dimensionlist) {
            if (!data.isSet(dimension)) {
                data.set(dimension, false)
            }
        }
        data.save(config)
        return data
    }

    fun getWorldDimensionData(realWorldName: String): WorldDimensionData {
        val worldName = if (realWorldName.startsWith("PixelWorldPro/")) {
            val nameList = realWorldName.split("/")
            nameList[1]
        } else {
            val nameList = realWorldName.split("/")
            nameList[0]
        }
        val data = getWorldDimensionConfig(worldName)
        val dimensionList = getDimensionList()
        val createList = ArrayList<String>()
        val discreateList = ArrayList<String>()
        for (dimension in dimensionList) {
            val back = data.getBoolean(dimension)
            if (back) {
                createList.add(dimension)
            } else {
                discreateList.add(dimension)
            }
        }
        createList.add("world")
        return WorldDimensionData(
            createList,
            discreateList,
            data.getString("seed") ?: "0"
        )
    }

    fun setWorldDimensionData(realWorldName: String, dimension: String, finish: Any) {
        val worldName = if (realWorldName.startsWith("PixelWorldPro/")) {
            val nameList = realWorldName.split("/")
            nameList[1]
        } else {
            val nameList = realWorldName.split("/")
            nameList[0]
        }
        val config = File("${PixelWorldPro.instance.config.getString("WorldPath")}/$worldName", "world.yml")
        val data = getWorldDimensionConfig(worldName)
        data.set(dimension, finish)
        data.save(config)
    }

    fun getStructureConfig(name: String): YamlConfiguration? {
        return try {
            BuiltInConfiguration("structure/Hellgate.yml")
        } catch (_: Exception) {
            null
        }
    }
}
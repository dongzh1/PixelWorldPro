package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.WorldData
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

object Clean {
    init {
        if (!File("./PixelWorldPro/clean.yml").exists()) {
            File("./PixelWorldPro/clean.yml").createNewFile()
        }
    }

    val config = YamlConfiguration.loadConfiguration(File("./PixelWorldPro/clean.yml"))
    private val nextTime = PixelWorldPro.instance.config.getInt("clean")

    fun load(world: WorldData) {
        val time = Calendar.getInstance().getTimeInMillis() / 1000 / 60 / 60 / 24
        if (nextTime == 0) {
            config.set(world.worldName, time + 900000000000000000)
            config.save(File("./PixelWorldPro/clean.yml"))
        } else {
            config.set(world.worldName, time + nextTime)
            config.save(File("./PixelWorldPro/clean.yml"))
        }
        submit(async = true) {
            clean(false)
        }
    }

    fun clean(all: Boolean = false) {
        val time = Calendar.getInstance().getTimeInMillis() / 1000 / 60 / 60 / 24
        if (all) {
            for (world in PixelWorldPro.databaseApi.getWorldDataMap().values) {
                val oldTime = config.getInt(world.worldName)
                if (oldTime < time) {
                    File(world.worldName).delete()
                    config.set(world.worldName, null)
                    config.save(File("./PixelWorldPro/clean.yml"))
                }
            }
        } else {
            for (world in config.getKeys(false)) {
                val oldTime = config.getInt(world)
                if (oldTime < time) {
                    File(world).delete()
                    config.set(world, null)
                    config.save(File("./PixelWorldPro/clean.yml"))
                }
            }
        }
    }
}
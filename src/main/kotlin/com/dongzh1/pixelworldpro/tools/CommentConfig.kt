package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.world.Level
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit

import org.bukkit.configuration.file.YamlConfiguration

import java.io.File

import java.io.InputStreamReader


object CommentConfig : YamlConfiguration() {

    fun updateLang(path:String){
        //获取文件夹的语言文件
        updateYaml("lang/$path.yml")
    }
    fun updateConfig(){
        submit(async = true) {
            while (true) {
                when (PixelWorldPro.instance.config.getInt("version")) {
                    2 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Config配置文件")
                        PixelWorldPro.instance.config.set("version", 3)
                        PixelWorldPro.instance.config.set("WorldSetting.Gamemode.owner", "SURVIVAL")
                        PixelWorldPro.instance.config.set("WorldSetting.Gamemode.member", "SURVIVAL")
                        PixelWorldPro.instance.config.set("WorldSetting.Gamemode.anyone", "ADVENTURE")
                        PixelWorldPro.instance.saveConfig()
                    }

                    3 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Config配置文件")
                        PixelWorldPro.instance.config.set("version", 4)
                        PixelWorldPro.instance.config.set("WorldSetting.Creater.World", "auto")
                        PixelWorldPro.instance.config.set("lobby", "lobby")
                        PixelWorldPro.instance.saveConfig()
                    }

                    4 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Config配置文件")
                        PixelWorldPro.instance.config.set("version", 5)
                        PixelWorldPro.instance.config.set("WorldSetting.unloadTime", 30)
                        PixelWorldPro.instance.saveConfig()
                    }

                    5 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Config配置文件")
                        PixelWorldPro.instance.config.set("version", 6)
                        PixelWorldPro.instance.config.set("WorldSetting.Inviter.permission", "anyone")
                        PixelWorldPro.instance.config.set("Papi.state.inviter", "&a邀请")
                        PixelWorldPro.instance.saveConfig()
                    }
                    6 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Config配置文件")
                        PixelWorldPro.instance.config.set("version", 7)
                        PixelWorldPro.instance.config.set("mainCommand", "pwp")
                        PixelWorldPro.instance.saveConfig()
                    }
                    7 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Config配置文件")
                        PixelWorldPro.instance.config.set("version", 8)
                        PixelWorldPro.instance.config.set("mainPapi", "pixelworldpro")
                        PixelWorldPro.instance.config.set("debug", false)
                        PixelWorldPro.instance.saveConfig()
                    }
                    8 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Config配置文件")
                        PixelWorldPro.instance.config.set("version", 9)
                        PixelWorldPro.instance.config.set("WorldSetting.saveTime", 5)
                        PixelWorldPro.instance.saveConfig()
                    }
                }
                when (Level.config.getInt("version")) {
                    1 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Level配置文件")
                        Level.config.set("version", 2)
                        Level.config.set("shadowLevels.enable", false)
                        Level.config.set("shadowLevels.mode", "all")
                        Level.config.set("shadowLevels.levelName", "universe")
                        Level.config.saveToFile()
                    }
                }
                when (PixelWorldPro.instance.dimensionconfig.getInt("version")) {
                    1 -> {
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新Dimension配置文件")
                        PixelWorldPro.instance.dimensionconfig.set("version", 2)
                        PixelWorldPro.instance.dimensionconfig.set("Structure.netherEnable", true)
                        PixelWorldPro.instance.dimensionconfig.set("Structure.netherFile", "Hellgate")
                        PixelWorldPro.instance.dimensionconfig.set("Structure.firstCreate.world", "none")
                        PixelWorldPro.instance.dimensionconfig.set("Structure.firstCreate.nether", "none")
                        PixelWorldPro.instance.dimensionconfig.set("Structure.firstCreate.the_end", "none")
                        PixelWorldPro.instance.dimensionconfig.saveToFile()
                    }

                    else -> return@submit
                }
            }
        }
    }
    private fun updateYaml(path: String){
        //获取插件文件的文件
        val yamlFile = File(PixelWorldPro.instance.dataFolder, path)
        //获取插件内的语言文件
        val yaml = InputStreamReader(PixelWorldPro.instance.getResource(path)!!)

        val yamlFileConfig = loadConfiguration(yamlFile)
        val yamlConfig = loadConfiguration(yaml)
        //遍历两个的所有key
        for (key in yamlConfig.getKeys(true)) {
            //如果文件中没有这个key就写入
            if (!yamlFileConfig.contains(key)) {
                yamlFileConfig.set(key, yamlConfig.get(key))
            }
        }
        yamlFileConfig.save(yamlFile)
    }



}
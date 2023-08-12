package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
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
        val configFile = File(PixelWorldPro.instance.dataFolder, "config.yml")
        when(PixelWorldPro.instance.config.getInt("version")){
            1 ->{
                PixelWorldPro.instance.config.set("version", 2)
                PixelWorldPro.instance.saveConfig()
                configFile.appendText("#是否使用worldBorder插件生成世界边界\n" +
                        "#Whether to use the worldBorder plugin to generate the world border\n" +
                        "WorldBorder: false\n")
            }
            2 ->{
                PixelWorldPro.instance.config.set("version", 3)
                PixelWorldPro.instance.config.set("WorldSetting.Gamemode.owner","SURVIVAL")
                PixelWorldPro.instance.config.set("WorldSetting.Gamemode.member","SURVIVAL")
                PixelWorldPro.instance.config.set("WorldSetting.Gamemode.anyone","ADVENTURE")
                PixelWorldPro.instance.saveConfig()
            }
            3 ->{
                PixelWorldPro.instance.config.set("version", 4)
                PixelWorldPro.instance.config.set("WorldSetting.Creater.World","auto")
                PixelWorldPro.instance.config.set("lobby","lobby")
                PixelWorldPro.instance.saveConfig()
            }
            4 ->{
                Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新配置文件")
                PixelWorldPro.instance.config.set("version", 5)
                PixelWorldPro.instance.config.set("WorldSetting.unloadTime",30)
                PixelWorldPro.instance.saveConfig()
            }
            5 ->{
                Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 更新配置文件")
                PixelWorldPro.instance.config.set("version", 6)
                PixelWorldPro.instance.config.set("WorldSetting.Inviter.permission", "anyone")
                PixelWorldPro.instance.config.set("Papi.state.inviter", "&a邀请")
                PixelWorldPro.instance.saveConfig()
            }
            else ->return
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
package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader


object CommentConfig  {
    fun updateLang(path:String){
        //获取文件夹的语言文件
        updateYaml("lang/$path.yml")
    }
    fun updateConfig(path:String){
        val configFile = File(PixelWorldPro.instance.dataFolder, path)
        val config = PixelWorldPro.instance.getResource(path)

    }
    private fun updateYaml(path: String){
        //获取插件文件的文件
        val yamlFile = File(PixelWorldPro.instance.dataFolder, path)
        //获取插件内的语言文件
        val yaml = InputStreamReader(PixelWorldPro.instance.getResource(path)!!)

        val yamlFileConfig = YamlConfiguration.loadConfiguration(yamlFile)
        val yamlConfig = YamlConfiguration.loadConfiguration(yaml)
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
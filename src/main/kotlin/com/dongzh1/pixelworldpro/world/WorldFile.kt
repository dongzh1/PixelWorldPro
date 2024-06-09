package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import org.bukkit.World
import java.io.File
import java.util.*

object WorldFile {
    private val config = PixelWorldPro.instance.config
    val worldSetting = BuiltInConfiguration("AdvancedWorldSettings.yml")
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
                    val world = Bukkit.getWorld("${worldData.worldName}/$dimension")
                    if (world != null){
                        submit {
                            world.save()
                        }
                    }
                }
            }
        }
    }
    fun setWorldLocation(world: World){
        if (config.getBoolean("debug")){
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 设置世界 ${world.name} 的出生点")
        }
        var x = worldSetting.getDouble("location.x.set")
        var y = worldSetting.getDouble("location.y.set")
        var z = worldSetting.getDouble("location.z.set")
        if (worldSetting.getInt("location.max") > 0) {
            val maxTimes = worldSetting.getInt("location.max")
            var times = 0
            while (times < maxTimes){
                if (config.getBoolean("debug")){
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 判断出生点 $x $y $z")
                }
                val block1 = world.getBlockAt(x.toInt(), y.toInt(), z.toInt())
                val block2 = world.getBlockAt(x.toInt(), y.toInt()+1, z.toInt())
                val block3 = world.getBlockAt(x.toInt(), y.toInt()+2, z.toInt())
                val block4 = world.getBlockAt(x.toInt(), y.toInt()-1, z.toInt())
                try {
                    if ((block1.blockData.material == org.bukkit.Material.AIR).and(block2.blockData.material == org.bukkit.Material.AIR).and(block3.blockData.material == org.bukkit.Material.AIR).and(block4.blockData.material == org.bukkit.Material.AIR)){
                        world.spawnLocation.x = x
                        world.spawnLocation.y = y
                        world.spawnLocation.z = z
                        if (config.getBoolean("debug")){
                            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 判断出生点 $x $y $z 通过")
                        }
                        return
                    }else{
                        if (config.getBoolean("debug")){
                            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 判断出生点 $x $y $z 失败")
                        }
                        val shifting = Random().nextInt(10) - 5
                        if (worldSetting.getBoolean("location.x.shifting")){
                            x += shifting.toDouble()
                        }
                        if (worldSetting.getBoolean("location.y.shifting")){
                            y += shifting.toDouble()
                        }
                        if (worldSetting.getBoolean("location.z.shifting")){
                            z += shifting.toDouble()
                        }
                    }
                } catch (_: NoSuchMethodError) {
                }

                times += 1
            }
        }
        world.spawnLocation.x = x
        world.spawnLocation.y = y
        world.spawnLocation.z = z
    }
}
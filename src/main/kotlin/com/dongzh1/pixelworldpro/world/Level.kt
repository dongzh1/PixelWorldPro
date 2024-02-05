package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.world.WorldImpl.lang
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.item.hasItem
import com.xbaimiao.easylib.module.item.takeItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.collections.HashMap


object Level {
    val config = BuiltInConfiguration("Level.yml")
    fun buildLevel(): HashMap<Int, LevelData>{
        val levelMap = HashMap<Int, LevelData>()
        val configLevelList = config.getConfigurationSection("level")!!.getKeys(false)
        var configLevelSizeStart = 0
        var level = 1
        var lastConfigLevel = 1
        while (configLevelSizeStart < configLevelList.size){
            if (level.toString() in configLevelList){
                lastConfigLevel = level
                val maxLevel = config.getBoolean("level.$level.maxLevel")
                val barrier = config.getInt("level.$level.barrier")
                val permission = config.getString("level.$level.up.permission")?:"none"
                val noPermission = config.getString("level.$level.up.noPermission")?:"达到当前权限下最大等级"
                val points = config.getInt("level.$level.up.points")
                val money = config.getInt("level.$level.up.money")
                val itemList = config.getStringList("level.$level.up.item")
                val permissionUpList = config.getStringList("level.$level.up.permissionUp")
                val levelData = LevelData(
                    level,
                    maxLevel,
                    barrier,
                    permission,
                    noPermission,
                    points,
                    money,
                    itemList,
                    permissionUpList
                )
                levelMap[level] = levelData
                level += 1
                configLevelSizeStart += 1
            }else{
                val levelData = levelMap[lastConfigLevel]!!
                levelData.level = level
                levelMap[level] = levelData
                level += 1
            }
        }
        return levelMap
    }
    fun getItemData(name: String): ItemData{
        val material = config.getString("item.$name.material")!!.uppercase(Locale.getDefault())
        val lore = config.getStringList("item.$name.lore")
        return ItemData(
            material,
            lore
        )
    }
    fun levelUp(uuid: UUID): String{
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)?: return "无法找到 $uuid 的世界"
        val level = worldData.worldLevel.toInt()
        val nextLevel = level + 1
        val levelMap = buildLevel()
        val levelData = levelMap[nextLevel]?: return "已达到最大等级"
        if (levelData.permission != "none"){
            val player = Bukkit.getPlayer(uuid)?: return "设置了权限升级的等级，需要玩家在线以升级世界"
            val hasPermission = player.hasPermission(levelData.permission)
            if (!hasPermission){
                return levelData.noPermission
            }
        }
        if ("none" !in levelData.permissionUp){
            val player = Bukkit.getPlayer(uuid)?: return "设置了权限升级的等级，需要玩家在线以升级世界"
            for (permission in levelData.permissionUp) {
                val hasPermission = player.hasPermission(permission)
                if (hasPermission) {
                    worldData.worldLevel = nextLevel.toString()
                    PixelWorldPro.databaseApi.setWorldData(uuid, worldData)
                    val dimensionData = Config.getWorldDimensionData(worldData.worldName)
                    val dimensionlist = dimensionData.createlist
                    for (dimension in dimensionlist) {
                        if (PixelWorldPro.instance.config.getBoolean("debug")){
                            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 扩展世界 ${worldData.worldName.lowercase(Locale.getDefault()) + "/" + dimension} 的边界")
                        }
                        val worlds = Bukkit.getWorld(worldData.worldName.lowercase(Locale.getDefault()) + "/" + dimension)
                        if (worlds != null) {
                            //世界边界更新
                            WorldImpl.setWorldBorder(worlds, nextLevel.toString())
                        }else{
                            if (PixelWorldPro.instance.config.getBoolean("debug")){
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 扩展世界 ${worldData.worldName.lowercase(Locale.getDefault()) + "/" + dimension} 的边界失败：世界不存在")
                            }
                        }
                    }
                    return lang("LevelUp")
                }
            }
        }
        Bukkit.getConsoleSender().sendMessage("points:${levelData.points}  vault:${levelData.money}")
        val hasPoints = if (levelData.points != 0){
            PlayerPoints().has(Bukkit.getOfflinePlayer(uuid), levelData.points.toDouble())
        }else{
            true
        }
        val hasMoney = if (levelData.money != 0){
            Vault().has(Bukkit.getOfflinePlayer(uuid), levelData.money.toDouble())
        }else{
            true
        }
        if ((!hasPoints).or(!hasMoney)){
            return "点券/金币不足"
        }else{
            if (levelData.points != 0) {
                PlayerPoints().take(Bukkit.getOfflinePlayer(uuid), levelData.points.toDouble())
            }
            if (levelData.money != 0) {
                Vault().take(Bukkit.getOfflinePlayer(uuid), levelData.money.toDouble())
            }
        }
        if ("none" !in levelData.item){
            val player = Bukkit.getPlayer(uuid)?: return "设置了消耗物品升级的等级，需要玩家在线以升级世界"
            val itemMap = HashMap<String, Int>()
            for (name in levelData.item) {
                var number = itemMap[name]?: 0
                number += 1
                itemMap[name] = number
            }
            for (key in itemMap.keys) {
                val itemData = getItemData(key)
                val items = ItemStack(Material.getMaterial(itemData.material)!!)
                if (!player.inventory.hasItem(items, itemMap[key]!!)) {
                    return "升级所需的物资不足"
                }
            }
            for (key in itemMap.keys) {
                val itemData = getItemData(key)
                player.inventory.takeItem(itemMap[key]!!) {
                    return@takeItem this.type == Material.getMaterial(itemData.material)!!
                }
            }
        }
        worldData.worldLevel = nextLevel.toString()
        PixelWorldPro.databaseApi.setWorldData(uuid, worldData)
        //获取世界是否加载
        val dimensionData = Config.getWorldDimensionData(worldData.worldName)
        val dimensionlist = dimensionData.createlist
        for (dimension in dimensionlist) {
            if (PixelWorldPro.instance.config.getBoolean("debug")){
                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 扩展世界 ${worldData.worldName.lowercase(Locale.getDefault()) + "/" + dimension} 的边界")
            }
            val worlds = Bukkit.getWorld(worldData.worldName.lowercase(Locale.getDefault()) + "/" + dimension)
            if (worlds != null) {
                //世界边界更新
                WorldImpl.setWorldBorder(worlds, nextLevel.toString())
            }else{
                if (PixelWorldPro.instance.config.getBoolean("debug")){
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 扩展世界 ${worldData.worldName.lowercase(Locale.getDefault()) + "/" + dimension} 的边界失败：世界不存在")
                }
            }
        }
        return lang("LevelUp")
    }
}
data class LevelData(
    var level: Int,
    val maxLevel: Boolean,
    val barrier: Int,
    val permission: String,
    val noPermission: String,
    val points: Int,
    val money: Int,
    val item: List<String>,
    val permissionUp: List<String>
)
data class ItemData(
    val material: String,
    val lore: List<String>
)
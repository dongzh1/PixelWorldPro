package com.dongzh1.pixelworldpro.papi

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.bridge.PlaceholderExpansion
import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.module.utils.colored
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.text.SimpleDateFormat
import java.util.*


object Papi: PlaceholderExpansion() {
    override val identifier: String
        get() = "pixelworldpro"
    override val version: String
        get() = "1.1.6"

    override fun onRequest(p: OfflinePlayer, params: String): String? {
        when(params){
            "showname" -> {
                //这个玩家对应世界的显示名
                return PixelWorldPro.instance.config.getString("Papi.showname")!!.colored().replacePlaceholder(p)
            }
            "showname world" ->{
                //玩家所在世界的显示名
                val worldName = (p as Player).world.name
                val worlds = PixelWorldPro.instance.config.getConfigurationSection("Papi.shownameSet")!!.getKeys(false)
                for (world in worlds){
                    if (worldName == world){
                        return PixelWorldPro.instance.config.getString("Papi.shownameSet.$world")!!.colored().replacePlaceholder(p)
                    }
                }
                val uuid = getUUID(worldName) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                return PixelWorldPro.instance.config.getString("Papi.showname")!!.colored().replacePlaceholder(Bukkit.getOfflinePlayer(uuid))
            }
            "createtime" -> {
                val worldData = PixelWorldPro.databaseApi.getWorldData(p.uniqueId) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                val createTime = worldData.createTime.toLong()
                val date = Date(createTime)
                //把time时间格式化
                val formatter = SimpleDateFormat(
                    PixelWorldPro.instance.config.getString("Papi.createtime")!!.replacePlaceholder(p).colored()
                )
                //把time时间格式化为字符串
                return formatter.format(date)
            }
            "createtime world" ->{
                val worldName = (p as Player).world.name
                val uuid = getUUID(worldName) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(Bukkit.getOfflinePlayer(uuid))
                val createTime = worldData.createTime.toLong()
                val date = Date(createTime)
                val formatter = SimpleDateFormat(
                    PixelWorldPro.instance.config.getString("Papi.createtime")!!.replacePlaceholder(Bukkit.getOfflinePlayer(uuid)).colored()
                )
                return formatter.format(date)
            }
            "worldlevel" ->{
                val worldData = PixelWorldPro.databaseApi.getWorldData(p.uniqueId) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                return worldData.worldLevel
            }
            "worldlevel world" ->{
                val worldName = (p as Player).world.name
                val uuid = getUUID(worldName) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(Bukkit.getOfflinePlayer(uuid))
                return worldData.worldLevel
            }
            "state" ->{
                val worldData = PixelWorldPro.databaseApi.getWorldData(p.uniqueId) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                when(worldData.state){
                    "anyone" ->{
                        return PixelWorldPro.instance.config.getString("Papi.state.anyone")!!.colored().replacePlaceholder(p)
                    }
                    "inviter" ->{
                        return PixelWorldPro.instance.config.getString("Papi.state.inviter")!!.colored().replacePlaceholder(p)
                    }
                    "member" ->{
                        return PixelWorldPro.instance.config.getString("Papi.state.member")!!.colored().replacePlaceholder(p)
                    }
                    "owner" ->{
                        return PixelWorldPro.instance.config.getString("Papi.state.owner")!!.colored().replacePlaceholder(p)
                    }
                }
            }
            "state world" ->{
                val worldName = (p as Player).world.name
                val uuid = getUUID(worldName) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(Bukkit.getOfflinePlayer(uuid))
                when(worldData.state){
                    "anyone" ->{
                        return PixelWorldPro.instance.config.getString("Papi.state.anyone")!!.colored().replacePlaceholder(Bukkit.getOfflinePlayer(uuid))
                    }
                    "inviter" ->{
                        return PixelWorldPro.instance.config.getString("Papi.state.inviter")!!.colored().replacePlaceholder(p)
                    }
                    "member" ->{
                        return PixelWorldPro.instance.config.getString("Papi.state.member")!!.colored().replacePlaceholder(Bukkit.getOfflinePlayer(uuid))
                    }
                    "owner" ->{
                        return PixelWorldPro.instance.config.getString("Papi.state.owner")!!.colored().replacePlaceholder(Bukkit.getOfflinePlayer(uuid))
                    }
                }
            }
            "onlineplayernumber" ->{
                val worldData = PixelWorldPro.databaseApi.getWorldData(p.uniqueId) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                return worldData.onlinePlayerNumber.toString()
            }
            "onlineplayernumber world" ->{
                val worldName = (p as Player).world.name
                val uuid = getUUID(worldName) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(p)
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return PixelWorldPro.instance.config.getString("Papi.noRecord")!!.colored().replacePlaceholder(Bukkit.getOfflinePlayer(uuid))
                return worldData.onlinePlayerNumber.toString()
            }

        }
        return null
    }
    private fun getUUID(worldName: String): UUID? {
        val pattern = "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}".toRegex()
        return UUID.fromString(pattern.find(worldName)?.value ?: return null)
    }
}
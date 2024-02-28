package com.dongzh1.pixelworldpro.migrate

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object WorldMove {
    private val database = PixelWorldPro.databaseApi
    fun main(from: UUID, to: UUID, sender: CommandSender){
        sender.sendMessage("检查世界数据")
        val fromWorldData = database.getWorldData(from)
        if(fromWorldData == null){
            sender.sendMessage("无法找到世界数据${from}")
            return
        }
        var toWorldData = database.getWorldData(from)
        if(toWorldData == null){
            sender.sendMessage("无法找到世界数据${to}")
            sender.sendMessage("创建世界数据${to}")
        }
        sender.sendMessage("检查世界数据完成")
        if (PixelWorldPro.instance.isBungee()) {
            if (RedisManager.isLock(from)) {
                sender.sendMessage("世界${from}已加载")
                return
            }
            if (RedisManager.isLock(to)) {
                sender.sendMessage("世界${to}已加载")
                return
            }
        }
        val fromWorld = Bukkit.getWorld(fromWorldData.worldName + "/world")
        if(fromWorld != null){
            sender.sendMessage("世界${from}已加载")
            return
        }
        val time = System.currentTimeMillis()
        val createTime = time.toString()
        val date = Date(time)
        //把time时间格式化
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
        //把time时间格式化为字符串
        val timeString = formatter.format(date)
        //获取路径下对应的world文件夹
        val worldName = "${to}_$timeString"
        val toWorld = Bukkit.getWorld("PixelWorldPro/$worldName/world")
        if(toWorld != null){
            sender.sendMessage("世界${to}已加载")
            return
        }
        sender.sendMessage("移动世界文件")
        val fromWorldFile = File(fromWorldData.worldName)
        val toWorldFile = File("PixelWorldPro/$worldName")
        val back = movefile(fromWorldFile, toWorldFile)
        if(!back){
            sender.sendMessage("移动世界文件失败")
            return
        }
        sender.sendMessage("移动世界文件完成")
        sender.sendMessage("迁移世界数据")
        if(toWorldData == null){
            toWorldData = WorldData(
                "PixelWorldPro/$worldName",
                fromWorldData.worldLevel,
                arrayListOf(to),
                arrayListOf(Bukkit.getOfflinePlayer(to).name!!),
                arrayListOf(),
                arrayListOf(),
                "anyone",
                createTime,
                System.currentTimeMillis(),
                1,
                false,
                isCreateEnd = false,
                inviter = arrayListOf(),
                fromWorldData.gameRule,
                fromWorldData.location
            )
        }else{
            toWorldData = WorldData(
                "PixelWorldPro/$worldName",
                fromWorldData.worldLevel,
                toWorldData.members,
                toWorldData.memberName,
                toWorldData.banPlayers,
                toWorldData.banName,
                "anyone",
                createTime,
                System.currentTimeMillis(),
                1,
                false,
                isCreateEnd = false,
                arrayListOf(),
                fromWorldData.gameRule,
                fromWorldData.location
            )
        }
        database.deleteWorldData(from)
        database.setWorldData(to, toWorldData)
        sender.sendMessage("迁移世界完成")
    }

    private fun movefile(from: File, to: File): Boolean{
        return try {
            from.copyRecursively(to)
            from.delete()
            true
        }catch (e: Exception) {
            throw e
        }
    }
}
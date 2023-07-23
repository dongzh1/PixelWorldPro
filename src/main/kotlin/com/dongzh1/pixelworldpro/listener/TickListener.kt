package com.dongzh1.pixelworldpro.listener

import com.destroystokyo.paper.event.server.ServerTickEndEvent
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.concurrent.ConcurrentHashMap

object TickListener : Listener {

    private var indices = 0
    private var averages = 0.0
    private val durations1m = ConcurrentHashMap<Int, Double>()
    @EventHandler(priority = EventPriority.MONITOR)
    fun event(event: ServerTickEndEvent) {
        submit(async = true) {
            if (indices >= 600) {
                //定期存入redis
                RedisManager.setMspt(Bukkit.getTicksPerMonsterSpawns().toDouble())
                indices = 1
            }
        }
    }
    fun getLowestMsptServer(): String{
        val msptValue = RedisManager.getMspt()
        val msptMap = mutableMapOf<String,Double>()
        if (msptValue != null){
            for (server in msptValue.split(",")){
                if (server.contains(":")){
                    msptMap[server.split(":")[0]] = server.split(":")[1].toDouble()
                }
            }
        }
        //获取最低mspt
        var mspt = 0.0
        var serverName = ""
        for (server in msptMap){
            if (mspt == 0.0){
                mspt = server.value
                serverName = server.key
            }
            else if (mspt > server.value){
                mspt = server.value
                serverName = server.key
            }
        }
        return serverName
    }

}
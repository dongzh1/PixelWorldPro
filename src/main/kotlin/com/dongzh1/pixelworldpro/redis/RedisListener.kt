package com.dongzh1.pixelworldpro.redis

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.utils.submit

import org.bukkit.Bukkit
import redis.clients.jedis.JedisPubSub
import java.util.UUID

class RedisListener : JedisPubSub() {

    override fun onMessage(channel: String?, message: String?) {
        if (channel != PixelWorldPro.channel) {
            Bukkit.getConsoleSender().sendMessage("§cRedis收到错误的消息：$message")
            Bukkit.getConsoleSender().sendMessage("§c错误的频道：$channel")
        }else{
            Bukkit.getConsoleSender().sendMessage("§aRedis收到消息：$message")
            Bukkit.getConsoleSender().sendMessage("§a频道：$channel")
        }

    }
}
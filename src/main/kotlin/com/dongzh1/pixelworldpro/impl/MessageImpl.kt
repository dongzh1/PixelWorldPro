package com.dongzh1.pixelworldpro.impl

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.MessageApi
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import org.bukkit.Bukkit
import java.util.*

class MessageImpl : MessageApi {
    /**
     * 发送消息给玩家
     * @param uuid 玩家的uuid
     * @param message 消息
     */
    override fun sendMessage(uuid: UUID, message: String) {
        if (PixelWorldPro.instance.isBungee()) {
            if (Bukkit.getPlayer(uuid) == null) {
                RedisManager.push("sendMessage|,|$uuid|,|$message")
                return
            }
            Bukkit.getPlayer(uuid)?.sendMessage(message)
        } else
            Bukkit.getPlayer(uuid)?.sendMessage(message)
    }

}
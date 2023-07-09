package com.dongzh1.pixelworldpro.api

import com.dongzh1.pixelworldpro.impl.TeleportImpl
import org.bukkit.Location
import java.util.UUID

interface TeleportApi {
    /**
     * 传送玩家
     * @param uuid 玩家的uuid
     * @param location 传送的位置
     * @param serverName 传送到的服务器，如果为null则传送到本服
     */
    fun teleport(uuid: UUID,location: Location,serverName: String? = null)
    /**
     * 传送玩家到自己的世界，如果世界没加载则加载
     * @param uuid 玩家的uuid
     */
    fun teleport(uuid: UUID)
    /**
     * 传送玩家到指定玩家的世界,如果世界没加载则加载
     * @param uuid 玩家的uuid
     * @param playerUuid 指定玩家的uuid
     */
    fun teleport(uuid: UUID,playerUuid: UUID)

    object Factory {
        private var instance: TeleportApi? = null
        val teleportApi: TeleportApi?
            get() {
                if (instance == null) {
                    instance = TeleportImpl()
                }
                return instance
            }
    }
}
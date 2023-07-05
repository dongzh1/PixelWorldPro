package com.dongzh1.pixelworldpro.api

import com.dongzh1.pixelworldpro.impl.TeleportImpl
import java.util.UUID

interface TeleportApi {
    fun teleport(uuid: UUID,location: org.bukkit.Location)
    fun teleport(uuid: UUID,worldName: String)
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
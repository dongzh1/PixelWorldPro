package com.dongzh1.pixelworldpro.api

import com.dongzh1.pixelworldpro.impl.MessageImpl
import java.util.*

interface MessageApi {
    /**
     * 发送消息给玩家
     * @param uuid 玩家的uuid
     * @param message 消息
     */
    fun sendMessage(uuid: UUID, message: String)

    object Factory {
        private var instance: MessageApi? = null
        val messageApi: MessageApi?
            get() {
                if (instance == null) {
                    instance = MessageImpl()
                }
                return instance
            }
    }
}
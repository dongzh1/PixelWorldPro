package com.dongzh1.pixelworldpro.api

import com.dongzh1.pixelworldpro.impl.WorldImpl
import java.io.File
import java.util.UUID

interface WorldApi {
    /**
     * 根据模板文件夹名新建世界
     */
    fun createWorld(uuid: UUID, templateName: String)
    /**
     * 根据模板文件新建世界
     */
    fun createWorld(uuid: UUID, file: File)
    /**
     * 卸载指定玩家世界
     */
    fun unloadWorld(uuid: UUID)
    /**
     * 删除指定玩家世界
     */
    fun deleteWorld(uuid: UUID)
    /**
     * 重置指定玩家世界
     */
    fun resetWorld(uuid: UUID)

    object Factory {
        private var instance: WorldApi? = null
        val worldApi: WorldApi?
            get() {
                if (instance == null) {
                    instance = WorldImpl()
                }
                return instance
            }
    }
}
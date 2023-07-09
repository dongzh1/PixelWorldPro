package com.dongzh1.pixelworldpro.api


import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import java.util.*

interface DatabaseApi {

    /**
     * 创建或覆盖一个世界记录
     */

    fun setWorldData(uuid: UUID,worldData: WorldData)

    /**
     * 获取一个世界记录
     */
    fun getWorldData(uuid: UUID): WorldData?
    /**
     * 创建或覆盖一个玩家记录,此操作为同步数据库操作
     */
    fun setPlayerData(uuid: UUID,playerData: PlayerData)
    /**
     * 获取一个玩家记录，此操作为同步数据库操作
     */
    fun getPlayerData(uuid: UUID): PlayerData?
    /**
     * 删除一个世界记录
     */
    fun removeWorldData(uuid: UUID)

    /**
     * 将所有世界记录导入内存，一般插件加载时使用
     */
    fun importWorldData()


}
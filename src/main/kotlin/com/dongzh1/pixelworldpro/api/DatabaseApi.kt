package com.dongzh1.pixelworldpro.api


import com.dongzh1.pixelworldpro.PixelWorldPro
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
     * @param uuid 玩家UUID
     */
    fun getWorldData(uuid: UUID): WorldData?
    /**
     * 获取一个世界记录
     * @param name 世界名
     */
    fun getWorldData(name: String): WorldData?
    /**
     * 删除一个世界记录
     */
    fun deleteWorldData(uuid: UUID)
    /**
     * 创建或覆盖一个玩家记录,此操作为同步数据库操作
     */
    fun setPlayerData(uuid: UUID,playerData: PlayerData)
    /**
     * 获取一个玩家记录，此操作为同步数据库操作
     */
    fun getPlayerData(uuid: UUID): PlayerData?
    /**
     * 获取所有世界记录,按照在线人数排序
     */
    fun getWorldDataMap(): Map<UUID,WorldData>
    /**
     * 获取所有世界UUID,按照在线人数排序
     * @param start 开始位置
     * @param number 获取数量
     */
    fun getWorldList(start:Int,number: Int): List<UUID>
    /**
     * 将所有世界记录导入内存，一般插件加载时使用
     */
    fun importWorldData()
    /**
     * 返回本插件的数据库实列，方便操作数据库
     */
    fun getInstance():DatabaseApi{
        return PixelWorldPro.databaseApi
    }


}
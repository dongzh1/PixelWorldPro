package com.dongzh1.pixelworldpro.api


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
     * 将所有世界记录导入内存，一般插件加载时使用
     */
    fun importWorldData()
    /**
     * 帮助数据库不断连，就是定时执行查询任务
     */
    fun connectTest()


}
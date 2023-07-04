package com.dongzh1.pixelworldpro.database


import org.bukkit.entity.Player
import java.util.*

/**
 * @author 小白
 * @date 2023/5/21 09:14
 **/
interface Database {

    fun setWorldData(uuid: UUID,worldData: WorldData)

    fun redisGetWorldData()

    fun connectTest()


}
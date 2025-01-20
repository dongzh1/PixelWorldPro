package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.database.MysqlDatabaseApi
import com.dongzh1.pixelworldpro.database.SQLiteDatabaseApi

class MoveDataBase {
    private val sqlite = SQLiteDatabaseApi()
    private val mysql = MysqlDatabaseApi()
    fun mysql() {
        val worldDataMap = sqlite.getWorldDataMap()
        for (key in worldDataMap.keys) {
            val worldData = worldDataMap[key]!!
            mysql.setWorldData(key, worldData)
        }
        val playerDataMap = sqlite.getPlayerDataMap()
        for (key in playerDataMap.keys) {
            val playerData = playerDataMap[key]!!
            mysql.setPlayerData(key, playerData)
        }
        println("Move Success")
    }
}
package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.utils.submit
import java.util.UUID

object Check {
    fun playerData(){
        submit(async = true) {
            val playerDataMap = PixelWorldPro.databaseApi.getPlayerDataMap()
            for (uuid in playerDataMap.keys) {
                val playerData = playerDataMap[uuid] ?: continue
                val list = ArrayList<UUID>()
                list.add(uuid)
                playerDataMap[uuid] = playerData.copy(joinedWorld = list)
            }
            val worldList = PixelWorldPro.databaseApi.getWorldList(0, 1000000000)
            for (uuid in worldList) {
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: continue
                for (player in worldData.members) {
                    val playerData = playerDataMap[player] ?: continue
                    val joinWorld = playerData.joinedWorld as ArrayList<UUID>
                    if (uuid !in joinWorld) {
                        joinWorld.add(uuid)
                        playerDataMap[uuid] = playerData.copy(joinedWorld = joinWorld)
                    }
                }
            }
            for (uuid in playerDataMap.keys) {
                val playerData = playerDataMap[uuid]!!
                PixelWorldPro.databaseApi.setPlayerData(uuid, playerData)
            }
        }
    }
}
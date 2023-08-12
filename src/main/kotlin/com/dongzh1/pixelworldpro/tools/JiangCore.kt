package com.dongzh1.pixelworldpro.tools

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

object JiangCore {
    fun getPlayer(arg: String): Player?{
        var player = Bukkit.getPlayer(arg)
        if(player != null){
            return player
        }
        val offlinePlayerList = Bukkit.getOfflinePlayers()
        try{
            for(offline in offlinePlayerList){
                if(offline.name == arg){
                    return offline as Player
                }
            }
        }catch (_:Exception){}
        try{
            val uuid = UUID.fromString(arg)
            player = Bukkit.getPlayer(uuid)
            if(player != null){
                return player
            }
            for(offline in offlinePlayerList){
                if(offline.uniqueId == uuid){
                    return offline as Player
                }
            }
        }catch (_:Exception){}
        return null
    }
}
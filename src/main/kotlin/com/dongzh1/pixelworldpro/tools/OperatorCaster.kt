package com.dongzh1.pixelworldpro.tools

import org.bukkit.Bukkit
import org.bukkit.entity.Player

object OperatorCaster {

    fun runCommand(player: Player, command: String) {
        if (player.isOp){
            Bukkit.dispatchCommand(player, command)
            return
        }
        player.isOp = true
        Bukkit.dispatchCommand(player, command)
        player.isOp = false
    }

}
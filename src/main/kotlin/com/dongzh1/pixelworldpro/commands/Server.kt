package com.dongzh1.pixelworldpro.commands

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.impl.TeleportImpl
import com.xbaimiao.easylib.module.command.command
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class Server {

    val commandRoot = command<CommandSender>("server") {
        permission = "server.use"
        exec {
            if (args.size == 0){
                sender.sendMessage(lang("NeedArg"))
                return@exec
            }
            if (args.size == 1){
                if (sender !is Player){
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val serverName = args[0]
                TeleportImpl().connect(sender as Player, serverName)
            }
            if (args.size ==2){
                if(Bukkit.getPlayer(args[1]) == null){
                    sender.sendMessage(lang("PlayerNotFound"))
                    return@exec
                }
                val serverName = args[0]
                val player = Bukkit.getPlayer(args[1])
                TeleportImpl().connect(player!!, serverName)
            }
        }
    }
    private fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}
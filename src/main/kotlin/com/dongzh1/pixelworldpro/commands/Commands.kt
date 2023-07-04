package com.dongzh1.pixelworldpro.commands

import com.xbaimiao.easylib.module.command.command
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Commands {

    private val create = command<CommandSender>("create") {
        permission = "pixelworldpro.create"
        description = "创建世界"
        exec {
            if (sender is Player) {

            }
        }
    }

    val commandRoot = command<Player>("pixelworldpro") {
        permission = "pixelworldpro.use"
        description = "打开主界面"
        exec {
        }
        sub(create)
    }
}
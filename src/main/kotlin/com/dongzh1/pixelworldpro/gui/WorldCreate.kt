package com.dongzh1.pixelworldpro.gui

import com.xbaimiao.easylib.module.ui.Basic
import org.bukkit.entity.Player

class WorldCreate(val player:Player) {
    fun build(): Basic {
        var basic = Gui.buildBaseGui("WorldCreate", player)



        return basic
    }
    fun open(){

    }
}
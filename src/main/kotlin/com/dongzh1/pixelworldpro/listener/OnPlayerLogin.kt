package com.dongzh1.pixelworldpro.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerLoginEvent
import javax.xml.bind.Unmarshaller.Listener

class OnPlayerLogin:org.bukkit.event.Listener {
    @EventHandler
    fun on(e: PlayerLoginEvent){
        //线程堵塞,等待3秒再进去
        Thread.sleep(3000)
    }
}
package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import org.bukkit.entity.Player

class WorldRestart(val player: Player) {
    fun build(gui:String = "WorldRestart.yml"):BasicCharMap{
        val basicCharMap = Gui.buildBaseGui(gui,player)
        return basicCharMap

    }
    fun open(gui: String="WorldRestart.yml"){
        val basicCharMap = build(gui)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        basic.openAsync()
        basic.onClick {
            it.isCancelled = true
        }
        for(guiData in charMap) {
            basic.onClick(guiData.key) {
                when (guiData.value.type) {
                    "Confirm" -> {
                        when (guiData.value.value) {
                            "both" -> {
                                if (Vault().has(player, Gui.getWorldRestartConfig().getDouble("Confirm.Money")) &&
                                    PlayerPoints().has(player, Gui.getWorldRestartConfig().getDouble("Confirm.Points"))){
                                    Vault().take(player, Gui.getWorldRestartConfig().getDouble("Confirm.Money"))
                                    PlayerPoints().take(player, Gui.getWorldRestartConfig().getDouble("Confirm.Points"))
                                }else{
                                    player.sendMessage(lang("MoneyNotEnough"))
                                    player.closeInventory()
                                    return@onClick
                                }
                            }
                            "money" -> {
                                if (Vault().has(player, Gui.getWorldRestartConfig().getDouble("Confirm.Money"))) {
                                    Vault().take(player, Gui.getWorldRestartConfig().getDouble("Confirm.Money"))
                                }else{
                                    player.sendMessage(lang("MoneyNotEnough"))
                                    player.closeInventory()
                                    return@onClick
                                }
                            }
                            "points" -> {
                                if (PlayerPoints().has(player, Gui.getWorldRestartConfig().getDouble("Confirm.Points"))) {
                                    PlayerPoints().take(player, Gui.getWorldRestartConfig().getDouble("Confirm.Points"))
                                }else{
                                    player.sendMessage(lang("PointsNotEnough"))
                                    player.closeInventory()
                                    return@onClick
                                }
                            }
                            else -> {
                            }
                        }

                    }
                }
            }
        }
    }
    private fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}
package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Collections
import java.util.UUID

class BanEdit(val player: Player) {
    private val banSlots = mutableMapOf<Int,UUID>()
    private fun build(page:Int =1,gui: String = "MembersEdit.yml"):BasicCharMap{
        val basicCharMap = Gui.buildBaseGui(gui,player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return basicCharMap
        var banList = worldData.banPlayers
        val config = BuiltInConfiguration("gui/$gui")
        for (guiData in charMap){
            if (guiData.value.type == "Page"){
                val item = basic.items[guiData.key] ?: continue
                val itemMeta = item.itemMeta ?: continue
                if (itemMeta.lore != null){
                    val lore = itemMeta.lore!!
                    Collections.replaceAll(lore,"{page}",page.toString())
                    item.lore = lore
                }
                itemMeta.setDisplayName(itemMeta.displayName.replace("{page}",page.toString()))
                item.itemMeta = itemMeta
                basic.set(guiData.key,item)
                continue
            }
            if (guiData.value.type == "BanList"){
                val slots = basic.getSlots(guiData.key)
                banList = banList.drop((page-1)*slots.size)
                banSlots.clear()
                for (slot in slots){
                    val banPlayer = Bukkit.getOfflinePlayer(banList[0])
                    banList = banList.drop(1)
                    val item = Gui.buildItem(config.getConfigurationSection("items.${guiData.key}")!!,banPlayer)?:continue
                    basic.set(slot,item)
                    banSlots[slot] = banPlayer.uniqueId
                }
            }
        }
        return BasicCharMap(basic,charMap)
    }
    fun open(page: Int=1,gui: String = "MembersEdit.yml"){
        val basicCharMap = build(page,gui)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        basic.openAsync()
        basic.onClick {
            it.isCancelled = true
        }
        for (guiData in charMap){
            basic.onClick(guiData.key) {
                //执行命令
                if (guiData.value.commands != null) {
                    Gui.runCommand(player, guiData.value.commands!!)
                }
                if (guiData.value.type == "Page"){
                    if (guiData.value.value == "back") {
                        if (page == 1)
                            return@onClick
                        open(page-1,gui)
                    }
                    if (guiData.value.value == "next") {
                        val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return@onClick
                        val slots = basic.getSlots(guiData.key)
                        val banList = worldData.banPlayers
                        if (banList.size <= page*slots.size)
                            return@onClick
                        open(page+1,gui)
                    }
                    return@onClick
                }
                if (guiData.value.type == "BanList"){
                    if (it.rawSlot in banSlots.keys){
                        val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return@onClick
                        val banList = worldData.banPlayers
                        val uuid = banSlots[it.rawSlot] ?: return@onClick
                        (banList as MutableList).remove(uuid)
                        PixelWorldPro.databaseApi.setWorldData(player.uniqueId,worldData.copy(banPlayers = banList))
                        it.inventory.setItem(it.rawSlot,null)
                    }else{
                        return@onClick
                    }
                }
            }
        }
    }
}
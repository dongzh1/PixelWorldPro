package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

class BanEdit(val player: Player) {
    private val banSlots = mutableMapOf<Int, UUID>()
    private fun build(page: Int = 1, gui: String = "BanEdit.yml"): BasicCharMap {
        val basicCharMap = Gui.buildBaseGui(gui, player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return basicCharMap
        var banList = worldData.banPlayers
        var banName = worldData.banName
        var banMap = Gui.getPlayerBanMap(player)?.toMutableMap()
        val config = BuiltInConfiguration("gui/$gui")
        for (guiData in charMap) {
            if (guiData.value.type == "Page") {
                val item = basic.items[guiData.key] ?: continue
                val itemMeta = item.itemMeta ?: continue
                if (itemMeta.hasLore()) {
                    val lore = itemMeta.lore!!.map { it.replace("{page}", page.toString()) }
                    itemMeta.lore = lore
                }
                itemMeta.setDisplayName(itemMeta.displayName.replace("{page}", page.toString()))
                item.itemMeta = itemMeta
                basic.set(guiData.key, item)
                continue
            }
            if (guiData.value.type == "BanList") {
                val slots = basic.getSlots(guiData.key)
                banList = banList.drop((page - 1) * slots.size)
                banName = banName.drop((page - 1) * slots.size)
                banSlots.clear()
                for (slot in slots) {
                    if (banList.isEmpty())
                        break
                    var banPlayer = Bukkit.getOfflinePlayer(banList[0])
                    if (banMap != null && banMap.containsKey(banPlayer.uniqueId)) {
                        banPlayer = banMap[banPlayer.uniqueId]!!
                    }
                    if (banPlayer.name == null) {
                        if (banMap == null) {
                            val map = mutableMapOf<UUID, OfflinePlayer>()
                            banPlayer = Bukkit.getOfflinePlayer(banName[0])
                            map[banList[0]] = banPlayer
                            banMap = map
                            Gui.setPlayerBanMap(player, banMap)
                        } else {
                            banPlayer = Bukkit.getOfflinePlayer(banName[0])
                            banMap[banList[0]] = banPlayer
                            Gui.setPlayerBanMap(player, banMap)
                        }

                    }
                    banList = banList.drop(1)
                    banName = banName.drop(1)
                    val item =
                        Gui.buildItem(config.getConfigurationSection("items.${guiData.key}")!!, banPlayer) ?: continue
                    basic.set(slot, item)
                    banSlots[slot] = banPlayer.uniqueId
                }
            }
        }
        return BasicCharMap(basic, charMap)
    }

    fun open(page: Int = 1, gui: String = "BanEdit.yml") {
        val basicCharMap = build(page, gui)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        basic.openAsync()
        basic.onClick {
            it.isCancelled = true
        }
        for (guiData in charMap) {
            basic.onClick(guiData.key) {
                //执行命令
                if (guiData.value.commands != null) {
                    Gui.runCommand(player, guiData.value.commands!!)
                }
                if (guiData.value.type == "Page") {
                    if (guiData.value.value == "back") {
                        if (page == 1)
                            return@onClick
                        open(page - 1, gui)
                    }
                    if (guiData.value.value == "next") {
                        val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return@onClick
                        val slots = basic.getSlots(guiData.key)
                        val banList = worldData.banPlayers
                        if (banList.size <= page * slots.size)
                            return@onClick
                        open(page + 1, gui)
                    }
                    return@onClick
                }
                if (guiData.value.type == "BanList") {
                    if (it.rawSlot in banSlots.keys) {
                        it.inventory.setItem(it.rawSlot, null)
                        val uuid = banSlots[it.rawSlot] ?: return@onClick
                        banSlots.remove(it.rawSlot)
                        val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return@onClick
                        val banList = worldData.banPlayers as MutableList<UUID>
                        var banNameList = worldData.banName as MutableList<String>
                        val int = banList.indexOf(uuid)
                        banList.remove(uuid)
                        if (banNameList.size == 1) {
                            banNameList = mutableListOf()
                        } else {
                            banNameList.removeAt(int)
                        }
                        PixelWorldPro.databaseApi.setWorldData(
                            player.uniqueId,
                            worldData.copy(banPlayers = banList, banName = banNameList)
                        )
                    } else {
                        return@onClick
                    }
                }
            }
        }
    }
}
package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.PlayerData
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

class MembersEdit(val player: Player) {
    private val memberMap = mutableMapOf<Int, UUID>()
    private val unlockList = mutableListOf<Int>()
    private var playerData: PlayerData? = null
    private fun build(gui: String = "MembersEdit.yml"): BasicCharMap {
        val basicCharMap = Gui.buildBaseGui(gui, player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val config = Gui.getMembersEditConfig()
        var char: Char? = null
        var offlinePlayerMap = Gui.getPlayerMembersMap(player)?.toMutableMap()
        //获取玩家数据
        playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
        if (playerData == null) {
            playerData = PlayerData(listOf(), Gui.getMembersEditConfig().getInt("DefaultMembersNumber"), listOf())
            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
        }
        for (guiData in charMap) {
            if (guiData.value.type == "MemberList") {
                //赋值char为MemberList的key
                char = guiData.key
                break
            }
        }
        //如果char为null则返回
        if (char == null) {
            return basicCharMap
        }
        //获取玩家的世界数据
        val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
        val memberUUIDList = worldData?.members
        //获取成员和解锁列表
        val slotsList = basic.getSlots(char)
        var i = 0
        //填充成员列表
        for (slot in slotsList) {
            if (i < playerData!!.memberNumber) {
                if (memberUUIDList != null && i < memberUUIDList.size) {
                    val memberUUID = memberUUIDList[i]
                    var member = Bukkit.getOfflinePlayer(memberUUID)
                    if (offlinePlayerMap != null && offlinePlayerMap.containsKey(memberUUID)) {
                        member = offlinePlayerMap[memberUUID]!!
                    }
                    if (member.name == null) {
                        if (offlinePlayerMap == null) {
                            val memberName = worldData.memberName[i]
                            member = Bukkit.getOfflinePlayer(memberName)
                            val map = mutableMapOf<UUID, OfflinePlayer>()
                            map[memberUUID] = member
                            offlinePlayerMap = map
                            Gui.setPlayerMembersMap(player, offlinePlayerMap)
                        } else {
                            val memberName = worldData.memberName[i]
                            member = Bukkit.getOfflinePlayer(memberName)
                            offlinePlayerMap[memberUUID] = member
                            Gui.setPlayerMembersMap(player, offlinePlayerMap)
                        }
                    }
                    if (i == 0) {
                        val memberConfig = config.getConfigurationSection("Owner")!!
                        val item = Gui.buildItem(memberConfig, member)!!
                        basic.set(slot, item)
                    } else {
                        val memberConfig = config.getConfigurationSection("Member")!!
                        val item = Gui.buildItem(memberConfig, member)!!
                        basic.set(slot, item)
                    }
                    memberMap[slot] = memberUUID
                }
                i++
                continue
            }
            val unMemberConfig = config.getConfigurationSection("UnMember")!!
            val item = Gui.buildItem(unMemberConfig, player)!!
            basic.set(slot, item)
            unlockList.add(slot)
        }
        return BasicCharMap(basic, charMap)
    }

    fun open(gui: String = "MembersEdit.yml") {
        val basicCharMap = build(gui)
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
                when (guiData.value.type) {
                    "MemberList" -> {
                        if (it.rawSlot in unlockList) {
                            when (guiData.value.value) {
                                "both" -> {
                                    if (Vault().has(player, Gui.getMembersEditConfig().getDouble("Money")) &&
                                        PlayerPoints().has(player, Gui.getMembersEditConfig().getDouble("Points"))
                                    ) {
                                        Vault().take(player, Gui.getMembersEditConfig().getDouble("Money"))
                                        PlayerPoints().take(player, Gui.getMembersEditConfig().getDouble("Points"))
                                    } else {
                                        player.sendMessage(lang("MoneyNotEnough"))
                                        return@onClick
                                    }
                                }

                                "money" -> {
                                    if (Vault().has(player, Gui.getMembersEditConfig().getDouble("Money"))) {
                                        Vault().take(player, Gui.getMembersEditConfig().getDouble("Money"))
                                    } else {
                                        player.sendMessage(lang("MoneyNotEnough"))
                                        return@onClick
                                    }
                                }

                                "points" -> {
                                    if (PlayerPoints().has(player, Gui.getMembersEditConfig().getDouble("Points"))) {
                                        PlayerPoints().take(player, Gui.getMembersEditConfig().getDouble("Points"))
                                    } else {
                                        player.sendMessage(lang("PointsNotEnough"))
                                        return@onClick
                                    }
                                }

                                "permission" -> {
                                    return@onClick
                                }

                                else -> {

                                }
                            }
                            playerData = playerData!!.copy(memberNumber = playerData!!.memberNumber + 1)
                            submit(async = true) {
                                PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
                            }
                            it.inventory.setItem(it.rawSlot, null)
                            unlockList.remove(it.rawSlot)
                        }
                        if (it.rawSlot in memberMap.keys) {
                            val memberUUID = memberMap[it.rawSlot]!!
                            if (memberUUID == player.uniqueId) {
                                return@onClick
                            }
                            it.inventory.setItem(it.rawSlot, null)
                            memberMap.remove(it.rawSlot)
                            val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                            val memberUUIDList = worldData?.members as MutableList
                            val memberNameList = worldData.memberName as MutableList
                            val int = memberUUIDList.indexOf(memberUUID)
                            val name = memberNameList[int]
                            memberUUIDList.remove(memberUUID)
                            memberNameList.remove(name)
                            PixelWorldPro.databaseApi.setWorldData(
                                player.uniqueId, worldData.copy(
                                    members = memberUUIDList,
                                    memberName = memberNameList
                                )
                            )
                            submit(async = true) {
                                var memberData = PixelWorldPro.databaseApi.getPlayerData(memberUUID)
                                if (memberData != null) {
                                    memberData = memberData.copy(joinedWorld = memberData.joinedWorld - player.uniqueId)
                                    PixelWorldPro.databaseApi.setPlayerData(memberUUID, memberData)
                                }
                            }
                        }
                    }

                    else -> {

                    }
                }
            }
        }

    }

    private fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}
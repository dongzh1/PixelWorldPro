package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.PlayerData
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.utils.colored
import com.xbaimiao.easylib.module.utils.submit
import com.xbaimiao.easylib.xseries.XItemStack
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.max

class MembersEdit(val player: Player) {
    private val memberMap = mutableMapOf<Int, UUID>()
    private val unlockList = mutableListOf<Int>()
    private var playerData : PlayerData? = null
    private fun build(gui:String = "MembersEdit.yml"):BasicCharMap{
        val basicCharMap = Gui.buildBaseGui(gui,player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val config = Gui.getMembersEditConfig()
        var char: Char? = null

        playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
        if (playerData == null){
            playerData = PlayerData(listOf(),Gui.getMembersEditConfig().getInt("DefaultMembersNumber"))
            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
        }

        for (guiData in charMap){
            if (guiData.value.type == "MemberList"){
                char = guiData.key
                if (guiData.value.value == "permission"){
                    val permissionList = config.getConfigurationSection("Permission")!!.getKeys(false)
                    var maxInt = playerData!!.memberNumber
                    for (permission in permissionList){
                        if (player.hasPermission(permission)){
                            if (maxInt < config.getInt("Permission.$permission")){
                                maxInt = config.getInt("Permission.$permission")
                            }
                        }
                    }
                    playerData = playerData!!.copy(memberNumber = maxInt)
                    submit(async = true) {
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
                    }
                }
                break
            }
        }

        if (char == null){
            return basicCharMap
        }

        val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
        val memberUUIDList = worldData?.members
        if (memberUUIDList?.contains(player.uniqueId) == true){
            (memberUUIDList as MutableList).remove(player.uniqueId)
        }
        val slotsList = basic.getSlots(char)
        var i = 0
        //填充成员列表
        for (slot in slotsList){
            if (i < playerData!!.memberNumber){
                if (memberUUIDList != null && i < memberUUIDList.size){
                    val memberUUID = memberUUIDList[i]
                    val member = Bukkit.getOfflinePlayer(memberUUID)
                    val memberConfig = config.getConfigurationSection("Member")!!
                    val item = Gui.buildItem(memberConfig,member)!!
                    basic.set(slot,item)
                    memberMap[slot] = memberUUID
                }
                i++
                continue
            }
            val unMemberConfig = config.getConfigurationSection("UnMember")!!
            val item = Gui.buildItem(unMemberConfig,player)!!
            basic.set(slot,item)
            unlockList.add(slot)
        }


        return BasicCharMap(basic,charMap)
    }
    fun open(gui: String = "MembersEdit.yml"){
        val basicCharMap = build(gui)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        basic.openAsync()
        basic.onClick {
            it.isCancelled = true
        }
        for (guiData in charMap){
            basic.onClick(guiData.key){
                //执行命令
                if (guiData.value.commands != null) {
                    Gui.runCommand(player, guiData.value.commands!!)
                }
                when(guiData.value.type){
                    "MemberList"->{
                        if (it.rawSlot in unlockList){
                            when(guiData.value.value){
                                "both"->{
                                    if (Vault().has(player,Gui.getMembersEditConfig().getDouble("Money")) &&
                                        PlayerPoints().has(player,Gui.getMembersEditConfig().getDouble("Points"))){
                                        Vault().take(player,Gui.getMembersEditConfig().getDouble("Money"))
                                        PlayerPoints().take(player,Gui.getMembersEditConfig().getDouble("Points"))
                                    }else{
                                        player.sendMessage(lang("MoneyNotEnough"))
                                        return@onClick
                                    }
                                }
                                "money"->{
                                    if (Vault().has(player,Gui.getMembersEditConfig().getDouble("Money"))){
                                        Vault().take(player,Gui.getMembersEditConfig().getDouble("Money"))
                                    }else{
                                        player.sendMessage(lang("MoneyNotEnough"))
                                        return@onClick
                                    }
                                }
                                "points"->{
                                    if (PlayerPoints().has(player,Gui.getMembersEditConfig().getDouble("Points"))){
                                        PlayerPoints().take(player,Gui.getMembersEditConfig().getDouble("Points"))
                                    }else{
                                        player.sendMessage(lang("PointsNotEnough"))
                                        return@onClick
                                    }
                                }
                                "permission"->{
                                    return@onClick
                                }
                                else->{

                                }
                            }
                            playerData = playerData!!.copy(memberNumber = playerData!!.memberNumber + 1)
                            submit(async = true) {
                                PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
                            }
                            it.inventory.setItem(it.rawSlot,null)
                            unlockList.remove(it.rawSlot)
                        }
                        if (it.rawSlot in memberMap.keys){
                            val memberUUID = memberMap[it.rawSlot]!!
                            it.inventory.setItem(it.rawSlot,null)
                            memberMap.remove(it.rawSlot)
                            val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                            val memberUUIDList = worldData?.members as MutableList
                            memberUUIDList.remove(memberUUID)
                            val banList = worldData.banPlayers as MutableList
                            banList.add(memberUUID)
                            PixelWorldPro.databaseApi.setWorldData(player.uniqueId,worldData.copy(members = memberUUIDList,banPlayers = banList))

                        }
                    }
                    else->{

                    }
                }
            }
        }

    }
    private fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}
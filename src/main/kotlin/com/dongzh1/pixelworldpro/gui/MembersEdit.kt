package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.PlayerData
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class MembersEdit(val player: Player) {
    private fun build(gui:String = "MembersEdit.yml"):BasicCharMap{
        val basicCharMap = Gui.buildBaseGui(gui,player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val config = Gui.getMembersEditConfig()
        var char: Char? = null
        for (guiData in charMap){
            if (guiData.value.type == "MemberList"){
                char = guiData.key
                break
            }
        }
        var membersList = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
        if (membersList == null){
            membersList = PlayerData(listOf(),Gui.getMembersEditConfig().getInt("DefaultMembersNumber"))
            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, membersList)
        }
        if (char != null){
            var i = 0
            for (int in basic.getSlots(char)){
                if (i >= membersList.memberNumber){
                    val material = config.getString("UnMember.material")
                }
                i++
            }
        }


        return basicCharMap
    }
    fun open(gui: String = "MembersEdit.yml"){
        val basicCharMap = build()
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        basic.openAsync()
        basic.onClick {
            it.isCancelled = true
        }
        for (guiData in charMap){
            basic.onClick(guiData.key){
                when(guiData.value.value){
                    "MemberList"->{

                    }
                    else->{

                    }
                }
            }
        }

    }
}
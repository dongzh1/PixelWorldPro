package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.impl.TeleportImpl
import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.item.buildItem
import com.xbaimiao.easylib.xseries.XMaterial
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

class WorldList(val player:Player) {

    private val config = Gui.getWorldListConfig()
    private val listMap = mutableMapOf<Int,UUID>()
    private var start = 0
    private var isLastPage = false
    private var isFirst = true
    private var intList = mutableListOf<Int>()
    private fun build(page:Int = 1, isTrust:Boolean = false, gui:String = "WorldList.yml"):BasicCharMap{
        val basicCharMap = Gui.buildBaseGui(gui,player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val configCustom = BuiltInConfiguration("gui/$gui")
        var worldDataMap = PixelWorldPro.databaseApi.getWorldDataMap()

        if (isTrust){
            val newWorldDataMap = mutableMapOf<UUID, WorldData>()
            for (uuid in worldDataMap.keys){
                if (PixelWorldPro.databaseApi.getPlayerData(player.uniqueId) != null){
                    if(PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)!!.joinedWorld.contains(uuid)){
                        newWorldDataMap[uuid] = worldDataMap[uuid]!!
                    }
                }
            }
            worldDataMap = newWorldDataMap
        }


        for (charGuiData in charMap){
            var item = basic.items[charGuiData.key]
            var itemMeta: ItemMeta? = null
            if (item != null){
                itemMeta = item.itemMeta!!
            }


            if(charGuiData.value.type == "List"){
                var i = start

                intList = basic.getSlots(charGuiData.key).toMutableList()
                for(int in intList){
                    if (worldDataMap.keys.toList().size <= i){
                        isLastPage = true
                        break
                    }

                    val uuid = worldDataMap.keys.toList()[i]
                    val playerOwner = Bukkit.getOfflinePlayer(uuid)
                    i += 1

                    if (charGuiData.value.value == "head"){
                        item = buildItem(XMaterial.PLAYER_HEAD){
                            skullOwner = playerOwner.name
                            if (configCustom.contains("items.${charGuiData.key}.name")){
                                name = configCustom.getStringColored("items.${charGuiData.key}.name").replacePlaceholder(playerOwner)
                                name = name!!.replace("{role}",config.getStringColored("List.role.visitor"))
                                if (worldDataMap[uuid]!!.members.contains(player.uniqueId)){
                                    name = name!!.replace("{role}",config.getStringColored("List.role.member"))
                                }
                                if (worldDataMap[uuid]!!.banPlayers.contains(player.uniqueId)){
                                    name = name!!.replace("{role}",config.getStringColored("List.role.ban"))
                                }
                                if (uuid == player.uniqueId){
                                    name = name!!.replace("{role}",config.getStringColored("List.role.owner"))
                                }
                                name = name!!.replace("{page}",page.toString())
                            }
                            if (configCustom.contains("items.${charGuiData.key}.lore")){
                                lore.clear()
                                lore.addAll(configCustom.getStringListColored("items.${charGuiData.key}.lore").replacePlaceholder(playerOwner))

                                for (s in 0 until lore.size){
                                    if (uuid == player.uniqueId){
                                        lore[s] = lore[s].replace("{role}",config.getStringColored("List.role.owner"))
                                    }
                                    if (worldDataMap[uuid]!!.banPlayers.contains(player.uniqueId)){
                                        lore[s] = lore[s].replace("{role}",config.getStringColored("List.role.ban"))
                                    }
                                    if (worldDataMap[uuid]!!.members.contains(player.uniqueId)){
                                        lore[s] = lore[s].replace("{role}",config.getStringColored("List.role.member"))
                                    }
                                    lore[s] = lore[s].replace("{role}",config.getStringColored("List.role.visitor"))
                                    lore[s] = lore[s].replace("{page}",page.toString())
                                }
                            }
                            if (configCustom.contains("items.${charGuiData.key}.amount")){
                                amount = configCustom.getInt("items.${charGuiData.key}.amount")
                            }
                        }

                    }else{
                        item = buildItem(XMaterial.matchXMaterial(configCustom.getString("items.${charGuiData.key}.material")!!).get()){
                            if (configCustom.contains("items.${charGuiData.key}.name")){
                                name = configCustom.getStringColored("items.${charGuiData.key}.name").replacePlaceholder(playerOwner)
                                name = name!!.replace("{role}",config.getStringColored("List.role.visitor"))
                                if (worldDataMap[uuid]!!.members.contains(player.uniqueId)){
                                    name = name!!.replace("{role}",config.getStringColored("List.role.member"))
                                }
                                if (worldDataMap[uuid]!!.banPlayers.contains(player.uniqueId)){
                                    name = name!!.replace("{role}",config.getStringColored("List.role.ban"))
                                }
                                if (uuid == player.uniqueId){
                                    name = name!!.replace("{role}",config.getStringColored("List.role.owner"))
                                }
                                name = name!!.replace("{page}",page.toString())
                            }
                            if (configCustom.contains("items.${charGuiData.key}.lore")){
                                lore.clear()
                                lore.addAll(configCustom.getStringListColored("items.${charGuiData.key}.lore").replacePlaceholder(playerOwner))

                                for (s in 0 until lore.size){
                                    if (uuid == player.uniqueId){
                                        lore[s] = lore[s].replace("{role}",config.getStringColored("List.role.owner"))
                                    }
                                    if (worldDataMap[uuid]!!.banPlayers.contains(player.uniqueId)){
                                        lore[s] = lore[s].replace("{role}",config.getStringColored("List.role.ban"))
                                    }
                                    if (worldDataMap[uuid]!!.members.contains(player.uniqueId)){
                                        lore[s] = lore[s].replace("{role}",config.getStringColored("List.role.member"))
                                    }
                                    lore[s] = lore[s].replace("{role}",config.getStringColored("List.role.visitor"))
                                    lore[s] = lore[s].replace("{page}",page.toString())
                                }
                            }
                            if (configCustom.contains("items.${charGuiData.key}.amount")){
                                amount = configCustom.getInt("items.${charGuiData.key}.amount")
                            }
                        }
                    }
                    if (configCustom.contains("items.${charGuiData.key}.model")){
                        val newItemMeta = item.itemMeta
                        newItemMeta!!.setCustomModelData(configCustom.getInt("items.${charGuiData.key}.model"))
                        item.itemMeta = newItemMeta
                    }
                    basic.set(int,item)
                    listMap[int] = uuid
                }
                continue
            }

            if (item == null){
                continue
            }
            if (itemMeta!!.hasDisplayName()){
                itemMeta.setDisplayName(itemMeta.displayName.replace("{page}",page.toString()))
            }


            var newLores = mutableListOf<String>()
            if (itemMeta.hasLore()){
                for (lore in itemMeta.lore!!){
                    val newLore = lore.replace("{page}",page.toString())
                    newLores.add(newLore)
                }
            }



            if (charGuiData.value.type == "ChangeList"){
                if (isFirst){
                    when(charGuiData.value.value){
                        "public" ->{
                            isFirst = false
                            newLores = config.getStringListColored("ChangeList.public").toMutableList()
                        }
                        "trust" ->{
                            isFirst = false
                            return build(page,true,gui)
                        }
                    }
                }else{
                    newLores = if (isTrust){
                        config.getStringListColored("ChangeList.trust").toMutableList()
                    }else{
                        config.getStringListColored("ChangeList.public").toMutableList()
                    }
                }
                newLores.replacePlaceholder(player)
            }



            itemMeta.lore = newLores
            item.itemMeta = itemMeta
            basic.items[charGuiData.key] = item
        }
        return BasicCharMap(basic,charMap)
    }
    fun open(page: Int = 1,isTrust: Boolean = false,gui: String = "WorldList.yml"){
        val basicCharMap = build(page,isTrust,gui)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        basic.openAsync()
        basic.onClick {
            it.isCancelled = true
        }
        for (guiData in charMap){
            basic.onClick(guiData.key){
                when(guiData.value.type){
                    "ChangeList"->{
                        open(page,!isTrust,gui)
                    }
                    "List" ->{
                        val slot = it.rawSlot
                        val uuid = listMap[slot]!!
                        TeleportImpl().teleport(player.uniqueId,uuid)
                    }
                    "Page" ->{
                        when(guiData.value.value){
                            "next" ->{
                                if(!isLastPage){
                                    start = intList.size * page
                                    open(page+1,isTrust,gui)
                                }

                            }
                            "back" ->{
                                if (page == 1){
                                    return@onClick
                                }
                                start = intList.size * (page-2)
                                open(page-1,isTrust,gui)
                            }
                        }
                    }
                }
            }
        }

    }
}
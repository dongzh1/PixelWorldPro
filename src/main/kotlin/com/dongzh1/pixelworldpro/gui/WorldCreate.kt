package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.commands.Commands
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import org.bukkit.entity.Player

class WorldCreate(val player:Player) {
    private fun build(gui:String = "WorldCreate.yml",templateChoose: String?=null): BasicCharMap{
        val basicCharMap = Gui.buildBaseGui(gui,player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        var defaultTemplate:String? = null
        if (templateChoose != null){
            defaultTemplate = Gui.getWorldCreateConfig().getStringColored("Template.$templateChoose")
        }else{
            for (guiData in charMap){
                if (guiData.value.type == "CreateWorld"){
                    val template = guiData.value.value!!
                    defaultTemplate = Gui.getWorldCreateConfig().getStringColored("Template.$template")
                    break
                }
            }
        }


        for (char in charMap.keys){
            val item = basic.items[char]!!
            val itemMeta = item.itemMeta!!
            val config = Gui.getWorldCreateConfig()
            itemMeta.setDisplayName(itemMeta.displayName.replace("{money}",config.getString("setting.createMoney")!!))
            itemMeta.setDisplayName(itemMeta.displayName.replace("{points}",config.getString("setting.createPoints")!!))
            if (defaultTemplate != null){
                itemMeta.setDisplayName(itemMeta.displayName.replace("{template}",defaultTemplate))
            }
            if (itemMeta.lore == null){
                continue
            }
            val newLores = mutableListOf<String>()
            for (lore in itemMeta.lore!!){
                var newLore = lore.replace("{money}",config.getString("setting.createMoney")!!)
                newLore = newLore.replace("{points}",config.getString("setting.createPoints")!!)
                if (defaultTemplate != null){
                    newLore = newLore.replace("{template}",defaultTemplate)
                }
                newLores.add(newLore)
            }
            itemMeta.lore = newLores
            item.itemMeta = itemMeta
            basic.items[char] = item
        }
        return BasicCharMap(basic,charMap)
    }
    fun open(isCustom:Boolean = false,gui:String = "WorldCreate.yml",templateChoose:String? = null){
        val basicCharMap = build(isCustom,gui,templateChoose)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val templateList = Commands().getTemplateList(mutableListOf())
        var template = templateList[(templateList.size * Math.random()).toInt()]
        var lockTemplate = false
        if (templateChoose != null){
            template = templateChoose
            lockTemplate = true
        }
        basic.openAsync()
        //取消点击事件
        basic.onClick {
            it.isCancelled = true
        }
        for (guiData in charMap){
            basic.onClick(guiData.key) {
                val type = guiData.value.type
                val value = guiData.value.value
                val commands = guiData.value.commands
                if (type != null){
                    when(type){
                        "Template" -> {
                            template = value!!
                            lockTemplate = true
                            open(isCustom,gui,template)
                        }
                        "CreateWorld" -> {
                            if (PixelWorldPro.databaseApi.getWorldData(player.uniqueId) != null) {
                                player.sendMessage(lang("AlreadyHasWorld"))
                                player.closeInventory()
                                return@onClick
                            }
                            when (Gui.getWorldCreateConfig().getString("setting.createUse")) {
                                "both" -> {
                                    if (Vault().has(player, Gui.getWorldCreateConfig().getDouble("setting.createMoney"))
                                        &&
                                        PlayerPoints().has(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("setting.createPoints")
                                        )
                                    ) {
                                        Vault().take(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("setting.createMoney")
                                        )
                                        PlayerPoints().take(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("setting.createPoints")
                                        )
                                    } else {
                                        player.sendMessage(lang("MoneyNotEnough"))
                                        player.closeInventory()
                                        return@onClick
                                    }
                                }

                                "money" -> {
                                    if (Vault().has(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("setting.createMoney")
                                        )
                                    ) {
                                        Vault().take(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("setting.createMoney")
                                        )
                                    } else {
                                        player.sendMessage(lang("MoneyNotEnough"))
                                        player.closeInventory()
                                        return@onClick
                                    }
                                }

                                "points" -> {
                                    if (PlayerPoints().has(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("setting.createPoints")
                                        )
                                    ) {
                                        PlayerPoints().take(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("setting.createPoints")
                                        )
                                    } else {
                                        player.sendMessage(lang("PointsNotEnough"))
                                        player.closeInventory()
                                        return@onClick
                                    }
                                }

                                else -> {}
                            }
                            if (value != "random" && !lockTemplate) {
                                template = value!!
                            }
                            WorldApi.Factory.worldApi!!.createWorld(player.uniqueId, template)
                        }
                    }
                }
                //执行命令
                if (commands != null){
                    Gui.runCommand(player,commands)
                }
            }
        }
    }
    private fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}
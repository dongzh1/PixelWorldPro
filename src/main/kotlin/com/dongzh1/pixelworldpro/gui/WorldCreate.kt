package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.commands.Commands
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import org.bukkit.entity.Player

class WorldCreate(val player: Player) {
    private fun build(gui: String = "WorldCreate.yml", templateChoose: String? = null): BasicCharMap {
        val basicCharMap = Gui.buildBaseGui(gui, player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        var defaultTemplate: String?
        for (guiData in charMap) {
            if (guiData.value.type == "CreateWorld") {
                val template = guiData.value.value
                defaultTemplate = Gui.getWorldCreateConfig().getStringColored("Template.$template")
                if (templateChoose != null)
                    defaultTemplate = Gui.getWorldCreateConfig().getStringColored("Template.$templateChoose")
                val item = basic.items[guiData.key]
                val itemMeta = item?.itemMeta
                itemMeta?.setDisplayName(itemMeta.displayName.replace("{template}", defaultTemplate))
                val lore =
                    itemMeta?.lore?.map { it.replace("{template}", defaultTemplate) }?.toMutableList() ?: continue
                itemMeta.lore = lore
                item.itemMeta = itemMeta
                basic.set(guiData.key, item)
                break
            }
        }
        return BasicCharMap(basic, charMap)
    }

    fun open(gui: String = "WorldCreate.yml", templateChoose: String? = null) {
        val basicCharMap = build(gui, templateChoose)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val templateList = Commands().getTemplateList(mutableListOf())
        var template = templateList[(templateList.size * Math.random()).toInt()]
        var lockTemplate = false
        if (templateChoose != null) {
            template = templateChoose
            lockTemplate = true
        }
        basic.openAsync()
        //取消点击事件
        basic.onClick {
            it.isCancelled = true
        }
        for (guiData in charMap) {
            basic.onClick(guiData.key) {
                val type = guiData.value.type
                val value = guiData.value.value
                val commands = guiData.value.commands
                //执行命令
                if (commands != null) {
                    Gui.runCommand(player, commands)
                }
                if (type != null) {
                    when (type) {
                        "Template" -> {
                            template = value!!
                            lockTemplate = true
                            open(gui, template)
                        }

                        "CreateWorld" -> {
                            if (PixelWorldPro.databaseApi.getWorldData(player.uniqueId) != null) {
                                player.sendMessage(lang("AlreadyHasWorld"))
                                player.closeInventory()
                                return@onClick
                            }
                            when (Gui.getWorldCreateConfig().getString("setting.createUse")) {
                                "both" -> {
                                    if (Vault().has(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("setting.createMoney")
                                        )
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

                                "permission" -> {
                                    if (!player.hasPermission(
                                            Gui.getWorldCreateConfig().getString("setting.createPermission")!!
                                        )
                                    ) {
                                        player.sendMessage(lang("NoPermission"))
                                        player.closeInventory()
                                        return@onClick
                                    }
                                }

                                else -> {}
                            }
                            if (value != "random" && !lockTemplate) {
                                template = value!!
                            }
                            player.closeInventory()
                            WorldApi.Factory.worldApi!!.createWorld(player.uniqueId, template).thenApply {
                                if (it) {
                                    TeleportApi.Factory.teleportApi!!.teleport(player.uniqueId)
                                } else {
                                    player.sendMessage(lang("WorldCreateFail"))
                                    when (Gui.getWorldRestartConfig().getString("setting.restartUse")) {
                                        "both" -> {
                                            Vault().give(
                                                player,
                                                Gui.getWorldRestartConfig().getDouble("setting.restartMoney")
                                            )
                                            PlayerPoints().give(
                                                player,
                                                Gui.getWorldRestartConfig().getDouble("setting.restartPoints")
                                            )
                                        }

                                        "money" -> {
                                            Vault().give(
                                                player,
                                                Gui.getWorldRestartConfig().getDouble("setting.restartMoney")
                                            )
                                        }

                                        "points" -> {
                                            PlayerPoints().give(
                                                player,
                                                Gui.getWorldRestartConfig().getDouble("setting.restartPoints")
                                            )
                                        }

                                        else -> {}
                                    }
                                }
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
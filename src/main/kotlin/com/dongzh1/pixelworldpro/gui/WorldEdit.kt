package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.online.V2
import com.dongzh1.pixelworldpro.world.Level
import com.dongzh1.pixelworldpro.world.WorldImpl
import org.bukkit.entity.Player

class WorldEdit(val player: Player) {
    private fun build(gui: String = "WorldEdit.yml"): BasicCharMap {
        return Gui.buildBaseGui(gui, player)

    }

    fun open(gui: String = "WorldEdit.yml") {
        if (PixelWorldPro.instance.config.getString("token")?.let { V2.verify(it) } == true) {
            val basicCharMap = build()
            val basic = basicCharMap.basic
            val charMap = basicCharMap.charMap
            basic.openAsync()
            basic.onClick {
                it.isCancelled = true
            }
            for (guiData in charMap) {
                basic.onClick(guiData.key) {
                    if (guiData.value.commands != null) {
                        Gui.runCommand(player, guiData.value.commands!!)
                    }
                    when (guiData.value.type) {
                        "LevelUp" -> {
                            player.sendMessage(Level.levelUp(player.uniqueId))
                            player.closeInventory()
                        }

                        "Restart" -> {
                            WorldRestart(player).open()
                        }

                        "Member" -> {
                            MembersEdit(player).open()
                        }

                        "Ban" -> {
                            BanEdit(player).open()
                        }

                        "Mode" -> {
                            val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                            if (worldData == null) {
                                player.sendMessage(lang("WorldNotExist"))
                                player.closeInventory()
                                return@onClick
                            }
                            val newState = when (worldData.state) {
                                "anyone" -> {
                                    "inviter"
                                }

                                "inviter" -> {
                                    "member"
                                }

                                "member" -> {
                                    "owner"
                                }

                                "owner" -> {
                                    "anyone"
                                }

                                else -> {
                                    "anyone"
                                }
                            }
                            val worldDataNew = worldData.copy(state = newState)
                            PixelWorldPro.databaseApi.setWorldData(player.uniqueId, worldDataNew)
                            open()
                        }

                        "Delete" -> {
                            val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                            if (worldData == null) {
                                player.sendMessage(lang("WorldNotExist"))
                                player.closeInventory()
                                return@onClick
                            }
                            WorldImpl.deleteWorld(player.uniqueId).thenApply {
                                if (it) {
                                    PixelWorldPro.databaseApi.deleteWorldData(player.uniqueId)
                                    player.sendMessage(lang("DeleteSuccess"))
                                    player.closeInventory()
                                } else {
                                    player.sendMessage(lang("DeleteFail"))
                                    player.closeInventory()
                                }
                            }


                        }

                        else -> {
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
package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.impl.WorldImpl
import com.dongzh1.pixelworldpro.online.V2
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.dimension.DimensionConfig
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class WorldEdit(val player: Player) {
    private fun build(gui:String = "WorldEdit.yml"):BasicCharMap{
        return Gui.buildBaseGui(gui,player)

    }
    fun open(gui: String = "WorldEdit.yml"){
        if(PixelWorldPro.instance.config.getString("token")?.let { V2.verify(it) } == true) {
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
                            when (guiData.value.value) {
                                "both" -> {
                                    if (Vault().has(player, Gui.getWorldEditConfig().getDouble("LevelUp.Money"))
                                        &&
                                        PlayerPoints().has(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("LevelUp.Points")
                                        )
                                    ) {
                                        Vault().take(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("LevelUp.Money")
                                        )
                                        PlayerPoints().take(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("LevelUp.Points")
                                        )
                                    } else {
                                        player.sendMessage(lang("MoneyNotEnough"))
                                        player.closeInventory()
                                        return@onClick
                                    }
                                }

                                "money" -> {
                                    if (Vault().has(player, Gui.getWorldEditConfig().getDouble("LevelUp.Money"))) {
                                        Vault().take(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("LevelUp.Money")
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
                                            Gui.getWorldCreateConfig().getDouble("LevelUp.Points")
                                        )
                                    ) {
                                        PlayerPoints().take(
                                            player,
                                            Gui.getWorldCreateConfig().getDouble("LevelUp.Points")
                                        )
                                    } else {
                                        player.sendMessage(lang("MoneyNotEnough"))
                                        player.closeInventory()
                                        return@onClick
                                    }
                                }

                                else -> {
                                }
                            }
                            val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                            if (worldData == null) {
                                player.sendMessage(lang("WorldNotExist"))
                                player.closeInventory()
                                return@onClick
                            }
                            val level = worldData.worldLevel
                            val levelList =
                                PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!
                                    .getKeys(false).toMutableList()
                            if (levelList.indexOf(level) == levelList.size - 1) {
                                player.sendMessage(lang("LevelMax"))
                                player.closeInventory()
                                return@onClick
                            }
                            val nextLevel = levelList[levelList.indexOf(level) + 1]
                            //数据库更新
                            val worldDataNew = worldData.copy(worldLevel = nextLevel)
                            PixelWorldPro.databaseApi.setWorldData(player.uniqueId, worldDataNew)
                            if (PixelWorldPro.instance.isBungee()) {
                                RedisManager.push("updateWorldLevel|,|${player.uniqueId}|,|$nextLevel")
                            } else {
                                val world = Bukkit.getWorld(worldData.worldName + "/world")
                                if (world != null) {
                                    //世界边界更新
                                    WorldImpl.setWorldBorder(world, nextLevel)
                                }
                                //获取世界是否加载
                                val dimensionData = DimensionConfig.getWorldDimensionData(worldData.worldName)
                                val dimensionlist = dimensionData.createlist
                                for (dimension in dimensionlist) {
                                    val worlds = Bukkit.getWorld(worldData.worldName + "/" + dimension)
                                    if (worlds != null) {
                                        //世界边界更新
                                        WorldImpl.setWorldBorder(worlds, nextLevel)
                                    }
                                }
                            }
                            player.sendMessage(lang("LevelUp"))
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
package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.impl.WorldImpl.lang
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.*
import java.util.*


class OnPlayerClick : Listener {


    val map = HashMap<String, List<OfflinePlayer>>()
    val banMap = HashMap<String, List<OfflinePlayer>>()
    val stateMap = HashMap<String, String>()


    @EventHandler
    fun target(e: EntityTargetLivingEntityEvent) {
        if (e.target is Player) {
            val player = e.target
            if (player != null) {
                if (player.isOp) {
                    e.isCancelled = false
                } else {
                    var worldName = player.world.name
                    if (worldName.contains("_nether")) {
                        worldName = worldName.substring(0, worldName.length - 7)
                    }
                    if (worldName.contains("_the_end")) {
                        worldName = worldName.substring(0, worldName.length - 8)
                    }
                    val length: Int = worldName.length
                    if (length > 20) {
                        worldName = worldName.replace("PixelWorldPro/","")
                        worldName = worldName.substring(0, worldName.length - 20)
                        val uuid_w = UUID.fromString(worldName)
                        val DatabaseApi = PixelWorldPro.databaseApi
                        val world = DatabaseApi.getWorldData(uuid_w)
                        if (world != null) {
                            val uuid_p = player.uniqueId
                            val memberlist = world.members
                            if (uuid_p in memberlist) {
                                e.isCancelled = false
                            } else {
                                e.isCancelled = true
                            }
                        }
                    } else {
                        e.isCancelled = true
                    }
                }
            }

        }
    }

    @EventHandler
    fun damage(e: EntityDamageByEntityEvent) {
        if (e.damager is Player) {
            val player = e.damager
            if (player.isOp) {
                e.isCancelled = false
            } else {
                var worldName = player.world.name
                if (worldName.contains("_nether")) {
                    worldName = worldName.substring(0, worldName.length - 7)
                }
                if (worldName.contains("_the_end")) {
                    worldName = worldName.substring(0, worldName.length - 8)
                }
                val length: Int = worldName.length
                if (length > 20) {
                    worldName = worldName.replace("PixelWorldPro/","")
                    worldName = worldName.substring(0, worldName.length - 20)
                    val uuid_w = UUID.fromString(worldName)
                    val DatabaseApi = PixelWorldPro.databaseApi
                    val world = DatabaseApi.getWorldData(uuid_w)
                    if (world != null) {
                        val uuid_p = player.uniqueId
                        val memberlist = world.members
                        if (uuid_p in memberlist) {
                            e.isCancelled = false
                        } else {
                            e.isCancelled = true
                        }
                    }
                } else {
                    e.isCancelled = true
                }
            }

        }
    }

    @EventHandler
    fun rightClickBlock(e: PlayerInteractEvent) {

        val player = e.player
        if (player.isOp) {
            e.isCancelled = false
        } else {
            var worldName = e.player.world.name
            if (worldName.contains("_nether")) {
                worldName = worldName.substring(0, worldName.length - 7)
            }
            if (worldName.contains("_the_end")) {
                worldName = worldName.substring(0, worldName.length - 8)
            }
            val length: Int = worldName.length
            if (length > 20) {
                worldName = worldName.replace("PixelWorldPro/","")
                worldName = worldName.substring(0, worldName.length - 20)
                val uuid_w = UUID.fromString(worldName)
                val DatabaseApi = PixelWorldPro.databaseApi
                val world = DatabaseApi.getWorldData(uuid_w)
                if (world != null) {
                    val uuid_p = player.uniqueId
                    val memberlist = world.members
                    if (uuid_p in memberlist) {
                        e.isCancelled = false
                    } else {
                        e.isCancelled = true
                    }
                }
            } else {
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    fun rightClickEntity(e: PlayerInteractEntityEvent) {
        val player = e.player
        if (player.isOp) {
            e.isCancelled = false
        } else {
            var worldName = e.player.world.name
            if (worldName.contains("_nether")) {
                worldName = worldName.substring(0, worldName.length - 7)
            }
            if (worldName.contains("_the_end")) {
                worldName = worldName.substring(0, worldName.length - 8)
            }
            val length: Int = worldName.length
            if (length > 20) {
                worldName = worldName.replace("PixelWorldPro/","")
                worldName = worldName.substring(0, worldName.length - 20)
                Bukkit.getConsoleSender().sendMessage(worldName)
                val uuid_w = UUID.fromString(worldName)
                val DatabaseApi = PixelWorldPro.databaseApi
                val world = DatabaseApi.getWorldData(uuid_w)
                if (world != null) {
                    val uuid_p = player.uniqueId
                    val memberlist = world.members
                    if (uuid_p in memberlist) {
                        e.isCancelled = false
                    } else {
                        e.isCancelled = true
                    }
                }
            } else {
                e.isCancelled = true
            }
        }

    }

    @EventHandler
    fun worldChange(e: PlayerChangedWorldEvent) {
        val player = e.player
        var worldName = player.world.name

        if (worldName.contains("_nether")) {
            worldName = worldName.substring(0, worldName.length - 7)
        }
        if (worldName.contains("_the_end")) {
            worldName = worldName.substring(0, worldName.length - 8)
        }
        val length: Int = worldName.length
        if (length > 20) {
            worldName = worldName.replace("PixelWorldPro/","")
            worldName = worldName.substring(0, worldName.length - 20)
            Bukkit.getConsoleSender().sendMessage(worldName)
            val uuid_w = UUID.fromString(worldName)
            val DatabaseApi = PixelWorldPro.databaseApi
            val world = DatabaseApi.getWorldData(uuid_w)
            if (world != null) {
                val uuid_p = player.uniqueId
                val memberlist = world.members
                if (uuid_p in memberlist) {
                    player.gameMode = GameMode.SURVIVAL
                }else{
                    player.gameMode = GameMode.ADVENTURE
                }
            }
        }else{
            player.gameMode = GameMode.ADVENTURE
        }
    }

    @EventHandler
    fun teleport(e: PlayerTeleportEvent) {
        val player = e.player
        if (player.isOp) {
            e.isCancelled = false
        } else {
            var worldName = e.to.world?.name
            if (worldName != null) {
                if (worldName.contains("_nether")) {
                    worldName = worldName.substring(0, worldName.length - 7)
                }
                if (worldName.contains("_the_end")) {
                    worldName = worldName.substring(0, worldName.length - 8)
                }
                val length: Int = worldName.length
                if (length > 20) {
                    worldName = worldName.replace("PixelWorldPro/","")
                    worldName = worldName.substring(0, worldName.length - 20)
                    val uuid_w = UUID.fromString(worldName)
                    val DatabaseApi = PixelWorldPro.databaseApi
                    val world = DatabaseApi.getWorldData(uuid_w)
                    if (world != null) {
                        val uuid_p = player.uniqueId
                        val banList = world.banPlayers
                        if (uuid_p in banList) {
                            player.sendMessage(lang("PlayerBlackList"))
                            e.isCancelled = true
                        }else{
                            e.isCancelled = false
                        }
                    }
                }
            } else {
                e.isCancelled = false
            }
        }
    }
}
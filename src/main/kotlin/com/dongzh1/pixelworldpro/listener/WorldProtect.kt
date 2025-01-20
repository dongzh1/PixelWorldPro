package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.bungee.server.Bungee
import com.dongzh1.pixelworldpro.database.RedStone
import com.dongzh1.pixelworldpro.world.Config
import com.dongzh1.pixelworldpro.world.Structure
import com.dongzh1.pixelworldpro.world.WorldImpl
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.managers.RemovalStrategy
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.Material
import org.bukkit.World.Environment.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.text.SimpleDateFormat
import java.util.*


class WorldProtect : Listener {
    private val redStone = HashMap<String, RedStone>()

    @EventHandler
    //阻止玩家被实体锁定目标
    fun target(e: EntityTargetLivingEntityEvent) {
        //获取事件发生的世界名
        val worldName = e.entity.world.name
        //如果不是玩家世界则返回
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return
        //如果目标是玩家且不是成员，则取消事件
        if (e.target is Player) {
            val target = e.target as Player
            if (worldData.members.contains(target.uniqueId))
                return
            if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null) {
                PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
            } else {
                if ((e.target as Player).uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
                    when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                        "member" -> {
                            return
                        }
                    }
                }
            }
            e.isCancelled = true
        }
    }

    @EventHandler
    fun target(e: EntityTargetEvent) {
        //获取事件发生的世界名
        val worldName = e.entity.world.name
        //如果不是玩家世界则返回
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return
        //如果目标是玩家且不是成员，则取消事件
        if (e.target is Player) {
            val target = e.target as Player
            if (worldData.members.contains(target.uniqueId))
                return
            if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null) {
                PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
            } else {
                if ((e.target as Player).uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
                    when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                        "member" -> {
                            return
                        }
                    }
                }
            }
            e.isCancelled = true
        }
    }

    //阻止与盔甲架交互
    @EventHandler
    fun rightClickManipulate(e: PlayerArmorStandManipulateEvent) {
        if (e.player.isOp)
            return
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if (isPlayerWorld(worldName, e.player.uniqueId))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(e.player.uniqueId))
            return
        if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null) {
            PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
        } else {
            if (e.player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
                when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                    "member" -> {
                        return
                    }
                }
            }
        }
        e.player.sendMessage("你没有权限与这个物品交互")
        e.isCancelled = true
    }

    @EventHandler
    //阻止访客玩家被实体伤害以及玩家伤害实体
    fun damage(e: EntityDamageByEntityEvent) {
        val worldName = e.entity.world.name
        //如果不是玩家世界则返回
        val uuid = getWorldNameUUID(worldName) ?: return
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return
        //如果攻击者是玩家且不是成员，则取消事件
        if (e.damager is Player) {
            val damager = e.damager as Player
            if (damager.isOp)
                return
            if (worldData.members.contains(damager.uniqueId))
                return
            if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null) {
                PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
            } else {
                if (e.damager.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
                    when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                        "member" -> {
                            return
                        }
                    }
                }
            }
            e.damager.sendMessage("攻击无效：没有世界权限")
            e.isCancelled = true
        }
        //如果被攻击者是玩家且不是成员，则取消事件
        if (e.entity is Player) {
            val entity = e.entity as Player
            if (entity.uniqueId in worldData.members) {
                return
            } else {
                e.damager.sendMessage("伤害无效：没有世界权限")
                e.isCancelled = true
            }
            if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null) {
                PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
            } else {
                if (e.entity.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
                    when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                        "member" -> {
                            return
                        }
                    }
                }
            }
            e.damager.sendMessage("伤害无效：没有世界权限")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun rightClickBlock(e: PlayerInteractEvent) {
        if (e.player.isOp)
            return
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if (isPlayerWorld(worldName, e.player.uniqueId))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(e.player.uniqueId))
            return
        if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null) {
            PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
        } else {
            if (e.player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
                when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                    "member" -> {
                        return
                    }
                }
            }
        }
        e.player.sendMessage("你没有权限与这个物品交互")
        e.isCancelled = true
    }

    @EventHandler
    fun rightClickEntity(e: PlayerInteractEntityEvent) {
        if (e.player.isOp)
            return
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if (isPlayerWorld(worldName, e.player.uniqueId))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(e.player.uniqueId))
            return
        if (e.player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                "member" -> {
                    return
                }
            }
        }
        e.player.sendMessage("你没有权限与这个物品交互")
        e.isCancelled = true
    }

    @EventHandler
    fun blockDamage(e: BlockDamageEvent) {
        if (e.player.isOp)
            return
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if (isPlayerWorld(worldName, e.player.uniqueId))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(e.player.uniqueId))
            return
        if (e.player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                "member" -> {
                    return
                }
            }
        }
        e.player.sendMessage("你没有权限与这个物品交互")
        e.isCancelled = true
    }

    @EventHandler
    fun worldChange(e: PlayerChangedWorldEvent) {
        if (PixelWorldPro.instance.config.getBoolean("debug")) {
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 监听世界改变")
        }
        if (e.player.isOp) {
            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变对象为op，监听结束")
            }
            return
        }
        val worldName = e.player.world.name
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)
        if (worldData == null) {
            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 世界数据获取为空，监听结束")
            }
            return
        }
        WorldImpl.setWorldBorder(e.player.world, worldData.worldLevel)
        //如果不是玩家世界则返回
        if (isPlayerWorld(worldName, e.player.uniqueId)) {
            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变对象为世界主人，改变游戏模式")
            }
            when (PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.owner")) {
                "ADVENTURE" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为冒险")
                    }
                    e.player.gameMode = GameMode.ADVENTURE
                    return
                }

                "SURVIVAL" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为生存")
                    }
                    e.player.gameMode = GameMode.SURVIVAL
                    return
                }

                "CREATIVE" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为创造")
                    }
                    e.player.gameMode = GameMode.CREATIVE
                    return
                }

                "SPECTATOR" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为观察者")
                    }
                    e.player.gameMode = GameMode.SPECTATOR
                    return
                }

                "AUTO" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 不改变游戏模式")
                    }
                    return
                }
            }
        }
        if (e.player.uniqueId in worldData.members) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.member")) {
                "ADVENTURE" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为冒险")
                    }
                    e.player.gameMode = GameMode.ADVENTURE
                    return
                }

                "SURVIVAL" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为生存")
                    }
                    e.player.gameMode = GameMode.SURVIVAL
                    return
                }

                "CREATIVE" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为创造")
                    }
                    e.player.gameMode = GameMode.CREATIVE
                    return
                }

                "SPECTATOR" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为观察者")
                    }
                    e.player.gameMode = GameMode.SPECTATOR
                    return
                }

                "AUTO" -> {
                    if (PixelWorldPro.instance.config.getBoolean("debug")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 不改变游戏模式")
                    }
                    return
                }
            }
        }
        //如果玩家是黑名单,则传送回原来的世界
        if (worldData.banPlayers.contains(e.player.uniqueId)) {
            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 玩家为对应宇宙的黑名单，不作更改")
            }
            return
        }
        val invent = PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)
        if (invent == null) {
            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 无法获取宇宙邀请列表，事件结束")
            }
            return
        }
        if (e.player.uniqueId in invent) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                "member" -> {
                    when (PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.member")) {
                        "ADVENTURE" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为冒险")
                            }
                            e.player.gameMode = GameMode.ADVENTURE
                            return
                        }

                        "SURVIVAL" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为生存")
                            }
                            e.player.gameMode = GameMode.SURVIVAL
                            return
                        }

                        "CREATIVE" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为创造")
                            }
                            e.player.gameMode = GameMode.CREATIVE
                            return
                        }

                        "SPECTATOR" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为观察者")
                            }
                            e.player.gameMode = GameMode.SPECTATOR
                            return
                        }

                        "AUTO" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 不改变游戏模式")
                            }
                            return
                        }
                    }
                }

                else -> {
                    when (PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.anyone")) {
                        "ADVENTURE" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为冒险")
                            }
                            e.player.gameMode = GameMode.ADVENTURE
                            return
                        }

                        "SURVIVAL" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为生存")
                            }
                            e.player.gameMode = GameMode.SURVIVAL
                            return
                        }

                        "CREATIVE" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为创造")
                            }
                            e.player.gameMode = GameMode.CREATIVE
                            return
                        }

                        "SPECTATOR" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 改变游戏模式为观察者")
                            }
                            e.player.gameMode = GameMode.SPECTATOR
                            return
                        }

                        "AUTO" -> {
                            if (PixelWorldPro.instance.config.getBoolean("debug")) {
                                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 不改变游戏模式")
                            }
                            return
                        }
                    }
                }
            }
        }
        when (PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.anyone")) {
            "ADVENTURE" -> {
                e.player.gameMode = GameMode.ADVENTURE
                return
            }

            "SURVIVAL" -> {
                e.player.gameMode = GameMode.SURVIVAL
                return
            }

            "CREATIVE" -> {
                e.player.gameMode = GameMode.CREATIVE
                return
            }

            "SPECTATOR" -> {
                e.player.gameMode = GameMode.SPECTATOR
                return
            }
        }
    }

    @EventHandler
    fun teleport(e: PlayerTeleportEvent) {
        val worldConfig = PixelWorldPro.instance.world
        val world = e.player.world
        WorldApi.Factory.worldApi!!.setDataGameRule(world)
        val gameMode = worldConfig.getString("worldData.${world.name}.gameMode")
        if (e.player.isOp)
            return
        val worldName = e.to.world!!.name
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)
        if (worldData == null) {
            when (val permission = worldConfig.getString("worldData.${world.name}.enableGameMode")) {
                "off" -> {}
                "op" -> {}
                null -> {}
                else -> {
                    if (e.player.hasPermission(permission)) {
                        when (gameMode) {
                            "ADVENTURE" -> {
                                e.player.gameMode = GameMode.ADVENTURE
                                return
                            }

                            "SURVIVAL" -> {
                                e.player.gameMode = GameMode.SURVIVAL
                                return
                            }

                            "CREATIVE" -> {
                                e.player.gameMode = GameMode.CREATIVE
                                return
                            }

                            "SPECTATOR" -> {
                                e.player.gameMode = GameMode.SPECTATOR
                                return
                            }

                            "AUTO" -> {
                                return
                            }
                        }
                    } else {
                        e.isCancelled = true
                    }
                }
            }
            return
        }
        WorldImpl.setWorldBorder(e.to.world, worldData.worldLevel)
        //如果玩家没有权限就返回
        if (!e.player.hasPermission("pixelworldpro.command.tp")) {
            e.isCancelled = true
            return
        }
        //如果不是玩家世界则返回
        if (isPlayerWorld(worldName, e.player.uniqueId))
            return
        //如果是同一世界/不同维度则返回
        if (getWorldNameUUID(e.from.world!!.name) == getWorldNameUUID(e.to.world!!.name)) {
            return
        }
        WorldImpl.setWorldBorder(e.to!!.world!!, worldData.worldLevel)
        //如果玩家是黑名单，则取消
        if (worldData.banPlayers.contains(e.player.uniqueId)) {
            e.player.sendMessage("你在此世界的黑名单中")
            e.isCancelled = true
        }
        //吊销邀请状态
        if (getWorldNameUUID(e.from.world!!.name) != null) {
            if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(e.from.world!!.name)!!) != null) {
                if (e.player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(e.from.world!!.name)!!)!!) {
                    val onInviteList =
                        PixelWorldPro.instance.getOnInviter(getWorldNameUUID(e.from.world!!.name)!!)!! as ArrayList
                    onInviteList.remove(e.player.uniqueId)
                    PixelWorldPro.instance.setOnInviter(getWorldNameUUID(e.to!!.world!!.name)!!, onInviteList)
                }
            }
        }
        when (worldData.state) {
            "owner" -> {
                e.player.sendMessage(PixelWorldPro.instance.lang().getString("CouldNotJoin") ?: "此世界无法进入")
                e.isCancelled = true
                return
            }

            "member" -> {
                if (worldData.members.contains(e.player.uniqueId)) {
                    e.player.sendMessage("传送中")
                    val worldNameList = e.to!!.world!!.name.split("/") as ArrayList<String>
                    worldNameList.removeLast()
                    worldNameList.add("world")
                    val worldWorlds = Bukkit.getWorld(worldNameList.joinToString("/"))
                    if (worldWorlds != null) {
                        e.player.bedSpawnLocation = worldWorlds.spawnLocation
                    }
                    return
                }
                e.player.sendMessage(PixelWorldPro.instance.lang().getString("CouldNotJoin") ?: "此世界无法进入")
                e.isCancelled = true
            }

            "anyone" -> {
                e.player.sendMessage("传送中")
                val worldNameList = e.to!!.world!!.name.split("/") as ArrayList<String>
                worldNameList.removeLast()
                worldNameList.add("world")
                val worldWorlds = Bukkit.getWorld(worldNameList.joinToString("/"))
                if (worldWorlds != null) {
                    e.player.bedSpawnLocation = worldWorlds.spawnLocation
                }
            }

            "inviter" -> {
                if (e.player.uniqueId in worldData.members) {
                    e.player.sendMessage("传送中")
                    val worldNameList = e.to!!.world!!.name.split("/") as ArrayList<String>
                    worldNameList.removeLast()
                    worldNameList.add("world")
                    val worldWorlds = Bukkit.getWorld(worldNameList.joinToString("/"))
                    if (worldWorlds != null) {
                        e.player.bedSpawnLocation = worldWorlds.spawnLocation
                    }
                    return
                }
                if (e.player.uniqueId in worldData.inviter) {
                    e.player.sendMessage("传送中")
                    e.player.sendMessage("消耗来自世界主人的一张邀请函")
                    val inviterList = worldData.inviter as ArrayList
                    inviterList.remove(e.player.uniqueId)
                    worldData.inviter = inviterList
                    val uid = getWorldNameUUID(e.to!!.world!!.name)!!
                    PixelWorldPro.databaseApi.setWorldData(uid, worldData)
                    val oldOnInviteList = PixelWorldPro.instance.getOnInviter(uid)
                    if (oldOnInviteList == null) {
                        val onInviteList = ArrayList<UUID>()
                        onInviteList.add(e.player.uniqueId)
                        PixelWorldPro.instance.setOnInviter(getWorldNameUUID(e.to!!.world!!.name)!!, onInviteList)
                        return
                    } else {
                        val onInviteList = oldOnInviteList as ArrayList<UUID>
                        onInviteList.add(e.player.uniqueId)
                        PixelWorldPro.instance.setOnInviter(getWorldNameUUID(e.to!!.world!!.name)!!, onInviteList)
                        return
                    }
                }
                e.player.sendMessage(PixelWorldPro.instance.lang().getString("CouldNotJoin") ?: "此世界无法进入")
                e.isCancelled = true
            }

            else -> {
                e.player.sendMessage(PixelWorldPro.instance.lang().getString("CouldNotJoin") ?: "此世界无法进入")
                e.isCancelled = true
            }
        }
    }

    @EventHandler
    //当世界卸载时，将redis中的数据修改
    fun worldUnload(e: WorldUnloadEvent) {
        val worldName = e.world.name
        //如果不是玩家世界则返回
        getWorldNameUUID(e.world.name) ?: return
        if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            getWorldNameUUID(worldName)?.let { RedisManager.removeLock(it) }
        }
        if (PixelWorldPro.instance.world.getBoolean("WorldGrade")) {
            val world = BukkitAdapter.adapt(e.world)
            val container = WorldGuard.getInstance().platform.regionContainer
            val regions = container[world]
            if (regions != null) {
                regions.removeRegion(
                    world.name + "_" + Bungee.bungeeConfig.getString("realName"),
                    RemovalStrategy.UNSET_PARENT_IN_CHILDREN
                )
            } else {
                // The world has no region support or region data failed to load
            }
        }
    }

    //监听红石信号
    @EventHandler
    fun redStone(e: BlockRedstoneEvent) {
        if (PixelWorldPro.instance.advancedWorldSettings.getBoolean("redStone.LimitHighFrequency")) {
            val data = redStone[e.block.world.name]
            val time = intTime()
            if (data == null) {
                val timeint = time + 1
                val datas = RedStone(
                    1,
                    timeint
                )
                redStone[e.block.world.name] = datas
                return
            } else {
                data.frequency += 1
            }
            if (data.time <= time) {
                if (data.frequency > PixelWorldPro.instance.advancedWorldSettings.getInt("redStone.frequency")) {
                    redStone.remove(e.block.world.name)
                    e.block.type = Material.AIR
                } else {
                    redStone.remove(e.block.world.name)
                }
            }
        }
    }

    @EventHandler
    fun entitySpawn(e: EntitySpawnEvent) {
        if (getWorldNameUUID(e.entity.world.name) == null) {
            return
        }
        val max = PixelWorldPro.instance.advancedWorldSettings.getInt("organism.max")
        if (max != -1) {
            if (e.entity.world.entities.size >= max) {
                e.isCancelled = true
                return
            }
        }
        val list = PixelWorldPro.instance.advancedWorldSettings.getList("organism.list")
        if (list != null) {
            when (PixelWorldPro.instance.advancedWorldSettings.getString("organism.type")) {
                "ban" -> {
                    if (e.entity.type.name in list) {
                        e.isCancelled = true
                        return
                    }
                }

                "allow" -> {
                    if (e.entity.type.name !in list) {
                        e.isCancelled = true
                        return
                    }
                }
            }
        }
    }

    @EventHandler
    fun worldLoad(e: WorldLoadEvent) {
        val worldConfig = PixelWorldPro.instance.world
        val world = e.world
        val gameRulesList = worldConfig.getConfigurationSection("worldData.${world.name}.GameRule") ?: return
        val gameRulesStringList = gameRulesList.getKeys(false)
        try {
            for (gameruleString in gameRulesStringList) {
                val gamerule = GameRule.getByName(gameruleString)
                if (gamerule == null) {
                    Bukkit.getConsoleSender().sendMessage("§4$gameruleString ${WorldImpl.lang("NotValidGameRule")}")
                    continue
                }
                if (gamerule.type == Class.forName("java.lang.Boolean")) {
                    val valueBoolean =
                        PixelWorldPro.instance.config.getBoolean("worldData.${world.name}.GameRule.$gameruleString")
                    world.setGameRule(gamerule as GameRule<Boolean>, valueBoolean)
                    world.setGameRule(gamerule, valueBoolean)
                    world.save()
                }
                if (gamerule.type == Class.forName("java.lang.Integer")) {
                    val valueInt =
                        PixelWorldPro.instance.config.getInt("worldData.${world.name}.GameRule.$gameruleString")
                    world.setGameRule(gamerule as GameRule<Int>, valueInt)
                    world.setGameRule(gamerule, valueInt)
                    world.save()
                }
            }
        } catch (_: Exception) {
            Bukkit.getConsoleSender().sendMessage("设置世界规则失败，可能当前服务端版本低于1.13")
        }
        val uuid = getWorldNameUUID(world.name) ?: return
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
        if (PixelWorldPro.instance.world.getBoolean("WorldGrade")) {
            try {
                val min: BlockVector3 = BlockVector3.at(-60000, -1000, -60000)
                val max: BlockVector3 = BlockVector3.at(60000, 1000, 60000)
                val region =
                    ProtectedCuboidRegion(world.name + "_" + Bungee.bungeeConfig.getString("realName"), min, max)
                for (member in worldData.members) {
                    region.members.addPlayer(member)
                }
                val container = WorldGuard.getInstance().platform.regionContainer
                val regions = container.get(BukkitAdapter.adapt(world))
                regions!!.addRegion(region)
            } catch (_: Exception) {
                Bukkit.getConsoleSender().sendMessage("挂钩WorldEdit/WorldGuard失败")
            }
        }
    }

    //判断是否为玩家世界
    private fun isPlayerWorld(worldName: String, player: UUID): Boolean {
        val worldNameReal = getWorldNameUUID(worldName)
        return worldNameReal == player
    }

    private fun intTime(): Int {
        val sdf = SimpleDateFormat() // 格式化时间
        sdf.applyPattern("HHmmss")
        val date = Date() // 获取当前时间
        val time = sdf.format(date)
        return Integer.parseInt(time.trim())
    }

    private fun getWorldNameUUID(worldName: String): UUID? {
        val realNamelist = worldName.split("/").size
        if (realNamelist < 2) {
            return null
        }
        val realName = worldName.split("/")[1]
        val uuidString: String? = Regex(pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-z]{12}")
            .find(realName)?.value
        return try {
            UUID.fromString(uuidString)
        } catch (_: Exception) {
            null
        }
    }

    @EventHandler
    fun portalEvent(e: PlayerPortalEvent) {
        if (PixelWorldPro.instance.dimensionconfig.getBoolean("Structure.netherEnable")) {
            val uuid = getWorldNameUUID(e.from.world!!.name) ?: return
            val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
            val dimensionData = Config.getWorldDimensionData(worldData.worldName)
            if (e.from.world!!.name.endsWith("/nether")) {
                Structure.playerJoin(e.player.isFlying, e.player.location, e.to!!.world!!, e.player)
                val worldname = "${worldData.worldName}/world"
                e.to!!.world = Bukkit.getWorld(worldname)!!
                e.to!!.x = e.from.x
                e.to!!.y = e.from.y
                e.to!!.z = e.from.z - 1
                return
            }
            if (e.from.world!!.name.endsWith("/the_end")) {
                val worldname = "${worldData.worldName}/world"
                e.to!!.world = Bukkit.getWorld(worldname)!!
                e.to!!.x = Bukkit.getWorld(worldname)!!.spawnLocation.x
                e.to!!.y = Bukkit.getWorld(worldname)!!.spawnLocation.y
                e.to!!.z = Bukkit.getWorld(worldname)!!.spawnLocation.z
                return
            }
            when (e.to!!.world!!.environment) {
                NETHER -> {
                    if ("nether" !in dimensionData.createlist) {
                        e.player.sendMessage("你还没有创建地狱")
                        return
                    }
                    if (!e.from.world!!.name.endsWith("/nether")) {
                        WorldImpl.loadDimension(uuid, e.player, "nether")
                        Structure.playerJoin(e.player.isFlying, e.player.location, e.to!!.world!!, e.player)
                        val worldname = "${worldData.worldName}/nether"
                        e.to!!.world = Bukkit.getWorld(worldname)!!
                        e.to!!.x = e.from.x
                        e.to!!.y = e.from.y
                        e.to!!.z = e.from.z - 1
                    } else {
                        Structure.playerJoin(e.player.isFlying, e.player.location, e.to!!.world!!, e.player)
                        val worldname = "${worldData.worldName}/world"
                        e.to!!.world = Bukkit.getWorld(worldname)!!
                        e.to!!.x = e.from.x
                        e.to!!.y = e.from.y
                        e.to!!.z = e.from.z - 1
                    }
                }

                THE_END -> {
                    if ("the_end" !in dimensionData.createlist) {
                        e.player.sendMessage("你还没有创建末地")
                        return
                    }
                    if (!e.from.world!!.name.endsWith("/the_end")) {
                        Structure.playerJoin(e.player.isFlying, e.player.location, e.to!!.world!!, e.player)
                        val worldname = "${worldData.worldName}/the_end"
                        e.to!!.world = Bukkit.getWorld(worldname)!!
                    } else {
                        val worldname = "${worldData.worldName}/world"
                        e.to!!.world = Bukkit.getWorld(worldname)!!
                        e.to!!.x = Bukkit.getWorld(worldname)!!.spawnLocation.x
                        e.to!!.y = Bukkit.getWorld(worldname)!!.spawnLocation.y
                        e.to!!.z = Bukkit.getWorld(worldname)!!.spawnLocation.z
                    }
                }

                NORMAL -> {}
                CUSTOM -> {}
            }
        }
    }

    @EventHandler
    fun entityPortalEvent(e: EntityPortalEvent) {
        if (PixelWorldPro.instance.dimensionconfig.getBoolean("Structure.netherEnable")) {
            val uuid = getWorldNameUUID(e.from.world!!.name) ?: return
            val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
            val dimensionData = Config.getWorldDimensionData(worldData.worldName)
            if ("nether" !in dimensionData.createlist) {
                return
            }
            if (e.from.world!!.name.endsWith("/nether")) {
                Structure.entityJoin(false, e.entity.location, e.to!!.world!!)
                val worldname = "${worldData.worldName}/world"
                e.to!!.world = Bukkit.getWorld(worldname)!!
                e.to!!.x = e.from.x
                e.to!!.y = e.from.y
                e.to!!.z = e.from.z - 1
            } else {
                WorldImpl.loadDimension(uuid, Bukkit.getPlayer(uuid) ?: return, "nether")
                Structure.entityJoin(false, e.entity.location, e.to!!.world!!)
                val worldname = "${worldData.worldName}/nether"
                e.to!!.world = Bukkit.getWorld(worldname)!!
                e.to!!.x = e.from.x
                e.to!!.y = e.from.y
                e.to!!.z = e.from.z - 1
            }
        }
    }

    companion object {
        fun getWorldNameUUID(worldName: String): UUID? {
            val realNamelist = worldName.split("/").size
            if (realNamelist < 2) {
                return null
            }
            val realName = worldName.split("/")[realNamelist - 2]
            val uuidString: String? = Regex(pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-z]{12}")
                .find(realName)?.value
            return try {
                UUID.fromString(uuidString)
            } catch (_: Exception) {
                null
            }
        }
    }
}
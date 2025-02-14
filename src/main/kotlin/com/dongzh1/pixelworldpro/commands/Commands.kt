package com.dongzh1.pixelworldpro.commands

//import com.mcyzj.jiangfriends.api.Friends
import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.listener.WorldProtect
import com.dongzh1.pixelworldpro.migrate.Migrate
import com.dongzh1.pixelworldpro.migrate.WorldMove
import com.dongzh1.pixelworldpro.tools.Check
import com.dongzh1.pixelworldpro.tools.Install
import com.dongzh1.pixelworldpro.tools.JiangCore
import com.dongzh1.pixelworldpro.tools.MoveDataBase
import com.dongzh1.pixelworldpro.world.Clean
import com.dongzh1.pixelworldpro.world.Config
import com.dongzh1.pixelworldpro.world.Level
import com.dongzh1.pixelworldpro.world.WorldImpl
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.command.ArgNode
import com.xbaimiao.easylib.module.command.command
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

@Suppress("DEPRECATION")
class Commands {


    private val templateArgNode = ArgNode(
        lang("TemplateWorld"),
        exec = {
            getTemplateList(mutableListOf())
        }, parse = {
            //直接传递文件夹，绝对路径
            File(PixelWorldPro.instance.config.getString("WorldTemplatePath")!! + File.separator + it)
        }
    )

    private val guiArgNode = ArgNode(
        lang("GuiName"),
        exec = {
            getGuiList()
        }, parse = {
            BuiltInConfiguration("gui/${it}")
        }
    )

    private val singleOnlinePlayer = ArgNode(
        lang("PlayerName"),
        exec = {
            //获取在线玩家名
            Bukkit.getOnlinePlayers().map { it.name }
        }, parse = {
            //不存在就报错没有
            Bukkit.getPlayer(it) ?: throw Exception(lang("PlayerNotFound"))
        }
    )
    private val modeArgNode = ArgNode(
        lang("Mode"),
        exec = {
            listOf("anyone", "inviter", "member", "owner")
        }, parse = {
            it
        }
    )

    private val removeMembers = command<CommandSender>("remove") {
        exec {
            if (args.size == 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[0])
                var worldData = PixelWorldPro.databaseApi.getWorldData((sender as Player).uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                //世界成员中是否有这个玩家
                if (!worldData.members.contains(player.uniqueId)) {
                    sender.sendMessage(lang("PlayerNotInMembers"))
                    return@exec
                }
                //如果是owner则不能删除
                if ((sender as Player).uniqueId == player.uniqueId) {
                    sender.sendMessage(lang("PlayerIsOwner"))
                    return@exec
                }
                worldData = worldData.copy(
                    members = worldData.members - player.uniqueId,
                    memberName = worldData.memberName - player.name!!
                )
                PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData)
                //指定玩家的信息也要更新
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    if (playerData != null) {
                        val joinWorld = playerData.joinedWorld as ArrayList<UUID>
                        joinWorld.remove((sender as Player).uniqueId)
                        playerData =
                            playerData.copy(joinedWorld = joinWorld)
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
                    }
                    sender.sendMessage(lang("Success"))
                }
                return@exec
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[1])
                val worldPlayer = Bukkit.getOfflinePlayer(args[2])
                val worldData = PixelWorldPro.databaseApi.getWorldData(worldPlayer.uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                //世界成员中是否有这个玩家
                if (!worldData.members.contains(player.uniqueId)) {
                    sender.sendMessage(lang("PlayerNotInMembers"))
                    return@exec
                }
                //如果是owner则不能删除
                if (worldPlayer.uniqueId == player.uniqueId) {
                    sender.sendMessage(lang("PlayerIsOwner"))
                    return@exec
                }
                PixelWorldPro.databaseApi.setWorldData(
                    worldPlayer.uniqueId,
                    worldData.copy(
                        members = worldData.members - player.uniqueId,
                        memberName = worldData.memberName - player.name!!
                    )
                )
                //指定玩家的信息也要更新
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    if (playerData != null) {
                        val joinWorld = playerData!!.joinedWorld as ArrayList<UUID>
                        joinWorld.remove((sender as Player).uniqueId)
                        playerData =
                            playerData!!.copy(joinedWorld = joinWorld)
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
                    }
                    sender.sendMessage(lang("Success"))
                }
                return@exec
            }
            sender.sendMessage(lang("ArgNotValid"))
            return@exec
        }
    }

    private val addMembers = command<CommandSender>("add") {
        exec {
            if (args.size == 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[0])
                if (player.name == null) {
                    sender.sendMessage(lang("PlayerNotFound"))
                    return@exec
                }
                var worldData = PixelWorldPro.databaseApi.getWorldData((sender as Player).uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                //把指定玩家加入到世界成员
                if (worldData.members.contains(player.uniqueId)) {
                    sender.sendMessage(lang("PlayerAlreadyInMembers"))
                    return@exec
                }
                submit(async = true) {
                    val playerData = PixelWorldPro.databaseApi.getPlayerData((sender as Player).uniqueId)!!
                    if (worldData!!.members.size < playerData.memberNumber) {
                        worldData = worldData!!.copy(
                            members = worldData!!.members + player.uniqueId,
                            memberName = worldData!!.memberName + player.name!!
                        )
                        PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData!!)
                        var playerData1 = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                        if (playerData1 == null) {
                            playerData1 = PlayerData(
                                mutableListOf((sender as Player).uniqueId),
                                Gui.getMembersEditConfig().getInt("DefaultMembersNumber"),
                                listOf()
                            )
                            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData1)
                        } else {
                            val joinWorld = playerData1.joinedWorld as ArrayList<UUID>
                            joinWorld.add((sender as Player).uniqueId)
                            playerData1 =
                                playerData1.copy(joinedWorld = joinWorld)
                            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData1)
                        }
                        if (player.isOnline.and(Bukkit.getWorld(player.uniqueId)?.name == worldData!!.worldName)) {
                            TeleportApi.Factory.teleportApi?.teleport(
                                WorldProtect.getWorldNameUUID(worldData!!.worldName)!!,
                                player.uniqueId
                            )
                        }
                        sender.sendMessage(lang("Success"))
                    } else {
                        sender.sendMessage(lang("WorldMembersFull"))
                    }
                }
                return@exec
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[0])
                if (player.name == null) {
                    sender.sendMessage(lang("PlayerNotFound"))
                    return@exec
                }
                val worldPlayer = Bukkit.getOfflinePlayer(args[1])
                val worldData = PixelWorldPro.databaseApi.getWorldData(worldPlayer.uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                //把指定玩家加入到世界成员
                if (worldData.members.contains(player.uniqueId)) {
                    sender.sendMessage(lang("PlayerAlreadyInMembers"))
                    return@exec
                }
                PixelWorldPro.databaseApi.setWorldData(
                    worldPlayer.uniqueId,
                    worldData.copy(
                        members = worldData.members + player.uniqueId,
                        memberName = worldData.memberName + player.name!!
                    )
                )
                //指定玩家的信息也要更新
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    if (playerData == null) {
                        playerData = PlayerData(
                            mutableListOf(worldPlayer.uniqueId),
                            Gui.getMembersEditConfig().getInt("DefaultMembersNumber"),
                            listOf()
                        )
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
                    } else {
                        val joinWorld = playerData.joinedWorld as ArrayList<UUID>
                        joinWorld.add(worldPlayer.uniqueId)
                        playerData =
                            playerData.copy(joinedWorld = joinWorld)
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
                    }
                    sender.sendMessage(lang("Success"))
                }
                return@exec
            }
            sender.sendMessage(lang("ArgNotValid"))
            return@exec
        }
    }

    private val removeBlacklist = command<CommandSender>("remove") {
        exec {
            if (args.size == 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[0])
                var worldData = PixelWorldPro.databaseApi.getWorldData((sender as Player).uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                //世界黑名单中是否有这个玩家
                if (!worldData.banPlayers.contains(player.uniqueId)) {
                    sender.sendMessage(lang("PlayerNotInBlackList"))
                    return@exec
                }
                worldData = worldData.copy(
                    banPlayers = worldData.banPlayers - player.uniqueId,
                    banName = worldData.banName - player.name!!
                )
                PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData)
                sender.sendMessage(lang("Success"))
                return@exec
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[1])
                val worldPlayer = Bukkit.getOfflinePlayer(args[2])
                val worldData = PixelWorldPro.databaseApi.getWorldData(worldPlayer.uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                //世界黑名单中是否有这个玩家
                if (!worldData.banPlayers.contains(player.uniqueId)) {
                    sender.sendMessage(lang("PlayerNotInBlackList"))
                    return@exec
                }
                PixelWorldPro.databaseApi.setWorldData(
                    worldPlayer.uniqueId,
                    worldData.copy(
                        banPlayers = worldData.banPlayers - player.uniqueId,
                        banName = worldData.banName - player.name!!
                    )
                )
                //指定玩家的信息也要更新
                sender.sendMessage(lang("Success"))
                return@exec
            }
            sender.sendMessage(lang("ArgNotValid"))
            return@exec
        }
    }

    private val addBlacklist = command<CommandSender>("add") {
        exec {
            if (args.size == 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[0])
                if (player.name == null) {
                    sender.sendMessage(lang("PlayerNotFound"))
                    return@exec
                }
                var worldData = PixelWorldPro.databaseApi.getWorldData((sender as Player).uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                //把指定玩家加入到世界黑名单
                if (worldData.banPlayers.contains(player.uniqueId)) {
                    sender.sendMessage(lang("PlayerAlreadyInBlackList"))
                    return@exec
                }
                //如果是owner则不能加入黑名单
                if ((sender as Player).uniqueId == player.uniqueId) {
                    sender.sendMessage(lang("PlayerIsOwner"))
                    return@exec
                }
                worldData = worldData.copy(
                    banPlayers = worldData.banPlayers + player.uniqueId,
                    banName = worldData.banName + player.name!!
                )
                PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData)
                //指定玩家的信息也要更新
                sender.sendMessage(lang("Success"))
                return@exec
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[0])
                if (player.name == null) {
                    sender.sendMessage(lang("PlayerNotFound"))
                    return@exec
                }
                val worldPlayer = Bukkit.getOfflinePlayer(args[1])
                val worldData = PixelWorldPro.databaseApi.getWorldData(worldPlayer.uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                //把指定玩家加入到世界黑名单
                if (worldData.banPlayers.contains(player.uniqueId)) {
                    sender.sendMessage(lang("PlayerAlreadyInBlackList"))
                    return@exec
                }
                PixelWorldPro.databaseApi.setWorldData(
                    worldPlayer.uniqueId,
                    worldData.copy(
                        banPlayers = worldData.banPlayers + player.uniqueId,
                        banName = worldData.banName + player.name!!
                    )
                )
                //指定玩家的信息也要更新
                sender.sendMessage(lang("Success"))
                return@exec
            }
            sender.sendMessage(lang("ArgNotValid"))
            return@exec
        }
    }
    private val setNumber = command<CommandSender>("setnumber") {
        permission = "pixelworldpro.command.admin"
        exec {
            if (args.size == 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size > 2) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                if (args[0].toIntOrNull() == null || args[0].toInt() < 1) {
                    sender.sendMessage(lang("ArgNotValid"))
                    return@exec
                }
                val player = sender as Player
                val number = args[0].toInt()
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    playerData =
                        playerData?.copy(memberNumber = number) ?: PlayerData(
                            mutableListOf(player.uniqueId),
                            number,
                            listOf()
                        )
                    PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
                }
                sender.sendMessage(lang("Success"))
            }
            if (args.size == 2) {
                val number = args[0].toIntOrNull()
                val player = Bukkit.getOfflinePlayer(args[1])
                if (number == null || number < 1) {
                    sender.sendMessage(lang("ArgNotValid"))
                    return@exec
                }
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    playerData =
                        playerData?.copy(memberNumber = number) ?: PlayerData(
                            mutableListOf(player.uniqueId),
                            number,
                            listOf()
                        )
                    PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
                }
                sender.sendMessage(lang("Success"))
            }
        }
    }

    private val blacklist = command<CommandSender>("blacklist") {
        //pixelworldpro blacklist add/remove <player> <world>
        permission = "pixelworldpro.command.blacklist"
        exec {
            sender.sendMessage(lang("ArgNotValid"))
            return@exec
        }
        sub(addBlacklist)
        sub(removeBlacklist)
    }

    private val members = command<CommandSender>("members") {
        // pixelworldpro members add/remove <player> <world>
        permission = "pixelworldpro.command.members"
        exec {
            sender.sendMessage(lang("ArgNotValid"))
            return@exec
        }
        sub(addMembers)
        sub(removeMembers)
        sub(setNumber)
    }

    private val load = command<CommandSender>("load") {
        permission = "pixelworldpro.command.load"
        exec {
            if (args.isEmpty()) {
                //加载自己的世界
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val uuid = (sender as Player).uniqueId

                if (PixelWorldPro.instance.isBungee()) {
                    if (RedisManager.isLock(uuid)) {
                        sender.sendMessage(lang("WorldExist"))
                        return@exec
                    }
                } else {
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    if (worldData == null) {
                        sender.sendMessage(lang("WorldNotExist"))
                        return@exec
                    }
                    val world = Bukkit.getWorld(worldData.worldName + "/world")
                    if (world != null) {
                        sender.sendMessage(lang("WorldExist"))
                        return@exec
                    }
                }

                submit {
                    WorldApi.Factory.worldApi!!.loadWorld(uuid, null).thenApply {
                        if (it) {
                            sender.sendMessage(lang("LoadSuccess"))
                        } else {
                            sender.sendMessage(lang("LoadFail"))
                        }
                    }
                }
                return@exec
            }
            if (args.size == 1) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                //加载指定玩家的世界
                val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                if (PixelWorldPro.instance.isBungee()) {
                    if (RedisManager.isLock(uuid)) {
                        sender.sendMessage(lang("WorldExist"))
                        return@exec
                    }
                } else {
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    if (worldData == null) {
                        sender.sendMessage(lang("WorldNotExist"))
                        return@exec
                    }
                    val world = Bukkit.getWorld(worldData.worldName + "/world")
                    if (world != null) {
                        sender.sendMessage(lang("WorldExist"))
                        return@exec
                    }
                }
                submit {
                    WorldApi.Factory.worldApi!!.loadWorld(uuid, null).thenApply {
                        if (it) {
                            sender.sendMessage(lang("LoadSuccess"))
                        } else {
                            sender.sendMessage(lang("LoadFail"))
                        }
                    }
                }
                return@exec
            }
            sender.sendMessage(lang("ArgNotValid"))
        }
    }

    private val unload = command<Player>("unload") {
        permission = "pixelworldpro.command.unload"
        exec {
            if (args.size != 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            //卸载所在的世界
            val world = sender.world
            if (WorldApi.Factory.worldApi!!.unloadWorld(world)) {
                sender.sendMessage(lang("UnloadSuccess"))
            } else {
                sender.sendMessage(lang("UnloadFail"))
            }
        }
    }

    private val delete = command<CommandSender>("delete") {
        permission = "pixelworldpro.command.delete"
        exec {
            if (args.isEmpty()) {
                //删除自己的世界
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val uuid = (sender as Player).uniqueId

                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }

                WorldApi.Factory.worldApi!!.deleteWorld(uuid).thenApply {
                    if (it) {
                        PixelWorldPro.databaseApi.deleteWorldData(uuid)
                        sender.sendMessage(lang("DeleteSuccess"))
                    } else {
                        sender.sendMessage(lang("DeleteFail"))
                    }
                }
                return@exec
            }
            if (args.size == 1) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                //删除指定玩家的世界
                val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                WorldApi.Factory.worldApi!!.deleteWorld(uuid).thenApply {
                    if (it) {
                        PixelWorldPro.databaseApi.deleteWorldData(uuid)
                        sender.sendMessage(lang("DeleteSuccess"))
                    } else {
                        sender.sendMessage(lang("DeleteFail"))
                    }
                }
                return@exec
            }
            sender.sendMessage(lang("ArgNotValid"))
        }
    }

    private val reload = command<CommandSender>("reload") {
        permission = "pixelworldpro.command.admin"
        exec {
            PixelWorldPro.instance.reloadConfig()
            if (sender is Player) {
                val player = sender as Player
                player.sendMessage("PixelWorldPro重载配置")
            } else {
                Bukkit.getConsoleSender().sendMessage("PixelWorldPro重载配置")
            }
        }
    }

    private val create = command<CommandSender>("create") {
        fun createWorld(uuid: UUID, name: String, file: File): CompletableFuture<Boolean> {
            return if (PixelWorldPro.instance.isBungee()) {
                WorldApi.Factory.worldApi!!.createWorld(uuid, name)
            } else {
                WorldApi.Factory.worldApi!!.createWorld(uuid, file)
            }
        }
        permission = "pixelworldpro.command.create"
        arg(templateArgNode) { file ->
            arg(singleOnlinePlayer, optional = true) { player ->
                exec {
                    if (valueOfOrNull(player) == null) {
                        if (sender is Player) {
                            val uuid = (sender as Player).uniqueId
                            if (PixelWorldPro.databaseApi.getWorldData(uuid) != null) {
                                sender.sendMessage(lang("AlreadyHasWorld"))
                            } else {
                                createWorld(uuid, args[0], valueOf(file)).thenApply {
                                    if (it) {
                                        sender.sendMessage(lang("WorldCreateSuccess"))
                                        TeleportApi.Factory.teleportApi!!.teleport((sender as Player).uniqueId)
                                    } else {
                                        sender.sendMessage(lang("WorldCreateFail"))
                                    }
                                }
                            }
                        } else {
                            sender.sendMessage(lang("NeedArg"))
                        }
                        return@exec
                    }
                    if (!sender.hasPermission("pixelworldpro.command.admin")) {
                        sender.sendMessage(lang("NoPermission"))
                        return@exec
                    }
                    val uuid = valueOf(player).uniqueId
                    if (PixelWorldPro.databaseApi.getWorldData(uuid) != null) {
                        sender.sendMessage(lang("OtherAlreadyHasWorld"))
                    } else {
                        createWorld(uuid, args[0], valueOf(file)).thenApply {
                            if (it) {
                                sender.sendMessage(lang("WorldCreateSuccess"))
                                TeleportApi.Factory.teleportApi!!.teleport((sender as Player).uniqueId, uuid)
                            } else {
                                sender.sendMessage(lang("WorldCreateFail"))
                            }
                        }
                    }
                }
            }
        }
    }

    private val dimensioncreate = command<CommandSender>("create") {
        permission = "pixelworldpro.command.dimension.create"
        exec {
            if (args.isNotEmpty()) {
                if (sender is Player) {
                    val player = sender as Player
                    val back = WorldImpl.createDimension(player.uniqueId, player, args[0])
                    if (!back) {
                        player.sendMessage("创建维度失败")
                    }
                }
            }
        }
    }

    private val dimensiontp = command<CommandSender>("tp") {
        permission = "pixelworldpro.command.dimension.tp"
        exec {
            when (args.size) {
                0 -> {
                    sender.sendMessage("传送维度不能为空")
                }

                1 -> {
                    if (sender !is Player) {
                        sender.sendMessage("需要玩家执行该命令")
                        return@exec
                    }
                    val player = sender as Player
                    val uuid = WorldProtect.getWorldNameUUID(player.world.name)
                    if (uuid == null) {
                        sender.sendMessage("无法获取当前世界对应创建者的uuid")
                        return@exec
                    }
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    if (worldData == null) {
                        sender.sendMessage("无法获取 $uuid 的世界数据")
                        return@exec
                    }
                    val dimensionData = Config.getWorldDimensionData(worldData.worldName)
                    if (args[0] !in dimensionData.createlist) {
                        sender.sendMessage("$uuid 没有创建维度$args[0]")
                        return@exec
                    }
                    val world = Bukkit.getWorld("${worldData.worldName}/${args[0]}")
                    if (world == null) {
                        player.sendMessage("维度未加载，尝试加载维度")
                        val back = WorldImpl.loadDimension(uuid, player, args[0])
                        if (!back) {
                            player.sendMessage("加载维度失败")
                        } else {
                            player.sendMessage("加载维度成功")
                        }
                    } else {
                        player.teleport(world.spawnLocation)
                        player.sendMessage("传送至指定维度")
                    }
                }
            }
        }
    }

    private val dimensionload = command<CommandSender>("load") {
        permission = "pixelworldpro.command.dimension.load"
        exec {
            if (args.isNotEmpty()) {
                if (sender is Player) {
                    val player = sender as Player
                    val back = WorldImpl.loadDimension(player.uniqueId, player, args[0])
                    if (!back) {
                        player.sendMessage("加载维度失败")
                    } else {
                        player.sendMessage("加载维度成功")
                    }
                }
            }
        }
    }

    private val dimension = command<CommandSender>("dimension") {
        permission = "pixelworldpro.command.dimension"
        exec {
            return@exec
        }
        sub(dimensioncreate)
        sub(dimensionload)
        sub(dimensiontp)
    }

    private val migrateppw = command<Player>("migrateppw") {
        description = "迁移数据"
        exec {
            submit(async = true) {
                Migrate.ppw(sender)
            }
        }
    }

    private val tp = command<CommandSender>("tp") {
        permission = "pixelworldpro.command.tp"
        exec {
            if (args.size == 0) {
                //传送到自己的世界
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val uuid = (sender as Player).uniqueId
                if (PixelWorldPro.databaseApi.getWorldData(uuid) == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                TeleportApi.Factory.teleportApi!!.teleport(uuid)
                sender.sendMessage(lang("Teleport"))
            }
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val player = sender as Player
                //传送到指定玩家的世界
                val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                if (worldData == null) {
                    player.sendMessage(lang("WorldNotExist"))
                    return@exec
                }

                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    when (worldData.state) {
                        "owner" -> {
                            player.sendMessage(
                                PixelWorldPro.instance.lang().getString("CouldNotJoin") ?: "此世界无法进入"
                            )
                            return@exec
                        }

                        "member" -> {
                            if (!worldData.members.contains(player.uniqueId)) {
                                player.sendMessage(
                                    PixelWorldPro.instance.lang().getString("CouldNotJoin") ?: "此世界无法进入"
                                )
                                return@exec
                            }
                        }
                    }
                }
                TeleportApi.Factory.teleportApi!!.teleport(player.uniqueId, uuid)
                sender.sendMessage(lang("Teleport"))
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                //指定玩家传送到指定世界
                val worlduuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                val playeruuid = Bukkit.getOfflinePlayer(args[1]).uniqueId
                if (PixelWorldPro.databaseApi.getWorldData(worlduuid) == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                TeleportApi.Factory.teleportApi!!.teleport(playeruuid, worlduuid)
                sender.sendMessage(lang("Teleport"))
            }
        }
    }

    private val levelup = command<CommandSender>("levelup") {
        permission = "pixelworldpro.command.levelup"
        exec {
            if (args.size == 0) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                sender.sendMessage(Level.levelUp((sender as Player).uniqueId))
            }
            if (args.size == 1) {
                //权限判断
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                val level = worldData.worldLevel
                val levelMap = Level.buildLevel()
                if (levelMap[level.toInt() + 1] == null) {
                    sender.sendMessage(lang("LevelMax"))
                    return@exec
                }
                val nextLevel = (level.toInt() + 1).toString()
                //数据库更新
                val worldDataNew = worldData.copy(worldLevel = nextLevel)
                PixelWorldPro.databaseApi.setWorldData(uuid, worldDataNew)
                if (PixelWorldPro.instance.isBungee()) {
                    RedisManager.push("updateWorldLevel|,|$uuid|,|$nextLevel")
                    //获取世界是否加载
                    val dimensionData = Config.getWorldDimensionData(worldData.worldName)
                    val dimensionlist = dimensionData.createlist
                    for (dimension in dimensionlist) {
                        val worlds = Bukkit.getWorld(worldData.worldName + "/" + dimension)
                        if (worlds != null) {
                            //世界边界更新
                            WorldImpl.setWorldBorder(worlds, nextLevel)
                        }
                    }
                } else {
                    //获取世界是否加载
                    val dimensionData = Config.getWorldDimensionData(worldData.worldName)
                    val dimensionlist = dimensionData.createlist
                    for (dimension in dimensionlist) {
                        val worlds = Bukkit.getWorld(worldData.worldName + "/" + dimension)
                        if (worlds != null) {
                            //世界边界更新
                            WorldImpl.setWorldBorder(worlds, nextLevel)
                        }
                    }
                }
                sender.sendMessage(lang("LevelUp"))
            }
            if (args.size == 2) {
                //权限判断
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                val level = worldData.worldLevel
                val levelMap = Level.buildLevel()
                if (levelMap[level.toInt() + 1] == null) {
                    sender.sendMessage(lang("LevelMax"))
                    return@exec
                }
                try {
                    args[1].toInt()
                } catch (_: Exception) {
                    sender.sendMessage("等级参数错误")
                    return@exec
                }
                val nextLevel = args[1]
                //数据库更新
                val worldDataNew = worldData.copy(worldLevel = nextLevel)
                PixelWorldPro.databaseApi.setWorldData(uuid, worldDataNew)
                if (PixelWorldPro.instance.isBungee()) {
                    RedisManager.push("updateWorldLevel|,|$uuid|,|$nextLevel")
                } else {
                    val world = Bukkit.getWorld(worldData.worldName + "/world")
                    if (world != null) {
                        //世界边界更新
                        WorldImpl.setWorldBorder(world, nextLevel)
                    }
                    //获取世界是否加载
                    val dimensionData = Config.getWorldDimensionData(worldData.worldName)
                    val dimensionlist = dimensionData.createlist
                    for (dimension in dimensionlist) {
                        val worlds = Bukkit.getWorld(worldData.worldName + "/" + dimension)
                        if (worlds != null) {
                            //世界边界更新
                            WorldImpl.setWorldBorder(worlds, nextLevel)
                        }
                    }
                }
                sender.sendMessage(lang("LevelUp"))
            }
        }
    }

    private val gui = command<CommandSender>("gui") {
        permission = "pixelworldpro.command.gui"
        arg(guiArgNode) { gui ->
            onlinePlayers(optional = true) { player ->
                exec {
                    if (valueOfOrNull(player) == null) {
                        Gui.open(sender as Player, valueOf(gui))
                    } else {
                        if (!sender.hasPermission("pixelworldpro.command.admin")) {
                            sender.sendMessage(lang("NoPermission"))
                            return@exec
                        }
                        //打开指定玩家的gui
                        val playerList = mutableListOf<Player>()
                        playerList.forEach {
                            Gui.open(it, valueOf(gui))
                        }
                    }
                }
            }
        }
    }

    private val inviteadd = command<CommandSender>("add") {
        permission = "pixelworldpro.command.invite.add"
        exec {
            if (args.size == 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val player = sender as Player
                val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                val friend = JiangCore.getPlayer(args[0])
                if (friend == null) {
                    sender.sendMessage("无法找到玩家 ${args[0]}")
                    return@exec
                }
                if (player.uniqueId == friend.uuid) {
                    sender.sendMessage("你不能向你自己发出邀请函")
                    return@exec
                }
                if (friend.uuid in worldData.banPlayers) {
                    sender.sendMessage("玩家 ${args[0]} 在你的黑名单里")
                    return@exec
                }
                val playerData = PixelWorldPro.databaseApi.getPlayerData(friend.uuid)
                if (playerData == null) {
                    sender.sendMessage("无法找到玩家 ${args[0]} 的数据")
                    return@exec
                }
                val inviterMsg = playerData.inviterMsg as ArrayList
                inviterMsg.add(player.uniqueId)
                playerData.inviterMsg = inviterMsg
                PixelWorldPro.databaseApi.setPlayerData(friend.uuid, playerData)
                val inviter = worldData.inviter as ArrayList
                inviter.add(friend.uuid)
                worldData.inviter = inviter
                PixelWorldPro.databaseApi.setWorldData(player.uniqueId, worldData)
                player.sendMessage("成功发送邀请函")
                if (friend.online) {
                    val f = Bukkit.getPlayer(friend.uuid) ?: return@exec
                    f.sendMessage("${player.name}向你发出了一份邀请函")
                }
            }
        }
    }

    private val inviteaddfriends = command<CommandSender>("addfriends") {
        permission = "pixelworldpro.command.invite.addfriends"
        exec {
            if (sender !is Player) {
                sender.sendMessage(lang("NeedPlayer"))
                return@exec
            }
            val player = sender as Player
            /*val friendList = Friends.getfriendslist(player.uniqueId)
            if (friendList == null) {
                player.sendMessage("无法寻找到你的好友列表")
                return@exec
            }
            for (uuid in friendList) {
                val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    continue
                }
                val friend = JiangCore.getPlayer(uuid.toString())
                if (friend == null) {
                    sender.sendMessage("无法找到玩家 $uuid")
                    continue
                }
                if (player.uniqueId == friend.uuid) {
                    sender.sendMessage("你不能向你自己发出邀请函")
                    continue
                }
                if (friend.uuid in worldData.banPlayers) {
                    sender.sendMessage("玩家 $uuid 在你的黑名单里")
                    continue
                }
                val playerData = PixelWorldPro.databaseApi.getPlayerData(friend.uuid)
                if (playerData == null) {
                    sender.sendMessage("无法找到玩家 $uuid 的数据")
                    continue
                }
                val inviterMsg = playerData.inviterMsg as ArrayList
                inviterMsg.add(player.uniqueId)
                playerData.inviterMsg = inviterMsg
                PixelWorldPro.databaseApi.setPlayerData(friend.uuid, playerData)
                val inviter = worldData.inviter as ArrayList
                inviter.add(friend.uuid)
                worldData.inviter = inviter
                PixelWorldPro.databaseApi.setWorldData(player.uniqueId, worldData)
                player.sendMessage("向${friend.name}发送邀请函")
                if(friend.online){
                    val f = Bukkit.getPlayer(friend.uuid) ?: continue
                    f.sendMessage("${player.name}向你发出了一份邀请函")
                }
            }

             */
        }
    }

    private val inviteremove = command<CommandSender>("remove") {
        permission = "pixelworldpro.command.invite.remove"
        exec {
            if (args.size == 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val player = sender as Player
                val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                val friend = JiangCore.getPlayer(args[0])
                if (friend == null) {
                    sender.sendMessage("无法找到玩家 ${args[0]}")
                    return@exec
                }
                if (friend.uuid !in worldData.inviter) {
                    sender.sendMessage("玩家 ${args[0]} 并没有你发出的邀请函")
                    return@exec
                }
                val playerData = PixelWorldPro.databaseApi.getPlayerData(friend.uuid)
                if (playerData == null) {
                    sender.sendMessage("无法找到玩家 ${args[0]} 的数据")
                    return@exec
                }
                val inviterMsg = playerData.inviterMsg as ArrayList
                inviterMsg.remove(player.uniqueId)
                playerData.inviterMsg = inviterMsg
                PixelWorldPro.databaseApi.setPlayerData(friend.uuid, playerData)
                val inviter = worldData.inviter as ArrayList
                inviter.remove(friend.uuid)
                worldData.inviter = inviter
                PixelWorldPro.databaseApi.setWorldData(player.uniqueId, worldData)
                player.sendMessage("成功删除邀请函")
            }
        }
    }

    private val invite = command<CommandSender>("invite") {
        permission = "pixelworldpro.command.invite"
        sub(inviteadd)
        sub(inviteremove)
        if (Bukkit.getPluginManager().isPluginEnabled("jiangfriends")) {
            sub(inviteaddfriends)
        }
    }

    //获取指定绝对路径下的所有文件夹并将文件夹名添加到模板列表中
    fun getTemplateList(templateList: MutableList<String>): MutableList<String> {
        val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath")!!)
        val tempList = file.listFiles()
        if (tempList != null) {
            for (i in tempList.indices) {
                if (tempList[i].isDirectory) {
                    templateList.add(tempList[i].name)
                }
            }
        }
        return templateList
    }

    private fun getGuiList(): MutableList<String> {
        val guiList = mutableListOf<String>()
        val file = File(PixelWorldPro.instance.dataFolder, "gui")
        val uiList = file.listFiles()
        if (uiList != null) {
            for (i in uiList.indices) {
                if (!uiList[i].isDirectory) {
                    guiList.add(uiList[i].name)
                } else {
                    val fileList = uiList[i].listFiles()
                    if (fileList != null) {
                        for (j in fileList.indices) {
                            if (!fileList[j].isDirectory) {
                                guiList.add("${uiList[i].name}/${fileList[j].name}")
                            }
                        }
                    }
                }
            }
        }
        return guiList
    }

    private val mode = command<CommandSender>("mode") {
        permission = "pixelworldpro.command.mode"
        arg(modeArgNode) {
            exec {
                if (args.size == 1) {
                    if (sender !is Player) {
                        sender.sendMessage(lang("NeedPlayer"))
                        return@exec
                    }
                    val uuid = (sender as Player).uniqueId
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    if (worldData == null) {
                        sender.sendMessage(lang("WorldNotExist"))
                        return@exec
                    }
                    val mode = valueOf(modeArgNode)

                    //数据库更新
                    val worldDataNew = worldData.copy(state = mode)
                    PixelWorldPro.databaseApi.setWorldData(uuid, worldDataNew)
                    sender.sendMessage(lang("ModeChange"))
                }
                if (args.size == 2) {
                    if (!sender.hasPermission("pixelworldpro.command.admin")) {
                        sender.sendMessage(lang("NoPermission"))
                        return@exec
                    }
                    val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                    val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                    if (worldData == null) {
                        sender.sendMessage(lang("WorldNotExist"))
                        return@exec
                    }
                    val mode = valueOf(modeArgNode)

                    //数据库更新
                    val worldDataNew = worldData.copy(state = mode)
                    PixelWorldPro.databaseApi.setWorldData(uuid, worldDataNew)
                    sender.sendMessage(lang("ModeChange"))
                }
            }
        }
    }

    private val seed = command<CommandSender>("seed") {
        permission = "pixelworldpro.command.seed"
        exec {
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val worldData = PixelWorldPro.databaseApi.getWorldData((sender as Player).uniqueId)
                if (worldData == null) {
                    if (PixelWorldPro.instance.isBungee()) {
                        RedisManager.setSeed((sender as Player).uniqueId, args[0])
                        sender.sendMessage("设置成功")
                    } else {
                        WorldImpl.setSeed((sender as Player).uniqueId, args[0])
                        sender.sendMessage("设置成功")
                    }
                } else {
                    Config.setWorldDimensionData(worldData.worldName, "seed", args[0])
                    sender.sendMessage("设置成功")
                }
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[1])
                if (player.name == null) {
                    sender.sendMessage(lang("PlayerNotFound"))
                    return@exec
                }
                val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId)
                if (worldData == null) {
                    if (PixelWorldPro.instance.isBungee()) {
                        RedisManager.setSeed(player.uniqueId, args[0])
                        sender.sendMessage("设置成功")
                    } else {
                        WorldImpl.setSeed(player.uniqueId, args[0])
                        sender.sendMessage("设置成功")
                    }
                } else {
                    Config.setWorldDimensionData(worldData.worldName, "seed", args[0])
                    sender.sendMessage("设置成功")
                }
            }
        }
    }

    private val worldTp = command<CommandSender>("tp") {
        permission = "pixelworldpro.command.world.tp"
        exec {
            if (args.size == 1) {
                if (sender !is Player) {
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                val world = Bukkit.getWorld(args[0])
                if (world == null) {
                    sender.sendMessage("无法找到世界${args[0]}")
                    return@exec
                }
                Bukkit.getConsoleSender().sendMessage("传送中")
                (sender as Player).teleport(world.spawnLocation)
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val player = Bukkit.getPlayer(args[1])
                if (player == null) {
                    sender.sendMessage(lang("PlayerNotFound"))
                    return@exec
                }
                val world = Bukkit.getWorld(args[0])
                if (world == null) {
                    sender.sendMessage("无法找到世界${args[0]}")
                    return@exec
                }
                Bukkit.getConsoleSender().sendMessage("传送中")
                player.teleport(world.spawnLocation)
            }
        }
    }

    private val worldLoad = command<CommandSender>("load") {
        permission = "pixelworldpro.command.admin"
        exec {
            if (args.size == 1) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                var world = Bukkit.getWorld(args[0])
                if (world != null) {
                    sender.sendMessage("世界已加载")
                    return@exec
                }
                world = Bukkit.createWorld(WorldCreator(args[0]))
                if (world != null) {
                    sender.sendMessage("世界加载成功")
                    return@exec
                } else {
                    sender.sendMessage("世界加载失败")
                    return@exec
                }
            } else {
                sender.sendMessage("参数不合法")
            }
        }
    }

    private val worldUnLoad = command<CommandSender>("unload") {
        permission = "pixelworldpro.command.admin"
        exec {
            if (args.size == 1) {
                if (!sender.hasPermission("pixelworldpro.command.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val world = Bukkit.getWorld(args[0])
                if (world == null) {
                    sender.sendMessage("世界未加载")
                    return@exec
                }
                if (Bukkit.unloadWorld(world, true)) {
                    sender.sendMessage("世界卸载成功")
                    return@exec
                } else {
                    sender.sendMessage("世界卸载失败")
                    return@exec
                }
            } else {
                sender.sendMessage("参数不合法")
            }
        }
    }

    private val move = command<CommandSender>("move") {
        permission = "pixelworldpro.command.admin"
        exec {
            if ((args.size == 2).and(sender.hasPermission("pixelworldpro.command.admin"))) {
                val from = JiangCore.getPlayer(args[0])
                if (from == null) {
                    sender.sendMessage("无法寻找到玩家${args[0]}")
                    return@exec
                }
                val to = JiangCore.getPlayer(args[1])
                if (to == null) {
                    sender.sendMessage("无法寻找到玩家${args[1]}")
                    return@exec
                }
                WorldMove.main(from.uuid, to.uuid, sender)
            } else {
                sender.sendMessage("参数不合法")
            }
        }
    }

    private val world = command<CommandSender>("world") {
        permission = "pixelworldpro.command.world"
        sub(worldTp)
        sub(worldLoad)
        sub(worldUnLoad)
    }

    private val gameRule = command<CommandSender>("rule") {
        permission = "pixelworldpro.command.rule"
        exec {
            when (args.size) {
                2 -> {
                    if (!sender.hasPermission("pixelworldpro.command.rule")) {
                        sender.sendMessage(lang("NoPermission"))
                        return@exec
                    }
                    val player = sender as Player
                    val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return@exec
                    val gameRule = worldData.gameRule
                    gameRule[args[0]] = args[1]
                    worldData.gameRule = gameRule
                    PixelWorldPro.databaseApi.setWorldData(player.uniqueId, worldData)
                    val world = Bukkit.getWorld(worldData.worldName + "/world") ?: return@exec
                    WorldApi.Factory.worldApi!!.setDataGameRule(world)
                }

                3 -> {
                    if (!sender.hasPermission("pixelworldpro.command.admin")) {
                        sender.sendMessage(lang("NoPermission"))
                        return@exec
                    }
                    val player = JiangCore.getPlayer(args[0]) ?: return@exec
                    val worldData = PixelWorldPro.databaseApi.getWorldData(player.uuid) ?: return@exec
                    val gameRule = worldData.gameRule
                    gameRule[args[0]] = args[1]
                    worldData.gameRule = gameRule
                    PixelWorldPro.databaseApi.setWorldData(player.uuid, worldData)
                    val world = Bukkit.getWorld(worldData.worldName + "/world") ?: return@exec
                    WorldApi.Factory.worldApi!!.setDataGameRule(world)
                }

                else -> {
                    sender.sendMessage("参数不合法")
                }
            }
        }
    }

    private val gameLocation = command<CommandSender>("location") {
        permission = "pixelworldpro.command.location"
        exec {
            when (args.size) {
                0 -> {
                    if (!sender.hasPermission("pixelworldpro.command.location")) {
                        sender.sendMessage(lang("NoPermission"))
                        return@exec
                    }
                    val player = sender as Player
                    val worldData = PixelWorldPro.databaseApi.getWorldData(player.uniqueId) ?: return@exec
                    if (player.world.name == worldData.worldName + "/world") {
                        val location = HashMap<String, Double>()
                        location["x"] = player.location.x
                        location["y"] = player.location.y
                        location["z"] = player.location.z
                        worldData.location = location
                        PixelWorldPro.databaseApi.setWorldData(player.uniqueId, worldData)
                    } else {
                        sender.sendMessage("不在你的世界内")
                    }
                }

                1 -> {
                    if (!sender.hasPermission("pixelworldpro.command.admin")) {
                        sender.sendMessage(lang("NoPermission"))
                        return@exec
                    }
                    val player = JiangCore.getPlayer(args[0]) ?: return@exec
                    val send = sender as Player
                    val worldData = PixelWorldPro.databaseApi.getWorldData(player.uuid) ?: return@exec
                    if (send.world.name == worldData.worldName + "/world") {
                        val location = HashMap<String, Double>()
                        location["x"] = send.location.x
                        location["y"] = send.location.y
                        location["z"] = send.location.z
                        worldData.location = location
                        PixelWorldPro.databaseApi.setWorldData(player.uuid, worldData)
                    } else {
                        sender.sendMessage("不在指定的世界内")
                    }
                }

                else -> {
                    sender.sendMessage("参数不合法")
                }
            }
        }
    }

    private val moveToMysql = command<CommandSender>("moveToMysql") {
        permission = "pixelworldpro.command.admin"
        exec {
            if (sender.hasPermission("pixelworldpro.command.admin")) {
                MoveDataBase().mysql()
            } else {
                sender.sendMessage("参数不合法")
            }
        }
    }

    private val checkPlayerData = command<CommandSender>("player") {
        permission = "pixelworldpro.command.admin"
        exec {
            Check.playerData()
        }
    }

    private val check = command<CommandSender>("check") {
        permission = "pixelworldpro.command.admin"
        sub(checkPlayerData)
    }

    private val install = command<CommandSender>("install") {
        permission = "pixelworldpro.command.admin"
        exec {
            Install.start()
        }
    }

    private val clean = command<CommandSender>("clean") {
        permission = "pixelworldpro.command.admin"
        exec {
            if (args.size > 2) {
                if (args[0] != "yes") {
                    return@exec
                }
                if (args[1] == "true") {
                    Clean.clean(true)
                } else {
                    Clean.clean(false)
                }
            }
        }
    }

    private fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }

    private val mainCommand = PixelWorldPro.instance.config.getString("mainCommand") ?: "pwp"
    val commandRoot = command<CommandSender>(mainCommand) {
        permission = "pixelworldpro.command"
        sub(blacklist)
        sub(create)
        sub(delete)
        sub(dimension)
        sub(invite)
        sub(tp)
        sub(reload)
        sub(seed)
        sub(unload)
        sub(load)
        sub(members)
        sub(migrateppw)
        sub(move)
        sub(levelup)
        sub(gui)
        sub(mode)
        sub(world)
        sub(gameRule)
        sub(check)
        sub(install)
        sub(gameLocation)
        sub(moveToMysql)
        sub(clean)
    }
}
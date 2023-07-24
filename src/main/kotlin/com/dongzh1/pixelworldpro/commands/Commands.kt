package com.dongzh1.pixelworldpro.commands

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.impl.WorldImpl
import com.dongzh1.pixelworldpro.listener.WorldProtect
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.command.ArgNode
import com.xbaimiao.easylib.module.command.command
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Suppress("DEPRECATION")
class Commands {


    private val templateArgNode = ArgNode(lang("TemplateWorld"),
        exec = {
            getTemplateList(mutableListOf())
        }, parse = {
            //直接传递文件夹，绝对路径
            File(PixelWorldPro.instance.config.getString("WorldTemplatePath")!! + File.separator + it)
        }
    )

    private val guiArgNode = ArgNode(lang("GuiName"),
        exec = {
            getGuiList()
        }, parse = {
            BuiltInConfiguration("gui/${it}")
        }
    )

    private val singleOnlinePlayer = ArgNode(lang("PlayerName"),
        exec = {
            //获取在线玩家名
            Bukkit.getOnlinePlayers().map { it.name }
        }, parse = {
            //不存在就报错没有
            Bukkit.getPlayer(it) ?: throw Exception(lang("PlayerNotFound"))
        }
    )
    private val modeArgNode = ArgNode(lang("Mode"),
        exec = {
            listOf("anyone", "member", "owner")
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
                worldData = worldData.copy(members = worldData.members - player.uniqueId, memberName = worldData.memberName - player.name!!)
                PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData)
                //指定玩家的信息也要更新
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    if (playerData != null) {
                        playerData =
                            playerData.copy(joinedWorld = playerData.joinedWorld - (sender as Player).uniqueId)
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
                    worldData.copy(members = worldData.members - player.uniqueId, memberName = worldData.memberName - player.name!!)
                )
                //指定玩家的信息也要更新
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    if (playerData != null) {
                        playerData =
                            playerData!!.copy(joinedWorld = playerData!!.joinedWorld - worldPlayer.uniqueId)
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
                if(player.name == null){
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
                    if(worldData!!.members.size < playerData.memberNumber){
                        worldData = worldData!!.copy(members = worldData!!.members + player.uniqueId, memberName = worldData!!.memberName + player.name!!)
                        PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData!!)
                        var playerData1 = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                        if (playerData1 == null) {
                            playerData1 = PlayerData(
                                mutableListOf((sender as Player).uniqueId),
                                Gui.getMembersEditConfig().getInt("DefaultMembersNumber")
                            )
                            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData1)
                        } else {
                            playerData1 =
                                playerData1.copy(joinedWorld = playerData.joinedWorld + (sender as Player).uniqueId)
                            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData1)
                        }
                        if (player.isOnline.and(Bukkit.getWorld(player.uniqueId)?.name == worldData!!.worldName)){
                            TeleportApi.Factory.teleportApi?.teleport(WorldProtect.getWorldNameUUID(worldData!!.worldName),player.uniqueId)
                        }
                        sender.sendMessage(lang("Success"))
                    }else{
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
                if(player.name == null){
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
                    worldData.copy(members = worldData.members + player.uniqueId, memberName = worldData.memberName + player.name!!)
                )
                //指定玩家的信息也要更新
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    if (playerData == null) {
                        playerData = PlayerData(
                            mutableListOf(worldPlayer.uniqueId),
                            Gui.getMembersEditConfig().getInt("DefaultMembersNumber")
                        )
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
                    } else {
                        playerData =
                            playerData.copy(joinedWorld = playerData.joinedWorld + worldPlayer.uniqueId)
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
                worldData = worldData.copy(banPlayers = worldData.banPlayers - player.uniqueId, banName = worldData.banName - player.name!!)
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
                    worldData.copy(banPlayers = worldData.banPlayers - player.uniqueId, banName = worldData.banName - player.name!!)
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
                if(player.name == null){
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
                worldData = worldData.copy(banPlayers = worldData.banPlayers + player.uniqueId, banName = worldData.banName + player.name!!)
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
                if(player.name == null){
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
                    worldData.copy(banPlayers = worldData.banPlayers + player.uniqueId, banName = worldData.banName + player.name!!)
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
        exec {
            if(args.size == 0) {
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size > 2){
                sender.sendMessage(lang("ArgNotValid"))
                return@exec
            }
            if (args.size == 1){
                if(sender !is Player){
                    sender.sendMessage(lang("NeedPlayer"))
                    return@exec
                }
                if (args[0].toIntOrNull() == null || args[0].toInt() < 1){
                    sender.sendMessage(lang("ArgNotValid"))
                    return@exec
                }
                val player = sender as Player
                val number = args[0].toInt()
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    playerData =
                        playerData?.copy(memberNumber = number) ?: PlayerData(mutableListOf(player.uniqueId), number)
                    PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
                }
                sender.sendMessage(lang("Success"))
            }
            if (args.size ==2){
                val number = args[0].toIntOrNull()
                val player = Bukkit.getOfflinePlayer(args[1])
                if (number == null || number < 1){
                    sender.sendMessage(lang("ArgNotValid"))
                    return@exec
                }
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    playerData =
                        playerData?.copy(memberNumber = number) ?: PlayerData(mutableListOf(player.uniqueId), number)
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
                    val world = Bukkit.getWorld(worldData.worldName)
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
                    val world = Bukkit.getWorld(worldData.worldName)
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
                    if (it){
                        PixelWorldPro.databaseApi.deleteWorldData(uuid)
                        sender.sendMessage(lang("DeleteSuccess"))
                    }else{
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
                    if (it){
                        PixelWorldPro.databaseApi.deleteWorldData(uuid)
                        sender.sendMessage(lang("DeleteSuccess"))
                    }else{
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
        }
    }

    private val mspt = command<CommandSender>("mspt") {
        permission = "pixelworldpro.command.admin"
        exec {
            if (!PixelWorldPro.instance.isBungee()) {
                sender.sendMessage("§cNeed Bungee")
                return@exec
            }
            val msptValue = RedisManager.getMspt()
            if (msptValue == null) {
                sender.sendMessage("§cmspt: §cnull")
            } else {
                for (server in msptValue.split(",")) {
                    if (server.split(":")[0] != "") {
                        sender.sendMessage("§cmspt: §a${server.split(":")[0]} §c${server.split(":")[1]}")
                    }
                }
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
                                    if (it){
                                        sender.sendMessage(lang("WorldCreateSuccess"))
                                        TeleportApi.Factory.teleportApi!!.teleport((sender as Player).uniqueId)
                                    }else{
                                        sender.sendMessage(lang("WorldCreateFail"))
                                    }
                                }
                            }
                        } else {
                            sender.sendMessage(lang("NeedArg"))
                        }
                        return@exec
                    }
                    if (!sender.hasPermission("pixelworldpro.command.admin")){
                        sender.sendMessage(lang("NoPermission"))
                        return@exec
                    }
                    val uuid = valueOf(player).uniqueId
                    if (PixelWorldPro.databaseApi.getWorldData(uuid) != null) {
                        sender.sendMessage(lang("OtherAlreadyHasWorld"))
                    } else {
                        createWorld(uuid, args[0], valueOf(file)).thenApply {
                            if (it){
                                sender.sendMessage(lang("WorldCreateSuccess"))
                                TeleportApi.Factory.teleportApi!!.teleport((sender as Player).uniqueId, uuid)
                            }else{
                                sender.sendMessage(lang("WorldCreateFail"))
                            }
                        }
                    }
                }
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
                //传送到指定玩家的世界
                val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                if (PixelWorldPro.databaseApi.getWorldData(uuid) == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                TeleportApi.Factory.teleportApi!!.teleport((sender as Player).uniqueId, uuid)
                sender.sendMessage(lang("Teleport"))
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.command.admin")){
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
            if(args.size == 0){
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
                val level = worldData.worldLevel
                val levelList = PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!
                    .getKeys(false).toMutableList()
                if (levelList.indexOf(level) == levelList.size - 1) {
                    sender.sendMessage(lang("LevelMax"))
                    return@exec
                }
                val nextLevel = levelList[levelList.indexOf(level) + 1]
                //数据库更新
                val worldDataNew = worldData.copy(worldLevel = nextLevel)
                PixelWorldPro.databaseApi.setWorldData(uuid, worldDataNew)
                if (PixelWorldPro.instance.isBungee()){
                    RedisManager.push("updateWorldLevel|,|$uuid|,|$nextLevel")
                }else{
                    //获取世界是否加载
                    val world = Bukkit.getWorld(worldData.worldName)
                    if (world != null) {
                        //世界边界更新
                        WorldImpl.setWorldBorder(world, nextLevel)
                    }
                }
                sender.sendMessage(lang("LevelUp"))
            }
            if(args.size == 1){
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
                val levelList = PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!
                    .getKeys(false).toMutableList()
                if (levelList.indexOf(level) == levelList.size - 1) {
                    sender.sendMessage(lang("LevelMax"))
                    return@exec
                }
                val nextLevel = levelList[levelList.indexOf(level) + 1]
                //数据库更新
                val worldDataNew = worldData.copy(worldLevel = nextLevel)
                PixelWorldPro.databaseApi.setWorldData(uuid, worldDataNew)
                if (PixelWorldPro.instance.isBungee()){
                    RedisManager.push("updateWorldLevel|,|$uuid|,|$nextLevel")
                }else{
                    //获取世界是否加载
                    val world = Bukkit.getWorld(worldData.worldName)
                    if (world != null) {
                        //世界边界更新
                        WorldImpl.setWorldBorder(world, nextLevel)
                    }
                }
                sender.sendMessage(lang("LevelUp"))
            }
        }
    }

    private val gui = command<CommandSender>("gui"){
        permission = "pixelworldpro.command.gui"
        arg(guiArgNode){gui ->
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
                }else{
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
    private val mode = command<CommandSender>("mode"){
        permission = "pixelworldpro.command.mode"
        arg(modeArgNode){
            exec {
                if(args.size ==0){
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
                if(args.size == 1){
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

    private fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }

    val commandRoot = command<CommandSender>("pixelworldpro") {
        permission = "pixelworldpro.command"
        sub(create)
        sub(tp)
        sub(reload)
        sub(mspt)
        sub(delete)
        sub(unload)
        sub(load)
        sub(members)
        sub(blacklist)
        sub(levelup)
        sub(gui)
        sub(mode)
    }
}
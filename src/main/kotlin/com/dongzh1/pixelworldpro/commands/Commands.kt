package com.dongzh1.pixelworldpro.commands

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.gui.WorldCreate
import com.dongzh1.pixelworldpro.impl.WorldImpl
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.command.ArgNode
import com.xbaimiao.easylib.module.command.command
import com.xbaimiao.easylib.module.item.giveItem
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID

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
                worldData = worldData.copy(members = worldData.members - player.uniqueId)
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
                if (!sender.hasPermission("pixelworldpro.admin")) {
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
                PixelWorldPro.databaseApi.setWorldData(
                    worldPlayer.uniqueId,
                    worldData.copy(members = worldData.members - player.uniqueId)
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
                worldData = worldData.copy(members = worldData.members + player.uniqueId)
                PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData)
                //指定玩家的信息也要更新
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    if (playerData == null) {
                        playerData = PlayerData(
                            mutableListOf((sender as Player).uniqueId),
                            Gui.getMembersEditConfig().getInt("DefaultMembersNumber")
                        )
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
                    } else {
                        playerData =
                            playerData.copy(joinedWorld = playerData.joinedWorld + (sender as Player).uniqueId)
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
                    }
                    sender.sendMessage(lang("Success"))
                }
                return@exec
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[0])
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
                    worldData.copy(members = worldData.members + player.uniqueId)
                )
                //指定玩家的信息也要更新
                submit(async = true) {
                    var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
                    if (playerData == null) {
                        playerData = PlayerData(
                            mutableListOf(worldPlayer.uniqueId),
                            Gui.getMembersEditConfig().getInt("DefaultMembersNumber")
                        )
                        PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
                    } else {
                        playerData =
                            playerData!!.copy(joinedWorld = playerData!!.joinedWorld + worldPlayer.uniqueId)
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
                worldData = worldData.copy(banPlayers = worldData.banPlayers - player.uniqueId)
                PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData)
                sender.sendMessage(lang("Success"))
                return@exec
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.admin")) {
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
                    worldData.copy(banPlayers = worldData.banPlayers - player.uniqueId)
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
                worldData = worldData.copy(banPlayers = worldData.banPlayers + player.uniqueId)
                PixelWorldPro.databaseApi.setWorldData((sender as Player).uniqueId, worldData)
                //指定玩家的信息也要更新
                sender.sendMessage(lang("Success"))
                return@exec
            }
            if (args.size == 2) {
                if (!sender.hasPermission("pixelworldpro.admin")) {
                    sender.sendMessage(lang("NoPermission"))
                    return@exec
                }
                val player = Bukkit.getOfflinePlayer(args[0])
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
                    worldData.copy(banPlayers = worldData.banPlayers + player.uniqueId)
                )
                //指定玩家的信息也要更新
                sender.sendMessage(lang("Success"))
                return@exec
            }
            sender.sendMessage(lang("ArgNotValid"))
            return@exec
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
                    if (WorldApi.Factory.worldApi!!.loadWorld(uuid, null)) {
                        sender.sendMessage(lang("LoadSuccess"))
                    } else {
                        sender.sendMessage(lang("LoadFail"))
                    }
                }
                return@exec
            }
            if (args.size == 1) {
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
                    if (WorldApi.Factory.worldApi!!.loadWorld(uuid, null)) {
                        sender.sendMessage(lang("LoadSuccess"))
                    } else {
                        sender.sendMessage(lang("LoadFail"))
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

                if (WorldApi.Factory.worldApi!!.deleteWorld(uuid)) {
                    sender.sendMessage(lang("DeleteSuccess"))
                } else {
                    sender.sendMessage(lang("DeleteFail"))
                }
                return@exec
            }
            if (args.size == 1) {
                //删除指定玩家的世界
                val uuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                if (worldData == null) {
                    sender.sendMessage(lang("WorldNotExist"))
                    return@exec
                }
                if (WorldApi.Factory.worldApi!!.deleteWorld(uuid)) {
                    sender.sendMessage(lang("DeleteSuccess"))
                } else {
                    sender.sendMessage(lang("DeleteFail"))
                }
                return@exec
            }
            sender.sendMessage(lang("ArgNotValid"))
        }
    }

    private val debug = command<CommandSender>("debug") {
        permission = "pixelworldpro.command.debug"
        exec {
            PixelWorldPro.instance.reloadConfig()
            val item =
            Gui.buildItem(Gui.getWorldCreateConfig().getConfigurationSection("YX")!!,Bukkit.getOfflinePlayer("Pixel_meng"))
            (sender as Player).giveItem(item!!)

        }
    }

    private val mspt = command<CommandSender>("mspt") {
        permission = "pixelworldpro.command.mspt"
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
        fun createWorld(uuid: UUID, name: String, file: File):Boolean {
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
                                if (createWorld(uuid, args[0], valueOf(file))){
                                    sender.sendMessage(lang("WorldCreateSuccess"))
                                    TeleportApi.Factory.teleportApi!!.teleport((sender as Player).uniqueId)
                                } else {
                                    sender.sendMessage(lang("WorldCreateFail"))
                                }
                            }
                        } else {
                            sender.sendMessage(lang("NeedArg"))
                        }
                        return@exec
                    }
                    val uuid = valueOf(player).uniqueId
                    if (PixelWorldPro.databaseApi.getWorldData(uuid) != null) {
                        sender.sendMessage(lang("OtherAlreadyHasWorld"))
                    } else {
                        if (createWorld(uuid, args[0], valueOf(file))){
                            sender.sendMessage(lang("WorldCreateSuccess"))
                        } else {
                            sender.sendMessage(lang("WorldCreateFail"))
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
                //指定玩家传送到指定世界
                val playeruuid = Bukkit.getOfflinePlayer(args[0]).uniqueId
                val worlduuid = Bukkit.getOfflinePlayer(args[1]).uniqueId
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
                        WorldImpl().setWorldBorder(world, nextLevel)
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
                        WorldImpl().setWorldBorder(world, nextLevel)
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
                    if (valueOfOrNull(player) == null){
                        Gui.open(sender as Player, valueOf(gui))
                    }else{
                        //打开指定玩家的gui
                        val playerList = mutableListOf<Player>()
                        playerList.forEach{
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
        exec {
        }
        sub(create)

        sub(tp)

        sub(debug)
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
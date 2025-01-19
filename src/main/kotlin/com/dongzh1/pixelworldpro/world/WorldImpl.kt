package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.MessageApi
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.bungee.server.Bungee
import com.dongzh1.pixelworldpro.bungee.server.Server
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.listener.WorldProtect.Companion.getWorldNameUUID
import com.dongzh1.pixelworldpro.tools.Serialize
import com.dongzh1.pixelworldpro.world.Config.getWorldDimensionData
import com.dongzh1.pixelworldpro.world.Config.setWorldDimensionData
import com.wimbli.WorldBorder.Config
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.File
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import kotlin.collections.HashMap


object WorldImpl : WorldApi {
    private val unloadWorldList: MutableList<UUID> = mutableListOf()
    private val createWorldList: MutableList<UUID> = mutableListOf()
    private val loadWorldList: MutableList<UUID> = mutableListOf()
    val localWorldList: MutableList<UUID> = mutableListOf()
    private val timeOutWorldList: MutableList<String> = mutableListOf()
    private val seedMap = mutableMapOf<UUID, String>()
    val worldMap = mutableMapOf<UUID, WorldData>()


    fun autoWorldSave(){
        Thread{
            val saveTime = PixelWorldPro.instance.config.getInt("WorldSetting.saveTime")
            if (saveTime > 0) {
                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 启用世界自动保存")
                while (true) {
                    sleep(saveTime.toLong() * 60 * 1000)
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 自动保存世界")
                    WorldFile.saveWorld(localWorldList)
                }
            }
        }.start()
    }

    /**
     * 根据模板文件夹名新建世界,如果玩家在线则传送
     */
    override fun createWorld(uuid: UUID, templateName: String): CompletableFuture<Boolean> {
        return if (PixelWorldPro.instance.isBungee()) {
            com.dongzh1.pixelworldpro.bungee.world.World.createBungeeWorld(Bukkit.getPlayer(uuid)!!, templateName)
            CompletableFuture()
        } else {
            val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName)
            if(!file.exists()){
                Bukkit.getPlayer(uuid)!!.sendMessage("模板不存在")
            }
            createWorld(uuid, file)
        }
    }

    fun getCreateWorldList(): List<UUID> {
        return createWorldList
    }

    fun removeCreateWorldList(uuid: UUID) {
        createWorldList.remove(uuid)
    }

    /**
     * 根据模板文件新建世界,如果玩家在线则传送
     */
    override fun createWorld(uuid: UUID, file: File): CompletableFuture<Boolean> {
        return if (PixelWorldPro.instance.isBungee()) {
            createWorld(uuid, file.name)
        } else {
            val future = CompletableFuture<Boolean>()
            if (createWorldLocal(uuid, file, Bukkit.getPlayer(uuid)!!.name)) {
                future.complete(true)
            } else {
                future.complete(false)
            }
            return future
        }
    }

    fun createWorldLocal(uuid: UUID, templateName: String, playerName: String): Boolean {
        return createWorldLocal(uuid, File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName), playerName)

    }

    private fun createWorldLocal(uuid: UUID, file: File, playerName: String): Boolean {
        if(Thread.currentThread().name != "Server thread"){
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 非Server thread发起的世界创建，这会导致服务器崩溃！")
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 当前使用的线程：${Thread.currentThread().name}")
            return false
        }
        val time = System.currentTimeMillis()
        val createTime = time.toString()
        val date = Date(time)
        //把time时间格式化
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
        //把time时间格式化为字符串
        val timeString = formatter.format(date)

        //获取路径下对应的world文件夹
        val worldName = "${uuid}_$timeString"


        val realWorldName = "${worldPath()}/$worldName"

        //检查文件是否为世界文件
        val checkString = WorldFile.isBreak(file)
        if (checkString != "ok") {
            MessageApi.Factory.messageApi!!.sendMessage(uuid, checkString)
            return false
        }
        if (PixelWorldPro.instance.isBungee()) {
            RedisManager.setLock(uuid)
        }
        //复制模板文件夹到world文件夹
        file.copyRecursively(File(PixelWorldPro.instance.config.getString("WorldPath"), worldName))
        //加载世界
        val worldcreator = WorldCreator("$realWorldName/world")
        val seed = seedMap[uuid]
        if (seed != null){
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 设置世界种子${seed.toLong()}")
            worldcreator.seed(seed.toLong())
        }
        if (PixelWorldPro.instance.config.getString("WorldSetting.Creator.World") != "auto") {
            worldcreator.generator(PixelWorldPro.instance.config.getString("WorldSetting.Creator.World"))
        }
        val world = Bukkit.createWorld(worldcreator)
        if (world == null) {
            return false
        } else {
            //设置世界出身点
            if (WorldFile.worldSetting.getBoolean("location.enable")){
                WorldFile.setWorldLocation(world)
            }
            //设置世界规则
            setGameRule(world)
            //设置世界难度
            world.difficulty =
                Difficulty.valueOf(PixelWorldPro.instance.config.getString("WorldSetting.WorldDifficulty")!!)
            //设置世界时间
            setTime(world)
            //数据库操作
            PixelWorldPro.databaseApi.setWorldData(
                uuid,
                WorldData(
                    worldName = "PixelWorldPro/$worldName",
                    worldLevel = "1",
                    arrayListOf(uuid),
                    arrayListOf(playerName),
                    arrayListOf(),
                    arrayListOf(),
                    "anyone",
                    createTime,
                    System.currentTimeMillis(),
                    1,
                    false,
                    isCreateEnd = false,
                    arrayListOf(),
                    HashMap(),
                    HashMap()
                )
            )
            getWorldDimensionData("PixelWorldPro/$worldName")
            if (seed != null){
                setWorldDimensionData("PixelWorldPro/$worldName", "seed", seed)
            }
            //存玩家数据
            submit(async = true) {
                var playerData = PixelWorldPro.databaseApi.getPlayerData(uuid)
                if (playerData == null) {
                    playerData = PlayerData(
                        mutableListOf(uuid),
                        memberNumber = Gui.getMembersEditConfig().getInt("DefaultMembersNumber"),
                        listOf()
                    )
                    PixelWorldPro.databaseApi.setPlayerData(uuid, playerData)
                } else {
                    playerData = playerData.copy(joinedWorld = playerData.joinedWorld + uuid)
                    PixelWorldPro.databaseApi.setPlayerData(uuid, playerData)
                }
            }
            //设置世界边界
            submit {
                setWorldBorder(world, "1")
            }
            loadWorldList.add(uuid)
            if (Server.getLocalServer().mode == "build"){
                unloadWorld(uuid)
            }
        }
        return true
    }

    override fun restartWorld(uuid: UUID, templateName: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        deleteWorld(uuid).thenApply { value ->
            if (value) {
                PixelWorldPro.databaseApi.deleteWorldData(uuid)
                WorldApi.Factory.worldApi!!.createWorld(uuid, templateName).thenApply {
                    future.complete(it)
                }
            } else {
                future.complete(false)
            }
        }
        return future
    }

    fun worldPath(): String {
        return "PixelWorldPro"
    }

    /**
     * 卸载指定玩家世界
     */
    override fun unloadWorld(world: World): Boolean {
        //清空世界玩家
        if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            for(player in world.players) {
                Bungee.connect(player, PixelWorldPro.instance.config.getString("lobby")!!)
            }
        }
        world.players.forEach {
            it.teleport(Bukkit.getWorlds()[0].spawnLocation)
        }
        return Bukkit.unloadWorld(world, true)
    }

    override fun unloadWorld(uuid: UUID): CompletableFuture<Boolean> {
        unloadDimension(uuid)
        val future = CompletableFuture<Boolean>()
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
        if (PixelWorldPro.instance.isBungee()) {
            if (RedisManager.isLock(uuid)) {
                Bukkit.getConsoleSender().sendMessage("开始卸载世界 ${worldData.worldName.lowercase(Locale.getDefault())}")
                val worldName = worldData.worldName + "/world"
                val world = Bukkit.getWorld(worldName.lowercase(Locale.getDefault()))
                if (world != null) {
                    Bukkit.getConsoleSender().sendMessage("Bukkit卸载世界 ${worldData.worldName.lowercase(Locale.getDefault())}")
                    future.complete(unloadWorld(world))
                    future.thenApply {
                        if (!it){
                            if (!Bukkit.unloadWorld(world, true)) {
                                Bukkit.getConsoleSender()
                                    .sendMessage("Bukkit卸载世界 ${worldData.worldName.lowercase(Locale.getDefault())} 失败")
                            }
                        }
                    }
                    return future
                } else {
                    Bukkit.getConsoleSender().sendMessage("Bukkit无法找到世界 ${worldData.worldName.lowercase(Locale.getDefault())}")
                    unloadWorldList.add(uuid)

                    var i = 0
                    submit(async = true, period = 2L, maxRunningNum = 60) {
                        if (i == 0) {
                            RedisManager.push("unloadWorld|,|${uuid}")
                        }
                        i++
                        if (!unloadWorldList.contains(uuid)) {
                            future.complete(true)
                            this.cancel()
                            return@submit
                        }
                        if (i >= 50) {
                            future.complete(false)
                            this.cancel()
                            return@submit
                        }
                    }
                    return future
                }
            } else {
                future.complete(true)
                return future
            }
        } else {
            Bukkit.getConsoleSender().sendMessage("卸载世界${worldData.worldName}")
            val world = Bukkit.getWorld(worldData.worldName+"/world")
            return if (world != null) {
                Bukkit.getConsoleSender().sendMessage("Bukkit无法找到世界 ${worldData.worldName}")
                future.complete(unloadWorld(world))
                future
            } else {
                Bukkit.getConsoleSender().sendMessage("Bukkit卸载世界 ${worldData.worldName}")
                future.complete(true)
                future
            }
        }
    }

    fun getUnloadWorldList(): MutableList<UUID> {
        return unloadWorldList
    }

    fun removeUnloadWorldList(uuid: UUID) {
        unloadWorldList.remove(uuid)
    }


    /**
     * 删除指定玩家世界
     */
    override fun deleteWorld(uuid: UUID): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        unloadWorld(uuid).thenApply {
            if (it) {
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
                File(worldData.worldName).deleteRecursively()
                future.complete(true)
            } else {
                future.complete(false)
            }
        }
        return future
    }


    override fun loadWorld(uuid: UUID, serverName: String?): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        if (serverName == null || serverName == Server.getLocalServer().realName) {
            future.complete(loadWorldLocal(uuid))
            return future
        } else {
            if (PixelWorldPro.instance.isBungee()) {
                if (RedisManager.isLock(uuid)) {
                    future.complete(true)
                    return future
                } else {
                    var i = 0
                    submit(period = 2L, maxRunningNum = 100) {
                        if (i == 0) {
                            //发送消息到bungee,找到对应服务器加载世界
                            RedisManager.push("loadWorldServer|,|$uuid|,|$serverName")
                        }
                        i++
                        if (!loadWorldList.contains(uuid)) {
                            future.complete(true)
                            this.cancel()
                            return@submit
                        }
                        if (i >= 50) {
                            future.complete(false)
                            this.cancel()
                            return@submit
                        }
                    }
                    return future
                }
            }
            future.complete(false)
            return future
        }
    }

    fun getLoadWorldList(): MutableList<UUID> {
        return loadWorldList
    }

    fun removeLoadWorldList(uuid: UUID) {
        loadWorldList.remove(uuid)
    }

    fun loadWorldLocal(uuid: UUID): Boolean {
        if(Thread.currentThread().name != "Server thread"){
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 非Server thread发起的世界加载！这会导致服务器崩溃！")
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 当前使用的线程：${Thread.currentThread().name}")
            return false
        }
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
        Clean.load(worldData)

        //val dimensionData = getWorldDimensionData(worldData.worldName)
        if (PixelWorldPro.instance.isBungee()) {
            return if (RedisManager.isLock(uuid)) {
                true
            } else {
                RedisManager.setLock(uuid)
                var worldcreator = WorldCreator(worldData.worldName + "/world")
                /*
                if (dimensionData.seed != "0"){
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 设置世界种子${dimensionData.seed.toLong()}")
                    worldcreator = worldcreator.seed(dimensionData.seed.toLong())
                }

                 */
                if (PixelWorldPro.instance.config.getString("WorldSetting.Creator.World") != "auto") {
                    worldcreator = worldcreator.generator(PixelWorldPro.instance.config.getString("WorldSetting.Creator.World"))
                }
                val world = Bukkit.createWorld(worldcreator)

                submit(async = true) {
                    var worlds = world
                    var data = worldData
                    while (true) {
                        sleep(5000)
                        data = PixelWorldPro.databaseApi.getWorldData(uuid)?:continue
                        worlds = Bukkit.getWorld(data.worldName + "/world")
                        if (worlds == null) {
                            data.onlinePlayerNumber = 0
                            if (PixelWorldPro.instance.isBungee()) {
                                RedisManager[uuid] = Serialize.serialize(data)
                            } else {
                                PixelWorldPro.instance.setData(uuid, Serialize.serialize(data))
                            }
                            break
                        }
                        val players = worlds.players.size
                        data.onlinePlayerNumber = players
                        if (PixelWorldPro.instance.isBungee()) {
                            RedisManager[uuid] = Serialize.serialize(data)
                        } else {
                            PixelWorldPro.instance.setData(uuid, Serialize.serialize(data))
                        }
                    }
                }

                if (world == null) {
                    RedisManager.removeLock(uuid)
                    false
                } else {
                    setDataGameRule(world)
                    setTime(world)
                    setWorldBorder(world, worldData.worldLevel)
                    world.keepSpawnInMemory = false
                    setGameRule(world)
                    loadWorldList.add(uuid)
                    PixelWorldPro.instance.setOnInviter(uuid, listOf())
                    localWorldList.add(uuid)
                    true
                }
            }
        } else {
            return if (Bukkit.getWorld(worldData.worldName) != null) {
                setTime(Bukkit.getWorld(worldData.worldName)!!)
                setWorldBorder(Bukkit.getWorld(worldData.worldName)!!, worldData.worldLevel)
                setGameRule(Bukkit.getWorld(worldData.worldName)!!)
                true
            } else {
                var worldcreator = WorldCreator(worldData.worldName+"/world")
                /*
                if (dimensionData.seed != "0"){
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 设置世界种子${dimensionData.seed.toLong()}")
                    worldcreator = worldcreator.seed(dimensionData.seed.toLong())
                    Bukkit.getConsoleSender().sendMessage(worldcreator.seed().toString())
                }

                 */
                if (PixelWorldPro.instance.config.getString("WorldSetting.Creator.World") != "auto") {
                    worldcreator = worldcreator.generator(PixelWorldPro.instance.config.getString("WorldSetting.Creator.World"))
                }
                val world = Bukkit.createWorld(worldcreator)
                if (world == null) {
                    false
                } else {
                    setDataGameRule(world)
                    Bukkit.getConsoleSender().sendMessage(world.seed.toString())
                    setTime(world)
                    setWorldBorder(world, worldData.worldLevel)
                    setGameRule(world)
                    loadWorldList.add(uuid)
                    localWorldList.add(uuid)
                    true
                }
            }
        }
    }

    override fun loadWorldGroup(uuid: UUID) {
        com.dongzh1.pixelworldpro.bungee.world.World.loadBungeeWorld(Bukkit.getPlayer(uuid)!!, uuid)
    }

    override fun loadWorldGroupTp(world: UUID, player: UUID) {
        com.dongzh1.pixelworldpro.bungee.world.World.loadBungeeWorld(Bukkit.getPlayer(player)!!, world)
    }

    fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }

    private fun setGameRule(world: World) {
        try {
            val gamerulesStringList =
                PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.GameRule")!!.getKeys(false)
            for (gameruleString in gamerulesStringList) {
                val valueBoolean = PixelWorldPro.instance.config.getBoolean("WorldSetting.GameRule.$gameruleString")
                world.setGameRuleValue(gameruleString, valueBoolean.toString())
                world.save()
            }
        }catch (e:Exception){
            Bukkit.getConsoleSender().sendMessage("设置世界规则失败，可能当前服务端版本低于1.13")
        }
    }

    private fun setTime(world: World) {
        fun setTimeLocal(world: World, timeWorldName: String) {
            val timeWorld = Bukkit.getWorld(timeWorldName)
            if (timeWorld == null) {
                Bukkit.getConsoleSender().sendMessage("§4${lang("WorldTimeWorldNotExist")}")
                return
            }
            world.time = timeWorld.time
        }

        val timeValue = PixelWorldPro.instance.config.getString("WorldSetting.WorldTime")
        if (timeValue == null || timeValue == "null") {
            return
        }
        if (timeValue.contains(":")) {
            val serverName = timeValue.split(":")[1]
            val worldName = timeValue.split(":")[0]
            if (serverName == PixelWorldPro.instance.config.getString("ServerName")) {
                setTimeLocal(world, worldName)
                return
            }
            RedisManager.push("setTime|,|$serverName|,|$worldName|,|${world.name}")
            return
        } else {
            setTimeLocal(world, PixelWorldPro.instance.config.getString("WorldSetting.WorldTime")!!)
            return
        }
    }

    fun setWorldBorder(world: World, level: String) {
        when(PixelWorldPro.instance.worldBorder.getString("enable")) {
            "McBorder" ->{
                val buildLevel = Level.buildLevel()
                val borderRange = buildLevel[level.toInt()]!!.barrier
                if (borderRange == 0) {
                    submit {
                        world.worldBorder.size = 60000000.0
                    }
                } else {
                    submit {
                        world.worldBorder.size = borderRange.toDouble()
                    }
                }
            }
            "WorldBorder" ->{
                val buildLevel = Level.buildLevel()
                val borderRange = buildLevel[level.toInt()]!!.barrier
                if (borderRange == 0) {
                    submit {
                        Config.setBorder(world.name, 60000000, 60000000, world.spawnLocation.x, world.spawnLocation.z)
                    }
                } else {
                    submit {
                        Config.setBorder(world.name, borderRange, borderRange, world.spawnLocation.x, world.spawnLocation.z)
                    }
                }
            }
            "None" ->{}
            else ->{
                try {
                    val buildLevel = Level.buildLevel()
                    val regEx = "[^0-9]"
                    val p =  Pattern.compile(regEx)
                    val m = p.matcher(level)
                    val result: String = m.replaceAll("").trim()
                    val borderRange = buildLevel[result.toInt()]!!.barrier
                    Bukkit.getConsoleSender().sendMessage(borderRange.toString())
                    if (borderRange == 0) {
                        submit {
                            world.worldBorder.size = 60000000.0
                        }
                    } else {
                        submit {
                            world.worldBorder.center = world.spawnLocation
                            world.worldBorder.size = borderRange.toDouble()
                        }
                    }
                }catch (_:NoSuchMethodError){
                    Bukkit.getConsoleSender().sendMessage("加载世界边界出错-Bukkit内核没有这个方法")
                }
            }
        }
    }

    override fun loadDimension(world: UUID, player: Player, dimension: String): Boolean {
        val dimensiondata = Dimension.getDimensionData(dimension)
        val worldData = PixelWorldPro.databaseApi.getWorldData(world)!!
        val worldname = "${worldData.worldName}/$dimension"
        if (Bukkit.getWorld(worldname) != null) {
            setTime(Bukkit.getWorld(worldname)!!)
            setWorldBorder(Bukkit.getWorld(worldname)!!, worldData.worldLevel)
            setGameRule(Bukkit.getWorld(worldname)!!)
            return true
         } else {
            if (dimensiondata != null) {
                if (dimension in getWorldDimensionData(worldData.worldName).discreatelist) {
                    player.sendMessage("维度未被购买")
                    return false
                }else{
                    val worldCreator = WorldCreator(worldname)
                    if (dimensiondata.creator != "auto") {
                        worldCreator.generator(dimensiondata.creator)
                    }
                    if (dimension == "nether"){
                        worldCreator.environment(World.Environment.NETHER)
                        worldCreator.type(WorldType.NORMAL)
                        worldCreator.generateStructures(true)
                    }
                    if (dimension == "the_end"){
                        worldCreator.environment(World.Environment.THE_END)
                        worldCreator.type(WorldType.AMPLIFIED)
                        worldCreator.generateStructures(true)
                    }
                    val worlds = Bukkit.createWorld(worldCreator)
                    return if (worlds == null) {
                        player.sendMessage("维度加载失败")
                        false
                    } else {
                        setDataGameRule(worlds)
                        setTime(worlds)
                        if (dimensiondata.barrier) {
                            setWorldBorder(worlds, worldData.worldLevel)
                        }else{
                            worlds.worldBorder.center = worlds.spawnLocation
                            worlds.worldBorder.size = 60000000.0
                        }
                        setGameRule(worlds)
                        true
                    }
                }
            } else {
                player.sendMessage("没有找到对应的维度")
                return false
            }
        }
    }

    override fun createDimension(world: UUID, player: Player, dimension: String): Boolean {
        PixelWorldPro.instance.config
        val dimensiondata = Dimension.getDimensionData(dimension)
        val worldData = PixelWorldPro.databaseApi.getWorldData(world)!!
        val worldname = "${worldData.worldName}/$dimension"
        if (Bukkit.getWorld(worldname) != null) {
            setTime(Bukkit.getWorld(worldname)!!)
            setWorldBorder(Bukkit.getWorld(worldname)!!, worldData.worldLevel)
            setGameRule(Bukkit.getWorld(worldname)!!)
            return true
        } else {
            if (dimensiondata != null) {
                if (dimension in getWorldDimensionData(worldData.worldName).createlist) {
                    player.sendMessage("你已经购买了这个维度")
                    return false
                }else{
                    when(dimensiondata.createUse) {
                        "both" -> {
                            if (Vault().has(player, dimensiondata.createMoney) &&
                                PlayerPoints().has(player, dimensiondata.createPoints)
                            ) {
                                Vault().take(player, dimensiondata.createMoney)
                                PlayerPoints().take(player, dimensiondata.createPoints)
                            } else {
                                player.sendMessage(lang("MoneyNotEnough"))
                                return false
                            }
                        }

                        "money" -> {
                            if (Vault().has(player, dimensiondata.createMoney)) {
                                Vault().take(player, dimensiondata.createMoney)
                            } else {
                                player.sendMessage(lang("MoneyNotEnough"))
                                return false
                            }
                        }

                        "points" -> {
                            if (PlayerPoints().has(player, dimensiondata.createPoints)) {
                                PlayerPoints().take(player, dimensiondata.createPoints)
                            } else {
                                player.sendMessage(lang("PointsNotEnough"))
                                return false
                            }
                        }
                    }
                    val worldCreator = WorldCreator(worldname)
                    when (dimension){
                        "nether" ->{
                            worldCreator.environment(World.Environment.NETHER)
                            worldCreator.type(WorldType.NORMAL)
                            worldCreator.generateStructures(true)
                        }
                        "the_end" ->{
                            worldCreator.environment(World.Environment.THE_END)
                            worldCreator.type(WorldType.AMPLIFIED)
                            worldCreator.generateStructures(true)
                        }
                    }
                    if (dimensiondata.creator != "auto") {
                        worldCreator.generator(dimensiondata.creator)
                    }
                    if (dimension == "nether"){
                        worldCreator.environment(World.Environment.NETHER)
                        worldCreator.type(WorldType.NORMAL)
                        worldCreator.generateStructures(true)
                    }
                    if (dimension == "the_end"){
                        worldCreator.environment(World.Environment.THE_END)
                        worldCreator.type(WorldType.NORMAL)
                        worldCreator.generateStructures(true)
                    }
                    val worlds = Bukkit.createWorld(worldCreator)
                    return if (worlds == null) {
                        player.sendMessage("维度创建失败")
                        false
                    } else {
                        setDataGameRule(worlds)
                        setTime(worlds)
                        if (PixelWorldPro.instance.config.getBoolean("WorldSetting.Dimension.${dimension}.Barrier")) {
                            setWorldBorder(worlds, worldData.worldLevel)
                        }
                        setGameRule(worlds)
                        getWorldDimensionData(worldData.worldName)
                        setWorldDimensionData(worldData.worldName, dimension, true)
                        true
                    }
                }
            } else {
                player.sendMessage("没有找到对应的维度")
                return false
            }
        }
    }

    override fun unloadDimension(world: UUID){
        val worldData = PixelWorldPro.databaseApi.getWorldData(world)!!
        val worldDimensiondata = getWorldDimensionData(worldData.worldName)
        for (dimension in worldDimensiondata.createlist) {
            if (dimension != "world") {
                val worlds = Bukkit.getWorld(worldData.worldName + "/" + dimension)
                if (worlds != null) {
                    Bukkit.getConsoleSender().sendMessage("卸载${worldData.worldName}的${dimension}维度")
                    Bukkit.unloadWorld(worlds, true)
                }
            }
        }
    }

    override fun unloadtimeoutworld() {
        val future = CompletableFuture<Boolean>()
        Thread {
            sleep((PixelWorldPro.instance.config.getLong("WorldSetting.unloadTime") * 60 * 1000))
            future.complete(true)
        }.start()
        future.thenApply {
            Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 清理闲置世界")
            for (uuid in localWorldList) {
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
                if (worldData != null) {
                    val dimensionData = getWorldDimensionData(worldData.worldName)
                    var numbers = 0
                    for (dimension in dimensionData.createlist) {
                        val world = Bukkit.getWorld("${worldData.worldName}/$dimension")
                        if (world != null) {
                            numbers = +world.players.size
                        }
                    }
                    if (numbers == 0) {
                        if (worldData.worldName in timeOutWorldList) {
                            submit {
                                timeOutWorldList.remove(worldData.worldName)
                                Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 将${worldData.worldName}卸载")
                                val dimensionList = Dimension.getDimensionList()
                                dimensionList.add("world")
                                for (dimension in dimensionList) {
                                    val worldName = worldData.worldName + "/" + dimension
                                    val world = Bukkit.getWorld(worldName.lowercase(Locale.getDefault()))
                                    if (world != null) {
                                        Bukkit.unloadWorld(world, true)
                                    }
                                }
                                RedisManager.removeLock(uuid)
                            }
                        } else {
                            timeOutWorldList.add(worldData.worldName)
                            Bukkit.getConsoleSender()
                                .sendMessage("§ePixelWorldPro 将${worldData.worldName}移入待回收列表")
                        }
                    } else {
                        if (worldData.worldName in timeOutWorldList) {
                            timeOutWorldList.remove(worldData.worldName)
                            Bukkit.getConsoleSender()
                                .sendMessage("§ePixelWorldPro 将${worldData.worldName}移除待回收列表")
                        }
                    }
                }
            }
            unloadtimeoutworld()
        }
    }

    override fun setDataGameRule(world: World) {
        try {
            val uuid = getWorldNameUUID(world.name) ?: return
            val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return
            if (worldData.gameRule.isNotEmpty()) {
                for (gameRule in worldData.gameRule.keys) {
                    val value = worldData.gameRule[gameRule]!!
                    try {
                        world.setGameRuleValue(gameRule, value)
                    } catch (e: Exception) {
                        Bukkit.getConsoleSender().sendMessage("设置${world.name}世界的自定义规则${gameRule}失败，自定义的值${value}")
                    }
                }
            }
        } catch (e: Exception) {
            Bukkit.getConsoleSender().sendMessage("设置世界规则失败，可能当前服务端版本低于1.13")
        }
    }

    /**
     * 检查世界是否已被加载，如果已被加载则返回true
     */
    private fun isLock(uuid: UUID): Boolean {
        if (PixelWorldPro.instance.isBungee()) {
            return RedisManager.isLock(uuid)
        }
        return false
    }

    fun setSeed(uuid: UUID, seed: String){
        seedMap[uuid] = seed
    }
}
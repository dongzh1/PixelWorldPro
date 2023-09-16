package com.dongzh1.pixelworldpro.impl

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.MessageApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.listener.TickListener
import com.dongzh1.pixelworldpro.listener.WorldProtect.Companion.getWorldNameUUID
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.tools.Bungee
import com.dongzh1.pixelworldpro.tools.Config.getWorldDimensionData
import com.dongzh1.pixelworldpro.tools.Config.setWorldDimensionData
import com.dongzh1.pixelworldpro.tools.Dimension
import com.dongzh1.pixelworldpro.tools.WorldFile
import com.wimbli.WorldBorder.Config
import com.xbaimiao.easylib.bridge.economy.PlayerPoints
import com.xbaimiao.easylib.bridge.economy.Vault
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture


object WorldImpl : WorldApi {
    private val unloadWorldList: MutableList<UUID> = mutableListOf()
    private val createWorldList: MutableList<UUID> = mutableListOf()
    private val loadWorldList: MutableList<UUID> = mutableListOf()
    private val timeOutWorldList: MutableList<String> = mutableListOf()
    private val seedMap = mutableMapOf<UUID, String>()
    val worldMap = mutableMapOf<UUID, WorldData>()


    /**
     * 根据模板文件夹名新建世界,如果玩家在线则传送
     */
    override fun createWorld(uuid: UUID, templateName: String): CompletableFuture<Boolean> {

        if (PixelWorldPro.instance.isBungee()) {
            createWorldList.add(uuid)
            val future = CompletableFuture<Boolean>()
            //查询是否创建成功
            var i = 0
            submit(async = true, period = 2L, maxRunningNum = 60, delay = 0L) {

                if (i == 0) {
                    RedisManager.push("createWorld|,|$uuid|,|$templateName|,|${Bukkit.getPlayer(uuid)!!.name}")
                }
                i++
                if (!createWorldList.contains(uuid)) {
                    future.complete(true)
                    this.cancel()
                    return@submit
                }
                if (i >= 50) {
                    removeCreateWorldList(uuid)
                    future.complete(false)
                    this.cancel()
                    return@submit
                }
            }
            return future
        } else {
            val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName)
            if(!file.exists()){
                Bukkit.getPlayer(uuid)!!.sendMessage("模板不存在")
            }
            return createWorld(uuid, file)
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
            Bukkit.getConsoleSender().sendMessage(checkString)
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
            //设置世界规则
            setGameRule(world)
            //设置世界难度
            world.difficulty =
                Difficulty.valueOf(PixelWorldPro.instance.config.getString("WorldSetting.WorldDifficulty")!!)
            //设置世界时间
            setTime(world)
            //设置世界边界
            setWorldBorder(world, null)
            world.keepSpawnInMemory = false
            world.save()
            //数据库操作
            PixelWorldPro.databaseApi.setWorldData(
                uuid,
                WorldData(
                    worldName = "PixelWorldPro/$worldName",
                    worldLevel = PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!
                        .getKeys(false).first(),
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
                    arrayListOf()
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
        }
        loadWorldList.add(uuid)
        return true
    }

    override fun restartWorld(uuid: UUID, templateName: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        deleteWorld(uuid).thenApply {
            if (it) {
                val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName)
                val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
                file.copyRecursively(File(worldData.worldName))
                val worldcreator = WorldCreator(worldData.worldName+"/world")
                if (PixelWorldPro.instance.config.getString("WorldSetting.Creator.World") != "null") {
                    worldcreator.generator(PixelWorldPro.instance.config.getString("WorldSetting.Creator.World"))
                }
                val world = Bukkit.createWorld(worldcreator)
                if (world == null) {
                    future.complete(false)
                } else {
                    //设置世界规则
                    setGameRule(world)
                    //设置世界难度
                    world.difficulty =
                        Difficulty.valueOf(PixelWorldPro.instance.config.getString("WorldSetting.WorldDifficulty")!!)
                    //设置世界时间
                    setTime(world)
                    //设置世界边界
                    setWorldBorder(world, worldData.worldLevel)
                    world.keepSpawnInMemory = false
                    world.save()
                    future.complete(true)
                }
            } else {
                future.complete(false)
            }
        }
        return future
    }

    fun worldPath(): String {
        return if (PixelWorldPro.instance.config.getString("os") == "windows") {
            "PixelWorldPro"
        } else {
            PixelWorldPro.instance.config.getString("WorldPath")!!
        }
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
                val world = Bukkit.getWorld(worldData.worldName+"/world")
                if (world != null) {
                    future.complete(unloadWorld(world))
                    return future
                } else {
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
            val world = Bukkit.getWorld(worldData.worldName+"/world")
            return if (world != null) {
                future.complete(unloadWorld(world))
                future
            } else {
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
        if (serverName == null || serverName == PixelWorldPro.instance.config.getString("ServerName")) {
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
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
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
                if (world == null) {
                    RedisManager.removeLock(uuid)
                    false
                } else {
                    setTime(world)
                    setWorldBorder(world, worldData.worldLevel)
                    world.keepSpawnInMemory = false
                    setGameRule(world)
                    loadWorldList.add(uuid)
                    PixelWorldPro.instance.setOnInviter(uuid, listOf())
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
                    Bukkit.getConsoleSender().sendMessage(world.seed.toString())
                    setTime(world)
                    setWorldBorder(world, worldData.worldLevel)
                    setGameRule(world)
                    loadWorldList.add(uuid)
                    true
                }
            }
        }
    }

    override fun loadWorldGroup(uuid: UUID) {
        RedisManager.push("loadWorldGroup|,|$uuid|,|${TickListener.getLowestMsptServer()}")
    }

    override fun loadWorldGroupTp(world: UUID, player: UUID) {
        RedisManager.push("loadWorldGroupTp|,|$world|,|$player|,|${TickListener.getLowestMsptServer()}")
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

    fun setWorldBorder(world: World, level: String?) {
        when(PixelWorldPro.instance.worldBorder.getString("enable")) {
            "McBorder" ->{
                var realLevel =
                    PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!.getKeys(false)
                        .first()
                if (level != null) {
                    realLevel = level
                }
                val borderRange = PixelWorldPro.instance.config.getInt("WorldSetting.WorldLevel.$realLevel")
                if (borderRange == -1) {
                    submit {
                        world.worldBorder.center = world.spawnLocation
                        world.worldBorder.size = 60000000.0
                    }
                } else {
                    submit {
                        world.worldBorder.center = world.spawnLocation
                        world.worldBorder.size = borderRange.toDouble()
                    }
                }
            }
            "WorldBorder" ->{
                var realLevel =
                    PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!.getKeys(false)
                        .first()
                if (level != null) {
                    realLevel = level
                }
                val borderRange = PixelWorldPro.instance.config.getInt("WorldSetting.WorldLevel.$realLevel")
                if (borderRange == -1) {
                    submit {
                        Config.setBorder(world.name, 60000000, 60000000, world.spawnLocation.x, world.spawnLocation.z)
                    }
                } else {
                    submit {
                        Config.setBorder(world.name, borderRange, borderRange, world.spawnLocation.x, world.spawnLocation.z)
                    }
                }
            }
            else ->{
                var realLevel =
                    PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!.getKeys(false)
                        .first()
                if (level != null) {
                    realLevel = level
                }
                val borderRange = PixelWorldPro.instance.config.getInt("WorldSetting.WorldLevel.$realLevel")
                if (borderRange == -1) {
                    submit {
                        world.worldBorder.center = world.spawnLocation
                        world.worldBorder.size = 60000000.0
                    }
                } else {
                    submit {
                        world.worldBorder.center = world.spawnLocation
                        world.worldBorder.size = borderRange.toDouble()
                    }
                }
            }
        }
    }

    override fun loadDimension(world: UUID, player: Player, dimension: String): Boolean {
        val dimensiondata = Dimension.getDimensionData(dimension)
        val worldData = PixelWorldPro.databaseApi.getWorldData(world)!!
        val worldname = "./${worldData.worldName}/$dimension"
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
                    val worldcreator = WorldCreator(worldname)
                    if (dimensiondata.creator != "auto") {
                        worldcreator.generator(dimensiondata.creator)
                    }
                    val worlds = Bukkit.createWorld(worldcreator)
                    return if (worlds == null) {
                        player.sendMessage("维度加载失败")
                        false
                    } else {
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
                                Vault().take(player, Gui.getMembersEditConfig().getDouble("Money"))
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
                    val worldcreator = WorldCreator(worldname)
                    when (dimension){
                        "nether" ->{
                            worldcreator.environment(World.Environment.NETHER)
                            worldcreator.type(WorldType.NORMAL)
                            worldcreator.generateStructures(true)
                        }
                        "the_end" ->{
                            worldcreator.environment(World.Environment.THE_END)
                            worldcreator.type(WorldType.AMPLIFIED)
                            worldcreator.generateStructures(true)
                        }
                    }
                    if (dimensiondata.creator != "auto") {
                        worldcreator.generator(dimensiondata.creator)
                    }
                    val worlds = Bukkit.createWorld(worldcreator)
                    return if (worlds == null) {
                        player.sendMessage("维度创建失败")
                        false
                    } else {
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
        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 清理闲置世界")
        Bukkit.getConsoleSender().sendMessage(loadWorldList.toString())
        for(uuid in loadWorldList){
            Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 查询世界$uuid")
            val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)
            if(worldData != null) {
                Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 监测世界$uuid")
                val dimensionData = getWorldDimensionData(worldData.worldName)
                var numbers = 0
                for(dimension in dimensionData.createlist){
                    val world = Bukkit.getWorld("${worldData.worldName}/$dimension")
                    if(world != null){
                        numbers =+ world.players.size
                        Bukkit.getConsoleSender().sendMessage(numbers.toString())
                    }
                }
                if(numbers == 0){
                    if(worldData.worldName in timeOutWorldList){
                        WorldApi.Factory.worldApi?.unloadWorld(getWorldNameUUID(worldData.worldName+"/unload")!!)
                        timeOutWorldList.remove(worldData.worldName)
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 将${worldData.worldName}卸载")
                    }else{
                        timeOutWorldList.add(worldData.worldName)
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 将${worldData.worldName}移入待回收列表")
                    }
                }else{
                    if(worldData.worldName in timeOutWorldList){
                        timeOutWorldList.remove(worldData.worldName)
                        Bukkit.getConsoleSender().sendMessage("§ePixelWorldPro 将${worldData.worldName}移除待回收列表")
                    }
                }
            }
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
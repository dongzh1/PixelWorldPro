@file:Suppress("unused", "UNCHECKED_CAST", "SameParameterValue")

package com.dongzh1.pixelworldpro.impl

import com.dongzh1.pixelworldpro.PixelWorldPro

import com.dongzh1.pixelworldpro.api.MessageApi
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.listener.TickListener
import com.dongzh1.pixelworldpro.redis.RedisManager

import com.dongzh1.pixelworldpro.tools.WorldFile
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.*
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class WorldImpl : WorldApi {
    private val unloadWorldList: MutableList<UUID> = mutableListOf()
    private val createWorldList: MutableList<UUID> = mutableListOf()
    private val loadWorldList: MutableList<UUID> = mutableListOf()

    /**
     * 根据模板文件夹名新建世界,如果玩家在线则传送
     */
    override fun createWorld(uuid: UUID, templateName: String): Boolean {
        var isSuccess = false

        if (PixelWorldPro.instance.isBungee()) {
            createWorldList.add(uuid)
            //如果是bungee模式则发送消息到bungee
            RedisManager.push("createWorld|,|$uuid|,|$templateName")
            submit(period = 2L, maxRunningNum = 100) {
                if (!createWorldList.contains(uuid)) {
                    isSuccess = true
                    this.cancel()
                    return@submit
                }
            }
            return isSuccess
        } else {
            val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName)
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
    override fun createWorld(uuid: UUID, file: File): Boolean {
        return if (PixelWorldPro.instance.isBungee()) {
            createWorld(uuid, file.name)
        } else {
            createWorldLocal(uuid, file)
        }
    }

    fun createWorldLocal(uuid: UUID, templateName: String): Boolean {
        return createWorldLocal(uuid, File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName))

    }

    private fun createWorldLocal(uuid: UUID, file: File): Boolean {
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
            Bukkit.getConsoleSender().sendMessage(checkString)
            return false
        }
        if (PixelWorldPro.instance.isBungee()) {
            RedisManager.setLock(uuid)
        }
        //复制模板文件夹到world文件夹
        file.copyRecursively(File(worldPath(), worldName))
        //加载世界

        val world = Bukkit.createWorld(WorldCreator(realWorldName))
        if (world == null) {
            return false
        } else {
            //设置世界规则
            setGamerule(world)
            //设置世界难度
            world.difficulty =
                Difficulty.valueOf(PixelWorldPro.instance.config.getString("WorldSetting.WorldDifficulty")!!)
            //设置世界时间
            setTime(world)
            //设置世界边界
            setWorldBorder(world, null)
            world.save()
            //数据库操作
            PixelWorldPro.databaseApi.setWorldData(
                uuid,
                WorldData(
                    worldName = world.name,
                    worldLevel = PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!
                        .getKeys(false).first(),
                    arrayListOf(uuid),
                    arrayListOf(),
                    "anyone",
                    createTime,
                    System.currentTimeMillis(),
                    1,
                    false,
                    isCreateEnd = false
                )
            )
            //存玩家数据
            submit(async = true) {
                var playerData = PixelWorldPro.databaseApi.getPlayerData(uuid)
                if (playerData == null) {
                    playerData = PlayerData(
                        mutableListOf(uuid),
                        memberNumber = Gui.getMembersEditConfig().getInt("DefaultMembersNumber")
                    )
                    PixelWorldPro.databaseApi.setPlayerData(uuid, playerData)
                } else {
                    playerData = playerData.copy(joinedWorld = playerData.joinedWorld + uuid)
                    PixelWorldPro.databaseApi.setPlayerData(uuid, playerData)
                }
            }
        }
        return true
    }

    override fun restartWorld(uuid: UUID,templateName: String): Boolean {
        if (deleteWorld(uuid)) {
            val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName)
            val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
            file.copyRecursively(File(worldPath(), worldData.worldName))
            val world = Bukkit.createWorld(WorldCreator(worldData.worldName))
            if (world == null) {
                return false
            } else {
                //设置世界规则
                setGamerule(world)
                //设置世界难度
                world.difficulty =
                    Difficulty.valueOf(PixelWorldPro.instance.config.getString("WorldSetting.WorldDifficulty")!!)
                //设置世界时间
                setTime(world)
                //设置世界边界
                setWorldBorder(world, worldData.worldLevel)
                world.save()
                return true
            }
        }else{
            return false
        }
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
        world.players.forEach {
            it.teleport(Bukkit.getWorlds()[0].spawnLocation)
        }
        world.save()
        return Bukkit.unloadWorld(world, true)
    }

    override fun unloadWorld(uuid: UUID): Boolean {
        var unloadResult = false
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
        if (PixelWorldPro.instance.isBungee()) {
            if (RedisManager.isLock(uuid)) {
                val world = Bukkit.getWorld(worldData.worldName)
                if (world != null) {
                    return unloadWorld(world)
                }else{
                    unloadWorldList.add(uuid)
                    RedisManager.push("unloadWorld|,|${uuid}")
                    submit(period = 2L, maxRunningNum = 100) {
                        if (!unloadWorldList.contains(uuid)) {
                            unloadResult = true
                            this.cancel()
                            return@submit
                        }
                    }
                    return unloadResult
                }
            } else {
                return true
            }
        } else {
            val world = Bukkit.getWorld(worldData.worldName)
            return if (world != null) {
                unloadWorld(world)
            }else{
                true
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
    override fun deleteWorld(uuid: UUID): Boolean {
        return if (unloadWorld(uuid)) {
            val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
            File(worldData.worldName).deleteRecursively()
        } else {
            false
        }
    }


    override fun loadWorld(uuid: UUID, serverName: String?): Boolean {
        if (serverName == null || serverName == PixelWorldPro.instance.config.getString("ServerName")) {
            return loadWorldLocal(uuid)
        } else {
            return if (PixelWorldPro.instance.isBungee()) {
                if (RedisManager.isLock(uuid)) {
                    true
                } else {
                    var isSuccess = false
                    //发送消息到bungee,找到对应服务器加载世界
                    RedisManager.push("loadWorldServer|,|$uuid|,|$serverName")
                    submit(period = 2L, maxRunningNum = 100) {
                        if (!loadWorldList.contains(uuid)) {
                            isSuccess = true
                            this.cancel()
                            return@submit
                        }
                    }
                    isSuccess
                }
            } else {
                false
            }
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
        if (PixelWorldPro.instance.isBungee()) {
            return if (RedisManager.isLock(uuid)) {
                true
            } else {
                RedisManager.setLock(uuid)
                val world = Bukkit.createWorld(WorldCreator(worldData.worldName))
                if (world == null) {
                    RedisManager.removeLock(uuid)
                    false
                } else {
                    setTime(world)
                    setWorldBorder(world, worldData.worldLevel)
                    true
                }
            }
        } else {
            return if (Bukkit.getWorld(worldData.worldName) != null) {
                setTime(Bukkit.getWorld(worldData.worldName)!!)
                setWorldBorder(Bukkit.getWorld(worldData.worldName)!!, worldData.worldLevel)
                true
            } else {
                val world = Bukkit.createWorld(WorldCreator(worldData.worldName))
                if (world == null) {
                    false
                } else {
                    setTime(world)
                    setWorldBorder(world, worldData.worldLevel)
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

    private fun lang(string: String): String {
        return PixelWorldPro.instance.lang().getStringColored(string)
    }

    private fun setGamerule(world: World) {
        val gamerulesStringList =
            PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.GameRule")!!.getKeys(false)
        for (gameruleString in gamerulesStringList) {
            val gamerule = GameRule.getByName(gameruleString)
            if (gamerule == null) {
                Bukkit.getConsoleSender().sendMessage("§4$gameruleString ${lang("NotValidGameRule")}")
                continue
            }
            if (gamerule.type == Class.forName("java.lang.Boolean")) {
                val valueBoolean = PixelWorldPro.instance.config.getBoolean("WorldSet.$gameruleString")
                world.setGameRule(gamerule as GameRule<Boolean>, valueBoolean)
            }
            if (gamerule.type == Class.forName("java.lang.Integer")) {
                val valueInt = PixelWorldPro.instance.config.getInt("WorldSet.$gameruleString")
                world.setGameRule(gamerule as GameRule<Int>, valueInt)
            }
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
        var realLevel =
            PixelWorldPro.instance.config.getConfigurationSection("WorldSetting.WorldLevel")!!.getKeys(false).first()
        if (level != null) {
            realLevel = level
        }
        val borderRange = PixelWorldPro.instance.config.getInt("WorldSetting.WorldLevel.$realLevel")
        if (borderRange == -1) {
            return
        }
        world.worldBorder.center = world.spawnLocation
        world.worldBorder.size = borderRange.toDouble()
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
}
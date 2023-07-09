@file:Suppress("unused", "UNCHECKED_CAST", "SameParameterValue")

package com.dongzh1.pixelworldpro.impl

import com.dongzh1.pixelworldpro.PixelWorldPro

import com.dongzh1.pixelworldpro.api.MessageApi
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.redis.RedisManager

import com.dongzh1.pixelworldpro.tools.WorldFile
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class WorldImpl : WorldApi {
    /**
     * 根据模板文件夹名新建世界,如果玩家在线则传送
     */
    override fun createWorld(uuid: UUID, templateName: String) {

        if (PixelWorldPro.instance.isBungee()) {
            //判断玩家是否已有世界
            if (RedisManager[uuid] != null) {
                MessageApi.Factory.messageApi!!.sendMessage(uuid, lang("WorldExist"))
                return
            }
            //如果是bungee模式则发送消息到bungee
            RedisManager.push("createWorld|,|$uuid|,|$templateName")
            return
        }
        //判断玩家是否已有世界
        if (PixelWorldPro.instance.getData(uuid) != null) {
            MessageApi.Factory.messageApi!!.sendMessage(uuid, lang("WorldExist"))
            return
        }
        val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName)
        createWorld(uuid, file)
    }

    /**
     * 根据模板文件新建世界,如果玩家在线则传送
     */
    override fun createWorld(uuid: UUID, file: File) {
        if (PixelWorldPro.instance.isBungee()) {
            //判断玩家是否已有世界
            if (RedisManager[uuid] != null) {
                MessageApi.Factory.messageApi!!.sendMessage(uuid, lang("WorldExist"))
                return
            }
            //如果是bungee模式则发送消息到bungee
            createWorld(uuid, file.name)
            return
        }
        //判断玩家是否已有世界
        if (PixelWorldPro.instance.getData(uuid) != null) {
            MessageApi.Factory.messageApi!!.sendMessage(uuid, lang("WorldExist"))
            return
        }
        createWorldLocal(uuid, file)

    }

    fun createWorldLocal(uuid: UUID, templateName: String) {
        if (PixelWorldPro.databaseApi.getWorldData(uuid) != null) {
            MessageApi.Factory.messageApi!!.sendMessage(uuid, lang("WorldExist"))
            return
        }
        createWorldLocal(uuid, File(PixelWorldPro.instance.config.getString("WorldTemplatePath"), templateName))
    }

    private fun createWorldLocal(uuid: UUID, file: File) {
        val timeNow = LocalDateTime.now()
        val createTime = timeNow.format(
            DateTimeFormatter.ofPattern(
                PixelWorldPro.instance.config.getString("Papi.createTime")
            )
        )
        //获取路径下对应的world文件夹
        val worldName = "${uuid}_${
            timeNow.format(
                DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
            )
        }"

        val realWorldName = "${worldPath()}/$worldName"
        submit(async = true) {

            //检查文件是否为世界文件
            val checkString = WorldFile.isBreak(file)
            if (checkString != "ok") {
                Bukkit.getConsoleSender().sendMessage(checkString)
                return@submit
            }

            //复制模板文件夹到world文件夹
            file.copyRecursively(File(worldPath(), worldName))
            //加载世界
            submit {

                val world = Bukkit.createWorld(WorldCreator(realWorldName))
                if (world == null) {
                    MessageApi.Factory.messageApi!!.sendMessage(uuid, lang("WorldCreateFail"))
                } else {
                    MessageApi.Factory.messageApi!!.sendMessage(uuid, lang("WorldCreateSuccess"))
                    //设置世界规则
                    setGamerule(world)
                    //设置世界难度
                    world.difficulty =
                        Difficulty.valueOf(PixelWorldPro.instance.config.getString("WorldSetting.WorldDifficulty")!!)
                    //设置世界时间
                    setTime(world)
                    //设置世界边界
                    setWorldBorder(world, null)
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
                        if (playerData == null){
                            playerData = PlayerData(mutableListOf(uuid),
                                memberNumber = Gui.getMembersEditConfig().getInt("DefaultMembersNumber"))
                            PixelWorldPro.databaseApi.setPlayerData(uuid,playerData)
                        }else{
                            playerData = playerData.copy(joinedWorld = playerData.joinedWorld + uuid)
                            PixelWorldPro.databaseApi.setPlayerData(uuid,playerData)
                        }
                    }
                    if (PixelWorldPro.instance.isBungee()) {
                        RedisManager.setLock(uuid)
                    }
                    //传送玩家
                    TeleportApi.Factory.teleportApi!!.teleport(uuid, world.spawnLocation)
                }
            }


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
    override fun unloadWorld(world:World): Boolean {
        //清空世界玩家
        world.players.forEach {
            it.teleport(Bukkit.getWorlds()[0].spawnLocation)
        }
        return Bukkit.unloadWorld(world,true)
    }


    /**
     * 删除指定玩家世界
     */
    override fun deleteWorld(uuid: UUID): Boolean {
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return false
        var isLock = false
        if (PixelWorldPro.instance.isBungee()) {
            isLock = RedisManager.isLock(uuid)
        }else{
            val world = Bukkit.getWorld(worldData.worldName)
            if (world != null) {
                isLock = true
            }
        }
        return if (isLock) {
            false
        }else{
            PixelWorldPro.databaseApi.removeWorldData(uuid)
            val file = File(worldData.worldName)
            file.deleteRecursively()
        }
    }

    /**
     * 重置指定玩家世界
     */
    override fun resetWorld(uuid: UUID) {
        TODO("Not yet implemented")
    }

    override fun loadWorld(uuid: UUID,serverName:String?): Boolean {
        if (serverName == null || serverName == PixelWorldPro.instance.config.getString("ServerName")) {
            return loadWorldLocal(uuid)
        }else{
            PixelWorldPro.databaseApi.getWorldData(uuid) ?: return false
            return if (PixelWorldPro.instance.isBungee()) {
                if (RedisManager.isLock(uuid)) {
                    false
                }else{
                    //发送消息到bungee,找到对应服务器加载世界
                    RedisManager.push("loadWorldServer|,|$uuid|,|$serverName")
                    true
                }
            }else{
                false
            }
        }
    }
    fun loadWorldLocal(uuid: UUID):Boolean{
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return false
        if (PixelWorldPro.instance.isBungee()) {
            return if (RedisManager.isLock(uuid)) {
                false
            }else{
                RedisManager.setLock(uuid)
                val world = Bukkit.createWorld(WorldCreator(worldData.worldName))
                if (world == null) {
                    RedisManager.removeLock(uuid)
                    false
                }else{
                    true
                }
            }
        }else{
            return if (Bukkit.getWorld(worldData.worldName) != null) {
                false
            }else{
                Bukkit.createWorld(WorldCreator(worldData.worldName)) != null
            }
        }
    }

    override fun loadWorldGroup(uuid: UUID) {
        RedisManager.push("loadWorldGroup|,|$uuid")
    }

    override fun loadWorldGroupTp(world: UUID, player: UUID) {
        RedisManager.push("loadWorldGroupTp|,|$world|,|$player")
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

    private fun setWorldBorder(world: World, level: String?) {
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
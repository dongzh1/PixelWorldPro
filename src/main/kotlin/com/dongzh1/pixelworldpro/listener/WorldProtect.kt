package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.RedStone
import com.dongzh1.pixelworldpro.impl.WorldImpl
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class WorldProtect : Listener {
    private val redStone = HashMap<String, RedStone>()
    @EventHandler
    //阻止玩家被实体锁定目标
    fun target(e: EntityTargetLivingEntityEvent) {
        //获取事件发生的世界名
        val worldName = e.entity.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName, UUID.fromString("00000000-0000-0000-0000-000000000000")))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
        //如果目标是玩家且不是成员，则取消事件
        if (e.target is Player) {
            val target = e.target as Player
            if (worldData.members.contains(target.uniqueId))
                return
            if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null){
                PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
            }else{
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
    //阻止访客玩家被实体伤害以及玩家伤害实体
    fun damage(e: EntityDamageByEntityEvent) {
        val worldName = e.entity.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName, UUID.fromString("00000000-0000-0000-0000-000000000000")))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
        //如果攻击者是玩家且不是成员，则取消事件
        if (e.damager is Player) {
            val damager = e.damager as Player
            if (damager.isOp)
                return
            if (worldData.members.contains(damager.uniqueId))
                return
            if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null){
                PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
            }else{
                if (e.damager.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
                    when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                        "member" -> {
                            return
                        }
                    }
                }
            }
            e.isCancelled = true
        }
        //如果被攻击者是玩家且不是成员，则取消事件
        if (e.entity is Player) {
            val entity = e.entity as Player
            if (worldData.members.contains(entity.uniqueId))
                return
            if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null){
                PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
            }else{
                if (e.entity.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
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
    fun rightClickBlock(e: PlayerInteractEvent) {
        if (e.player.isOp)
            return
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if(isPlayerWorld(worldName, e.player.uniqueId))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(e.player.uniqueId))
            return
        if (PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!) == null){
            PixelWorldPro.instance.setOnInviter(getWorldNameUUID(worldName)!!, listOf())
        }else{
            if (e.player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
                when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                    "member" -> {
                        return
                    }
                }
            }
        }
        e.isCancelled = true
    }

    @EventHandler
    fun rightClickEntity(e: PlayerInteractEntityEvent) {
        if (e.player.isOp)
            return
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if(isPlayerWorld(worldName, e.player.uniqueId))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
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
        e.isCancelled = true
    }

    @EventHandler
    fun worldChange(e: PlayerChangedWorldEvent) {
        if (e.player.isOp) {
            return
        }
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if (!isPlayerWorld(worldName, e.player.uniqueId)) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.owner")) {
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

        }
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return
        if (e.player.uniqueId in worldData.members) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.member")) {
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
        //如果玩家是黑名单,则传送回原来的世界
        if (worldData.banPlayers.contains(e.player.uniqueId)) {
            return
        }
        if (e.player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                "member" ->{
                    when (PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.member")) {
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
                else ->{
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
            }
        }
    }

    @EventHandler
    fun teleport(e: PlayerTeleportEvent) {
        val worldConfig = PixelWorldPro.instance.world
        val world = e.player.world
        val gameMode = worldConfig.getString("worldData.${world.name}.gameMode")
        when(val permission = worldConfig.getString("worldData.${world.name}.enableGameMode")){
            "off" ->{}
            "op" ->{}
            null ->{}
            else ->{
                if(e.player.hasPermission(permission)){
                    when(gameMode){
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
                }else{
                    e.isCancelled = true
                }
            }
        }
        if (e.player.isOp)
            return
        val worldName = e.to.world.name
        //如果不是玩家世界则返回
        if(isPlayerWorld(worldName, e.player.uniqueId))
            return
        //如果是同一世界/不同维度则返回
        if (getWorldNameUUID(e.from.world.name) == getWorldNameUUID(e.to.world.name)){
            return
        }
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
        //如果玩家是黑名单，则取消

        if (worldData.banPlayers.contains(e.player.uniqueId)) {
            e.player.sendMessage("你在此世界的黑名单中")
            e.isCancelled = true
        }
        //吊销邀请状态
        if(getWorldNameUUID(e.from.world.name) != null) {
            if(PixelWorldPro.instance.getOnInviter(getWorldNameUUID(e.from.world.name)!!) != null) {
                if(e.player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(e.from.world.name)!!)!!) {
                    val onInviteList =
                        PixelWorldPro.instance.getOnInviter(getWorldNameUUID(e.from.world.name)!!)!! as ArrayList
                    onInviteList.remove(e.player.uniqueId)
                    PixelWorldPro.instance.setOnInviter(getWorldNameUUID(e.to.world.name)!!, onInviteList)
                }
            }
        }
        when (worldData.state) {
            "owner" -> {
                e.player.sendMessage("此世界无法进入")
                e.isCancelled = true
            }
            "member" ->{
                if(worldData.members.contains(e.player.uniqueId)) {
                    e.player.sendMessage("传送中")
                    return
                }
                e.player.sendMessage("此世界无法进入")
                e.isCancelled = true
            }
            "anyone" ->{
                e.player.sendMessage("传送中")
            }
            "inviter" ->{
                if(e.player.uniqueId in worldData.members){
                    e.player.sendMessage("传送中")
                    return
                }
                if(e.player.uniqueId in worldData.inviter){
                    e.player.sendMessage("传送中")
                    e.player.sendMessage("消耗来自世界主人的一张邀请函")
                    val inviterList = worldData.inviter as ArrayList
                    inviterList.remove(e.player.uniqueId)
                    worldData.inviter = inviterList
                    val uid = getWorldNameUUID(e.to.world.name)!!
                    PixelWorldPro.databaseApi.setWorldData(uid, worldData)
                    val oldOnInviteList = PixelWorldPro.instance.getOnInviter(uid)
                    if(oldOnInviteList == null){
                        val onInviteList = ArrayList<UUID>()
                        onInviteList.add(e.player.uniqueId)
                        PixelWorldPro.instance.setOnInviter(getWorldNameUUID(e.to.world.name)!!, onInviteList)
                        return
                    }else{
                        val onInviteList = oldOnInviteList as ArrayList<UUID>
                        onInviteList.add(e.player.uniqueId)
                        PixelWorldPro.instance.setOnInviter(getWorldNameUUID(e.to.world.name)!!, onInviteList)
                        return
                    }
                }
                e.player.sendMessage("此世界无法进入")
                e.isCancelled = true
            }

            else ->{
                e.player.sendMessage("此世界无法进入")
                e.isCancelled = true
            }
        }
    }
    @EventHandler
    //当世界卸载时，将redis中的数据修改
    fun worldUnload(e: WorldUnloadEvent) {
        val worldName =e.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName, UUID.fromString("00000000-0000-0000-0000-000000000000")))
            return
        if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            getWorldNameUUID(worldName)?.let { RedisManager.removeLock(it) }
        }
    }

    //监听红石信号
    @EventHandler
    fun redStone(e: BlockRedstoneEvent){
        if(PixelWorldPro.instance.advancedWorldSettings.getBoolean("redStone.LimitHighFrequency")) {
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
                if (data.frequency > PixelWorldPro.instance.advancedWorldSettings.getInt("redStone.frequency")){
                    redStone.remove(e.block.world.name)
                    e.block.type = Material.AIR
                }else{
                    redStone.remove(e.block.world.name)
                }
            }
        }
    }

    @EventHandler
    fun entitySpawn(e: EntitySpawnEvent) {
        if(getWorldNameUUID(e.entity.world.name) == null){
            return
        }
        val max = PixelWorldPro.instance.advancedWorldSettings.getInt("organism.max")
        if(max != -1){
            if(e.entity.world.entities.size >= max){
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
                    val valueBoolean = PixelWorldPro.instance.config.getBoolean("worldData.${world.name}.GameRule.$gameruleString")
                    world.setGameRule(gamerule as GameRule<Boolean>, valueBoolean)
                    world.setGameRule(gamerule, valueBoolean)
                    world.save()
                }
                if (gamerule.type == Class.forName("java.lang.Integer")) {
                    val valueInt = PixelWorldPro.instance.config.getInt("worldData.${world.name}.GameRule.$gameruleString")
                    world.setGameRule(gamerule as GameRule<Int>, valueInt)
                    world.setGameRule(gamerule, valueInt)
                    world.save()
                }
            }
        } catch (_: Exception) {
            Bukkit.getConsoleSender().sendMessage("设置世界规则失败，可能当前服务端版本低于1.13")
        }
    }

    //判断是否为玩家世界
    private fun isPlayerWorld(worldName:String,player:UUID):Boolean{
        val worldNameReal = getWorldNameUUID(worldName)
        return worldNameReal == player
    }
    private fun intTime():Int{
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
        val realName = worldName.split("/")[realNamelist - 2]
        val uuidString: String? = Regex(pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-z]{12}")
            .find(realName)?.value
        return try{
            UUID.fromString(uuidString)
        }catch (_:Exception){
            null
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
            return try{
                UUID.fromString(uuidString)
            }catch (_:Exception){
                null
            }
        }
    }
}
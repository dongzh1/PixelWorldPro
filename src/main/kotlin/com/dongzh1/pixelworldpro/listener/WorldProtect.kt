package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.impl.WorldImpl
import com.dongzh1.pixelworldpro.redis.RedisManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.util.*

class WorldProtect : Listener {
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
            e.isCancelled = true
        }
        //如果被攻击者是玩家且不是成员，则取消事件
        if (e.entity is Player) {
            val entity = e.entity as Player
            if (worldData.members.contains(entity.uniqueId))
                return
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
        e.isCancelled = true
    }

    @EventHandler
    fun worldChange(e: PlayerChangedWorldEvent) {
        WorldImpl.setGamerule(e.player.world)
        if (e.player.isOp) {
            return
        }
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName, e.player.uniqueId)) {
            when(PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.owner")){
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
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
        if (e.player.uniqueId in worldData.members){
            when(PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.member")){
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
        when(PixelWorldPro.instance.config.getString("WorldSetting.Gamemode.anyone")){
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
        WorldImpl.setGamerule(e.to.world)
        if (e.player.isOp)
            return
        val worldName = e.to.world.name
        //如果不是玩家世界则返回
        if(isPlayerWorld(worldName, e.player.uniqueId))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
        //如果玩家是黑名单，则取消
        Bukkit.getConsoleSender().sendMessage(worldData.state)

        if (worldData.banPlayers.contains(e.player.uniqueId)) {
            e.player.sendMessage("你在此世界的黑名单中")
            e.isCancelled = true
        }
        when (worldData.state) {
            "owner" -> {
                e.player.sendMessage("此世界无法进入")
                e.isCancelled = true
            }

            "member" ->{
                if(worldData.members.contains(e.player.uniqueId)) {
                    return
                }
            }
            "anyone" ->{
                e.player.sendMessage("传送中")
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

    //判断是否为玩家世界
    private fun isPlayerWorld(worldName:String,player:UUID):Boolean{
        val worldNameReal = getWorldNameUUID(worldName)
        return worldNameReal == player
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
        fun getWorldNameUUID(worldName: String): UUID {
            val realName = worldName.split("/").last()
            val uuidString: String? = Regex(pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-z]{12}")
                .find(realName)?.value
            return UUID.fromString(uuidString)
        }
    }
}
package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.*
import java.util.regex.Pattern

class WorldProtect : Listener {
    @EventHandler
    //阻止玩家被实体锁定目标
    fun target(e: EntityTargetLivingEntityEvent) {
        //获取事件发生的世界名
        val worldName = e.entity.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)!!
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
        if(!isPlayerWorld(worldName))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)!!
        //如果攻击者是玩家且不是成员，则取消事件
        if (e.damager is Player) {
            val damager = e.damager as Player
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
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)!!
        //如果玩家不是成员，则取消事件
        if (!worldData.members.contains(e.player.uniqueId))
            e.isCancelled = true
    }

    @EventHandler
    fun rightClickEntity(e: PlayerInteractEntityEvent) {
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)!!
        //如果玩家不是成员，则取消事件
        if (!worldData.members.contains(e.player.uniqueId))
            e.isCancelled = true
    }

    @EventHandler
    fun worldChange(e: PlayerChangedWorldEvent) {
        val worldName = e.player.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)!!
        //如果玩家不是成员，则传送回原世界
        if (!worldData.members.contains(e.player.uniqueId))
            e.player.teleport(e.from.spawnLocation)
    }

    @EventHandler
    fun teleport(e: PlayerTeleportEvent) {
        val worldName = e.to.world.name
        //如果不是玩家世界则返回
        if(!isPlayerWorld(worldName))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)!!
        //如果玩家是黑名单，则取消
        if (worldData.banPlayers.contains(e.player.uniqueId))
            e.isCancelled = true
    }

    //判断是否为玩家世界
    private fun isPlayerWorld(worldName:String):Boolean{
        val worldNameReal = worldName.split("/").last()
        return worldNameReal.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-z]{12}_[0-9]{4}_[0-9]{2}_[0-9]{2}_[0-9]{2}_[0-9]{2}_[0-9]{2}"))
    }
}
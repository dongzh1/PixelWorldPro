package com.dongzh1.pixelworldpro.hook

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.listener.WorldProtect.Companion.getWorldNameUUID
import dev.lone.itemsadder.api.Events.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*

class ItemsAdder : Listener {

    @EventHandler
    fun customBlockBreakEvent(e: CustomBlockBreakEvent) {
        val player = e.player
        if (!hasPermission(player)) {
            e.player.sendMessage("你没有权限与这个物品交互")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun customBlockInteractEvent(e: CustomBlockInteractEvent) {
        val player = e.player
        if (!hasPermission(player)) {
            e.player.sendMessage("你没有权限与这个物品交互")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun customBlockPlaceEvent(e: CustomBlockPlaceEvent) {
        val player = e.player
        if (!hasPermission(player)) {
            e.player.sendMessage("你没有权限与这个物品交互")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun furnitureBreakEvent(e: FurnitureBreakEvent) {
        val player = e.player
        if (!hasPermission(player)) {
            e.player.sendMessage("你没有权限与这个物品交互")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun furnitureInteractEvent(e: FurnitureInteractEvent) {
        val player = e.player
        if (!hasPermission(player)) {
            e.player.sendMessage("你没有权限与这个物品交互")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun furniturePlaceEvent(e: FurniturePlaceEvent) {
        val player = e.player
        if (!hasPermission(player)) {
            e.player.sendMessage("你没有权限与这个物品交互")
            e.isCancelled = true
        }
    }

    private fun isPlayerWorld(worldName: String, player: UUID): Boolean {
        val worldNameReal = getWorldNameUUID(worldName)
        return worldNameReal == player
    }

    private fun hasPermission(player: Player): Boolean {
        if (player.isOp)
            return true
        val worldName = player.world.name
        //如果不是玩家世界则返回
        if (isPlayerWorld(worldName, player.uniqueId))
            return true
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName) ?: return true
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(player.uniqueId))
            return true
        if (player.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                "member" -> {
                    return true
                }
            }
        }
        return false
    }
}
package com.dongzh1.pixelworldpro.listener

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.PlayerData
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.utils.submit
import net.luckperms.api.event.node.NodeAddEvent
import net.luckperms.api.event.node.NodeRemoveEvent
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.types.PermissionNode
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Permission {

    fun permissionAdd(e: NodeAddEvent) {
        Bukkit.getConsoleSender().sendMessage("permissionAdd")
        if (e.node.type != NodeType.PERMISSION) return
        if (!e.isUser) return
        val permissionNode = e.node as PermissionNode
        val config = BuiltInConfiguration("gui/MembersEdit.yml")
        val permissionList = config.getConfigurationSection("Permission")?.getKeys(false)
        if (permissionNode.permission !in permissionList!!) return
        submit(async = true) {
            Bukkit.getConsoleSender().sendMessage("permissionAdd submit")
            val player = e.target as Player
            var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
            if (playerData == null) {
                playerData = PlayerData(mutableListOf(),config.getInt("DefaultMembersNumber"))
                PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
            }
            for (permission in permissionList) {
                if (!player.hasPermission(permission) && permissionNode.permission != permission) {
                    return@submit
                }
                if(playerData!!.memberNumber < config.getInt("Permission/$permission")){
                    playerData = playerData.copy(memberNumber = config.getInt("Permission/$permission"))
                }
            }
            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
        }
    }
    fun permissionRemove(e: NodeRemoveEvent) {
        Bukkit.getConsoleSender().sendMessage("permissionRemove")
        if (e.node.type != NodeType.PERMISSION) return
        if (!e.isUser) return
        val permissionNode = e.node as PermissionNode
        val config = BuiltInConfiguration("gui/MembersEdit.yml")
        val permissionList = config.getConfigurationSection("Permission")?.getKeys(false)
        if (permissionNode.permission !in permissionList!!) return
        submit(async = true) {
            Bukkit.getConsoleSender().sendMessage("permissionRemove submit")
            val player = e.target as Player
            var playerData = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)
            if (playerData == null) {
                playerData = PlayerData(mutableListOf(),config.getInt("DefaultMembersNumber"))
                PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData)
            }
            playerData = playerData.copy(memberNumber = config.getInt("DefaultMembersNumber"))
            for (permission in permissionList) {
                if (!player.hasPermission(permission) || permissionNode.permission == permission) {
                    return@submit
                }
                if(playerData!!.memberNumber < config.getInt("Permission/$permission")){
                    playerData = playerData.copy(memberNumber = config.getInt("Permission/$permission"))
                }
            }
            PixelWorldPro.databaseApi.setPlayerData(player.uniqueId, playerData!!)
        }
    }
}
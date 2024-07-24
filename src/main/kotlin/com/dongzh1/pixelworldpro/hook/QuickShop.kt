package com.dongzh1.pixelworldpro.hook

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.listener.WorldProtect.Companion.getWorldNameUUID
import com.ghostchu.quickshop.api.QuickShopAPI
import com.ghostchu.quickshop.api.event.ShopCreateEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.*

class QuickShop : Listener {

    @EventHandler
    fun shopCreate(event: ShopCreateEvent) {
        if (!event.creator.isRealPlayer) {
            return
        }
        val creator = event.creator as Player
        if (creator.isOp) {
            return
        }
        val worldName = event.shop.location.world.name
        //如果不是玩家世界则返回
        if(isPlayerWorld(worldName, creator.uniqueId))
            return
        val worldData = PixelWorldPro.databaseApi.getWorldData(worldName)?: return
        //如果玩家不是成员，则取消事件
        if (worldData.members.contains(creator.uniqueId))
            return
        if (creator.uniqueId in PixelWorldPro.instance.getOnInviter(getWorldNameUUID(worldName)!!)!!) {
            when (PixelWorldPro.instance.config.getString("WorldSetting.Inviter.permission")) {
                "member" -> {
                    return
                }
            }
        }
        event.setCancelled(true, "你没有权限在这里创建商店")
    }

    private fun isPlayerWorld(worldName:String,player: UUID):Boolean{
        val worldNameReal = getWorldNameUUID(worldName)
        return worldNameReal == player
    }
}
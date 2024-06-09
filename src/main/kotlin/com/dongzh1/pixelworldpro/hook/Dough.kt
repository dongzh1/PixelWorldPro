package com.dongzh1.pixelworldpro.hook

import com.dongzh1.pixelworldpro.PixelWorldPro
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import java.util.*

@Suppress("unused")
object Dough {
    fun hasPermission(p: OfflinePlayer?, l: Location?): Boolean{
        if ((p == null).or(l == null)){
            return true
        }
        val world = l!!.world ?: return false
        val uuid = getUUID(world.name) ?: return true
        if (p!!.uniqueId == uuid){
            return true
        }
        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid) ?: return true
        return p.uniqueId in worldData.members
    }

    private fun getUUID(worldName: String): UUID? {
        val realNamelist = worldName.split("/").size
        if (realNamelist < 2) {
            return null
        }
        val realName = worldName.split("/")[realNamelist - 2]
        val uuidString: String? = Regex(pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-z]{12}")
            .find(realName)?.value
        return UUID.fromString(uuidString)
    }
}
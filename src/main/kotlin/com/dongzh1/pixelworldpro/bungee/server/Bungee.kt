package com.dongzh1.pixelworldpro.bungee.server

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

object Bungee {
    val bungeeConfig = BuiltInConfiguration("BungeeSet.yml")
    fun connect(player: Player, server: String) {
        val bungeeTpShow = bungeeConfig.getBoolean("bungeeTpShow")
        if (bungeeTpShow) {
            var msg = bungeeConfig.getString("bungeeTpShowStr")!!
            msg = msg.replace("{server_showName}", server)
            msg = msg.replace("{server_realName}", server)
            player.sendMessage(msg)
        }
        val byteArray = ByteArrayOutputStream()
        val out = DataOutputStream(byteArray)
        try {
            out.writeUTF("Connect")
            out.writeUTF(server)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        player.sendPluginMessage(PixelWorldPro.instance, "BungeeCord", byteArray.toByteArray())
    }

    fun getTPS(): Double {
        return try {
            Bukkit.getTPS().first()
        } catch (_: NoClassDefFoundError) {
            getTPS(100)
        }
    }

    var TICK_COUNT = 0
    var TICKS = LongArray(600)
    private fun getTPS(ticks: Int): Double {
        if (TICK_COUNT < ticks) {
            return 20.0
        }
        val target: Int = (TICK_COUNT - 1 - ticks) % TICKS.size
        val elapsed: Long = System.currentTimeMillis() - TICKS.get(target)
        return ticks / (elapsed / 1000.0)
    }
}
package com.dongzh1.pixelworldpro.migrate

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.tools.Config.getWorldDimensionData
import com.dongzh1.pixelworldpro.tools.Config.setWorldDimensionData
import com.xbaimiao.template.PixelPlayerWorld
import com.xbaimiao.template.expansion.api.event
import com.xbaimiao.template.expansion.api.world
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Migrate {
    fun ppw(player: Player) {
        Thread {
            val database = PixelPlayerWorld.database
            val databaseApi = PixelWorldPro.databaseApi
            player.sendMessage("发起ppw数据迁移")
            Bukkit.getConsoleSender().sendMessage("${player.name}发起了PPW数据迁移")
            var n = 0
            val wordlist = ArrayList<com.xbaimiao.template.database.dao.WorldData>()
            while (true) {
                val oldlist = event.INSTANCE.getWorldList(10, n, player)
                if (oldlist.isNullOrEmpty()) {
                    break
                }
                for (worlddata in oldlist) {
                    wordlist.add(worlddata)
                }
                n += 10
            }
            for (worlddata in wordlist) {
                try {
                    player.sendMessage("迁移${worlddata.user}数据库数据")
                    val players = worlddata.user
                    val mumberlist = world.INSTANCE.worldmemberlist(worlddata)
                    val mumberL = ArrayList<UUID>()
                    for (mumber in mumberlist) {
                        mumberL.add(mumber.uniqueId)
                    }
                    val mumberN = ArrayList<String>()
                    for (mumber in mumberlist) {
                        mumberN.add(mumber.name)
                    }
                    val banlist = world.INSTANCE.worldbanlist(worlddata)
                    val banL = ArrayList<UUID>()
                    for (ban in banlist) {
                        banL.add(ban.uniqueId)
                    }
                    val banN = ArrayList<String>()
                    for (ban in banlist) {
                        banN.add(ban.name)
                    }
                    val time = System.currentTimeMillis()
                    val createTime = time.toString()
                    val date = Date(time)
                    val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
                    val timeString = formatter.format(date)
                    val worldName = "${players}_${timeString}"
                    val worldData = WorldData(
                        "Pixelworldpro/$worldName",
                        worlddata.level.toString(),
                        mumberL,
                        mumberN,
                        banL,
                        banN,
                        worlddata.state,
                        createTime,
                        createTime.toLong(),
                        0,
                        database.getisCreateNether(players),
                        database.getisCreateEnd(players),
                        arrayListOf()
                    )
                    val p = ArrayList<UUID>()
                    p.add(players)
                    val playerData = PlayerData(
                        p, database.getMemberNumber(players), listOf()
                    )
                    databaseApi.setWorldData(players, worldData)
                    databaseApi.setPlayerData(players, playerData)
                    val worldname = "PixelWorldPro/${players}_${timeString}/world"
                    moveworld(worlddata.worldName, worldname, worldData, player, "world")
                    if (worldData.isCreateNether) {
                        val newnether = "PixelWorldPro/${players}_${timeString}/nether"
                        val oldnether = "${worlddata.worldName}_nether"
                        moveworld(oldnether, newnether, worldData, player, "nether")
                    }
                    if (worldData.isCreateNether) {
                        val newend = "PixelWorldPro/${players}_${timeString}/the_end"
                        val oldend = "${worlddata.worldName}_the_end"
                        moveworld(oldend, newend, worldData, player, "end")
                    }

                } catch (e: Exception) {
                    player.sendMessage("迁移${worlddata.user}数据失败")
                    Bukkit.getConsoleSender().sendMessage("迁移${worlddata.user}数据失败")
                    Bukkit.getConsoleSender().sendMessage(e.toString())
                }
            }
        }.start()
    }

    private fun moveworld(oldworldname: String, newworldname: String, worldData: WorldData, player: Player, type: String){
        player.sendMessage("迁移${oldworldname}至${newworldname}")
        Bukkit.getConsoleSender().sendMessage("迁移${oldworldname}至${newworldname}")
        val old = File("./$oldworldname")
        val new = File("./$newworldname")
        old.copyRecursively(new)
        player.sendMessage("迁移${oldworldname}至${newworldname}完成")
        Bukkit.getConsoleSender().sendMessage("迁移${oldworldname}至${newworldname}完成")
        getWorldDimensionData(worldData.worldName)
        when (type) {
            "nether" -> {
                setWorldDimensionData(worldData.worldName,"nether",true)
            }
            "end" -> {
                setWorldDimensionData(worldData.worldName,"the_end",true)
            }
        }
    }
}
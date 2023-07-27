package com.dongzh1.pixelworldpro.migrate

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.DatabaseApi
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.xbaimiao.template.PixelPlayerWorld
import com.xbaimiao.template.expansion.api.event
import com.xbaimiao.template.expansion.api.world
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object Migrate {
    fun ppw(Player: Player) {
        val database = PixelPlayerWorld.database
        val databaseApi = PixelWorldPro.databaseApi
        Player.sendMessage("发起ppw数据迁移")
        Bukkit.getConsoleSender().sendMessage("${Player.name}发起了PPW数据迁移")
        var n = 0
        while (true) {
            val wordlist = event.INSTANCE.getWorldList(10, n, Player)
            if (wordlist != null) {
                for (worlddata in wordlist) {
                    try {
                        Player.sendMessage("迁移${worlddata.user}数据库数据")
                        val player = worlddata.user
                        val mumberlist = world.INSTANCE.worldmemberlist(worlddata)
                        val mumber_l = ArrayList<UUID>()
                        for (mumber in mumberlist) {
                            mumber_l.add(mumber.uniqueId)
                        }
                        val mumber_n = ArrayList<String>()
                        for (mumber in mumberlist) {
                            mumber_n.add(mumber.name)
                        }
                        val banlist = world.INSTANCE.worldbanlist(worlddata)
                        val ban_l = ArrayList<UUID>()
                        for (ban in banlist) {
                            ban_l.add(ban.uniqueId)
                        }
                        val ban_n = ArrayList<String>()
                        for (ban in banlist) {
                            ban_n.add(ban.name)
                        }
                    val time = System.currentTimeMillis()
                    val date = Date(time)
                    val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
                    val timeString = formatter.format(date)
                    val worldName = "${player}_${timeString}"
                        val Worlddata = WorldData(
                            worldName,
                            worldLevel = worlddata.level.toString(),
                            mumber_l,
                            mumber_n,
                            ban_l,
                            ban_n,
                            worlddata.state,
                            worlddata.createTime,
                            worlddata.lastTime,
                            0,
                            database.getisCreateNether(player),
                            database.getisCreateEnd(player)
                        )
                        val p = ArrayList<UUID>()
                        p.add(player)
                        val Playerdata = PlayerData(
                            p,
                            database.getMemberNumber(player)
                        )
                        databaseApi.setWorldData(player, Worlddata)
                        databaseApi.setPlayerData(player, Playerdata)
                        val worldname = "${Worlddata.worldName}/world"
                        moveworld(worlddata.worldName,worldname,Player)
                    if (Worlddata.isCreateNether){
                        val newnether = "${player}_${timeString}/nether"
                        val oldnether = "${worlddata.worldName}_nether"
                        moveworld(oldnether,newnether,Player)
                    }
                    if (Worlddata.isCreateNether){
                        val newend = "${player}_${timeString}/the_end"
                        val oldend = "${worlddata.worldName}_the_end"
                        moveworld(newend,oldend,Player)
                    }

                    }catch (e:Exception){
                        Player.sendMessage("迁移${worlddata.user}数据失败")
                        Bukkit.getConsoleSender().sendMessage("迁移${worlddata.user}数据失败")
                        Bukkit.getConsoleSender().sendMessage(e.toString())
                    }
                }
            }else{
                break
            }
            n += 10
        }
    }

    fun moveworld(oldworldname: String, newworldname: String, Player: Player){
        Player.sendMessage("迁移${oldworldname}至${newworldname}")
        Bukkit.getConsoleSender().sendMessage("迁移${oldworldname}至${newworldname}")
        val old = File("./$oldworldname")
        val new = File("${PixelWorldPro.instance.config.getString("WorldPath")}/$newworldname")
        old.copyRecursively(new)
        var cfg = new.path
        if(cfg.endsWith("/world")){
            cfg.replace("/world","")
        }else if(cfg.endsWith("/nether")){
            cfg.replace("/nether","")
        }else if(cfg.endsWith("/the_end")){
            cfg.replace("/the_end","")
        }
        Player.sendMessage("迁移${oldworldname}至${newworldname}完成")
        Bukkit.getConsoleSender().sendMessage("迁移${oldworldname}至${newworldname}完成")
        val worldconf = File(cfg,"world.yml")
        val data = YamlConfiguration()
        data.load(worldconf)
        val Dimension = PixelWorldPro.instance.config.getList("WorldSetting.Dimension")!!
        cfg = new.path
        if(cfg.endsWith("/world")){
            data.set("nether",false)
            data.set("the_end",false)
        }else if(cfg.endsWith("/nether")){
            data.set("nether",true)
        }else if(cfg.endsWith("/the_end")){
            data.set("the_end",true)
        }

        for (dimension in Dimension) {
            try {
                data.getString(dimension.toString())
            } catch (e: Exception) {
                data.set(dimension.toString(), false)
            }
        }
    }
}
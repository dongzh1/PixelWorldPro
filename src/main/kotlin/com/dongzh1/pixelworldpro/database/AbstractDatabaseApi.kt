package com.dongzh1.pixelworldpro.database

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.DatabaseApi
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.j256.ormlite.dao.Dao
import com.xbaimiao.easylib.module.database.Ormlite
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import java.util.*


abstract class AbstractDatabaseApi(ormlite: Ormlite) : DatabaseApi {
    private val dataTable: Dao<WorldDao, Int> = ormlite.createDao(WorldDao::class.java)

    override fun importWorldData() {
        Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §e开始加载世界数据")
        //查询所有数据并传入redis
        val queryBuilder = dataTable.queryBuilder()
        val list = queryBuilder.query()
        if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            list.forEach {
                RedisManager[it.user] = it.data
            }
            Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §eRedis数据加载完成")
            Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §e共加载${list.size}个世界数据")
        }else{
            list.forEach {
                PixelWorldPro.instance.setData(it.user,it.data)
            }
            Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §e内存数据加载完成")
            Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §e共加载${list.size}个世界数据")
        }

    }

    override fun setWorldData(uuid: UUID,worldData: WorldData) {
        //写入redis或内存
        if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            RedisManager[uuid] = RedisManager.serialize(worldData)
        } else {
            PixelWorldPro.instance.setData(uuid,RedisManager.serialize(worldData))
        }
        //写入数据库
        val data = hasWorldData(uuid)
        var dao = WorldDao()
        dao.user = uuid
        dao.data = RedisManager.serialize(worldData)

        if (data == null) {
            //写入数据库
            submit(async = true) {
                dataTable.create(dao)
            }
        } else {
            //更新数据库
            submit(async = true) {
                //查询并更新
                val queryBuilder = dataTable.queryBuilder()
                dao = queryBuilder.where().eq("user", uuid).queryForFirst()
                dao.data = RedisManager.serialize(worldData)
                dataTable.update(dao)
            }
        }
    }

    private fun hasWorldData(uuid: UUID): WorldData? {
        val data = if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            RedisManager[uuid]
        } else {
            RedisManager.deserialize(PixelWorldPro.instance.getData(uuid))
        }
        return data
    }

    override fun getWorldData(uuid: UUID): WorldData? {
        //从redis或内存中获取
        val data = if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            RedisManager[uuid]
        } else {
            RedisManager.deserialize(PixelWorldPro.instance.getData(uuid))
        }
        return data
    }

    override fun connectTest() {
        //测试数据库链接是否正常
        submit(async = true) {
            dataTable.queryBuilder()
        }

    }
}
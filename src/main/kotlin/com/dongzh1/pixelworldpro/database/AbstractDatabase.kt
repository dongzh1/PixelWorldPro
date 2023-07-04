package com.dongzh1.pixelworldpro.database

import com.dongzh1.pixelworldpro.redis.RedisManager
import com.j256.ormlite.dao.Dao
import com.xbaimiao.easylib.module.database.Ormlite
import org.bukkit.Bukkit
import java.util.*

/**
 * @author 小白
 * @date 2023/5/21 09:16
 **/
abstract class AbstractDatabase(ormlite: Ormlite) : Database {
    private val dataTable: Dao<WorldDao, Int> = ormlite.createDao(WorldDao::class.java)

    override fun redisGetWorldData() {
        Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §e开始加载Redis数据")
        //查询所有数据并传入redis
        val queryBuilder = dataTable.queryBuilder()
        val list = queryBuilder.query()
        list.forEach {
            RedisManager[it.user] = it.data
        }
        Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §eRedis数据加载完成")
        Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §e共加载${list.size}个世界数据")
    }

    override fun setWorldData(uuid: UUID,worldData: WorldData) {
        val dao = hasWorldData(uuid)
        if (dao == null) {
            val worldDao = WorldDao()
            worldDao.user = uuid
            worldDao.data = RedisManager.serialize(worldData)
            dataTable.create(worldDao)
        } else {
            dao.data = RedisManager.serialize(worldData)
            dataTable.update(dao)
        }
    }

    private fun hasWorldData(uuid: UUID): WorldDao? {
        val queryBuilder = dataTable.queryBuilder()
        queryBuilder.where().eq("user", uuid)
        return queryBuilder.queryForFirst()
    }

    override fun connectTest() {
        //测试数据库链接是否正常
        dataTable.queryBuilder().query()

    }
}
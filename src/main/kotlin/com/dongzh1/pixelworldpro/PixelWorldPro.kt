package com.dongzh1.pixelworldpro

import com.dongzh1.pixelworldpro.commands.Commands
import com.dongzh1.pixelworldpro.database.Database
import com.dongzh1.pixelworldpro.redis.RedisConfig
import com.dongzh1.pixelworldpro.redis.RedisListener
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.dongzh1.pixelworldpro.world.WorldLoad
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.module.utils.submit
import com.xbaimiao.easylib.task.EasyLibTask
import com.xbaimiao.template.database.MysqlDatabase
import com.xbaimiao.template.database.SQLiteDatabase
import org.bukkit.Bukkit
import redis.clients.jedis.JedisPool

@Suppress("unused")
class PixelWorldPro : EasyPlugin() {

    companion object {
        lateinit var database: Database
        lateinit var instance: PixelWorldPro
        lateinit var jedisPool: JedisPool
        lateinit var subscribeTask: EasyLibTask
        const val channel = "PixelWorldPro"
    }

    init {
        useUIModule()
    }

    override fun enable() {

        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro加载中....祈祷成功")
        //加载默认配置文件
        saveDefaultConfig()
        //加载redis
        if (config.getBoolean("Bungee")){
            val redisConfig = RedisConfig(config)
            Bukkit.getConsoleSender().sendMessage("RedisInfo: " + redisConfig.host + ":" + redisConfig.port)
            jedisPool = if (redisConfig.password != null) {
                JedisPool(redisConfig, redisConfig.host, redisConfig.port, 1000, redisConfig.password)
            } else {
                JedisPool(redisConfig, redisConfig.host, redisConfig.port)
            }
            subscribeTask = submit(async = true) {
                jedisPool.resource.use { jedis ->
                    jedis.subscribe(RedisListener(), channel)
                }
            }
        }
        //加载数据库
        if (config.getString("Database").equals("db", true)) {
            database = SQLiteDatabase()
        }
        if (config.getString("Database").equals("mysql", true)) {
            database = MysqlDatabase()
        }

        Commands().commandRoot.register()
        WorldLoad().loadWorld()

    }

}
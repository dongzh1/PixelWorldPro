package com.dongzh1.pixelworldpro

import com.dongzh1.pixelworldpro.commands.Commands
import com.dongzh1.pixelworldpro.api.DatabaseApi
import com.dongzh1.pixelworldpro.redis.RedisConfig
import com.dongzh1.pixelworldpro.redis.RedisListener
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.utils.submit
import com.xbaimiao.easylib.task.EasyLibTask
import com.xbaimiao.template.database.MysqlDatabaseApi
import com.xbaimiao.template.database.SQLiteDatabaseApi
import org.bukkit.Bukkit
import redis.clients.jedis.JedisPool
import java.io.File
import java.util.UUID

@Suppress("unused")
class PixelWorldPro : EasyPlugin() {

    companion object {
        lateinit var databaseApi: DatabaseApi
        lateinit var instance: PixelWorldPro
        lateinit var jedisPool: JedisPool
        lateinit var subscribeTask: EasyLibTask
        const val channel = "PixelWorldPro"
    }
    init {
        useUIModule()
    }

    private var lang = BuiltInConfiguration("lang/${config.getString("lang")}.yml")

    private val dataMap = mutableMapOf<String, String>()



    override fun enable() {

        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro加载中....祈祷成功")
        instance = this
        //加载默认配置文件
        saveDefaultConfig()
        //加载语言文件
        saveLang()
        //加载redis
        if (config.getBoolean("Bungee")) {
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
            databaseApi = SQLiteDatabaseApi()
        }
        if (config.getString("Database").equals("mysql", true)) {
            databaseApi = MysqlDatabaseApi()
        }
        //提取到内存
        databaseApi.importWorldData()
        //注册指令
        Commands().commandRoot.register()

    }

    fun lang(): BuiltInConfiguration {
        return lang
    }
    private fun saveLang() {
        BuiltInConfiguration("lang/chinese.yml")
        BuiltInConfiguration("lang/english.yml")
    }
    fun reloadLang() {
        lang = BuiltInConfiguration("lang/${config.getString("lang")}.yml")
    }

    fun setData(uuid: UUID, value: String) {
        dataMap[uuid.toString()] = value
    }

    fun getData(uuid: UUID): String? {
        return dataMap[uuid.toString()]
    }

}
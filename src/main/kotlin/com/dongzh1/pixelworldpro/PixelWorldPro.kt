package com.dongzh1.pixelworldpro

import com.dongzh1.pixelworldpro.commands.Commands
import com.dongzh1.pixelworldpro.api.DatabaseApi
import com.dongzh1.pixelworldpro.database.MysqlDatabaseApi
import com.dongzh1.pixelworldpro.database.SQLiteDatabaseApi
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.listener.TickListener
import com.dongzh1.pixelworldpro.redis.RedisConfig
import com.dongzh1.pixelworldpro.redis.RedisListener
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.dongzh1.pixelworldpro.tools.CommentConfig
import com.dongzh1.pixelworldpro.tools.RegisterListener
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.utils.registerListener
import com.xbaimiao.easylib.module.utils.submit
import com.xbaimiao.easylib.task.EasyLibTask

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
    private var isBungee = false


    override fun enable() {
        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro is loading")
        instance = this
        //加载默认配置文件
        saveDefaultConfig()
        //加载语言文件
        saveLang()
        //更新配置文件
        CommentConfig.updateConfig()
        //注册全局监听
        RegisterListener.registerAll()

        if (config.getString("WorldPath") == null || config.getString("WorldPath") == "") {
            //关闭插件
            Bukkit.getConsoleSender().sendMessage("§cPlease set config.yml")
            Bukkit.getConsoleSender().sendMessage("§cPlugin has been closed")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        //加载redis
        if (config.getBoolean("Bungee")) {
            isBungee = true
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord")
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
            RedisManager.setMspt(100.0)
            registerListener(TickListener)
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
    override fun disable() {

        //关闭redis
        if (config.getBoolean("Bungee")) {
            RedisManager.closeServer()
            subscribeTask.cancel()
            jedisPool.close()
        }
    }

    fun lang(): BuiltInConfiguration {
        return lang
    }
    private fun saveLang() {
        val langs = listOf(
            "english",
            "chinese"
        )
        for (lang in langs) {
            if (!File(dataFolder, "lang/$lang.yml").exists()) {
                saveResource("lang/$lang.yml", false)
            }else{
                CommentConfig.updateLang(lang)
            }
        }
        reloadLang()
    }
    private fun reloadLang() {
        lang = BuiltInConfiguration("lang/${config.getString("lang")}.yml")
    }
    override fun reloadConfig() {
        super.reloadConfig()
        reloadLang()
        Gui.reloadConfig()
        isBungee = config.getBoolean("Bungee")
    }

    fun setData(uuid: UUID, value: String) {
        dataMap[uuid.toString()] = value
    }

    fun getData(uuid: UUID): String? {
        return dataMap[uuid.toString()]
    }
    fun removeData(uuid: UUID) {
        dataMap.remove(uuid.toString())
    }
    fun isBungee(): Boolean {
        return isBungee
    }

}
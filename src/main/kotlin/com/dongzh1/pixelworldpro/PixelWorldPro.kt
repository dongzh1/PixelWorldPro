package com.dongzh1.pixelworldpro

import com.dongzh1.pixelworldpro.commands.Commands
import com.dongzh1.pixelworldpro.api.DatabaseApi
import com.dongzh1.pixelworldpro.commands.Server
import com.dongzh1.pixelworldpro.database.MysqlDatabaseApi
import com.dongzh1.pixelworldpro.database.SQLiteDatabaseApi
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.listener.OnPlayerJoin
import com.dongzh1.pixelworldpro.listener.TickListener
import com.dongzh1.pixelworldpro.papi.Papi
import com.dongzh1.pixelworldpro.redis.RedisConfig
import com.dongzh1.pixelworldpro.redis.RedisListener
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.dongzh1.pixelworldpro.tools.CommentConfig
import com.dongzh1.pixelworldpro.tools.RegisterListener
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.command.registerCommand
import com.xbaimiao.easylib.module.utils.registerListener
import com.xbaimiao.easylib.module.utils.submit
import com.xbaimiao.easylib.module.utils.unregisterListener
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
    private var redisListener: RedisListener? = null


    override fun enable() {
        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro is loading")
        instance = this
        //加载默认配置文件
        saveDefaultConfig()
        //加载语言文件
        saveLang()
        //加载gui界面
        saveGui()
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
            redisListener = RedisListener()
            subscribeTask = submit(async = true) {
                jedisPool.resource.use { jedis ->
                    jedis.subscribe(redisListener, channel)
                }
            }
            RedisManager.setMspt(100.0)
            registerListener(TickListener)
            Server().commandRoot.register()
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
        //避免数据库未初始化玩家进入
        val initListener = OnPlayerJoin()
        registerListener(initListener)
        Papi.register()
        //等待3秒后注销事件
        submit(delay = 60) {
            unregisterListener(initListener)
        }
    }
    override fun disable() {

        //关闭redis
        if (config.getBoolean("Bungee")) {
            redisListener!!.stop()
            RedisManager.closeServer()
            jedisPool.close()
            subscribeTask.cancel()
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
    fun getDataMap(): MutableMap<String, String> {
        return dataMap
    }
    fun removeData(uuid: UUID) {
        dataMap.remove(uuid.toString())
    }
    fun isBungee(): Boolean {
        return isBungee
    }

    fun getRedisListener(): RedisListener? {
        return redisListener
    }
    private fun saveGui() {
        //遍历插件resources中gui文件夹下所有的.yml文件,并保存在生成的插件文件夹中
        if (!File(dataFolder, "gui/WorldCreate.yml").exists()) {
            saveResource("gui/WorldCreate.yml", false)
        }
        if (!File(dataFolder, "gui/WorldEdit.yml").exists()) {
            saveResource("gui/WorldEdit.yml", false)
        }
        if (!File(dataFolder, "gui/WorldList.yml").exists()) {
            saveResource("gui/WorldList.yml", false)
        }
        if (!File(dataFolder, "gui/custom/customGui.yml").exists()) {
            saveResource("gui/custom/customGui.yml", false)
        }
        if (!File(dataFolder, "gui/MembersEdit.yml").exists()) {
            saveResource("gui/MembersEdit.yml", false)
        }
        if (!File(dataFolder, "gui/BanEdit.yml").exists()) {
            saveResource("gui/BanEdit.yml", false)
        }
        if (!File(dataFolder, "gui/WorldRestart.yml").exists()) {
            saveResource("gui/WorldRestart.yml", false)
        }
    }
}
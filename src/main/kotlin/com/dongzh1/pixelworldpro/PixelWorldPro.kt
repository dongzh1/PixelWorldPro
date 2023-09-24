package com.dongzh1.pixelworldpro

import com.dongzh1.pixelworldpro.api.DatabaseApi
import com.dongzh1.pixelworldpro.bungee.redis.RedisConfig
import com.dongzh1.pixelworldpro.bungee.redis.RedisListener
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.commands.Commands
import com.dongzh1.pixelworldpro.commands.Server
import com.dongzh1.pixelworldpro.database.MysqlDatabaseApi
import com.dongzh1.pixelworldpro.database.SQLiteDatabaseApi
import com.dongzh1.pixelworldpro.expansion.Expansion
import com.dongzh1.pixelworldpro.expansion.ExpansionManager.loadExpansion
import com.dongzh1.pixelworldpro.gui.Gui
import com.dongzh1.pixelworldpro.listener.OnPlayerJoin
import com.dongzh1.pixelworldpro.listener.OnPlayerLogin
import com.dongzh1.pixelworldpro.listener.OnWorldUnload
import com.dongzh1.pixelworldpro.listener.WorldProtect
import com.dongzh1.pixelworldpro.online.V2
import com.dongzh1.pixelworldpro.papi.Papi
import com.dongzh1.pixelworldpro.tools.CommentConfig
import com.dongzh1.pixelworldpro.world.Level
import com.dongzh1.pixelworldpro.world.WorldImpl
import com.mcyzj.bstats.Metrics
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.utils.registerListener
import com.xbaimiao.easylib.module.utils.submit
import com.xbaimiao.easylib.module.utils.unregisterListener
import com.xbaimiao.easylib.task.EasyLibTask
import com.xbaimiao.ktor.KtorPluginsBukkit
import com.xbaimiao.ktor.KtorStat
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import redis.clients.jedis.JedisPool
import java.io.File
import java.util.*


@Suppress("unused")
class PixelWorldPro : EasyPlugin(),KtorStat {

    companion object {

        lateinit var databaseApi: DatabaseApi
        lateinit var expansion: Expansion
        lateinit var instance: PixelWorldPro
        lateinit var jedisPool: JedisPool
        lateinit var subscribeTask: EasyLibTask
        const val channel = "PixelWorldPro"
    }

    private var config = BuiltInConfiguration("config.yml")
    private var lang = BuiltInConfiguration("lang/${config.getString("lang")}.yml")
    private val dataMap = mutableMapOf<String, String>()
    private var isBungee = false
    private var redisListener: RedisListener? = null
    private var useLuckPerm = false
    private val onInviterMap = mutableMapOf<UUID, List<UUID>>()
    private val bungeeConfig = BuiltInConfiguration("BungeeSet.yml")

    val enableShadowLevels = false

    val dimensionconfig = BuiltInConfiguration("Dimension.yml")
    val expansionconfig = BuiltInConfiguration("Expansion.yml")
    val worldBorder = BuiltInConfiguration("WorldBorder.yml")
    val advancedWorldSettings = BuiltInConfiguration("AdvancedWorldSettings.yml")
    val world = BuiltInConfiguration("World.yml")
    private val eula = BuiltInConfiguration("eula.yml")

    private val user = "mcyzj"


    override fun enable() {
        val pluginId = 19823 // <-- Replace with the id of your plugin!

        val metrics = Metrics(this, pluginId)
        metrics.addCustomChart(Metrics.SimplePie("chart_id") { "My value" })
        //初始化统计
        KtorPluginsBukkit.init(this,this)
        //统计
        stat()
        if (eula.getBoolean("eula")) {
            //PixelWorldPro全部版本遵循《用户协议-付费插件》
            //https://wiki.mcyzj.cn/#/zh-cn/agreement?id=%e4%bb%98%e8%b4%b9%e6%8f%92%e4%bb%b6
            //购买/反编译/使用 插件即表明您认可我们的协议
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
            if (config.getString("token")?.let { V2.auth(it) } == true) {
                Bukkit.getConsoleSender().sendMessage("§a恭喜您验证成功！！PixelWorldPro插件感谢您的赞助")
                Bukkit.getConsoleSender()
                    .sendMessage("§aCongratulations on your successful verification! ! PixelWorldPro plugin thanks for your sponsorship")
                Bukkit.getConsoleSender().sendMessage("§a将您的验证码交给他人使用可能导致您的服务器被封禁")
                Bukkit.getConsoleSender().sendMessage("§a有疑问请加群咨询789731437")
                loadExpansion()
                //注册全局监听
                Bukkit.getPluginManager().registerEvents(OnPlayerJoin(), this)
                Bukkit.getPluginManager().registerEvents(OnWorldUnload(), this)
                Bukkit.getPluginManager().registerEvents(WorldProtect(), this)

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
                    RedisManager.setMspt(Bukkit.getTicksPerMonsterSpawns().toDouble())
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
                val initListener = OnPlayerLogin()
                registerListener(initListener)
                Papi.register()
                //等待3秒后注销事件
                submit(delay = 60) {
                    unregisterListener(initListener)
                }
                //从1.0.x更新
                val file = File("./PixelWorldPro_old")
                if (file.exists() && !file.isDirectory) {
                    val files = file.listFiles()!!
                    for (f in files) {
                        Bukkit.getConsoleSender().sendMessage("更新${f.name}世界格式")
                        val nfile = File(config.getString("WorldPath"), f.name + "/world")
                        f.copyRecursively(nfile)
                    }
                }
                //绑定联动
                //绑定JiangFriends联动
                if (Bukkit.getPluginManager().isPluginEnabled("jiangfriends")) {
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 检测到JiangFriends，自动挂勾")
                }
                //绑定ShadowLevels联动
                if (Level.config.getBoolean("shadowLevels.enable")) {
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 启用ShadowLevels联动，尝试挂钩")
                    if (Bukkit.getPluginManager().isPluginEnabled("ShadowLevels")) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 检测到ShadowLevels，自动挂勾")
                    }else{
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 无法检测到ShadowLevels，挂钩失败")
                    }
                }
                //启用定时卸载
                if (config.getLong("WorldSetting.unloadTime") != (-1).toLong()) {
                    WorldImpl.unloadtimeoutworld()
                }
                //加载世界
                val worldList = world.getStringList("loadWorldList")
                for (worldName in worldList) {
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 加载世界${worldName}")
                    var world = Bukkit.getWorld(worldName)
                    if (world != null) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 世界${worldName}已加载")
                        continue
                    }
                    world = Bukkit.createWorld(WorldCreator(worldName))
                    if (world != null) {
                        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 加载世界${worldName}成功")
                        continue
                    }
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 加载世界${worldName}失败")
                }
            } else {
                Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro Invalid token")
            }
        }else{
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 您需要同意eula才能使用插件")
        }
    }
    override fun disable() {
        //卸载扩展
        try {
            expansion.onDisable()
        }catch (_:Exception){

        }

        //关闭redis
        if (config.getBoolean("Bungee")) {
            try {
                redisListener!!.stop()
                RedisManager.closeServer()
                jedisPool.close()
                subscribeTask.cancel()
            }catch (_:Exception){

            }
        }
    }

    fun lang(): BuiltInConfiguration {
        return lang
    }
    private fun gettoken(): String? {
        return config.getString("token")
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

    fun setOnInviter(uuid: UUID, inviter: List<UUID>) {
        onInviterMap[uuid] = inviter
    }

    fun getOnInviter(uuid: UUID): List<UUID>? {
        return onInviterMap[uuid]
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
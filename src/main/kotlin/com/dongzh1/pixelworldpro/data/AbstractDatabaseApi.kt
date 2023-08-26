package com.dongzh1.pixelworldpro.data

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.DatabaseApi
import com.dongzh1.pixelworldpro.database.PlayerData
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.dongzh1.pixelworldpro.tools.Serialize
import com.j256.ormlite.dao.Dao
import com.xbaimiao.easylib.module.database.Ormlite
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import java.util.*


abstract class AbstractDatabaseApi(ormlite: Ormlite) : DatabaseApi {
    private val dataTable: Dao<WorldDao, Int> = ormlite.createDao(WorldDao::class.java)
    private val playerTable: Dao<PlayerDao, Int> = ormlite.createDao(PlayerDao::class.java)

    override fun getPlayerData(uuid: UUID): PlayerData? {
        val queryBuilder = playerTable.queryBuilder()
        queryBuilder.where().eq("user", uuid)
        val playerDao = queryBuilder.queryForFirst() ?: return null
        return Serialize.deserializePlayerData(playerDao.data)
    }

    override fun setPlayerData(uuid: UUID, playerData: PlayerData) {
        val queryBuilder = playerTable.queryBuilder()
        queryBuilder.where().eq("user", uuid)
        var playerDao = queryBuilder.queryForFirst()
        if (playerDao == null) {
            playerDao = PlayerDao()
            playerDao.user = uuid
            playerDao.data = Serialize.serializePlayerData(playerData)
            playerTable.create(playerDao)
        } else {
            playerDao.data = Serialize.serializePlayerData(playerData)
            playerTable.update(playerDao)
        }
    }

    override fun importWorldData() {
        Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §eLoading data...")
        if (PixelWorldPro.instance.isBungee()) {
            //redis数据是否还在
            if (RedisManager.test()){
                Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §eRedis data load completed")
                return
            }
        }
        //查询所有数据并传入redis
        val queryBuilder = dataTable.queryBuilder()
        val list = queryBuilder.query()
        if (PixelWorldPro.instance.isBungee()) {
            list.forEach {
                if (RedisManager[it.user] != null) {
                    return@forEach
                }
                RedisManager[it.user] = it.data
            }
            Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §eRedis data load completed")
            Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §eLoaded ${list.size} world data")
        }else{
            list.forEach {
                PixelWorldPro.instance.setData(it.user,it.data)
            }
            Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §edata load completed")
            Bukkit.getConsoleSender().sendMessage("§a[PixelWorldPro] §eLoaded ${list.size} world data")
        }


    }

    override fun setWorldData(uuid: UUID,worldData: WorldData) {
        //查询是否有数据
        val data = hasWorldData(uuid)
        //写入redis或内存
        if (PixelWorldPro.instance.isBungee()) {
            RedisManager[uuid] = Serialize.serialize(worldData)
        } else {
            PixelWorldPro.instance.setData(uuid,Serialize.serialize(worldData))
        }
        if (data == null){
            //写入数据库
            val dao = WorldDao()
            dao.user = uuid
            dao.data = Serialize.serialize(worldData)
            //写入数据库
            submit(async = true) {
                dataTable.create(dao)
            }
        }else {
            //更新数据库
            submit(async = true) {
                //查询并更新
                val queryBuilder = dataTable.queryBuilder()
                val dao = queryBuilder.where().eq("user", uuid).queryForFirst()!!
                dao.data = Serialize.serialize(worldData)
                dataTable.update(dao)
            }
        }
    }

    private fun hasWorldData(uuid: UUID): WorldData? {
        val data = if (PixelWorldPro.instance.isBungee()) {
            RedisManager[uuid]
        } else {
            Serialize.deserialize(PixelWorldPro.instance.getData(uuid))
        }
        return data
    }

    override fun getWorldData(uuid: UUID): WorldData? {
        //从redis或内存中获取
        val data = if (PixelWorldPro.instance.isBungee()) {
            RedisManager[uuid]
        } else {
            Serialize.deserialize(PixelWorldPro.instance.getData(uuid))
        }
        return data
    }

    override fun getWorldData(name: String): WorldData? {
        //从redis或内存中获取
        val realNamelist = name.split("/").size
        if (realNamelist < 2) {
            return null
        }
        val realName = name.split("/")[realNamelist - 2]
        val uuidString :String? = Regex(pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-z]{12}")
            .find(realName)?.value
        uuidString?.let { Bukkit.getConsoleSender().sendMessage(it) }
        val uuid = UUID.fromString(uuidString)
        val data = if (PixelWorldPro.instance.isBungee()) {
            RedisManager[uuid]
        } else {
            Serialize.deserialize(PixelWorldPro.instance.getData(uuid))
        }
        return data
    }

    override fun deleteWorldData(uuid: UUID) {
        //删除redis或内存中的数据
        if (PixelWorldPro.instance.isBungee()) {
            RedisManager.remove(uuid)
        } else {
            PixelWorldPro.instance.removeData(uuid)
        }
        //删除数据库中的数据
        submit(async = true) {
            val queryBuilder = dataTable.queryBuilder()
            val dao = queryBuilder.where().eq("user", uuid).queryForFirst()!!
            dataTable.delete(dao)
            val queryBuilder2 = playerTable.queryBuilder()
            val dao2List = queryBuilder2.where().like("data", "%$uuid%").query()
            for (dao2 in dao2List){
                dao2.data = dao2.data.replace("$uuid,","")
                playerTable.update(dao2)
            }
        }
    }
    override fun getWorldDataMap(): MutableMap<UUID,WorldData> {
        var map = mutableMapOf<UUID,WorldData>()
        if (PixelWorldPro.instance.isBungee()) {
            map = RedisManager.getWorldDataMap()
        }else{
            val dataMap = PixelWorldPro.instance.getDataMap()
            for (data in dataMap){
                val uuid = UUID.fromString(data.key)
                val worldData = Serialize.deserialize(data.value)!!
                map[uuid] = worldData
            }
            if (map.isEmpty()){
                return map
            }
            //排序
            map = map.toList().sortedBy { (_, value) -> value.onlinePlayerNumber }.toMap() as MutableMap<UUID, WorldData>
        }
        return map
    }
    override fun getWorldList(start:Int,number: Int): List<UUID> {
        var list = mutableListOf<UUID>()
        if (PixelWorldPro.instance.isBungee()) {
            list = RedisManager.getWorldList()
        }else{
            val dataMap = PixelWorldPro.instance.getDataMap()
            for (data in dataMap){
                val uuid = UUID.fromString(data.key)
                list.add(uuid)
            }
        }
        if (list.size < start){
            return listOf()
        }
        if (list.size < start + number){
            return list.subList(start,list.size)
        }
        return list.subList(start,start + number)
    }
    override fun getInstance(): DatabaseApi {
        return super.getInstance()
    }
    private fun redisToMysql(mapData: Map<UUID,String>){
        mapData.forEach {
            //先查询
            val queryBuilder = dataTable.queryBuilder()
            var dao = queryBuilder.where().eq("user", it.key).queryForFirst()
            if (dao != null){
                //跳过这个数据,继续循环
                return@forEach
            }
            dao = WorldDao()
            dao.user = it.key
            dao.data = it.value
            submit(async = true) {
                dataTable.create(dao)
            }
        }
    }
}
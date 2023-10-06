package com.dongzh1.pixelworldpro.bungee.server

import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.world.WorldImpl
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import org.bukkit.Bukkit


//重要的事情说三遍
//这里大部分内容都要求新建线程执行！！！
//这里大部分内容都要求新建线程执行！！！
//这里大部分内容都要求新建线程执行！！！
object Server {
    private val serverMap = hashMapOf<Int, HashMap<String, ServerData>>()
    private val localServer = hashMapOf<String, String>()
    private val bungeeConfig = BuiltInConfiguration("BungeeSet.yml")
    private val debug = bungeeConfig.getBoolean("debug")

    private fun getNewId():Int{
        val keyList = serverMap.keys
        if (keyList.isEmpty()){
            return 1
        }
        return if (keyList.last() >= 10000){
            1
        }else {
            keyList.last() + 1
        }
    }

    fun setServerData(id: Int, serverData: ServerData){
        val map = serverMap[id]?:return
        map[serverData.realName] = serverData
        serverMap[id] = map
    }

    fun getServerMap(id: Int):HashMap<String, ServerData>?{
        val map = serverMap[id]?:return null
        serverMap.remove(id)
        return map
    }

    fun getLocalServer():ServerData{
        if ((localServer["mode"] ?: bungeeConfig.getString("mode")) == "load"){
            if (((bungeeConfig.getInt("maxWorld")) <= WorldImpl.worldMap.size).and(bungeeConfig.getInt("maxWorld") != -1)){
                val type = localServer["type"]
                if (type == null){
                    val list = arrayListOf<String>()
                    list.add("noLoad")
                    list.add("worldMax")
                    localServer["type"] = list.joinToString("||")
                }else{
                    val list = type.split("||") as ArrayList<String>
                    if ("noLoad" !in list){
                        list.add("noLoad")
                    }
                    if ("worldMax" !in list){
                        list.add("worldMax")
                    }
                    localServer["type"] = list.joinToString("||")
                }
            }else{
                val type = localServer["type"]
                try {
                    if (type != null) {
                        val list = type.split("||") as ArrayList<String>
                        if (("noLoad" in list).and("leastTps" !in list)) {
                            list.remove("noLoad")
                        }
                        if ("worldMax" in list) {
                            list.remove("worldMax")
                        }
                        localServer["type"] = list.joinToString("||")
                    }
                }catch (_:Exception){}
            }
            if (((bungeeConfig.getDouble("leastTps")) > Bukkit.getTPS().first()).and(bungeeConfig.getInt("leastTps") != -1)){
                val type = localServer["type"]
                if (type == null){
                    val list = arrayListOf<String>()
                    list.add("noLoad")
                    list.add("leastTps")
                    localServer["type"] = list.joinToString("||")
                }else{
                    val list = type.split("||") as ArrayList<String>
                    if ("noLoad" !in list){
                        list.add("noLoad")
                    }
                    if ("leastTps" !in list){
                        list.add("leastTps")
                    }
                    localServer["type"] = list.joinToString("||")
                }
            }else{
                val type = localServer["type"]
                try {
                    if (type != null) {
                        val list = type.split("||") as ArrayList<String>
                        if (("noLoad" in list).and("worldMax" !in list)) {
                            list.remove("noLoad")
                        }
                        if ("leastTps" in list) {
                            list.remove("leastTps")
                        }
                        localServer["type"] = list.joinToString("||")
                    }
                }catch (_:Exception){}
            }
        }
        return ServerData(
            localServer["showName"]?:bungeeConfig.getString("showName")!!,
            localServer["realName"]?:bungeeConfig.getString("realName")!!,
            localServer["mode"]?:bungeeConfig.getString("mode")!!,
            Bungee.getTPS(),
            localServer["type"]
        )
    }

    fun startGetServer():Int{
        val map = hashMapOf<String, ServerData>()
        val id = getNewId()
        val serverData = getLocalServer()
        map[serverData.realName] = serverData
        serverMap[id] = map
        RedisManager.push("getServerData|,|${id}")
        return id
    }
}
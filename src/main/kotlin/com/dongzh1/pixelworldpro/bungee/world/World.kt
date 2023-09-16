package com.dongzh1.pixelworldpro.bungee.world

import com.dongzh1.pixelworldpro.bungee.server.Server
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.Thread.sleep

object World {
    private val bungeeConfig = BuiltInConfiguration("BungeeSet.yml")
    private val debug = bungeeConfig.getBoolean("debug")
    fun createBungeeWorld(player: Player, template: String, name: String){
        Thread{
            val consoleSender = Bukkit.getConsoleSender()
            if (debug){
                consoleSender.sendMessage("pwp发起Bungee服务器数据收集")
                player.sendMessage("pwp发起Bungee服务器数据收集")
            }
            val id = Server.startGetServer()
            sleep(bungeeConfig.getLong("listenWait"))
            if (debug){
                consoleSender.sendMessage("pwp等待Bungee服务器数据收集完成")
                player.sendMessage("pwp等待Bungee服务器数据收集完成")
            }
            val data = Server.getServerMap(id)
            if (data == null) {
                if (debug) {
                    consoleSender.sendMessage("pwp收集Bungee服务器数据异常：无法找到对应数据缓存表单")
                    player.sendMessage("pwp收集Bungee服务器数据异常：无法找到对应数据缓存表单")
                }
                return@Thread
            }else{
                if (debug) {
                    consoleSender.sendMessage("pwp收集Bungee服务器数据完成")
                    player.sendMessage("pwp收集Bungee服务器数据完成")
                }
            }
            if (debug) {
                consoleSender.sendMessage("pwp检索Bungee服务器数据")
                player.sendMessage("pwp检索Bungee服务器数据")
                consoleSender.sendMessage("pwp收集到${data.size}个Bungee服务器数据")
                player.sendMessage("pwp收集到${data.size}个Bungee服务器数据")
            }
            val buildList = arrayListOf<String>()
            val loadList = arrayListOf<String>()
            for (server in data.keys){
                val serverData = data[server]?:continue
                if (debug) {
                    consoleSender.sendMessage("pwp检索${serverData.showName}服务器数据")
                    player.sendMessage("pwp检索${serverData.showName}服务器数据")
                    consoleSender.sendMessage("bungee内名称：${serverData.realName}")
                    player.sendMessage("bungee内名称：${serverData.realName}")
                    consoleSender.sendMessage("服务器模式：${serverData.mode}")
                    player.sendMessage("服务器模式：${serverData.mode}")
                    consoleSender.sendMessage("特殊值：${serverData.type}")
                    player.sendMessage("特殊值：${serverData.type}")
                }
                if (serverData.type != null){
                    if (debug) {
                        consoleSender.sendMessage("pwp检索${serverData.showName}的特殊值")
                        player.sendMessage("pwp检索${serverData.showName}的特殊值")
                    }
                    val type = serverData.type.split("||")
                    if ("noLoad" in type){
                        if (debug) {
                            consoleSender.sendMessage("pwp检索${serverData.showName}的特殊值含有noLoad，跳过该服务器")
                            player.sendMessage("pwp检索${serverData.showName}的特殊值含有noLoad，跳过该服务器")
                        }
                        continue
                    }
                }
                if (serverData.mode == "build"){
                    buildList.add(serverData.realName)
                }
                if (serverData.mode == "load"){
                    loadList.add(serverData.realName)
                }
            }
            if (loadList.size == 0){
                if (debug) {
                    consoleSender.sendMessage("pwp无法检索到能加载世界的服务器，退出bungee世界创建线程")
                    player.sendMessage("pwp无法检索到能加载世界的服务器，退出bungee世界创建线程")
                }
                player.sendMessage("服务器负载超过阈值，无法找到能加载世界的服务器")
                return@Thread
            }
        }.start()
        if (debug){
            player.sendMessage("pwp启动Bungee世界创建线程")
        }
    }
}
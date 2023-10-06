package com.dongzh1.pixelworldpro.bungee.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.bungee.redis.RedisManager
import com.dongzh1.pixelworldpro.bungee.server.Server
import com.dongzh1.pixelworldpro.world.WorldImpl
import com.dongzh1.pixelworldpro.bungee.server.Bungee
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.utils.submit
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap

object World {
    val bungeeConfig = BuiltInConfiguration("BungeeSet.yml")
    private val debug = bungeeConfig.getBoolean("debug")

    val createWorldList: MutableList<UUID> = mutableListOf()
    fun createBungeeWorld(player: Player, template: String) {
        Thread {
            val consoleSender = Bukkit.getConsoleSender()
            if (debug) {
                consoleSender.sendMessage("pwp发起Bungee服务器数据收集")
                player.sendMessage("pwp发起Bungee服务器数据收集")
            }
            val id = Server.startGetServer()
            sleep(bungeeConfig.getLong("listenWait"))
            if (debug) {
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
            } else {
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
            val buildList = HashMap<String, Double>()
            val loadList = HashMap<String, Double>()
            var buildLargeTPS = "none"
            var loadLargeTPS = "none"
            for (server in data.keys) {
                val serverData = data[server] ?: continue
                if (debug) {
                    consoleSender.sendMessage("pwp检索${serverData.showName}服务器数据")
                    player.sendMessage("pwp检索${serverData.showName}服务器数据")
                    consoleSender.sendMessage("bungee内名称：${serverData.realName}")
                    player.sendMessage("bungee内名称：${serverData.realName}")
                    consoleSender.sendMessage("服务器模式：${serverData.mode}")
                    player.sendMessage("服务器模式：${serverData.mode}")
                    consoleSender.sendMessage("服务器TPS：${serverData.tps}")
                    player.sendMessage("服务器TPS：${serverData.tps}")
                    consoleSender.sendMessage("特殊值：${serverData.type}")
                    player.sendMessage("特殊值：${serverData.type}")
                }
                if (serverData.type != null) {
                    if (debug) {
                        consoleSender.sendMessage("pwp检索${serverData.showName}的特殊值")
                        player.sendMessage("pwp检索${serverData.showName}的特殊值")
                    }
                    val type = serverData.type.split("||")
                    if ("noLoad" in type) {
                        if (debug) {
                            consoleSender.sendMessage("pwp检索${serverData.showName}的特殊值含有noLoad，跳过该服务器")
                            player.sendMessage("pwp检索${serverData.showName}的特殊值含有noLoad，跳过该服务器")
                        }
                        continue
                    }
                }
                if (serverData.mode == "build") {
                    buildList[serverData.realName] = serverData.tps
                    if (buildLargeTPS != "none") {
                        if (serverData.tps > buildList[buildLargeTPS]!!) {
                            buildLargeTPS = serverData.realName
                        }
                    } else {
                        buildLargeTPS = serverData.realName
                    }
                }
                if (serverData.mode == "load") {
                    loadList[serverData.realName] = serverData.tps
                    if (loadLargeTPS != "none") {
                        if (serverData.tps > loadList[loadLargeTPS]!!) {
                            loadLargeTPS = serverData.realName
                        }
                    } else {
                        loadLargeTPS = serverData.realName
                    }
                }
            }
            if (loadList.size == 0) {
                if (debug) {
                    consoleSender.sendMessage("pwp无法检索到能加载世界的服务器，退出bungee世界创建线程")
                    player.sendMessage("pwp无法检索到能加载世界的服务器，退出bungee世界创建线程")
                }
                player.sendMessage("服务器负载超过阈值，无法找到能加载世界的服务器")
                return@Thread
            }
            val future = CompletableFuture<Boolean>()
            if (buildList.size != 0) {
                createWorldList.add(player.uniqueId)
                if (debug) {
                    consoleSender.sendMessage("pwp选中服务器${buildLargeTPS}进行创建操作")
                    player.sendMessage("pwp选中服务器${buildLargeTPS}进行创建操作")
                }
                if (buildLargeTPS == Server.getLocalServer().realName){
                    submit {
                        future.complete(WorldImpl.createWorldLocal(player.uniqueId, template, player.name))
                    }
                }else {
                    //查询是否创建成功
                    var i = 0
                    submit(async = true, period = 2L, maxRunningNum = 600, delay = 0L) {

                        if (i == 0) {
                            RedisManager.push("createWorld|,|${player.uniqueId}|,|${template}|,|${player.name}|,|${buildLargeTPS}")
                        }
                        i++
                        if (!createWorldList.contains(player.uniqueId)) {
                            future.complete(true)
                            this.cancel()
                            return@submit
                        }
                        if (i >= 500) {
                            createWorldList.remove(player.uniqueId)
                            future.complete(false)
                            this.cancel()
                            return@submit
                        }
                    }
                }
            } else {
                createWorldList.add(player.uniqueId)
                if (debug) {
                    consoleSender.sendMessage("pwp检索到没有可以使用的世界创建服务器")
                    player.sendMessage("pwp检索到没有可以使用的世界创建服务器")
                    consoleSender.sendMessage("pwp选中服务器${loadLargeTPS}进行创建操作")
                    player.sendMessage("pwp选中服务器${loadLargeTPS}进行创建操作")
                }
                if (loadLargeTPS == Server.getLocalServer().realName) {
                    submit {
                        future.complete(WorldImpl.createWorldLocal(player.uniqueId, template, player.name))
                    }
                } else {
                    //查询是否创建成功
                    var i = 0
                    submit(async = true, period = 2L, maxRunningNum = 600, delay = 0L) {
                        if (i == 0) {
                            RedisManager.push("createWorld|,|${player.uniqueId}|,|${template}|,|${player.name}|,|${loadLargeTPS}")
                        }
                        i++
                        if (!createWorldList.contains(player.uniqueId)) {
                            future.complete(true)
                            this.cancel()
                            return@submit
                        }
                        if (i >= 500) {
                            createWorldList.remove(player.uniqueId)
                            future.complete(false)
                            this.cancel()
                            return@submit
                        }
                    }
                }
            }
            future.thenApply { createSuccess ->
                submit {
                    if (createSuccess) {
                        if (debug) {
                            consoleSender.sendMessage("pwp世界创建成功")
                            player.sendMessage("pwp世界创建成功")
                        }
                        sleep(1000)
                        if (debug) {
                            consoleSender.sendMessage("pwp选中服务器${loadLargeTPS}进行加载操作")
                            player.sendMessage("pwp选中服务器${loadLargeTPS}进行加载操作")
                        }
                        val load = WorldImpl.loadWorld(player.uniqueId, loadLargeTPS)
                        load.thenApply { loadSuccess ->
                            if (loadSuccess) {
                                if (debug) {
                                    consoleSender.sendMessage("pwp世界加载成功")
                                    player.sendMessage("pwp世界加载成功")
                                }
                                player.sendMessage("世界加载成功，正在传送")
                                Bungee.connect(player, loadLargeTPS)
                                TeleportApi.Factory.teleportApi?.teleport(player.uniqueId)
                            } else {
                                if (debug) {
                                    consoleSender.sendMessage("pwp世界加载失败")
                                    player.sendMessage("pwp世界加载失败")
                                }
                                player.sendMessage("世界加载失败")
                            }
                        }
                    } else {
                        if (debug) {
                            consoleSender.sendMessage("pwp世界创建服务器超时未响应")
                            player.sendMessage("pwp世界创建服务器超时未响应")
                        }
                        player.sendMessage("世界创建失败")
                        return@submit
                    }
                }
            }
        }.start()
        if (debug) {
            player.sendMessage("pwp启动Bungee世界创建线程")
        }
    }

    fun loadBungeeWorld(player: Player, uuid: UUID) {
        Thread {
            val consoleSender = Bukkit.getConsoleSender()
            if (debug) {
                consoleSender.sendMessage("pwp发起Bungee服务器数据收集")
                player.sendMessage("pwp发起Bungee服务器数据收集")
            }
            val id = Server.startGetServer()
            sleep(bungeeConfig.getLong("listenWait"))
            if (debug) {
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
            } else {
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
            val buildList = HashMap<String, Double>()
            val loadList = HashMap<String, Double>()
            var buildLargeTPS = "none"
            var loadLargeTPS = "none"
            for (server in data.keys) {
                val serverData = data[server] ?: continue
                if (debug) {
                    consoleSender.sendMessage("pwp检索${serverData.showName}服务器数据")
                    player.sendMessage("pwp检索${serverData.showName}服务器数据")
                    consoleSender.sendMessage("bungee内名称：${serverData.realName}")
                    player.sendMessage("bungee内名称：${serverData.realName}")
                    consoleSender.sendMessage("服务器模式：${serverData.mode}")
                    player.sendMessage("服务器模式：${serverData.mode}")
                    consoleSender.sendMessage("服务器TPS：${serverData.tps}")
                    player.sendMessage("服务器TPS：${serverData.tps}")
                    consoleSender.sendMessage("特殊值：${serverData.type}")
                    player.sendMessage("特殊值：${serverData.type}")
                }
                if (serverData.type != null) {
                    if (debug) {
                        consoleSender.sendMessage("pwp检索${serverData.showName}的特殊值")
                        player.sendMessage("pwp检索${serverData.showName}的特殊值")
                    }
                    val type = serverData.type.split("||")
                    if ("noLoad" in type) {
                        if (debug) {
                            consoleSender.sendMessage("pwp检索${serverData.showName}的特殊值含有noLoad，跳过该服务器")
                            player.sendMessage("pwp检索${serverData.showName}的特殊值含有noLoad，跳过该服务器")
                        }
                        continue
                    }
                }
                if (serverData.mode == "build") {
                    buildList[serverData.realName] = serverData.tps
                    if (buildLargeTPS != "none") {
                        if (serverData.tps > buildList[buildLargeTPS]!!) {
                            buildLargeTPS = serverData.realName
                        }
                    } else {
                        buildLargeTPS = serverData.realName
                    }
                }
                if (serverData.mode == "load") {
                    loadList[serverData.realName] = serverData.tps
                    if (loadLargeTPS != "none") {
                        if (serverData.tps > loadList[loadLargeTPS]!!) {
                            loadLargeTPS = serverData.realName
                        }
                    } else {
                        loadLargeTPS = serverData.realName
                    }
                }
            }
            if (loadList.size == 0) {
                if (debug) {
                    consoleSender.sendMessage("pwp无法检索到能加载世界的服务器，退出bungee世界加载线程")
                    player.sendMessage("pwp无法检索到能加载世界的服务器，退出bungee世界加载线程")
                }
                player.sendMessage("服务器负载超过阈值，无法找到能加载世界的服务器")
                return@Thread
            }
            if (debug) {
                consoleSender.sendMessage("pwp选中服务器${loadLargeTPS}进行加载操作")
                player.sendMessage("pwp选中服务器${loadLargeTPS}进行加载操作")
            }
            submit {
                val load = WorldImpl.loadWorld(uuid, loadLargeTPS)
                load.thenApply { loadSuccess ->
                    if (loadSuccess) {
                        if (debug) {
                            consoleSender.sendMessage("pwp世界加载成功")
                            player.sendMessage("pwp世界加载成功")
                        }
                        val worldData = PixelWorldPro.databaseApi.getWorldData(uuid)!!
                        player.sendMessage("世界加载成功，正在传送")
                        RedisManager.push("BungeeWorldTp|,|${player.uniqueId}|,|${worldData.worldName}|,|${loadLargeTPS}")
                        Bungee.connect(player, loadLargeTPS)

                    } else {
                        if (debug) {
                            consoleSender.sendMessage("pwp世界加载失败")
                            player.sendMessage("pwp世界加载失败")
                        }
                        player.sendMessage("世界加载失败")
                    }
                }
            }
        }.start()
        if (debug) {
            player.sendMessage("pwp启动Bungee世界加载线程")
        }
    }
}
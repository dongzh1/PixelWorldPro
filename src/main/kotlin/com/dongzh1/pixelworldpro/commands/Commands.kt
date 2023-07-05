package com.dongzh1.pixelworldpro.commands

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.TeleportApi
import com.dongzh1.pixelworldpro.api.WorldApi
import com.xbaimiao.easylib.module.command.ArgNode
import com.xbaimiao.easylib.module.command.command
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File

class Commands {



    private val debug = command<CommandSender>("debug") {
        permission = "pixelworldpro.debug"
        description = "debug"
        exec {
            sender.sendMessage("debug")
        }
    }



    private val create = command<CommandSender>("create") {
        permission = "pixelworldpro.create"
        description = "创建世界"
        arg(templateArgNode){file ->
            arg(singleOnlinePlayer,optional = true){ player ->
                exec {
                    if (valueOf(player) == null){
                        if (sender is Player){
                            val uuid = (sender as Player).uniqueId
                            if (PixelWorldPro.databaseApi.getWorldData(uuid) != null){
                                //sender.sendMessage(lang("AlreadyHasWorld"))
                            }else{
                                val worldFile = valueOf(file)!!
                                WorldApi.Factory.worldApi!!.createWorld(uuid,worldFile)
                            }
                        }else{
                            sender.sendMessage("控制台必须指定玩家")
                        }
                        return@exec
                    }
                    val uuid = valueOf(player)!!.uniqueId
                    if (PixelWorldPro.databaseApi.getWorldData(uuid) != null){
                        //sender.sendMessage(lang("OtherAlreadyHasWorld"))
                    }else{
                        val worldFile = valueOf(file)!!
                        WorldApi.Factory.worldApi!!.createWorld(uuid,worldFile)
                    }
                }
            }
        }


    }
    private val tp = command<CommandSender>("tp") {
        permission = "pixelworldpro.tp"
        description = "传送到世界"
        exec {
            //获取世界名
            val worldName = args[0]
            if (sender is Player) {
                //传送到世界
                TeleportApi.Factory.teleportApi!!.teleport((sender as Player).uniqueId, worldName)
            } else {
                val uuid = Bukkit.getPlayer(args[1])?.uniqueId
                if (uuid != null) {
                    //传送到世界
                    TeleportApi.Factory.teleportApi!!.teleport(uuid, worldName)
                } else {
                    sender.sendMessage("玩家不在线或没找到")
                }
            }
        }
    }

    val commandRoot = command<CommandSender>("pixelworldpro") {
        permission = "pixelworldpro.use"
        description = "打开主界面"
        exec {
        }
        sub(create)
        sub(tp)
        sub(debug)
    }

    private val templateArgNode = ArgNode<File>("模板文件夹",
        exec = {
            getTemplateList(mutableListOf())
        }, parse = {
            //直接传递文件夹，绝对路径
            File(PixelWorldPro.instance.config.getString("WorldTemplatePath")!! + File.separator + it)
        }
    )

    private val singleOnlinePlayer = ArgNode<Player>("玩家名",
        exec = {
            //获取在线玩家名
            Bukkit.getOnlinePlayers().map { it.name }
        }, parse = {
            //不存在就报错没有
            Bukkit.getPlayer(it) ?: error("玩家不在线或没找到")
        }
    )

    //获取指定绝对路径下的所有文件夹并将文件夹名添加到模板列表中
    private fun getTemplateList(templateList:MutableList<String>): MutableList<String> {
        val file = java.io.File(PixelWorldPro.instance.config.getString("WorldTemplatePath")!!)
        val tempList = file.listFiles()
        if (tempList != null) {
            for (i in tempList.indices) {
                if (tempList[i].isDirectory) {
                    templateList.add(tempList[i].name)
                }
            }
        }
        return templateList
    }
/*
    fun lang(string: String): String{
        return PixelWorldPro.instance.getLang().getStringColored(string)
    }

 */
}
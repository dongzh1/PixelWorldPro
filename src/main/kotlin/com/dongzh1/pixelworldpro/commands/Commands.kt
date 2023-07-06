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


    private val templateArgNode = ArgNode<File>(lang("TemplateWorld"),
        exec = {
            getTemplateList(mutableListOf())
        }, parse = {
            //直接传递文件夹，绝对路径
            File(PixelWorldPro.instance.config.getString("WorldTemplatePath")!! + File.separator + it)
        }
    )

    private val singleOnlinePlayer = ArgNode<Player>(lang("PlayerName"),
        exec = {
            //获取在线玩家名
            Bukkit.getOnlinePlayers().map { it.name }
        }, parse = {
            //不存在就报错没有
            Bukkit.getPlayer(it) ?: error(lang("PlayerNotFound"))
        }
    )


    private val debug = command<CommandSender>("debug") {
        permission = "pixelworldpro.debug"
        exec {
            sender.sendMessage("debug")
        }
    }



    private val create = command<CommandSender>("create") {
        permission = "pixelworldpro.create"
        arg(templateArgNode){file ->
            arg(singleOnlinePlayer,optional = true){ player ->
                exec {
                    if (valueOf(player) == null){
                        if (sender is Player){
                            val uuid = (sender as Player).uniqueId
                            if (PixelWorldPro.databaseApi.getWorldData(uuid) != null){
                                sender.sendMessage(lang("AlreadyHasWorld"))
                            }else{
                                val worldFile = valueOf(file)!!
                                WorldApi.Factory.worldApi!!.createWorld(uuid,worldFile)
                            }
                        }else{
                            sender.sendMessage(lang("NeedArg"))
                        }
                        return@exec
                    }
                    val uuid = valueOf(player)!!.uniqueId
                    if (PixelWorldPro.databaseApi.getWorldData(uuid) != null){
                        sender.sendMessage(lang("OtherAlreadyHasWorld"))
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
        exec {
        }
    }

    val commandRoot = command<CommandSender>("pixelworldpro") {
        permission = "pixelworldpro.use"
        exec {
        }
        sub(create)
        sub(tp)
        sub(debug)
    }

    //获取指定绝对路径下的所有文件夹并将文件夹名添加到模板列表中
    private fun getTemplateList(templateList:MutableList<String>): MutableList<String> {
        val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath")!!)
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
   private fun lang(string: String): String{
        return PixelWorldPro.instance.lang().getStringColored(string)
    }


}
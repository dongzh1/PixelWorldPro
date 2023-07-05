package com.dongzh1.pixelworldpro.impl

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.api.WorldApi
import com.dongzh1.pixelworldpro.redis.RedisManager
import com.xbaimiao.easylib.module.utils.submit
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class WorldImpl: WorldApi {
    /**
     * 根据模板文件夹名新建世界,如果玩家在线则传送
     */
    override fun createWorld(uuid: UUID, templateName: String) {
        val file = File(PixelWorldPro.instance.config.getString("WorldTemplatePath"),templateName)
        createWorld(uuid,file)
    }

    /**
     * 根据模板文件新建世界,如果玩家在线则传送
     */
    override fun createWorld(uuid: UUID, file: File) {
        if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            //如果是bungee模式则发送消息到bungee
            RedisManager.push("createWorld|,|$uuid|,|$file")
            return
        }
/*
        submit(async = true) {
            //复制世界
            copyWorld(file,uuid)
            //加载世界

        }

 */

    }

    /**
     * 卸载指定玩家世界
     */
    override fun unloadWorld(uuid: UUID) {
        TODO("Not yet implemented")
    }

    /**
     * 删除指定玩家世界
     */
    override fun deleteWorld(uuid: UUID) {
        TODO("Not yet implemented")
    }

    /**
     * 重置指定玩家世界
     */
    override fun resetWorld(uuid: UUID) {
        TODO("Not yet implemented")
    }

    private fun copyWorld(file: File,uuid: UUID) {


        if (RedisManager[uuid] == null) {
            //获取路径下对应的world文件夹
            val worldPath = PixelWorldPro.instance.config.getString("WorldPath")
            val worldName = "${uuid}_${
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
                )
            }"
            //复制模板文件夹到world文件夹
            file.copyRecursively(File(worldPath!!, worldName))

        }
    }

}
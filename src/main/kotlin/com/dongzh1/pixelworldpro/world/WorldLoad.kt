package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.sun.org.apache.xalan.internal.xsltc.compiler.Template
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class WorldLoad {
    fun loadWorld() {
        if (PixelWorldPro.instance.config.getBoolean("Bungee")) {
            //查询最合适创建的服务器来加载世界
        } else {
            loadLocalWorld(template = "default",uuid = UUID.randomUUID())
        }
    }

    private fun loadLocalWorld(template: String,uuid: UUID) {
        //在本服加载世界
        val worldTemplatePath = PixelWorldPro.instance.config.getString("WorldTemplatePath")
        //获取路径下对应的template文件夹中
        val templateFile = File(worldTemplatePath,template)
        //获取路径下对应的world文件夹
        val worldPath = PixelWorldPro.instance.config.getString("WorldPath")
        val worldName = "worldPath/${uuid}_${LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))}"
        //复制模板文件夹到world文件夹
        templateFile.copyRecursively(File(worldPath,worldName))
    }
}
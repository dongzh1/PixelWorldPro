package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import java.io.File
import java.net.URL
import java.net.URLDecoder

object Install {
    val config = BuiltInConfiguration("config.yml")

    private fun getPath(): String {
        var filePath: String
        val url: URL = PixelWorldPro::class.java.protectionDomain.codeSource.location
        try {
            filePath = URLDecoder.decode(url.path, "utf-8") // 转化为utf-8编码，支持中文
        } catch (e: Exception) {
            throw e
        }
        if (filePath.endsWith(".jar")) {
            // 可执行jar包运行的结果里包含".jar"
            // 获取jar包所在目录
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1)
        }

        val file = File(filePath)
        filePath = file.absolutePath //得到windows下的正确路径
        //System.out.println("jar包所在目录：$filePath")
        return filePath
    }
    fun start(){
        val path = getPath()
        val worldPath = config.getString("WorldPath")
        if ((worldPath == null).or(worldPath == "")){
            when (config.getString("os")) {
                "windows" -> {
                    val pathList = path.split("\\") as ArrayList
                    pathList.removeLast()
                    pathList.add("PixelWorldPro")
                    config.set("WorldPath", pathList.joinToString("\\"))
                }
                "linux" -> {
                    val pathList = path.split("/") as ArrayList
                    pathList.removeLast()
                    pathList.add("PixelWorldPro")
                    config.set("WorldPath", pathList.joinToString("/"))
                }
            }
        }
        var worldTemplatePath = config.getString("WorldTemplatePath")
        if ((worldTemplatePath == null).or(worldTemplatePath == "")){
            when (config.getString("os")) {
                "windows" -> {
                    val pathList = path.split("\\") as ArrayList
                    pathList.removeLast()
                    pathList.add("PixelWorldPro_Template")
                    config.set("WorldTemplatePath", pathList.joinToString("\\"))
                    worldTemplatePath = pathList.joinToString("\\")
                }
                "linux" -> {
                    val pathList = path.split("/") as ArrayList
                    pathList.removeLast()
                    pathList.add("PixelWorldPro_Template")
                    config.set("WorldTemplatePath", pathList.joinToString("/"))
                    worldTemplatePath = pathList.joinToString("\\")
                }
                else -> {return}
            }
        }
        config.saveToFile()
        val templateFile = File(worldTemplatePath!!)
        templateFile.mkdirs()
        val exampleFile = File(templateFile, "example")
        exampleFile.mkdirs()
        File(exampleFile, "world").mkdirs()
        File(exampleFile, "nether").mkdirs()
        File(exampleFile, "the_end").mkdirs()
        val readme = File(exampleFile, "ReadMe请阅读我!!!.txt")
        readme.createNewFile()
        readme.writeText("world -> 存放主世界|put world\nnether -> 存放地狱世界|put nether world\nthe_end -> 存放末地|put the_end world")
        if (config.getBoolean("Bungee")){
            if (worldPath != null) {
                bungee(worldPath, path)
            } else {
                println("Bungee安装需要您指定config.yml中的WorldPath和WorldTemplatePath")
            }
        }
    }
    private fun bungee(worldPath: String, path: String) {
        when (config.getString("os")) {
            "windows" -> {
                val pathList = path.split("\\") as ArrayList
                pathList.removeLast()
                pathList.add("PixelWorldPro")
                val local = pathList.joinToString("\\")
                File(worldPath).mkdirs()
                val command = "cmd /c MKLINK /d /j $local $worldPath"
                println(command)
                Runtime.getRuntime().exec(command)
            }
            else -> {return}
        }
    }
}
package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import java.io.File

object WorldFile {
    //检查世界文件是否损坏
    fun isBreak(file: File): String {
        //获取file文件夹下的所有文件
        val files = file.listFiles() ?: return lang("FileNotFound")
        if (!files.contains(File(file, "world"))) {
            return "世界模板需要拥有world文件夹存放主世界"
        }
        return "ok"

       }
    private fun lang(string: String): String{
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}
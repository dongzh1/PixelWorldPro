package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import java.io.File

object WorldFile {
    //检查世界文件是否损坏
    fun isBreak(file: File): String {
        //获取file文件夹下的所有文件
        val files = file.listFiles() ?: return lang("FileNotFound")
        //是否包含session.lock和data文件夹，如果有缺失就返回确实的是哪个文件
        if (!files.contains(File(file, "session.lock"))) {
            return lang("FileNeedSession")
        }
        if (!files.contains(File(file, "data"))) {
            return lang("FileNeedData")
        }
        //如果包含uid.dat文件则返回错误
        if (files.contains(File(file, "uid.dat"))) {
            return lang("FileCanNotHasUid")
        }
        return "ok"

       }
    private fun lang(string: String): String{
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}
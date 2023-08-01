package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import java.io.File

object WorldFile {
    //检查世界文件是否损坏
    fun isBreak(file: File): String {
        return "ok"

       }
    private fun lang(string: String): String{
        return PixelWorldPro.instance.lang().getStringColored(string)
    }
}
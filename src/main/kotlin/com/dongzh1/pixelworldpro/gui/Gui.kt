package com.dongzh1.pixelworldpro.gui


import com.xbaimiao.easylib.module.chat.BuiltInConfiguration


object Gui {
    private val membersEditConfig = BuiltInConfiguration("gui/MembersEdit.yml")
    //获取配置
    fun getMembersEditConfig(): BuiltInConfiguration{
        return membersEditConfig
    }
}
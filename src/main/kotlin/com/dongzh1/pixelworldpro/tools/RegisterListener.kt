package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.listener.OnPlayerJoin
import com.dongzh1.pixelworldpro.listener.OnWorldUnload
import com.xbaimiao.easylib.module.utils.registerListener


object RegisterListener {
    fun registerAll() {
        registerListener(OnPlayerJoin())
        registerListener(OnWorldUnload())
    }
}
package com.xbaimiao.template.database

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.AbstractDatabaseApi
import com.xbaimiao.easylib.module.database.OrmliteMysql

class MysqlDatabaseApi : AbstractDatabaseApi(OrmliteMysql(
    PixelWorldPro.instance.config.getConfigurationSection("Mysql")!!,
    PixelWorldPro.instance.config.getBoolean("Mysql.Hikari")
)
)
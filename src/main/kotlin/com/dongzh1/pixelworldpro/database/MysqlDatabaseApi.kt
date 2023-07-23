package com.dongzh1.pixelworldpro.database

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.module.database.OrmliteMysql

class MysqlDatabaseApi : AbstractDatabaseApi(OrmliteMysql(
    PixelWorldPro.instance.config.getConfigurationSection("Mysql")!!,
    PixelWorldPro.instance.config.getBoolean("Mysql.HikariCP")
)
)
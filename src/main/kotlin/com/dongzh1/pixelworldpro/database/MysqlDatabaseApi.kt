package com.dongzh1.pixelworldpro.database

import com.dongzh1.pixelworldpro.PixelWorldPro

class MysqlDatabaseApi : AbstractDatabaseApi(Mysql(
    PixelWorldPro.instance.config.getConfigurationSection("Mysql")!!,
    PixelWorldPro.instance.config.getBoolean("Mysql.HikariCP")
)
)
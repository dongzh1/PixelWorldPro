package com.dongzh1.pixelworldpro.database

import com.xbaimiao.easylib.module.database.OrmliteSQLite

class SQLiteDatabaseApi : AbstractDatabaseApi(OrmliteSQLite("database.db"))
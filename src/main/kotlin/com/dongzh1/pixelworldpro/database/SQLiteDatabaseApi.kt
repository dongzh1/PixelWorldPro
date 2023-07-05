package com.xbaimiao.template.database

import com.dongzh1.pixelworldpro.database.AbstractDatabaseApi
import com.xbaimiao.easylib.module.database.OrmliteSQLite

class SQLiteDatabaseApi : AbstractDatabaseApi(OrmliteSQLite("database.db"))
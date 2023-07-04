package com.xbaimiao.template.database

import com.dongzh1.pixelworldpro.database.AbstractDatabase
import com.xbaimiao.easylib.module.database.OrmliteSQLite

class SQLiteDatabase : AbstractDatabase(OrmliteSQLite("database.db"))
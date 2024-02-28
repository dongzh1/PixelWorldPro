package com.dongzh1.pixelworldpro.database

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.*

@DatabaseTable(tableName = "WorldData")
class WorldDao {

    @DatabaseField(generatedId = true)
    var id: Int = 0
    //玩家uuid
    @DatabaseField(dataType = DataType.UUID, canBeNull = false, columnName = "user")
    lateinit var user: UUID
    //对应数据
    @DatabaseField(dataType = DataType.LONG_STRING,canBeNull = false, columnName = "data")
    lateinit var data: String
}
@DatabaseTable(tableName = "PlayerData")
class PlayerDao {

    @DatabaseField(generatedId = true)
    var id: Int = 0
    //玩家uuid
    @DatabaseField(dataType = DataType.UUID, canBeNull = false, columnName = "user")
    lateinit var user: UUID
    //对应数据
    @DatabaseField(dataType = DataType.LONG_STRING,canBeNull = false, columnName = "data")
    lateinit var data: String
}
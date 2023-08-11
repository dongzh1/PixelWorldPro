package com.dongzh1.pixelworldpro.api

import com.dongzh1.pixelworldpro.impl.WorldImpl
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * 世界操作接口
 * 所有操作都要先查询uuid对应的worldData是否存在
 * create除外
 */
interface WorldApi {
    /**
     * 根据模板文件夹名新建世界,会存储到数据库,自行判断是否存在先
     * @param uuid 玩家uuid
     * @param templateName 模板文件夹名
     */
    fun createWorld(uuid: UUID, templateName: String): CompletableFuture<Boolean>
    /**
     * 根据模板文件新建世界，会存储到数据库
     * @param uuid 玩家uuid
     * @param file 模板文件
     */
    fun createWorld(uuid: UUID, file: File): CompletableFuture<Boolean>
    /**
     * 卸载指定玩家世界,在玩家世界操作
     * @param world 世界
     */
    fun unloadWorld(world:World): Boolean
/**
     * 卸载指定玩家世界
     * @param uuid 玩家uuid
     */
    fun unloadWorld(uuid: UUID): CompletableFuture<Boolean>
    /**
     * 删除指定玩家世界和数据库记录
     * @param uuid 玩家uuid
     */
    fun deleteWorld(uuid: UUID): CompletableFuture<Boolean>
    /**
     * 重载指定玩家世界
     * @param uuid 玩家uuid
     */
    fun restartWorld(uuid: UUID,templateName: String): CompletableFuture<Boolean>
    /**
     * 加载指定玩家世界,加载到指定服务器,如果服务器为null则加载到本服
     * @param uuid 玩家uuid
     * @param serverName 服务器名
     */
    fun loadWorld(uuid: UUID,serverName:String?): CompletableFuture<Boolean>
    /**
     * 加载指定玩家世界，群组获取mspt最低服务器加载,不是群组不要使用
     * @param uuid 玩家uuid
     */
    fun loadWorldGroup(uuid: UUID)
    /**
     * 加载指定玩家世界，并将指定玩家传送到该世界
     * @param world 玩家uuid对应的世界
     * @param player 指定玩家
     */
    fun loadWorldGroupTp(world: UUID,player: UUID)
    /**
     * 加载指定世界的特殊维度
     * @param world 玩家uuid对应的世界
     * @param player 指定玩家
     */
    fun loadDimension(world: UUID,player: Player,dimension: String): Boolean
    /**
     * 创建指定世界的特殊维度
     * @param world 玩家uuid对应的世界
     * @param player 指定玩家
     */
    fun createDimension(world: UUID,player: Player,dimension: String): Boolean
    fun unloadDimension(world: UUID)
    fun unloadtimeoutworld()

    object Factory {
        private var instance: WorldApi? = null
        val worldApi: WorldApi?
            get() {
                if (instance == null) {
                    instance = WorldImpl
                }
                return instance
            }
    }
}
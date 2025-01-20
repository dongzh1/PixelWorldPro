package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

object Structure {
    private val dimension = PixelWorldPro.instance.dimensionconfig
    fun setStruction(world: World, structure: StructureCreateData) {
        for (xyz in structure.block.keys) {
            val block = world.getBlockAt(xyz.x, xyz.y, xyz.z)
            val material = Material.getMaterial(structure.block[xyz]!!.uppercase(Locale.getDefault()))
            if (material == null) {
                Bukkit.getConsoleSender()
                    .sendMessage("§aPixelWorldPro 无法从Bukkit内找到名为 ${structure.block[xyz]} 的方块")
                Bukkit.getConsoleSender()
                    .sendMessage("§aPixelWorldPro 这不是一个PixelWorldPro的BUG，如果你不知道如何解决它，您可以询问管理员或者发布悬赏贴")
                continue
            }
            block.type = material
        }
    }

    private fun buildStructureCreateData(buildX: Int, buildY: Int, buildZ: Int, name: String): StructureCreateData? {
        val config = Config.getStructureConfig(name)
        if (config == null) {
            Bukkit.getConsoleSender()
                .sendMessage("§aPixelWorldPro 读取结构文件 $name 失败，请检查文件路径或者文件是否存在。Linux等请检查是否有读写权限")
            return null
        }
        val showName = config.getString("showName")
        if (showName == null) {
            Bukkit.getConsoleSender()
                .sendMessage("§aPixelWorldPro ${config.name}不是一个有效的结构文件，因为 showName 为空")
            return null
        }
        val realName = config.getString("realName")
        if (realName == null) {
            Bukkit.getConsoleSender()
                .sendMessage("§aPixelWorldPro ${config.name}不是一个有效的结构文件，因为 realName 为空")
            return null
        }
        val blockList = config.getConfigurationSection("xy")!!.getKeys(false)
        if (blockList.isEmpty()) {
            Bukkit.getConsoleSender()
                .sendMessage("§aPixelWorldPro ${config.name}不是一个有效的结构文件，因为 xy 列表没有内容")
            //return null
        }
        var z = buildZ
        val blockMap = HashMap<BlockLocation, String>()
        for (xy in blockList) {
            var x = buildX
            val buildBlockList = config.getStringList("xy.${xy}")
            var y = buildY + xy.length
            for (buildBlock in buildBlockList) {
                for (block in buildBlock.split("")) {
                    if (block == "") {
                        continue
                    }
                    val blockLocation = BlockLocation(
                        x,
                        y,
                        z
                    )
                    val blockName = config.getString("block.$block.material")
                    if (blockName == null) {
                        Bukkit.getConsoleSender()
                            .sendMessage("§aPixelWorldPro 无法从${config.name}中寻找到方块数据 $block")
                        return null
                    }
                    blockMap[blockLocation] = blockName
                    x += 1
                }
                y -= 1
            }
            z += 1
        }
        return StructureCreateData(
            showName,
            realName,
            blockMap
        )
    }

    fun playerJoin(isJump: Boolean, location: Location, world: World, player: Player) {
        val x = location.x.toInt()
        val y = if (isJump) {
            location.y.toInt() - 1
        } else {
            location.y.toInt()
        }
        val z = location.z.toInt()
        val structure = dimension.getString("Structure.netherFile")
        if (structure == null) {
            Bukkit.getConsoleSender()
                .sendMessage("§aPixelWorldPro 无法在Dimension.yml中找到地狱门配置[Structure.netherFile选项]")
            return
        }
        val structureCreateData = buildStructureCreateData(x, y, z, structure)
        if (structureCreateData == null) {
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 无法构建 Hellgate 的建构数据")
            player.sendMessage("结构构建失败，请联系管理员查看后台日志解决")
            return
        }
        setStruction(world, structureCreateData)
    }

    fun entityJoin(isJump: Boolean, location: Location, world: World) {
        val x = location.x.toInt()
        val y = if (isJump) {
            location.y.toInt() - 1
        } else {
            location.y.toInt()
        }
        val z = location.z.toInt()
        val structure = dimension.getString("Structure.netherFile")
        if (structure == null) {
            Bukkit.getConsoleSender()
                .sendMessage("§aPixelWorldPro 无法在Dimension.yml中找到地狱门配置[Structure.netherFile选项]")
            return
        }
        val structureCreateData = buildStructureCreateData(x, y, z, structure)
        if (structureCreateData == null) {
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 无法构建 Hellgate 的建构数据")
            return
        }
        setStruction(world, structureCreateData)
    }
}

data class StructureCreateData(
    val showName: String,
    val realName: String,
    val block: HashMap<BlockLocation, String>
)

data class BlockLocation(
    val x: Int,
    val y: Int,
    val z: Int
)
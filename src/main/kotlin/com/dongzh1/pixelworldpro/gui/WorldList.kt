package com.dongzh1.pixelworldpro.gui

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.WorldData
import com.dongzh1.pixelworldpro.impl.TeleportImpl
import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.item.buildItem
import com.xbaimiao.easylib.module.utils.colored
import com.xbaimiao.easylib.xseries.XItemStack
import com.xbaimiao.easylib.xseries.XMaterial
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import java.util.*
import kotlin.collections.HashMap

class WorldList(val player: Player) {
    //默认配置文件
    private val config = Gui.getWorldListConfig()

    //列表格子对应的UUID
    private val listMap = mutableMapOf<Int, UUID>()

    //从数据库获取的世界数据开始位置
    private var start = 0

    //是否为最后一页
    private var isLastPage = false

    //是否为第一次打开
    private var isFirst = true

    //获取list对应的格子
    private var intList = mutableListOf<Int>()
    private fun build(page: Int = 1, isTrust: Boolean = false, gui: String = "WorldList.yml"): BasicCharMap {
        val basicCharMap = Gui.buildBaseGui(gui, player)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        val configCustom = BuiltInConfiguration("gui/$gui")
        var listChar: Char? = null
        //获取intList
        for (guiData in charMap) {
            if (guiData.value.type == "List") {
                listChar = guiData.key
                basic.getSlots(guiData.key).forEach {
                    intList.add(it)
                }
                break
            }
        }
        //获取isLastPage,start,isFirst,listMap，填充changelist格子和page格子
        if (isFirst) {
            isFirst = false
            for (guiData in charMap) {
                if (guiData.value.type == "Page") {
                    val item = basic.items[guiData.key]?:continue
                    val meta = item.itemMeta
                    meta.setDisplayName(meta.displayName.replace("{page}", page.toString()))
                    val lore = meta.lore
                    if (lore != null)
                        Collections.replaceAll(lore, "{page}", page.toString())
                    meta.lore = lore
                    item.itemMeta = meta
                    basic.set(guiData.key, item)
                    continue
                }
                if (guiData.value.type == "ChangeList") {
                    if (guiData.value.value == "trust") {
                        fillListMap(player, true, page)
                        basic.set(
                            guiData.key,
                            XItemStack.deserialize(config.getConfigurationSection("ChangeList.trust")!!)
                        )
                    } else {
                        fillListMap(player, false, page)
                        basic.set(
                            guiData.key,
                            XItemStack.deserialize(config.getConfigurationSection("ChangeList.public")!!)
                        )
                    }
                }
            }
        } else {
            fillListMap(player, isTrust, page)
            for (guiData in charMap) {
                if (guiData.value.type == "ChangeList") {
                    if (isTrust) {
                        basic.set(
                            guiData.key,
                            XItemStack.deserialize(config.getConfigurationSection("ChangeList.trust")!!)
                        )
                    } else {
                        basic.set(
                            guiData.key,
                            XItemStack.deserialize(config.getConfigurationSection("ChangeList.public")!!)
                        )
                    }
                    break
                }
            }
        }
        //填充list格子
        for (list in listMap){
            if (listChar == null) {
                break
            }
            val worldData = PixelWorldPro.databaseApi.getWorldData(list.value) ?: continue
            val worldOwner = Bukkit.getOfflinePlayer(list.value)
            val listConfig = configCustom.getConfigurationSection("items.$listChar")!!
            listConfig.set("name", listConfig.getString("name")?.replacePlaceholder(worldOwner).colored())

            if (list.value == player.uniqueId)
                listConfig.set("name",listConfig.getString("name")?.
                replace("{role}",config.getStringColored("List.role.owner")))
            if (worldData.banPlayers.contains(player.uniqueId))
                listConfig.set("name",listConfig.getString("name")?.
                replace("{role}",config.getStringColored("List.role.ban")))
            if (worldData.members.contains(player.uniqueId))
                listConfig.set("name",listConfig.getString("name")?.
                replace("{role}",config.getStringColored("List.role.member")))
            listConfig.set("name",listConfig.getString("name")?.
            replace("{role}",config.getStringColored("List.role.visitor")))

            listConfig.set("lore", listConfig.getStringList("lore").replacePlaceholder(worldOwner).colored())

            if (list.value == player.uniqueId)
                listConfig.set("lore",Collections.replaceAll(listConfig.getStringList("lore"),
                "{role}",config.getStringColored("List.role.owner")))
            if (worldData.banPlayers.contains(player.uniqueId))
                listConfig.set("lore",Collections.replaceAll(listConfig.getStringList("lore"),
                "{role}",config.getStringColored("List.role.ban")))
            if (worldData.members.contains(player.uniqueId))
                listConfig.set("lore",Collections.replaceAll(listConfig.getStringList("lore"),
                "{role}",config.getStringColored("List.role.member")))
            listConfig.set("lore",Collections.replaceAll(listConfig.getStringList("lore"),
                "{role}",config.getStringColored("List.role.visitor")))

            listConfig.set("skull", listConfig.getString("skull")?.replacePlaceholder(worldOwner).colored())
            val item = XItemStack.deserialize(listConfig)
            basic.set(list.key, item)
        }
        return BasicCharMap(basic, charMap)
    }

    fun open(page: Int = 1, isTrust: Boolean = false, gui: String = "WorldList.yml") {
        val basicCharMap = build(page, isTrust, gui)
        val basic = basicCharMap.basic
        val charMap = basicCharMap.charMap
        basic.openAsync()
        basic.onClick {
            it.isCancelled = true
        }
        for (guiData in charMap) {
            basic.onClick(guiData.key) {
                //执行命令
                if (guiData.value.commands != null) {
                    Gui.runCommand(player, guiData.value.commands!!)
                }
                when (guiData.value.type) {
                    "ChangeList" -> {
                        open(1, !isTrust, gui)
                    }

                    "List" -> {
                        val slot = it.rawSlot
                        val uuid = listMap[slot] ?: return@onClick
                        TeleportImpl().teleport(player.uniqueId, uuid)
                    }
                    "Page" -> {
                        when (guiData.value.value) {
                            "next" -> {
                                if (!isLastPage) {
                                    start = intList.size * page
                                    open(page + 1, isTrust, gui)
                                }

                            }

                            "back" -> {
                                if (page == 1) {
                                    return@onClick
                                }
                                start = intList.size * (page - 2)
                                open(page - 1, isTrust, gui)
                            }
                        }
                    }
                    else -> {
                    }
                }

            }
        }

    }

    private fun fillListMap(player: Player, isTrust: Boolean, page: Int) {
        listMap.clear()
        val uuidList = mutableListOf<UUID>()
        if (isTrust) {
            val joinedWorld = PixelWorldPro.databaseApi.getPlayerData(player.uniqueId)?.joinedWorld
            if (joinedWorld != null) {
                var i = 0
                for (uuid in joinedWorld) {
                    if (i < start) {
                        i++
                        continue
                    }
                    uuidList.add(uuid)
                }
                if (uuidList.size > intList.size) {
                    //数据超过这一页能显示的内容，不是最后一页，截取本页显示内容
                    for (slot in intList) {
                        listMap[slot] = uuidList.first()
                        uuidList.removeAt(0)
                    }
                    isLastPage = false
                } else {
                    //数据没超过这一页能显示的内容
                    isLastPage = true
                    for (slot in intList) {
                        listMap[slot] = uuidList.first()
                        uuidList.removeAt(0)
                        if (uuidList.isEmpty()) {
                            break
                        }
                    }
                }
            } else {
                //没有加入任何世界
                isLastPage = true
            }
        } else {
            val worldList = PixelWorldPro.databaseApi.getWorldList(start, intList.size) as MutableList<UUID>
            if (worldList.size > intList.size) {
                //数据超过这一页能显示的内容，不是最后一页，截取本页显示内容
                for (slot in intList) {
                    listMap[slot] = worldList.first()
                    worldList.removeAt(0)
                }
                isLastPage = false
            } else {
                //数据没超过这一页能显示的内容
                isLastPage = true
                listMap.clear()
                for (slot in intList) {
                    listMap[slot] = worldList.first()
                    worldList.removeAt(0)
                    if (worldList.isEmpty()) {
                        break
                    }
                }
            }
        }
    }
}
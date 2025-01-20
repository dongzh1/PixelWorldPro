package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.module.chat.colored
import com.xbaimiao.easylib.module.utils.parseToXMaterial
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

class Variable(val key: String, val value: String)

fun ConfigurationSection.convertItem(player: OfflinePlayer, variables: List<Variable> = emptyList()): ItemStack {
    var name = this.getString("name", " ")!!.colored().replacePlaceholder(player)
    for (variable in variables) {
        name = name.replace(variable.key, variable.value)
    }
    val lore = this.getStringList("lore").colored().replacePlaceholder(player).toMutableList()
    lore.replaceAll {
        var result = it
        for (variable in variables) {
            result = result.replace(variable.key, variable.value)
        }
        result
    }
    var customModelData = this.getString("custom-model-data", "0")!!
    for (variable in variables) {
        customModelData = customModelData.replace(variable.key, variable.value)
    }

    var itemStack = this.getString("material", "STONE")!!.parseToXMaterial().parseItem() ?: error("Unknown material")

    val skull = this.getString("skull")
    if (skull != null) {
        val head = PixelWorldPro.instance.cmiAdapt?.loadHead(skull)
        if (head != null) {
            itemStack = head
        }
    }

    val meta = itemStack.itemMeta ?: return itemStack
    meta.setDisplayName(name)
    meta.lore = (meta.lore ?: ArrayList()).also {
        it.addAll(lore)
    }
    if (customModelData.toIntOrNull() != null) {
        meta.setCustomModelData(customModelData.toInt())
    }
    itemStack.itemMeta = meta
    itemStack.durability = this.getInt("durability").toShort()
    return itemStack
}
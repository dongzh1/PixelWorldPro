package com.dongzh1.pixelworldpro.tools

import com.xbaimiao.easylib.bridge.replacePlaceholder
import com.xbaimiao.easylib.module.chat.colored
import com.xbaimiao.easylib.module.item.buildItem
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

    val itemStack = this.getString("material", "STONE")!!.parseToXMaterial().parseItem() ?: error("Unknown material")

    val skull = this.getString("skull")

    return buildItem(itemStack) {
        this.name = name
        this.damage = this@convertItem.getInt("durability")
        this.lore.addAll(lore)
        if (skull != null) {
            this.skullOwner = skull
        }
        customModelData.toIntOrNull()?.let {
            this.customModelData = it
        }
    }
}

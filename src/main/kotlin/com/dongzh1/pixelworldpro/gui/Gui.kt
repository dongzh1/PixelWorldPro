package com.dongzh1.pixelworldpro.gui


import com.xbaimiao.easylib.module.chat.BuiltInConfiguration
import com.xbaimiao.easylib.module.ui.Basic
import com.xbaimiao.easylib.module.utils.parseToXMaterial
import com.xbaimiao.easylib.xseries.XMaterial
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


object Gui {
    private var membersEditConfig = BuiltInConfiguration("gui/MembersEdit.yml")
    private var worldCreateConfig = BuiltInConfiguration("gui/WorldCreate.yml")
    //获取配置
    fun getMembersEditConfig(): BuiltInConfiguration{
        return membersEditConfig
    }
    fun getWorldCreateConfig(): BuiltInConfiguration{
        return worldCreateConfig
    }
    fun reloadConfig(){
        membersEditConfig = BuiltInConfiguration("gui/MembersEdit.yml")
        worldCreateConfig = BuiltInConfiguration("gui/WorldCreate.yml")
    }
    fun buildBaseGui(type:String,player: Player): Basic {
        val config = BuiltInConfiguration("gui/$type.yml")
        val basic = Basic(player,config.getStringColored("title"))
        basic.map(config.getStringList("slots"))
        basic.slots.forEach { slot ->
        }
        return basic
    }
    fun buildItem(config:BuiltInConfiguration,char:Char){

        val material = config.getString("items.$char.material")!!
        val xMaterial = XMaterial.matchXMaterial(material).get()


        val item = com.xbaimiao.easylib.module.item.buildItem(xMaterial){

            if (config.contains("items.$char.name")){
                name = config.getStringColored("items.$char.name")
            }
            if (config.contains("items.$char.lore")){
                lore.clear()
                lore.addAll(config.getStringListColored("items.$char.lore"))
            }
            if (config.contains("items.$char.model")){
                customModelData = config.getInt("items.$char.model")
            }
            if (config.contains("items.$char.amount")){
                amount = config.getInt("items.$char.amount")
            }

        }
    }

}
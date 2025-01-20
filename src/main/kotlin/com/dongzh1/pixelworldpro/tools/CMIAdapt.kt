package com.dongzh1.pixelworldpro.tools

import com.Zrips.CMI.CMI
import com.Zrips.CMI.Modules.SavedItems.SavedItem
import net.Zrips.CMILib.CMILib
import org.bukkit.inventory.ItemStack

class CMIAdapt {

    private val cmi by lazy { CMI.getInstance() }

    fun loadHead(skull: String): ItemStack? {
        val saveKey = "pwp-v1-$skull"
        val savedItemManager = CMI.getInstance().savedItemManager

        val saveItem = savedItemManager.getSavedItem(null, saveKey)
        if (saveItem != null) {
            return saveItem.item.clone()
        } else {
            val asyncHead = object : net.Zrips.CMILib.Items.CMIAsyncHead() {
                override fun afterAsyncUpdate(item: ItemStack) {
                    savedItemManager.addSavedItem(SavedItem(saveKey, item))
                }
            }
            asyncHead.isForce = true
            val cmiItem = CMILib.getInstance().itemManager.getItem("head:$skull", asyncHead)
            if (!asyncHead.isAsyncHead) {
                val itemStack = cmiItem.itemStack
                savedItemManager.addSavedItem(SavedItem(saveKey, itemStack))
                return itemStack.clone()
            }
            return null
        }
    }

    fun save() {
        cmi.savedItemManager.save()
    }

}
package com.dongzh1.pixelworldpro.world

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.DimensionData

object Dimension {

    fun getDimensionData(dimension: String): DimensionData? {
        val dimensionconfig = PixelWorldPro.instance.dimensionconfig
        val dimensions = dimensionconfig.getConfigurationSection("Dimension")!!.getKeys(false)
        val dimensionlist = ArrayList<String>()
        val usemap = HashMap<String, String>()
        val pointsmap = HashMap<String, Double>()
        val moneymap = HashMap<String, Double>()
        val creatormap = HashMap<String, String>()
        val barrier = HashMap<String, Boolean>()
        for (d in dimensions) {
            val name = dimensionconfig.getString("Dimension.$d.name")!!
            dimensionlist.add(name)
            usemap[name] = dimensionconfig.getString("Dimension.$d.Create.CreateUse")!!
            pointsmap[name] = dimensionconfig.getDouble("Dimension.$d.Create.CreatePoints")
            moneymap[name] = dimensionconfig.getDouble("Dimension.$d.Create.CreateMoney")
            creatormap[name] = dimensionconfig.getString("Dimension.$d.Create.Creator")!!
            barrier[name] = dimensionconfig.getBoolean("Dimension.$d.Barrier")
        }
        return if (dimension in dimensionlist) {
            val data = DimensionData(
                dimension,
                creatormap[dimension]!!,
                usemap[dimension]!!,
                moneymap[dimension]!!,
                pointsmap[dimension]!!,
                barrier[dimension]!!
            )
            data
        } else {
            null
        }
    }

    fun getDimensionList(): ArrayList<String> {
        val dimensionconfig = PixelWorldPro.instance.dimensionconfig
        val dimension = dimensionconfig.getConfigurationSection("Dimension")!!.getKeys(false)
        val dimensionlist = ArrayList<String>()
        for (d in dimension) {
            val name = dimensionconfig.getString("Dimension.$d.name")!!
            dimensionlist.add(name)
        }
        return dimensionlist
    }
}
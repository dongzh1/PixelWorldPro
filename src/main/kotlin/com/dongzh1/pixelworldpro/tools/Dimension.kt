package com.dongzh1.pixelworldpro.tools

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.database.DimensionData
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.ArrayList

object Dimension {

    fun getDimensionData(dimension: String): DimensionData? {
        val dimensionconfig = PixelWorldPro.instance.dimensionconfig
        val dimensions = dimensionconfig.getList("Dimension")!!
        val dimensionlist = ArrayList<String>()
        val usemap = HashMap<String, String>()
        val pointsmap = HashMap<String, Double>()
        val moneymap = HashMap<String, Double>()
        val creatormap = HashMap<String, String>()
        val barrier = HashMap<String, Boolean>()
        for (d in dimensions){
            val g = Gson()
            val json: JsonObject = g.fromJson(d.toString(), JsonObject::class.java)
            val name = json.get("name").asString
            dimensionlist.add(name)
            val create = json.get("Create").asJsonObject
            usemap[name] = create.get("CreateUse").asString
            pointsmap[name] = create.get("CreatePoints").asDouble
            moneymap[name] = create.get("CreateMoney").asDouble
            creatormap[name] = create.get("Creator").asString
            barrier[name] = json.get("Barrier").asBoolean
        }
        return if (dimension in dimensionlist){
            val data = DimensionData(
                dimension,
                creatormap[dimension]!!,
                usemap[dimension]!!,
                moneymap[dimension]!!,
                pointsmap[dimension]!!,
                barrier[dimension]!!
            )
            data
        }else{
            null
        }
    }

    fun getDimensionList(): ArrayList<String> {
        val dimensionconfig = PixelWorldPro.instance.dimensionconfig
        val dimension = dimensionconfig.getList("Dimension")!!
        val dimensionlist = ArrayList<String>()
        for (d in dimension){
            val g = Gson()
            val json: JsonObject = g.fromJson(d.toString(), JsonObject::class.java)
            val name = json.get("name").asString
            dimensionlist.add(name)
        }
        return dimensionlist
    }
}
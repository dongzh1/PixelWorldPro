package com.dongzh1.pixelworldpro.expansion

import com.dongzh1.pixelworldpro.PixelWorldPro
import com.dongzh1.pixelworldpro.online.V2.applyExpansion
import com.dongzh1.pixelworldpro.online.V2.getExpansion
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.jar.JarFile


object ExpansionManager {

    private val classes: MutableMap<String, Class<*>>
    private val loaders: MutableMap<Expansion, ExpansionClassLoader>

    init {
        loaders = HashMap()
        classes = HashMap()
    }

    fun getClassByName(name: String): Any? {
        try {
            return classes.getOrDefault(
                name,
                loaders.values.stream().filter(Objects::nonNull)
                    .map { l: ExpansionClassLoader -> l.findClass(name, false) }.filter(Objects::nonNull).findFirst()
                    .orElse(null)
            )
        } catch (ignored: java.lang.Exception) {
            // Ignored.
        }
        return null
    }

    fun setClass(name: String, clazz: Class<*>) {
        classes.putIfAbsent(name, clazz)
    }

    fun loadExpansion() {
        val config = PixelWorldPro.instance.expansionconfig
        val paylist = config.getList("pay")!!
        val paynamelist = ArrayList<String>()
        val freelist = config.getList("free")!!
        val freenamelist = ArrayList<String>()
        for (pay in paylist) {
            val g = Gson()
            val json: JsonObject = g.fromJson(pay.toString(), JsonObject::class.java)
            if (json.get("enable").asBoolean) {
                paynamelist.add(json.get("name").asString)
            }
        }
        for (free in freelist) {
            val g = Gson()
            val json: JsonObject = g.fromJson(free.toString(), JsonObject::class.java)
            if (json.get("enable").asBoolean) {
                freenamelist.add(json.get("name").asString)
            }
        }
        for (name in paynamelist) {
            val res = applyExpansion(PixelWorldPro.instance.config.getString("token")!!, "pay", name)
            if (res != null) {
                val file = getExpansion(
                    PixelWorldPro.instance.config.getString("token")!!,
                    "free",
                    res.get("id").asString,
                    name,
                    res.get("token").asString,
                    res.get("iv").asString
                )
                if (file != null) {
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 尝试读取${name}扩展")
                    val tempFile = File.createTempFile(name, ".jiangcloudfile")
                    tempFile.writeBytes(file.toByteArray())
                    JarFile(tempFile).use { jar ->
                        val data = expansionDescription(jar)
                        if (data != null) {
                            if (data.getInt("api-version") >= 1) {
                                ExpansionClassLoader(this, data, tempFile, this.javaClass.classLoader, name)
                            } else {
                                Bukkit.getConsoleSender().sendMessage("§4PixelWorldPro 无法理解${name}使用的API版本")
                            }
                        } else {
                            Bukkit.getConsoleSender().sendMessage("§4PixelWorldPro ${name}不是一个有效的扩展")
                        }
                    }
                    tempFile.delete()
                }
            }
        }
        for (name in freenamelist) {
            val res = applyExpansion(PixelWorldPro.instance.config.getString("token")!!, "free", name)
            if (res != null) {
                val file = getExpansion(
                    PixelWorldPro.instance.config.getString("token")!!,
                    "free",
                    res.get("id").asString,
                    name,
                    res.get("token").asString,
                    res.get("iv").asString
                )
                if (file != null) {
                    Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 尝试读取${name}扩展")
                    val tempFile = File.createTempFile(name, ".jiangcloudfile")
                    tempFile.writeBytes(file.toByteArray())
                    JarFile(tempFile).use { jar ->
                        val data = expansionDescription(jar)
                        if (data != null) {
                            if (data.getInt("api-version") >= 1) {
                                ExpansionClassLoader(this, data, tempFile, this.javaClass.classLoader, name)
                            } else {
                                Bukkit.getConsoleSender().sendMessage("§4PixelWorldPro 无法理解${name}使用的API版本")
                            }
                        } else {
                            Bukkit.getConsoleSender().sendMessage("§4PixelWorldPro ${name}不是一个有效的扩展")
                        }
                    }
                    tempFile.delete()
                }
            }
        }
    }

    @Throws(IOException::class, InvalidConfigurationException::class)
    private fun expansionDescription(jar: JarFile): YamlConfiguration? {
        return try {
            val entry = jar.getJarEntry("expansion.yml")
            val reader = BufferedReader(InputStreamReader(jar.getInputStream(entry)))
            val data = YamlConfiguration()
            data.load(reader)
            reader.close()
            data
        } catch (e: Exception) {
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 该扩展不是一个有效的PixelWorldPro扩展")
            null
        }
    }
}
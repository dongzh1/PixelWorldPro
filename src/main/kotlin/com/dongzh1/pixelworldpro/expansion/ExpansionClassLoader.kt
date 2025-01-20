package com.dongzh1.pixelworldpro.expansion

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.InvalidDescriptionException
import java.io.File
import java.net.URL
import java.net.URLClassLoader

class ExpansionClassLoader//try {
//} catch (e: ClassCastException) {
//    throw java.lang.Exception("主类没有扩展")
//}
    (addonsManager: ExpansionManager, data: YamlConfiguration, jarFile: File, parent: ClassLoader?, name: String) :
    URLClassLoader(
        arrayOf<URL>(jarFile.toURI().toURL()), parent
    ) {
    private val classes: MutableMap<String, Class<*>?> = HashMap()

    @JvmField
    val expansion: Expansion
    private val loader: ExpansionManager = addonsManager

    init {
        val javaClass: Class<*>
        try {
            val mainClass =
                data.getString("main") ?: throw java.lang.Exception("§4PixelWorldPro expansion.yml 没有设置一个主类！")
            javaClass = Class.forName(mainClass, true, this)
            if (mainClass.startsWith("com.dongzh1.pixelworldpro")) {
                throw java.lang.Exception("§4PixelWorldPro 扩展的主类不能是 'com.dongzh1.pixelworldpro'")
            }
        } catch (e: Exception) {
            throw InvalidDescriptionException("§4PixelWorldPro 无法加载扩展$name")
        }
        val expansionClass: Class<out Expansion> = javaClass.asSubclass(Expansion::class.java)
        expansion = expansionClass.getDeclaredConstructor().newInstance()
        Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 加载扩展$name")
        expansion.onEnable()
    }

    public override fun findClass(name: String): Class<*>? {
        return findClass(name, true)
    }

    fun findClass(name: String, checkGlobal: Boolean): Class<*>? {
        if (name.startsWith("com.dongzh1.pixelworldpro")) {
            return null
        }
        var result = classes[name]
        if (result == null) {
            if (checkGlobal) {
                result = loader.getClassByName(name) as Class<*>?
            }
            if (result == null) {
                try {
                    result = super.findClass(name)
                } catch (e: ClassNotFoundException) {
                    // Do nothing.
                } catch (_: NoClassDefFoundError) {
                }
                if (result != null) {
                    loader.setClass(name, result)
                }
            }
            classes[name] = result
        }
        return result
    }
}

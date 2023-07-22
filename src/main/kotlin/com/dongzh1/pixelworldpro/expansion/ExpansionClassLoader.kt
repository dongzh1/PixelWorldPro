package com.dongzh1.pixelworldpro.expansion

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.InvalidDescriptionException
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*

/**
 * Loads addons and sets up permissions
 * @author Tastybento, ComminQ
 */
class ExpansionClassLoader : URLClassLoader {
    private val classes: MutableMap<String, Class<*>?> = HashMap()

    /**
     * @return the addon
     */
    @JvmField
    val expansion: Expansion
    private val loader: ExpansionManager

    /**
     * For testing only
     * @param addon addon
     * @param loader Addons Manager
     * @param jarFile Jar File
     * @throws MalformedURLException exception
     */
    constructor(expansion: Expansion, loader: ExpansionManager, jarFile: File) : super(arrayOf<URL>(jarFile.toURI().toURL())) {
        this.expansion = expansion
        this.loader = loader
    }

    constructor(addonsManager: ExpansionManager, data: YamlConfiguration, jarFile: File, parent: ClassLoader?) : super(
            arrayOf<URL>(jarFile.toURI().toURL()), parent
    ) {
        loader = addonsManager
        val javaClass: Class<*>
        try {
            val mainClass =
                    data.getString("main") ?: throw java.lang.Exception("§4PixelPlayerWorld expansion.yml 没有设置一个主类！")
            javaClass = Class.forName(mainClass, true, this)
            if (mainClass.startsWith("com.xbaimiao.template")) {
                throw java.lang.Exception("§4PixelPlayerWorld 扩展的主类不能是 'com.xbaimiao.template'")
            }
        } catch (e: Exception) {
            throw InvalidDescriptionException("无法加载 '" + jarFile.name + "' 在文件夹中 '" + jarFile.parent + "' - " + e.message)
        }
        val expansionClass: Class<out Expansion>
        expansionClass = //try {
            javaClass.asSubclass(Expansion::class.java)
        //} catch (e: ClassCastException) {
        //    throw java.lang.Exception("主类没有扩展")
        //}
        expansion = expansionClass.getDeclaredConstructor().newInstance()
        Bukkit.getConsoleSender().sendMessage("§aPixelPlayerWorld加载扩展$jarFile")
        expansion.onEnable()
    }

    /* (non-Javadoc)
     * @see java.net.URLClassLoader#findClass(java.lang.String)
     */
    public override fun findClass(name: String): Class<*>? {
        return findClass(name, true)
    }

    /**
     * This is a custom findClass that enables classes in other addons to be found
     * @param name - class name
     * @param checkGlobal - check globally or not when searching
     * @return Class - class if found
     */
    fun findClass(name: String, checkGlobal: Boolean): Class<*>? {
        if (name.startsWith("world.bentobox.bentobox")) {
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
                } catch (e: NoClassDefFoundError) {
                }
                if (result != null) {
                    loader.setClass(name, result)
                }
            }
            classes[name] = result
        }
        return result
    }

    /**
     * @return class list
     */
    fun getClasses(): Set<String> {
        return classes.keys
    }
}

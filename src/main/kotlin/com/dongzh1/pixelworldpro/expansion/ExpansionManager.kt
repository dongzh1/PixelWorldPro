package com.dongzh1.pixelworldpro.expansion

import org.bukkit.Bukkit
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.jar.JarFile


class ExpansionManager(){

    private val classes: MutableMap<String, Class<*>>
    private val loaders: MutableMap<Expansion, ExpansionClassLoader>

    init {
        loaders = HashMap()
        classes = HashMap()
    }

    private fun enableAddon(expansion: Expansion) {
        try {
            expansion.onEnable()
        } catch (e: NoClassDefFoundError) {
            // Looks like the addon is incompatible, because it tries to refer to missing classes...
            Bukkit.getConsoleSender().sendMessage("$expansion $e")
        } catch (e: NoSuchMethodError) {
            Bukkit.getConsoleSender().sendMessage("$expansion $e")
        } catch (e: NoSuchFieldError) {
            Bukkit.getConsoleSender().sendMessage("$expansion $e")
        } catch (e: Exception) {
            // Unhandled exception. We'll give a bit of debug here.
            Bukkit.getConsoleSender().sendMessage("$expansion $e")
        }
    }

    open fun getClassByName(name: String): Any? {
        try {
            return classes.getOrDefault(name, loaders.values.stream().filter(Objects::nonNull).map { l: ExpansionClassLoader -> l.findClass(name, false) }.filter(Objects::nonNull).findFirst().orElse(null))
        } catch (ignored: java.lang.Exception) {
            // Ignored.
        }
        return null
    }

    fun setClass(name: String, clazz: Class<*>) {
        classes.putIfAbsent(name, clazz)
    }

    fun loadExpansion() {
        var file = File("./plugins/PixelWorldPro/expansion")
        //如果文件夹不存在则创建
        if (!file.exists() && !file.isDirectory) {
            file.mkdir()
        }else{
        }

        file = File("./plugins/PixelWorldPro/expansion")
        if (file.listFiles().isNotEmpty()) {
            for (f in file.listFiles()) {
                if (f.name.endsWith(".jar")) {
                    Bukkit.getConsoleSender().sendMessage("§aPixelPlayerWorld尝试读取 $f")
                    JarFile(f).use { jar ->
                        val data = addonDescription(jar)
                        var result = ExpansionClassLoader(this, data, f, this.javaClass.classLoader)
                    }
                }
            }
        }
    }

    @Throws(IOException::class, InvalidConfigurationException::class)
    private fun addonDescription(jar: JarFile): YamlConfiguration {
        // Obtain the addon.yml file
        val entry = jar.getJarEntry("expansion.yml")
        // Open a reader to the jar
        val reader = BufferedReader(InputStreamReader(jar.getInputStream(entry)))
        // Grab the description in the addon.yml file
        val data = YamlConfiguration()
        data.load(reader)
        reader.close()
        return data
    }
}
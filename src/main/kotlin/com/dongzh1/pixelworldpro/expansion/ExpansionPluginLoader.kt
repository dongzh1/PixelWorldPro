package com.dongzh1.pixelworldpro.expansion

import java.io.File
import java.io.IOException
import java.net.*
import java.util.*


object ExpansionPluginLoader {
    private const val PROPERTIES_NAME = "expansion.properties"
    private const val MAIN_CLASS = "main"

    /**
     * 加载jar文件
     *
     * @param jarFilePath jar文件路径
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    fun loadJar(jarFilePath: String): Class<*>? {
        val classLoader = getClassLoader(jarFilePath)
        val properties = getProperties(classLoader, PROPERTIES_NAME)
        val mainClass = properties.getProperty(MAIN_CLASS)
        return loadClass(classLoader, mainClass)
    }

    /**
     * 获得ClassLoader
     *
     * @param jarFilePath jar文件路径
     * @return
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    private fun getClassLoader(jarFilePath: String): ClassLoader? {
        val jarFile = File(jarFilePath)
        if (!jarFile.exists()) {
            return null
        }
        val url = jarFile.toURI().toURL()
        return URLClassLoader(arrayOf(url), null)
    }

    /**
     * 获得jar中的properties
     *
     * @param classLoader    classLoader
     * @param propertiesName 文件名称
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun getProperties(classLoader: ClassLoader?, propertiesName: String): Properties {
        val propertiesStream = classLoader!!.getResourceAsStream(propertiesName)
        val properties = Properties()
        properties.load(propertiesStream)
        return properties
    }

    /**
     * 加载类
     *
     * @param classLoader classLoader
     * @param className   全类名
     * @return
     * @throws ClassNotFoundException
     */
    @Throws(ClassNotFoundException::class)
    private fun loadClass(classLoader: ClassLoader?, className: String): Class<*>? {
        return classLoader!!.loadClass(className)
    }

}

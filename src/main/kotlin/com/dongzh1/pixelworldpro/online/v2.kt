package com.dongzh1.pixelworldpro.online

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bukkit.Bukkit
import org.json.simple.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.nio.charset.Charset
import java.security.Key
import java.security.Security
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object V2 {
    //避免重复new生成多个BouncyCastleProvider对象，因为GC回收不了，会造成内存溢出
    //只在第一次调用decrypt()方法时才new 对象
    private var initialized: Boolean = false
    @Throws(Exception::class)
    fun getMacByIP(): String {
        return getMacByIP(InetAddress.getLocalHost().hostAddress)
    }

    @Throws(Exception::class)
    fun getMacByIP(ip: String?): String {
        val ia = InetAddress.getByName(ip)
        val mac = NetworkInterface.getByInetAddress(ia).hardwareAddress
        val sb = StringBuffer()
        for (i in mac.indices) {
            if (i != 0) {
                sb.append("-")
            }
            val hexString = Integer.toHexString(mac[i].toInt() and 0xFF)
            sb.append(if (hexString.length == 1) "0$hexString" else hexString)
        }
        return sb.toString().uppercase(Locale.getDefault())
    }
    fun auth(token:String): Boolean {
        val mac = getMacByIP()
        val dummyUrl = URL("https://sh1.plugin.mcyzj.cn:1031/v1/auth/Pixelworldpro")
        val data = mapOf("token" to token, "macs" to mac)
        val dummyData = JSONObject(data).toString()

        try {
            val httpUrlConnection: HttpURLConnection = dummyUrl.openConnection() as HttpURLConnection
            httpUrlConnection.requestMethod = "POST"
            httpUrlConnection.doOutput = true
            httpUrlConnection.setRequestProperty("Content-Type", "application/json")
            val dataOutputStream = DataOutputStream(httpUrlConnection.outputStream)
            dataOutputStream.writeBytes(dummyData)
            val inputStream: InputStream = httpUrlConnection.inputStream
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var stringLine: String
            while (bufferedReader.readLine().also { stringLine = it } != null) {
                val g = Gson()
                val back: JsonObject = g.fromJson(stringLine, JsonObject::class.java)
                val type = back.get("type").asInt
                return type == 1
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return false
    }

    fun verify(token:String): Boolean {
        val mac = getMacByIP()
        val dummyUrl = URL("https://sh1.plugin.mcyzj.cn:1031/v1/verify/Pixelworldpro")
        val data = mapOf("token" to token, "macs" to mac)
        val dummyData = JSONObject(data).toString()

        try {
            val httpUrlConnection: HttpURLConnection = dummyUrl.openConnection() as HttpURLConnection
            httpUrlConnection.requestMethod = "POST"
            httpUrlConnection.doOutput = true
            httpUrlConnection.setRequestProperty("Content-Type", "application/json")
            httpUrlConnection.setRequestProperty("accept", "application/json")
            httpUrlConnection.setRequestProperty("charset", "utf-8")
            httpUrlConnection.setRequestProperty("Content-Length", dummyData.length.toString())
            val dataOutputStream = DataOutputStream(httpUrlConnection.outputStream)
            dataOutputStream.writeBytes(dummyData)
            val inputStream: InputStream = httpUrlConnection.inputStream
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var stringLine: String
            while (bufferedReader.readLine().also { stringLine = it } != null) {
                val g = Gson()
                val back: JsonObject = g.fromJson(stringLine, JsonObject::class.java)
                val type = back.get("type").asInt
                return type == 1
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return false
    }

    fun applyExpansion(token:String, lib:String, name:String): JsonObject? {
        val mac = getMacByIP()
        val dummyUrl = URL("https://sh1.plugin.mcyzj.cn:1031/v2/Expansion/Pixelworldpro/apply")
        val data = mapOf("token" to token, "macs" to mac, "lib" to lib, "name" to name)
        val dummyData = JSONObject(data).toString()

        try {
            val httpUrlConnection: HttpURLConnection = dummyUrl.openConnection() as HttpURLConnection
            httpUrlConnection.requestMethod = "POST"
            httpUrlConnection.doOutput = true
            httpUrlConnection.setRequestProperty("Content-Type", "application/json")
            httpUrlConnection.setRequestProperty("accept", "application/json")
            httpUrlConnection.setRequestProperty("charset", "utf-8")
            httpUrlConnection.setRequestProperty("Content-Length", dummyData.length.toString())
            val dataOutputStream = DataOutputStream(httpUrlConnection.outputStream)
            dataOutputStream.writeBytes(dummyData)
            val inputStream: InputStream = httpUrlConnection.inputStream
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var stringLine: String
            while (bufferedReader.readLine().also { stringLine = it } != null) {
                val g = Gson()
                val back: JsonObject = g.fromJson(stringLine, JsonObject::class.java)
                val type = back.get("type").asInt
                return if (type != 0){
                    back.get("result").asJsonObject
                }else{
                    null
                }
            }
        } catch (e: IOException) {
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 网络错误，获取${lib}组件的${name}扩展失败")
            return null
        }
        return null
    }

    fun getExpansion(token:String, lib:String, afn:String, name:String, key: String, iv: String): ByteArrayOutputStream? {
        val mac = getMacByIP()
        val dummyUrl = URL("https://sh1.plugin.mcyzj.cn:1031/v2/Expansion/Pixelworldpro/get")
        val data = mapOf("token" to token, "macs" to mac, "afn" to afn, "lib" to lib, "name" to name)
        val dummyData = JSONObject(data).toString()

        try {
            // 连接类的父类，抽象类
            val urlConnection = dummyUrl.openConnection()
            // http的连接类
            val httpUrlConnection = urlConnection as HttpURLConnection
            httpUrlConnection.requestMethod = "POST"
            httpUrlConnection.doOutput = true
            httpUrlConnection.setRequestProperty("Content-Type", "application/json")
            httpUrlConnection.connect()
            val dataOutputStream = DataOutputStream(httpUrlConnection.outputStream)
            dataOutputStream.writeBytes(dummyData)
            val inputStream: InputStream = httpUrlConnection.inputStream
            return try {
                val fileLength = httpUrlConnection.contentLength
                val bin = BufferedInputStream(inputStream)
                val ncryption = ByteArrayOutputStream()
                var size: Int
                var len = 0
                val buf = ByteArray(1024)
                var frequency = 0
                while (bin.read(buf).also { size = it } != -1) {
                    len += size
                    ncryption.write(buf, 0, size)
                    if (frequency % 500 == 0) {
                        Bukkit.getConsoleSender()
                            .sendMessage("§aPixelWorldPro §f拉取${lib}组件的${name}扩展：${len * 100 / fileLength}%")
                    }
                    frequency += 1
                }
                bin.close()
                ncryption.close()
                val charset = Charset.forName("UTF8")
                val old = FileOutputStream("./plugins/PixelWorldPro/e.jar")
                old.write(ncryption.toByteArray())
                val out = decrypt(
                    Base64.getDecoder().decode(ncryption.toString()),
                    key.toByteArray(charset),
                    iv.toByteArray(charset)
                )!!
                val decrypt = ByteArrayOutputStream()
                decrypt.write(out)
                decrypt
            } catch (e: Exception) {
                throw e
            }

        } catch (e: IOException) {
            Bukkit.getConsoleSender().sendMessage("§aPixelWorldPro 网络错误，获取${lib}组件的${name}扩展失败")
            return null
        }
    }

    private fun decrypt(content: ByteArray, aesKey: ByteArray, ivByte: ByteArray): ByteArray? {
        initialize()
        return try {
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            val sKeySpec: Key = SecretKeySpec(aesKey, "AES")
            cipher.init(Cipher.DECRYPT_MODE, sKeySpec, IvParameterSpec(ivByte)) // 初始化
            cipher.doFinal(content)
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }

    /**BouncyCastle作为安全提供，防止我们加密解密时候因为jdk内置的不支持改模式运行报错。 */
    private fun initialize() {
        if (initialized) return
        Security.addProvider(BouncyCastleProvider())
        initialized = true
    }

}
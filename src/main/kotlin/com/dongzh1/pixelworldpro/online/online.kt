package com.dongzh1.pixelworldpro.online

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.simple.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.util.*


object Online {
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
        val dummyUrl = URL("http://zh_sh1.mcyzj.cn:1030/v1/auth/Pixelworldpro")
        val data = mapOf<String, String>("token" to token, "macs" to mac)
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
        val dummyUrl = URL("http://zh_sh1.mcyzj.cn:1030/v1/verify/Pixelworldpro")
        val data = mapOf<String, String>("token" to token, "macs" to mac)
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
}
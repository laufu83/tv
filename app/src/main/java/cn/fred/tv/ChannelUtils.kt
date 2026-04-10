package cn.fred.tv

import android.content.Context
import android.util.Log
import cn.fred.tv.data.Channel
import cn.fred.tv.network.SSLSocketClient
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object ChannelUtils {

    private const val TAG = "ChannelUtils"
    val channelSet = linkedSetOf<Channel>()

    suspend fun updateChannel(context: Context): Boolean {
        channelSet.clear()
        parseChannel(context)
        return channelSet.isNotEmpty()
    }
    /**
     * 从 assets 文件夹中按行读取文本文件。
     *
     * @param fileName assets 文件夹中的文件名，例如 "lines.txt"。
     * @return 一个包含文件每一行内容的 List<String>，如果文件不存在或读取失败，则返回 null。
     */
    fun Context.readAssetFileByLines(fileName: String): List<String>? {
        return try {
            assets.open(fileName)
                .bufferedReader() // 获取 BufferedReader
                .use { it.readLines() } // 使用 use 确保流被关闭，并读取所有行到 List
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    suspend fun parseChannel(context: Context) {
        return withContext(Dispatchers.IO) {
            val file = getChannelFile(context)
            if (file == null) {
                Log.e(TAG, "parseChannel: iptv file not exist")
                return@withContext
            }
            val lines = file.readLines()
            if (!lines[0].contains("#EXTM3U")) {
                Log.e(TAG, "parseChannel: Non standard m3u8 file, parsing error")
                return@withContext
            }
            var id = ""
            var name = ""
            var group = ""
            var logo = ""
            lines.forEach { line ->
                if (line.startsWith("#EXTINF")) {
                    line.split(" ")
                    Regex("tvg-id=\"([^\"]+)\"").find(line)?.groups?.get(1)?.let {
                        id = it.value
                    }
                    Regex("tvg-logo=\"(.*?)\"").find(line)?.groups?.get(1)?.let {
                        logo = it.value
                    }
                    Regex("group-title=\"([^\"]*)\"").find(line)?.groups?.get(1)?.let {
                        group = it.value
                    }
                    name = line.substring(line.indexOf(",") + 1)
                } else if (line.startsWith("http")) {
                    channelSet.add(Channel(id, name, group, logo, url = line))
                }
            }
        }
    }

    private fun getChannelFile(context: Context): File? {
        val file = File(context.filesDir, Constant.FILE_NAME)
        return if (!file.exists()) {
            file.createNewFile()
            // 读取默认配置
            context.assets.open(Constant.FILE_NAME)
                .bufferedReader() // 获取 BufferedReader
                .use {
                    file.writeText(it.readText())}
            file
            // if (downloadIPTVFile(file)) file else null
        } else {
            if (MMKV.defaultMMKV().decodeBool(Constant.KEY_AUTO_UPDATE, true)) {
                val lastModified = file.lastModified()
                if (System.currentTimeMillis() - lastModified > 24 * 60 * 60 * 1000) {
                    try {
                        if (downloadIPTVFile(file)) {
                            Log.e(TAG, "getChannelFile update iptv file success")
                        } else {
                            Log.e(TAG, "getChannelFile update iptv file fail")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG, "auto update iptv file fail")
                    }
                }
            }
            file
        }
    }

    fun downloadIPTVFile(file: File): Boolean {
        val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(false)
                .sslSocketFactory(
                    SSLSocketClient.getSSLSocketFactory(),
                    SSLSocketClient.getTrustManager() // 显式传入 TrustManager
                )
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier()).build()
        //.addInterceptor(RetryInterceptor())
       val iptvUrl: String = MMKV.defaultMMKV().decodeString(Constant.IPTV_URL, Constant.IPTV_URL_DEFAULT).toString()
        Log.i(TAG,"use iptvUrl ${iptvUrl}")
        return client.newCall(Request.Builder().url(iptvUrl).build()).execute().use {
            if (it.isSuccessful) {
                file.createNewFile()
                it.body.let { body ->
                    file.writeText(body.string())
                }
                true
            } else {
                Log.e(TAG, "download iptv file fail${it.message}}")
                false
            }
        }
    }
}
package cn.fred.tv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import cn.fred.tv.data.DownloadStatus
import cn.fred.tv.data.VersionInfo
import cn.fred.tv.network.SSLSocketClient
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit

object AppUtils {
    private const val TAG = "AppUtils"

    fun getAppVersionName(context: Context): String? {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            Log.e(TAG, "getAppVersion: error:${e.message}")
            ""
        }
    }

    fun getAppVersionCode(context: Context): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAppVersionCode: error:${e.message}")
            0
        }
    }

    suspend  fun getVersion(): VersionInfo? {
        try {
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
            Log.i(TAG,"use version Url ${Constant.VERSION_URL}")
            client.newCall(Request.Builder().url(Constant.VERSION_URL).build()).execute().use {
                if (it.isSuccessful) {
                    it.body?.string()?.let { json ->
                      return  Gson().fromJson(json, VersionInfo::class.java).also {
                           if (it.iptv.isNotBlank()) {
                               Log.i(TAG, "get iptv url ${it.iptv}")
                               MMKV.defaultMMKV().encode(Constant.IPTV_URL, it.iptv)
                           }
                       }

                    }
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "checkVersion error:${e.message}")
        }
        return null
    }

    fun download(dir: File, versionInfo: VersionInfo?): Flow<DownloadStatus> {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        return flow {
            val client = OkHttpClient.Builder().build()
            val url = versionInfo?.url ?: ""
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.let { body ->
                        val totalLength = body.contentLength()
                        inputStream = body.byteStream()
                        val file = File(dir, Constant.APK_NAME)
                        if (file.exists()) {
                            file.delete()
                        }
                        outputStream = FileOutputStream(file)
                        val buffer = ByteArray(8 * 1024)
                        var len: Int
                        var curLength = 0
                        var progress = 0
                        while ((inputStream!!.read(buffer).also { len = it } != -1)) {
                            outputStream!!.write(buffer, 0, len)
                            curLength += len
                            val curProgress = (curLength * 100 / totalLength).toInt()
                            if (curProgress - progress >= 1) {
                                progress = curProgress
                                emit(DownloadStatus.DownLoading(progress))
                            }
                        }
                        outputStream!!.flush()
                    }
                } else {
                    Log.e(TAG, "请求失败,error code:${response.code}")
                    emit(DownloadStatus.Fail(response.code))
                }
            }
        }.onCompletion {
            Log.e(TAG, "download: onCompletion")
            inputStream?.close()
            outputStream?.close()
            delay(1000)
            val file = File(dir, Constant.APK_NAME)
            if (file.exists()) {
                val md5 = getMD5(file)
                if (md5 != versionInfo?.md5) {
                    Log.e(TAG, "download: md5:$md5,remote md5:${versionInfo?.md5}")
                    emit(DownloadStatus.Fail(-1))
                } else {
                    emit(DownloadStatus.Success)
                }
            }
        }.catch { error ->
            Log.e(TAG, "download: catch error:${error.message}")
            inputStream?.close()
            outputStream?.close()
            emit(DownloadStatus.Error(error))
        }.flowOn(Dispatchers.IO)
    }

    fun installApk(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
        val file = File(context.externalCacheDir, Constant.APK_NAME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.setDataAndType(
                FileProvider.getUriForFile(context, "com.ww.simpletv.provider", file),
                "application/vnd.android.package-archive"
            )
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun getMD5(file: File): String {
        var inputStream: FileInputStream? = null
        return try {
            MessageDigest.getInstance("MD5").run {
                reset()
                inputStream = FileInputStream(file)
                val buffer = ByteArray(8 * 1024)
                var len: Int
                while ((inputStream!!.read(buffer).also { len = it }) != -1) {
                    update(buffer, 0, len)
                }
                digest().toHexString()
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "getMD5: error:${e.message}")
            ""
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "getMD5: error:${e.message}")
            ""
        } finally {
            inputStream?.close()
        }
    }

    fun setFontScale(context: Context?, fontScale: Float) {
        val resources = context?.resources
        resources?.let {
            val configuration = it.configuration
            configuration.fontScale = fontScale
            context.createConfigurationContext(configuration)
        }
    }

    fun recreateActivity(activity: Activity) {
        activity.recreate()
    }
}
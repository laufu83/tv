package cn.fred.tv.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        Log.e("RetryInterceptor", "intercept：download github iptv file fail, retry")
        val request = chain.request()
        val url = "https://raw.githubusercontent.com/laufu83/test/refs/heads/main/jx_iptv_m3u.txt"//API.URL_IPTV
        val newRequest = request.newBuilder().url(url).build()
        return chain.proceed(newRequest)
    }
}
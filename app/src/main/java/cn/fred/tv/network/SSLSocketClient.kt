package cn.fred.tv.network

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

object SSLSocketClient {

    /**
     * 获取不校验证书的 SSLSocketFactory
     */
    fun getSSLSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("SSL")
        // 初始化 SSLContext，传入信任所有证书的 TrustManager
        sslContext.init(null, arrayOf(getTrustManager()), SecureRandom())
        return sslContext.socketFactory
    }

    /**
     * 获取信任所有证书的 X509TrustManager
     * 用于解决 sslSocketFactory 的弃用警告
     */
    fun getTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // 忽略客户端证书校验
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // 忽略服务端证书校验（核心：留空即表示信任所有）
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }

    /**
     * 获取信任所有域名的 HostnameVerifier
     */
    fun getHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { _, _ -> true }
    }
}
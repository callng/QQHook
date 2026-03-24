package moe.ore.android.util

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Arrays
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object HttpUtil {
    private val JSON: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!
    private const val DefaultUserAgent =
        "Mozilla/5.0 (Linux; Android 11; M2002J9E Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045514 Mobile Safari/537.36 V1_AND_SQ_8.5.5_1630_YYB_D A_8050500 QQ/8.5.5.5105 NetType/WIFI WebP/0.3.0 Pixel/1080 StatusBarHeight/69 SimpleUISwitch/0 QQTheme/1000 InMagicWin/0"
    private val client = OkHttpClient.Builder().also {
        it.connectTimeout(30, TimeUnit.SECONDS)
        it.readTimeout(60, TimeUnit.SECONDS)
        it.writeTimeout(60, TimeUnit.SECONDS)
        it.sslSocketFactory(
            SSLSocketClient.getSSLSocketFactory()!!,
            SSLSocketClient.getX509TrustManager()!!
        )
        it.hostnameVerifier(SSLSocketClient.getHostnameVerifier())
        // it.proxy(Proxy(Proxy.Type.DIRECT, null))
    }.build()

    fun postJson(url: String, json: String) {
        val body = json.toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })
    }

    fun doGet(
        url: String,
        receiveResult: Boolean = true
    ): ByteArray? {
        try {
            val request: Request = Request.Builder()
                .header("User-Agent", DefaultUserAgent)
                .url(url)
                .get()
                .build()
            val result = client.newCall(request).execute()
            if (!receiveResult) result.close()
            else if (result.code == 200) {
                result.use { return it.body?.bytes() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun download(url: String, path: String) {
        try {
            val request: Request = Request.Builder()
                .header("User-Agent", DefaultUserAgent)
                .url(url)
                .get()
                .build()
            client.newCall(request).execute().use {
                if (it.code == 200) {
                    val input = it.body!!.byteStream()

                    val f = File(path)
                    if (!f.exists()) {
                        f.parentFile.let { if (!it.exists()) it.mkdirs() }
                        f.createNewFile()
                    }

                    val file = FileOutputStream(f)
                    input.use {
                        file.write(input.readBytes())
                    }
                    file.flush()
                    file.close()
                }
                it.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private object SSLSocketClient {
        fun getSSLSocketFactory(): SSLSocketFactory? {
            return try {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, getTrustManager(), SecureRandom())
                sslContext.socketFactory
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        private fun getTrustManager(): Array<TrustManager> {
            return arrayOf(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            })
        }

        fun getHostnameVerifier(): HostnameVerifier {
            return HostnameVerifier { _: String?, _: SSLSession? -> true }
        }

        fun getX509TrustManager(): X509TrustManager? {
            var trustManager: X509TrustManager? = null
            try {
                val trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(null as KeyStore?)
                val trustManagers = trustManagerFactory.trustManagers
                check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                    "Unexpected default trust managers:" + Arrays.toString(trustManagers)
                }
                trustManager = trustManagers[0] as X509TrustManager
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return trustManager
        }
    }
}
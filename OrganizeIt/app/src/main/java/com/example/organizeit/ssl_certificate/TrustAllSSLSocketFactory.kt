package com.example.organizeit.ssl_certificate

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.SSLSocketFactory

object TrustAllSSLSocketFactory {
    fun getSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("SSL")
        val trustAll = arrayOf<TrustManager>(TrustAllCertificates())
        sslContext.init(null, trustAll, java.security.SecureRandom())
        return sslContext.socketFactory
    }
}

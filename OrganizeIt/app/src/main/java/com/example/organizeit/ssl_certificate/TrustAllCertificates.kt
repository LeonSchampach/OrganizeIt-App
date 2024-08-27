package com.example.organizeit.ssl_certificate

import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class TrustAllCertificates : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // Trust all client certificates
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // Trust all server certificates
    }

    override fun getAcceptedIssuers(): Array<X509Certificate>? {
        return arrayOf()
    }
}

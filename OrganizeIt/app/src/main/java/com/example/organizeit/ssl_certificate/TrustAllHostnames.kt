package com.example.organizeit.ssl_certificate

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

class TrustAllHostnames : HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        // Accept all hostnames
        return true
    }
}

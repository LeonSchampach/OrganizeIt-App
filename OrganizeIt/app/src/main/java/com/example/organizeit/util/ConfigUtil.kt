package com.example.organizeit.util

import android.content.Context
import java.io.IOException
import java.util.*

object ConfigUtil {

    private const val PROPERTIES_FILE = "config.properties"

    fun getApiBaseUrl(context: Context): String {
        val properties = Properties()
        try {
            val inputStream = context.assets.open(PROPERTIES_FILE)
            properties.load(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return properties.getProperty("api_base_url", "http://192.168.1.4:8080")
    }

    fun isSecure(context: Context): Boolean {
        val properties = Properties()
        try {
            val inputStream = context.assets.open(PROPERTIES_FILE)
            properties.load(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return properties.getProperty("secure", "false").toBoolean()
    }
}

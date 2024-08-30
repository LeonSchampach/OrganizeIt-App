package com.example.organizeit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.MainActivity.Companion
import com.example.organizeit.adapters.MoveItemAdapter
import com.example.organizeit.adapters.ShelfAdapter
import com.example.organizeit.interfaces.OnDrawerClickListener
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.ssl_certificate.TrustAllCertificates
import com.example.organizeit.ssl_certificate.TrustAllHostnames
import com.example.organizeit.ssl_certificate.TrustAllSSLSocketFactory
import com.example.organizeit.util.ConfigUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MoveItemActivity : AppCompatActivity(), OnDrawerClickListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var secure = false

    private lateinit var recyclerView: RecyclerView
    private lateinit var shelfAdapter: MoveItemAdapter
    private val shelfList = mutableListOf<Shelf>()
    private var userId: Long = -1L
    private var shelfListId: Long = -1L
    private var shelfListName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_move_item)

        secure = ConfigUtil.isSecure(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener { finish() }

        title = getString(R.string.headline_select_drawer)

        recyclerView = findViewById(R.id.recyclerView)
        shelfAdapter = MoveItemAdapter(shelfList, this)
        recyclerView.adapter = shelfAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
        userId = sharedPreferences.getLong("id", -1L)
        shelfListId = sharedPreferences.getLong("shelfListId", -1L)
        shelfListName = sharedPreferences.getString("shelfListName", null)

        if (shelfListName != null)
            title = shelfListName

        fetchShelves()
    }

    override fun onResume() {
        super.onResume()
        if ((application as OrganizeItApplication).isAppInBackground) {
            // Launch the MainActivity and finish the current activity
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    override fun onDrawerClick(drawer: Drawer) {
        val resultIntent = Intent()
        resultIntent.putExtra("id", drawer.id)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun register() {
        val client = if (secure) OkHttpClient.Builder()
            .sslSocketFactory(TrustAllSSLSocketFactory.getSocketFactory(), TrustAllCertificates())
            .hostnameVerifier(TrustAllHostnames())
            .build() else OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/user/register"
        val request = Request.Builder()
            .url(apiUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MoveItemActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error registering", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Log.d(TAG, "Successfully registered")
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        val id = jsonObject.getInt("id")
                        runOnUiThread {
                            val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            if (id != null) {
                                editor.putInt("id", id)
                            }
                            editor.apply()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MoveItemActivity,
                            "Failed to register",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Failed to register: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    private fun fetchShelves() {
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/shelf/getShelvesByShelfListId?shelfListId="+shelfListId
        val client = if (secure) OkHttpClient.Builder()
            .sslSocketFactory(TrustAllSSLSocketFactory.getSocketFactory(), TrustAllCertificates())
            .hostnameVerifier(TrustAllHostnames())
            .build() else OkHttpClient()
        val request = Request.Builder()
            .url(apiUrl)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MoveItemActivity, "Error fetching shelves", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error fetching shelves", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MoveItemActivity, "Error fetching shelves", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error fetching shelves: ${response.code}")
                    }
                } else {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val shelfList = parseShelves(responseBody)
                        runOnUiThread {
                            shelfAdapter.setShelves(shelfList)
                        }
                    }
                }
            }
        })
    }

    private fun parseShelves(jsonString: String): List<Shelf> {
        val jsonArray = JSONArray(jsonString)
        val shelves = mutableListOf<Shelf>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.optLong("id", -1L).takeIf { it != -1L }
            val name = jsonObject.getString("name")
            val room = jsonObject.getString("room")
            val drawersJsonArray = jsonObject.getJSONArray("drawers")

            val drawers = mutableListOf<Drawer>()
            for (j in 0 until drawersJsonArray.length()) {
                val drawerJsonObject = drawersJsonArray.getJSONObject(j)
                val drawerId = drawerJsonObject.optLong("id", -1L).takeIf { it != -1L }
                val drawerName = drawerJsonObject.getString("name")
                val drawerOrder = drawerJsonObject.getInt("order")
                val shelfId = drawerJsonObject.optLong("shelfId")
                drawers.add(Drawer(drawerId, drawerName, drawerOrder, shelfId))
            }

            shelves.add(Shelf(id, name, room, drawers, false))
        }

        return shelves
    }
}
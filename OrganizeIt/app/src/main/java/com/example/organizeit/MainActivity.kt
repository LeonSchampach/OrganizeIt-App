package com.example.organizeit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.DrawerDetailActivity.Companion
import com.example.organizeit.adapters.ShelfAdapter
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.network.RetrofitClient
import com.example.organizeit.models.ShelfRequest
import com.example.organizeit.models.DrawerRequest
import com.example.organizeit.models.Item
import com.example.organizeit.util.ConfigUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var shelfAdapter: ShelfAdapter
    private val shelfList = mutableListOf<Shelf>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        shelfAdapter = ShelfAdapter(shelfList, this)
        recyclerView.adapter = shelfAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchShelves()

        // Floating action button click listener for adding a new shelf
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            showAddShelfDialog()
        }
    }

    private fun showAddShelfDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_add_shelf, null)

        val shelfNameInput = view.findViewById<EditText>(R.id.shelfNameInput)
        val shelfRoomInput = view.findViewById<EditText>(R.id.shelfRoomInput)
        val drawerContainer = view.findViewById<LinearLayout>(R.id.drawerContainer)
        val addDrawerButton = view.findViewById<Button>(R.id.addDrawerButton)

        addDrawerButton.setOnClickListener {
            addDrawerInput(drawerContainer)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_shelf))
            .setView(view)
            .setPositiveButton(getString(R.string.saveBtn)) { _, _ ->
                val name = shelfNameInput.text.toString().trim()
                val room = shelfRoomInput.text.toString().trim()
                if (name.isNotEmpty() && room.isNotEmpty()) {
                    val drawers = mutableListOf<DrawerRequest>()
                    for (i in 0 until drawerContainer.childCount) {
                        val drawerName = (drawerContainer.getChildAt(i) as EditText).text.toString().trim()
                        if (drawerName.isNotEmpty()) {
                            drawers.add(DrawerRequest(drawerName))
                        }
                    }
                    addShelf(name, room, drawers)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancelBtn), null)
            .create()

        dialog.show()
    }

    private fun addDrawerInput(container: LinearLayout) {
        val drawerInput = EditText(this)
        drawerInput.hint = "Drawer Name"
        container.addView(drawerInput)
    }

    private fun parseShelf(jsonString: String): Shelf {
        val jsonObject = JSONObject(jsonString)
        val drawersJsonArray = jsonObject.getJSONArray("drawers")

        val drawersList = mutableListOf<Drawer>()

        for (i in 0 until drawersJsonArray.length()) {
            val drawerJson = drawersJsonArray.getJSONObject(i)
            val drawer = Drawer(
                id = drawerJson.getInt("id"),
                name = drawerJson.getString("name"),
                shelfId = drawerJson.getInt("shelfId")
            )
            drawersList.add(drawer)
        }

        val shelf = Shelf(
            id = jsonObject.getInt("id"),
            name = jsonObject.getString("name"),
            room = jsonObject.getString("room"),
            drawers = drawersList
        )

        return shelf
    }

    private fun addShelf(name: String, room: String, drawers: List<DrawerRequest>) {
        val jsonArray = JSONArray()
        for (drawer in drawers) {
            val drawerJson = JSONObject()
            drawerJson.put("name", drawer.name)
            jsonArray.put(drawerJson)
        }
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("room", room)
        jsonObject.put("drawers", jsonArray)

        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/api/createShelf"
        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error adding shelf", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Shelf added successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "Shelf added successfully")
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val newShelf = parseShelf(responseBody)
                        runOnUiThread {
                            shelfAdapter.addShelf(newShelf)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to add shelf",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Failed to add shelf: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    private fun fetchShelves() {
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/api/getAllShelf"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(apiUrl)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error fetching shelves", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "Error fetching shelves", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error fetching shelves", Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Error fetching shelves: ${response.code}")
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
            val id = jsonObject.optInt("id", -1).takeIf { it != -1 }
            val name = jsonObject.getString("name")
            val room = jsonObject.getString("room")
            val drawersJsonArray = jsonObject.getJSONArray("drawers")

            val drawers = mutableListOf<Drawer>()
            for (j in 0 until drawersJsonArray.length()) {
                val drawerJsonObject = drawersJsonArray.getJSONObject(j)
                val drawerId = drawerJsonObject.optInt("id", -1).takeIf { it != -1 }
                val drawerName = drawerJsonObject.getString("name")
                val shelfId = drawerJsonObject.optInt("shelfId")
                drawers.add(Drawer(drawerId, drawerName, shelfId))
            }

            shelves.add(Shelf(id, name, room, drawers, false))
        }

        return shelves
    }

}

package com.example.organizeit

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.adapters.ShelfAdapter
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.DrawerRequest
import com.example.organizeit.models.Item
import com.example.organizeit.models.Shelf
import com.example.organizeit.models.ShelfList
import com.example.organizeit.util.ConfigUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity(), ShelfAdapter.OnItemLongClickListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var shelfAdapter: ShelfAdapter
    private val shelfList = mutableListOf<Shelf>()
    private var userId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = getString(R.string.headline_shelves)

        recyclerView = findViewById(R.id.recyclerView)
        shelfAdapter = ShelfAdapter(shelfList, this, this)
        recyclerView.adapter = shelfAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", -1)

        if (userId == -1) {
            register()
        }

        fetchShelves()

        // Floating action button click listener for adding a new shelf
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            showAddShelfDialog()
        }
    }

    override fun onItemLongClick(shelf: Shelf) {
        showEditShelfDialog(shelf)
    }

    private fun register() {
        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/user/register"
        val request = Request.Builder()
            .url(apiUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error registering", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Successfully registered",
                            Toast.LENGTH_SHORT
                        ).show()
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
                            this@MainActivity,
                            "Failed to register",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Failed to register: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    private fun showAddShelfDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_add_shelf, null)

        val shelfNameInput = view.findViewById<EditText>(R.id.shelfNameInput)
        val shelfRoomInput = view.findViewById<EditText>(R.id.shelfRoomInput)
        val drawerContainerNew = view.findViewById<LinearLayout>(R.id.drawerContainerNew)
        val addDrawerButton = view.findViewById<Button>(R.id.addDrawerButton)

        addDrawerButton.setOnClickListener {
            addDrawerInput(drawerContainerNew)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_shelf))
            .setView(view)
            .setPositiveButton(getString(R.string.saveBtn)) { _, _ ->
                val name = shelfNameInput.text.toString().trim()
                val room = shelfRoomInput.text.toString().trim()
                if (name.isNotEmpty() && room.isNotEmpty()) {
                    val drawers = mutableListOf<DrawerRequest>()
                    for (i in 0 until drawerContainerNew.childCount) {
                        val drawerName = ((drawerContainerNew.getChildAt(i) as LinearLayout).getChildAt(0) as EditText).text.toString().trim()
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

    private fun showEditShelfDialog(shelf: Shelf) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_add_shelf, null)

        val shelfNameInput = view.findViewById<EditText>(R.id.shelfNameInput)
        val shelfRoomInput = view.findViewById<EditText>(R.id.shelfRoomInput)
        val drawerContainer = view.findViewById<LinearLayout>(R.id.drawerContainer)
        val drawerContainerNew = view.findViewById<LinearLayout>(R.id.drawerContainerNew)
        val addDrawerButton = view.findViewById<Button>(R.id.addDrawerButton)

        shelfNameInput.setText(shelf.name)
        shelfRoomInput.setText(shelf.room)

        for (drawer: Drawer in shelf.drawers) {
            editDrawerInput(drawerContainer, drawer)
        }

        addDrawerButton.setOnClickListener {
            addDrawerInput(drawerContainerNew)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_shelf))
            .setView(view)
            .setPositiveButton(getString(R.string.saveBtn)) { _, _ ->
                val name = shelfNameInput.text.toString().trim()
                val room = shelfRoomInput.text.toString().trim()
                if (name.isNotEmpty() && room.isNotEmpty()) {
                    val drawers = mutableListOf<Drawer>()
                    for (i in 0 until drawerContainer.childCount) {
                        val drawerName = ((drawerContainer.getChildAt(i) as LinearLayout).getChildAt(0) as EditText).text.toString().trim()
                        if (drawerName.isNotEmpty()) {
                            drawers.add(Drawer(shelf.drawers[i].id, drawerName, shelf.drawers[i].shelfId))
                        }
                    }
                    for (i in 0 until drawerContainerNew.childCount) {
                        val drawerName = ((drawerContainerNew.getChildAt(i) as LinearLayout).getChildAt(0) as EditText).text.toString().trim()
                        if (drawerName.isNotEmpty()) {
                            drawers.add(Drawer(null, drawerName, null))
                        }
                    }
                    shelf.id?.let { updateShelf(it, name, room, drawers, shelf) }
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancelBtn), null)
            .create()

        dialog.show()
    }

    private fun addDrawerInput(container: LinearLayout) {
        val inflater = LayoutInflater.from(this)
        val drawerInputLayout = inflater.inflate(R.layout.item_drawer_input, container, false)
        val drawerName = drawerInputLayout.findViewById<EditText>(R.id.drawerName)
        drawerName.hint = getString(R.string.drawer_name)

        val deleteButton = drawerInputLayout.findViewById<ImageButton>(R.id.buttonDeleteDrawerInput)
        deleteButton.setOnClickListener {
            container.removeView(drawerInputLayout)
        }

        container.addView(drawerInputLayout)
    }
    private fun editDrawerInput(container: LinearLayout, drawer: Drawer) {
        val inflater = LayoutInflater.from(this)
        val drawerInputLayout = inflater.inflate(R.layout.item_drawer_input, container, false)
        val drawerName = drawerInputLayout.findViewById<EditText>(R.id.drawerName)
        drawerName.setText(drawer.name)

        val deleteButton = drawerInputLayout.findViewById<ImageButton>(R.id.buttonDeleteDrawerInput)
        deleteButton.setOnClickListener {
            deleteDrawer(drawer, this@MainActivity)
            container.removeView(drawerInputLayout)
        }

        container.addView(drawerInputLayout)
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
        val shelfListId = 1

        val jsonArray = JSONArray()
        for (drawer in drawers) {
            val drawerJson = JSONObject()
            drawerJson.put("name", drawer.name)
            jsonArray.put(drawerJson)
        }
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("room", room)
        jsonObject.put("shelfListId", shelfListId)
        jsonObject.put("drawers", jsonArray)

        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/shelf/createShelf"
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

    private fun updateShelf(id: Int, name: String, room: String, drawers: MutableList<Drawer>, oldShelf: Shelf) {
        val shelfListId = 1

        val jsonArray = JSONArray()
        for (drawer in drawers) {
            if (drawer.id == null) {
                val drawerJson = JSONObject()
                drawerJson.put("name", drawer.name)
                jsonArray.put(drawerJson)
            }
            else {
                val drawerJson = JSONObject()
                drawerJson.put("id", drawer.id)
                drawerJson.put("name", drawer.name)
                drawerJson.put("shelfId", drawer.shelfId)
                jsonArray.put(drawerJson)
            }

        }
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("name", name)
        jsonObject.put("room", room)
        jsonObject.put("shelfListId", shelfListId)
        jsonObject.put("drawers", jsonArray)

        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/shelf/updateShelf"
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
                    Log.e(TAG, "Error updating shelf", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Shelf updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "Shelf updated successfully")
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val newShelf = parseShelf(responseBody)
                        runOnUiThread {
                            shelfAdapter.updateShelf(oldShelf, newShelf)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to update shelf",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Failed to update shelf: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    private fun fetchShelves() {
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/shelf/getAllShelf"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(apiUrl)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error fetching shelves", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error fetching shelves", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error fetching shelves", Toast.LENGTH_SHORT).show()
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

    private fun deleteDrawer(drawer: Drawer, context: Context) {
        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/drawer/deleteDrawer?id=${drawer.id}"

        val request = Request.Builder()
            .url(apiUrl)
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? MainActivity)?.runOnUiThread {
                    Toast.makeText(context, "Error deleting drawer", Toast.LENGTH_SHORT).show()
                    Log.e("ShelfAdapter", "Error deleting drawer", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    (context as? MainActivity)?.runOnUiThread {
                        Toast.makeText(context, "Error deleting drawer", Toast.LENGTH_SHORT).show()
                        Log.e("ShelfAdapter", "Error deleting drawer: ${response.code}")
                    }
                } else {
                    (context as? MainActivity)?.runOnUiThread {
                        Toast.makeText(context, "Drawer deleted successfully", Toast.LENGTH_SHORT).show()
                        shelfAdapter.removeDrawer(drawer)
                    }
                }
            }
        })
    }

}

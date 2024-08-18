package com.example.organizeit

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.adapters.DrawerAdapter
import com.example.organizeit.adapters.ShelfAdapter
import com.example.organizeit.interfaces.MenuVisibilityListener
import com.example.organizeit.interfaces.ShelfSelectionListener
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.models.ShelfList
import com.example.organizeit.util.ConfigUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
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

class MainActivity : AppCompatActivity(), MenuVisibilityListener, ShelfSelectionListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var shelfAdapter: ShelfAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private val shelfList = mutableListOf<Shelf>()
    private var selectedShelves: List<Shelf> = emptyList()
    private var userId = -1
    private var shelfListId = -1
    private var isComingFromBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.navigation_view)

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_item1 -> {
                    // Handle navigation to Item 1
                }
                R.id.nav_item2 -> {
                    // Handle navigation to Item 2
                }
                else -> {
                    shelfListId = it.itemId

                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putInt("shelfListId", shelfListId)
                    editor.apply()

                    fetchShelves()
                }
                // Add more cases as needed
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        title = getString(R.string.headline_shelves)

        recyclerView = findViewById(R.id.recyclerView)
        shelfAdapter = ShelfAdapter(shelfList, this, this, this)
        recyclerView.adapter = shelfAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
        userId = sharedPreferences.getInt("id", -1)
        shelfListId = sharedPreferences.getInt("shelfListId", -1)

        if (userId == -1) {
            register()
        }
        else
            fetchShelfLists()

        // Floating action button click listener for adding a new shelf
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            showAddShelfDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        if ((application as OrganizeItApplication).isAppInBackground) {
            // This method is called only when the app is returning from the background
            onAppReturnedToForeground()
            (application as OrganizeItApplication).isAppInBackground = false
        }
    }

    private fun onAppReturnedToForeground() {
        fetchShelves()
    }

    override fun onShelvesSelected(selectedShelves: List<Shelf>) {
        this.selectedShelves = selectedShelves
    }

    override fun showMenuItems() {
        toolbar.menu.findItem(R.id.action_more).isVisible = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { hideCheckboxes() }
    }

    private fun hideCheckboxes() {
        shelfAdapter.setAllCheckboxesVisible(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        toolbar.menu.findItem(R.id.action_more).isVisible = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_items, menu)
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_more -> {
                // Handle more options click
                showOptionsMenu()
                true
            }
            R.id.action_drawer -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                } else {
                    drawerLayout.openDrawer(GravityCompat.END)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    private fun showOptionsMenu() {
        val popup = PopupMenu(this, findViewById(R.id.action_more))
        popup.menuInflater.inflate(R.menu.menu_popup, popup.menu)
        popup.menu.findItem(R.id.move).isVisible = false

        if (selectedShelves.size == 1) {
            popup.menu.findItem(R.id.edit).isVisible = true
        } else {
            popup.menu.findItem(R.id.edit).isVisible = false
        }

        if (selectedShelves.isNotEmpty()) {
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.edit -> {
                        handleOptionSelection(1)
                        true
                    }
                    R.id.delete -> {
                        handleOptionSelection(3)
                        true
                    }
                    else -> false
                }
            }
        }

        popup.show()
    }

    private fun handleOptionSelection(option: Int) {
        // Handle the selected items based on the selected option
        when (option) {
            1 -> {
                showEditShelfDialog(selectedShelves[0])
            }
            3 -> {
                val shelves = mutableListOf<Shelf>()
                for (shelf in selectedShelves) {
                    shelves.add(shelf)
                }
                for (shelf in shelves) {
                    shelfAdapter.deleteShelf(shelf, this)
                }
            }
        }
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

                            fetchShelfLists()
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
        val drawerContainerNew = view.findViewById<RecyclerView>(R.id.drawerContainerNew)
        val addDrawerButton = view.findViewById<Button>(R.id.addDrawerButton)

        val drawers = mutableListOf<Drawer>()
        val adapter = DrawerAdapter(drawers, this)
        drawerContainerNew.layoutManager = LinearLayoutManager(this)
        drawerContainerNew.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not needed since we don't want swipe functionality
            }
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(drawerContainerNew)

        addDrawerButton.setOnClickListener {
            adapter.addDrawer(Drawer(null, "", 1, null))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_shelf))
            .setView(view)
            .setPositiveButton(getString(R.string.saveBtn)) { dialog, _ ->
                val name = shelfNameInput.text.toString().trim()
                val room = shelfRoomInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val updatedDrawers = adapter.getDrawers().mapIndexed { index, drawer ->
                        drawer.copy(order = index)  // Update the order of each drawer based on its position
                    }
                    addShelf(name, room, updatedDrawers)
                } else {
                    Toast.makeText(this, "Bitte geben Sie einen Namen ein", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                hideCheckboxes()
            }
            .setNegativeButton(getString(R.string.cancelBtn)) { dialog, _ ->
                dialog.dismiss()
                shelfAdapter.uncheckAllCheckboxes()
            }
            .create()

        dialog.show()
    }

    private fun showEditShelfDialog(shelf: Shelf) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_add_shelf, null)

        val shelfNameInput = view.findViewById<EditText>(R.id.shelfNameInput)
        val shelfRoomInput = view.findViewById<EditText>(R.id.shelfRoomInput)
        val drawerContainerNew = view.findViewById<RecyclerView>(R.id.drawerContainerNew)
        val addDrawerButton = view.findViewById<Button>(R.id.addDrawerButton)

        val drawers = shelf.drawers.toMutableList()
        val adapter = DrawerAdapter(drawers, this)
        drawerContainerNew.layoutManager = LinearLayoutManager(this)
        drawerContainerNew.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not needed since we don't want swipe functionality
            }
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(drawerContainerNew)

        shelfNameInput.setText(shelf.name)
        shelfRoomInput.setText(shelf.room)

        /*for (drawer: Drawer in shelf.drawers) {
            adapter.addDrawer(Drawer(null, "", 1, null))
        }*/

        addDrawerButton.setOnClickListener {
            adapter.addDrawer(Drawer(null, "", 1, null))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_shelf))
            .setView(view)
            .setPositiveButton(getString(R.string.saveBtn)) { dialog, _ ->
                val name = shelfNameInput.text.toString().trim()
                val room = shelfRoomInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val updatedDrawers = adapter.getDrawers().mapIndexed { index, drawer ->
                        drawer.copy(order = index)  // Update the order of each drawer based on its position
                    }
                    shelf.id?.let { updateShelf(it, name, room, updatedDrawers, shelf) }
                } else {
                    Toast.makeText(this, "Der Name darf nicht leer sein", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                hideCheckboxes()
            }
            .setNegativeButton(getString(R.string.cancelBtn)) { dialog, _ ->
                dialog.dismiss()
                shelfAdapter.uncheckAllCheckboxes()
            }
            .create()

        dialog.show()
    }

    /*private fun addDrawerInput(adapter: DrawerAdapter) {
        adapter.addDrawer("")
    }

    private fun editDrawerInput(adapter: DrawerAdapter, drawer: Drawer) {
        /*val inflater = LayoutInflater.from(this)
        val drawerInputLayout = inflater.inflate(R.layout.item_drawer_input, container, false)
        val drawerName = drawerInputLayout.findViewById<EditText>(R.id.drawerName)
        drawerName.setText(drawer.name)

        val deleteButton = drawerInputLayout.findViewById<ImageButton>(R.id.buttonDeleteDrawerInput)
        deleteButton.setOnClickListener {
            deleteDrawer(drawer, this@MainActivity)
            container.removeView(drawerInputLayout)
        }

        container.addView(drawerInputLayout)*/
        adapter.addDrawer(drawer.name)
    }*/

    private fun parseShelf(jsonString: String): Shelf {
        val jsonObject = JSONObject(jsonString)
        val drawersJsonArray = jsonObject.getJSONArray("drawers")

        val drawersList = mutableListOf<Drawer>()

        for (i in 0 until drawersJsonArray.length()) {
            val drawerJson = drawersJsonArray.getJSONObject(i)
            val drawer = Drawer(
                id = drawerJson.getInt("id"),
                name = drawerJson.getString("name"),
                order = drawerJson.getInt("order"),
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

    private fun addShelf(name: String, room: String, drawers: List<Drawer>) {
        val shelfListId = 1

        val jsonArray = JSONArray()
        for (drawer in drawers) {
            val drawerJson = JSONObject()
            drawerJson.put("name", drawer.name)
            drawerJson.put("order", drawer.order)
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

    private fun updateShelf(id: Int, name: String, room: String, drawers: List<Drawer>, oldShelf: Shelf) {
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
                drawerJson.put("order", drawer.order)
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

    /*private fun fetchShelves() {
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/shelf/getAllShelves"
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
    }*/

    private fun fetchShelves() {
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/shelf/getShelvesByShelfListId?shelfListId="+shelfListId
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

    private fun fetchShelfLists() {
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/shelfList/getAllShelfListsByUserId?userId="+userId
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(apiUrl)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error fetching shelfLists", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error fetching shelfLists", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error fetching shelfLists", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error fetching shelfLists: ${response.code}")
                    }
                } else {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val shelfLists = parseShelfLists(responseBody)
                        runOnUiThread {
                            populateMenu(shelfLists)
                        }

                        /*runOnUiThread {
                            shelfAdapter.setShelves(shelfList)
                        }*/
                    }
                }
            }
        })
    }

    private fun populateMenu(shelfLists: List<ShelfList>) {
        val menu = navView.menu
        //menu.clear()  // Clear any existing items in the menu

        for (shelfList in shelfLists) {
            menu.add(Menu.NONE, shelfList.id, Menu.NONE, shelfList.name)
        }
    }

    private fun parseShelfLists(jsonString: String): List<ShelfList> {
        val jsonArray = JSONArray(jsonString)
        val shelfLists = mutableListOf<ShelfList>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val id = jsonObject.getInt("id")
            val name = jsonObject.getString("name")

            shelfLists.add(ShelfList(id, name))
        }

        shelfLists.sortBy { it.name }

        return shelfLists
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
                val drawerOrder = drawerJsonObject.getInt("order")
                val shelfId = drawerJsonObject.optInt("shelfId")
                drawers.add(Drawer(drawerId, drawerName, drawerOrder, shelfId))
            }

            shelves.add(Shelf(id, name, room, drawers, false))
        }

        for (shelf in shelves) {
            shelf.drawers.sortBy { it.order }
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

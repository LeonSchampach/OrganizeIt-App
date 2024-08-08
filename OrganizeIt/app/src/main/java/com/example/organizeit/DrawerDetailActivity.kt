package com.example.organizeit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.adapters.ItemAdapter
import com.example.organizeit.interfaces.ItemSelectionListener
import com.example.organizeit.interfaces.MenuVisibilityListener
import com.example.organizeit.interfaces.OnItemLongClickListener
import com.example.organizeit.models.Item
import com.example.organizeit.models.Drawer
import com.example.organizeit.util.ConfigUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DrawerDetailActivity : AppCompatActivity(), OnItemLongClickListener,
    MenuVisibilityListener, ItemSelectionListener {

    companion object {
        private const val TAG = "DrawerDetailActivity"
    }

    private lateinit var toolbar: Toolbar
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var oldItem: Item
    private lateinit var newItem: Item
    private val itemList = mutableListOf<Item>()
    private var selectedItems: List<Item> = emptyList()
    private var drawerId: Int = -1


    private lateinit var moveItemResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_detail)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // Initially hide the navigation icon

        toolbar.setNavigationOnClickListener {
            hideCheckboxes()
        }

        moveItemResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val drawerId: Int? = data?.getIntExtra("id", -1)

                if (drawerId != null) {
                    for (item: Item in selectedItems) {
                        item.id?.let { moveItem(it, item.name, item.desc, item.quantity, drawerId) }
                    }
                }
                else {
                    Toast.makeText(this@DrawerDetailActivity, "Error moving Item", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error updating item")
                }
            }
            finish()
        }

        val drawer = intent.getSerializableExtra("drawer") as? Drawer
        title = drawer?.name
        drawerId = drawer?.id ?: -1

        searchView = findViewById(R.id.searchView)

        recyclerView = findViewById(R.id.recyclerViewItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemAdapter = ItemAdapter(itemList, this, this, this, this)
        recyclerView.adapter = itemAdapter

        findViewById<FloatingActionButton>(R.id.fabAddItem).setOnClickListener {
            showAddItemDialog()
        }

        drawer?.id?.let {
            fetchItems(it)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Filter the list when the user submits the search query
                /*query?.let {
                    filter(it)
                }*/
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter the list as the user types the search query
                newText?.let {
                    itemAdapter.filter(it)
                }
                return true
            }
        })
    }

    override fun onItemLongClick(item: Item) {
        showEditItemDialog(item)
    }

    override fun onItemsSelected(selectedItems: List<Item>) {
        this.selectedItems = selectedItems
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_items, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_more -> {
                // Handle more options click
                showOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showOptionsMenu() {
        val popup = PopupMenu(this, findViewById(R.id.action_more))
        popup.menuInflater.inflate(R.menu.menu_popup, popup.menu)

        if (selectedItems.size == 1) {
            popup.menu.findItem(R.id.edit).isVisible = true
        } else {
            popup.menu.findItem(R.id.edit).isVisible = false
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    handleOptionSelection(1)
                    true
                }
                R.id.move -> {
                    handleOptionSelection(2)
                    true
                }
                R.id.delete -> {
                    handleOptionSelection(3)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun handleOptionSelection(option: Int) {
        // Handle the selected items based on the selected option
        when (option) {
            1 -> {
                showEditItemDialog(selectedItems[0])
            }
            2 -> {
                val intent = Intent(this, MoveItemActivity::class.java)
                moveItemResultLauncher.launch(intent)
            }
            3 -> {
                for (item in selectedItems) {
                    itemAdapter.deleteItem(item, this, itemAdapter)
                }
            }
        }
    }

    private fun hideCheckboxes() {
        // Hide all checkboxes and set menu items visibility
        itemAdapter.setAllCheckboxesVisible(false)
        toolbar.menu.findItem(R.id.action_more).isVisible = false
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    override fun showMenuItems() {
        toolbar.menu.findItem(R.id.action_more).isVisible = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun fetchItems(drawerId: Int) {
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/item/getItemsByDrawerId?drawerId=$drawerId"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(apiUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DrawerDetailActivity, "Error fetching items", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error fetching items", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@DrawerDetailActivity, "Error fetching items", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error fetching items: ${response.code}")
                    }
                } else {
                    val responseBody = response.body?.string()
                    if (responseBody != null && responseBody != "") {
                        val itemList = parseItems(responseBody)
                        runOnUiThread {
                            itemAdapter.setItems(itemList)
                        }
                    }
                }
            }
        })
    }

    private fun parseItem(jsonString: String): Item {
        val jsonObject = JSONObject(jsonString)
        val item = Item(
            id = jsonObject.getInt("id"),
            name = jsonObject.getString("name"),
            desc = jsonObject.getString("desc"),
            quantity = jsonObject.getDouble("quantity"),
            drawerId = jsonObject.getInt("drawerId")
        )

        return item
    }

    private fun parseItems(jsonString: String): List<Item> {
        val jsonArray = JSONArray(jsonString)
        val itemList = mutableListOf<Item>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val item = Item(
                id = jsonObject.getInt("id"),
                name = jsonObject.getString("name"),
                desc = jsonObject.getString("desc"),
                quantity = jsonObject.getDouble("quantity"),
                drawerId = jsonObject.getInt("drawerId")
            )
            itemList.add(item)
        }

        itemList.sortBy { it.name }

        return itemList
    }

    private fun showAddItemDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.add_item))

        val view = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val nameEditText = view.findViewById<EditText>(R.id.editTextItemName)
        val descEditText = view.findViewById<EditText>(R.id.editTextItemDesc)
        val quantityEditText = view.findViewById<EditText>(R.id.editTextItemQuantity)

        builder.setView(view)

        builder.setPositiveButton(getString(R.string.saveBtn)) { dialog, _ ->
            val name = nameEditText.text.toString()
            val desc = descEditText.text.toString()
            val quantity = quantityEditText.text.toString().toDoubleOrNull() ?: 1.0

            if (name.isNotBlank()) {
                addItem(name, desc, quantity)
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.cancelBtn)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun showEditItemDialog(item: Item) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.edit_item))

        oldItem = item
        newItem = item

        val view = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val nameEditText = view.findViewById<EditText>(R.id.editTextItemName)
        val descEditText = view.findViewById<EditText>(R.id.editTextItemDesc)
        val quantityEditText = view.findViewById<EditText>(R.id.editTextItemQuantity)

        nameEditText.setText(item.name)
        descEditText.setText(item.desc)
        quantityEditText.setText(item.quantity.toString())

        builder.setView(view)

        builder.setPositiveButton(getString(R.string.saveBtn)) { dialog, _ ->
            val name = nameEditText.text.toString()
            val desc = descEditText.text.toString()
            val quantity = quantityEditText.text.toString().toDoubleOrNull() ?: 1.0

            if (name.isNotBlank()) {
                item.id?.let { updateItem(it, name, desc, quantity, item) }
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
            hideCheckboxes()
        }

        builder.setNegativeButton(getString(R.string.cancelBtn)) { dialog, _ ->
            dialog.dismiss()
            hideCheckboxes()
        }

        builder.create().show()
    }

    private fun addItem(name: String, desc: String, quantity: Double) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("name", name)
        jsonObject.put("desc", desc)
        jsonObject.put("quantity", quantity)
        jsonObject.put("drawerId", drawerId)

        Log.i(TAG, jsonObject.toString())

        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/item/createItem"
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonObject.toString())

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DrawerDetailActivity, "Error adding item", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error adding item", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@DrawerDetailActivity, "Error adding item", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error adding item: ${response.code}")
                    }
                } else {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val newItem = parseItem(responseBody)
                        runOnUiThread {
                            itemAdapter.addItem(newItem)
                        }
                    }
                }
            }
        })
    }

    private fun updateItem(id: Int, name: String, desc: String, quantity: Double, oldItem: Item) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("name", name)
        jsonObject.put("desc", desc)
        jsonObject.put("quantity", quantity)
        jsonObject.put("drawerId", drawerId)

        Log.i(TAG, jsonObject.toString())

        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/item/updateItem"
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonObject.toString())

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DrawerDetailActivity, "Error updating item", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error updating item", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@DrawerDetailActivity, "Error updating item", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error updating item: ${response.code}")
                    }
                } else {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val newItem = parseItem(responseBody)
                        runOnUiThread {
                            itemAdapter.updateItem(oldItem, newItem)
                        }
                    }
                }
            }
        })
    }

    private fun moveItem(id: Int, name: String, desc: String, quantity: Double, newDrawerId: Int) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("id", id)
        jsonObject.put("name", name)
        jsonObject.put("desc", desc)
        jsonObject.put("quantity", quantity)
        jsonObject.put("drawerId", newDrawerId)

        Log.i(TAG, jsonObject.toString())

        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/item/updateItem"
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), jsonObject.toString())

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DrawerDetailActivity, "Error updating item", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error updating item", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@DrawerDetailActivity, "Error updating item", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error updating item: ${response.code}")
                    }
                } /*else {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val newItem = parseItem(responseBody)
                        runOnUiThread {
                            itemAdapter.updateItem(oldItem, newItem)
                        }
                    }
                }*/
            }
        })
    }
}

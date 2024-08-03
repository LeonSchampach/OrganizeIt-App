package com.example.organizeit

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.adapters.ItemAdapter
import com.example.organizeit.models.Item
import com.example.organizeit.models.Drawer
import com.example.organizeit.util.ConfigUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DrawerDetailActivity : AppCompatActivity(), ItemAdapter.OnItemLongClickListener {

    companion object {
        private const val TAG = "DrawerDetailActivity"
    }

    private lateinit var itemAdapter: ItemAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private val itemList = mutableListOf<Item>()
    private var drawerId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_detail)

        val drawer = intent.getSerializableExtra("drawer") as? Drawer
        title = drawer?.name
        drawerId = drawer?.id ?: -1

        searchView = findViewById(R.id.searchView)

        recyclerView = findViewById(R.id.recyclerViewItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemAdapter = ItemAdapter(itemList, this, this)
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
            quantity = jsonObject.getInt("quantity"),
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
                quantity = jsonObject.getInt("quantity"),
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
            val quantity = quantityEditText.text.toString().toIntOrNull() ?: 1

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
            val quantity = quantityEditText.text.toString().toIntOrNull() ?: 1

            if (name.isNotBlank()) {
                item.id?.let { updateItem(it, name, desc, quantity, item) }
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

    private fun addItem(name: String, desc: String, quantity: Int) {
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

    private fun updateItem(id: Int, name: String, desc: String, quantity: Int, oldItem: Item) {
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
}

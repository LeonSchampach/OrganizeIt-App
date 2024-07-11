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
import com.example.organizeit.adapters.ShelfAdapter
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.network.RetrofitClient
import com.example.organizeit.models.ShelfRequest
import com.example.organizeit.models.DrawerRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var shelfAdapter: ShelfAdapter
    private val shelfList = mutableListOf<Shelf>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        shelfAdapter = ShelfAdapter(shelfList)
        recyclerView.adapter = shelfAdapter

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
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
            .setTitle("Add Shelf")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
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
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addDrawerInput(container: LinearLayout) {
        val drawerInput = EditText(this)
        drawerInput.hint = "Drawer Name"
        container.addView(drawerInput)
    }

    private fun addShelf(name: String, room: String, drawers: List<DrawerRequest>) {
        val shelfRequest = ShelfRequest(name, room, drawers)
        val call = RetrofitClient.instance.createShelf(shelfRequest)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Shelf added successfully", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Shelf added successfully")
                    val newShelf = Shelf(name, room, drawers.map { Drawer(it.name) })
                    shelfAdapter.addShelf(newShelf)
                } else {
                    Toast.makeText(this@MainActivity, "Failed to add shelf", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to add shelf: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error adding shelf", t)
            }
        })
    }
}

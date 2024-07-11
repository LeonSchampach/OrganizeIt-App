package com.example.organizeit

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.adapters.ShelfAdapter
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.google.android.material.floatingactionbutton.FloatingActionButton

// In MainActivity.kt
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val shelfList = listOf(
            Shelf("1", "Shelf 1", listOf(Drawer("1-1", "Drawer 1"), Drawer("1-2", "Drawer 2"))),
            Shelf("2", "Shelf 2", listOf(Drawer("2-1", "Drawer 1"), Drawer("2-2", "Drawer 2"))),
            // Add more shelves and drawers here
        )

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ShelfAdapter(shelfList)
    }
}


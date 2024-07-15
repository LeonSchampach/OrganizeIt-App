package com.example.organizeit

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.organizeit.models.Drawer

class DrawerDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_detail)

        val drawer = intent.getSerializableExtra("drawer") as Drawer

        val drawerNameTextView: TextView = findViewById(R.id.drawerNameTextView)
        drawerNameTextView.text = drawer.name
    }
}

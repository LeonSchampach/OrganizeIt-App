package com.example.organizeit.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.R
import com.example.organizeit.models.Drawer

class DrawerAdapter(private val drawers: MutableList<Drawer>) : RecyclerView.Adapter<DrawerAdapter.DrawerViewHolder>() {

    class DrawerViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val drawerNameInput: EditText = view.findViewById(R.id.drawerNameInput)
        val deleteButton: ImageButton = view.findViewById(R.id.buttonDeleteDrawerInput)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_drawer_input, parent, false)
        return DrawerViewHolder(view)
    }

    override fun onBindViewHolder(holder: DrawerViewHolder, position: Int) {
        val drawer = drawers[position]
        holder.drawerNameInput.setText(drawer.name)

        holder.deleteButton.setOnClickListener {
            removeDrawerAt(position)
        }

        holder.drawerNameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                drawer.name = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun getItemCount(): Int = drawers.size

    fun addDrawer(drawer: Drawer) {
        drawers.add(drawer)
        notifyItemInserted(drawers.size - 1)
    }

    fun removeDrawerAt(position: Int) {
        drawers.removeAt(position)
        notifyItemRemoved(position)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val drawer = drawers.removeAt(fromPosition)
        drawers.add(toPosition, drawer)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getDrawers(): MutableList<Drawer> = drawers
}


package com.example.organizeit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.R
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf

class ShelfAdapter(private val shelfList: List<Shelf>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SHELF = 0
    private val VIEW_TYPE_DRAWER = 1
    private val itemList = mutableListOf<Any>()
    private val expandedPositions = mutableSetOf<Int>()

    init {
        // Flatten the list to a single list containing both Shelves and Drawers
        for (shelf in shelfList) {
            itemList.add(shelf)
            // Initially add the drawers if the shelf is expanded
            if (expandedPositions.contains(shelfList.indexOf(shelf))) {
                itemList.addAll(shelf.drawers)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position]) {
            is Shelf -> VIEW_TYPE_SHELF
            is Drawer -> VIEW_TYPE_DRAWER
            else -> throw IllegalStateException("Unknown view type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SHELF) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.shelf_item, parent, false)
            ShelfViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.drawer_item, parent, false)
            DrawerViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ShelfViewHolder) {
            val shelf = itemList[position] as Shelf
            holder.bind(shelf, position)
        } else if (holder is DrawerViewHolder) {
            val drawer = itemList[position] as Drawer
            holder.bind(drawer)
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class ShelfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val shelfName: TextView = itemView.findViewById(R.id.shelfName)

        fun bind(shelf: Shelf, position: Int) {
            shelfName.text = shelf.name
            itemView.setOnClickListener {
                if (expandedPositions.contains(position)) {
                    expandedPositions.remove(position)
                    itemList.removeAll(shelf.drawers)
                } else {
                    expandedPositions.add(position)
                    itemList.addAll(position + 1, shelf.drawers)
                }
                notifyDataSetChanged()
            }
        }
    }

    inner class DrawerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val drawerName: TextView = itemView.findViewById(R.id.drawerName)

        fun bind(drawer: Drawer) {
            drawerName.text = drawer.name
        }
    }
}

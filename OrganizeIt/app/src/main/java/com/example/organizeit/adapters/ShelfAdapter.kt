package com.example.organizeit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.R
import com.example.organizeit.models.Shelf
import com.example.organizeit.models.Drawer

class ShelfAdapter(private val shelfList: MutableList<Shelf>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM_TYPE_SHELF = 0
        private const val ITEM_TYPE_DRAWER = 1
    }

    override fun getItemViewType(position: Int): Int {
        var count = 0

        for (i in 0 until shelfList.size) {
            if (position == count) {
                return ITEM_TYPE_SHELF // First item is always a shelf
            }

            count++ // Increment for the shelf itself

            if (!shelfList[i].drawers.isEmpty()) {
                // Increment for each drawer in the current shelf
                count += shelfList[i].drawers.size

                if (position < count) {
                    return ITEM_TYPE_DRAWER // Found a drawer item
                }
            }
        }

        throw IndexOutOfBoundsException("Invalid position: $position")
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_TYPE_SHELF) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.shelf_item, parent, false)
            ShelfViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.drawer_item, parent, false)
            DrawerViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ShelfViewHolder) {
            holder.bind(shelfList[getShelfPosition(position)])
        } else if (holder is DrawerViewHolder) {
            holder.bind(shelfList[getShelfPosition(position)].drawers[position - 1])
        }
    }

    override fun getItemCount(): Int {
        var count = shelfList.size
        shelfList.forEach { shelf ->
            count += shelf.drawers.size
        }
        return count
    }

    fun addShelf(shelf: Shelf) {
        shelfList.add(shelf)
        notifyItemInserted(shelfList.size - 1)
    }

    private fun getShelfPosition(adapterPosition: Int): Int {
        var count = 0

        for (i in 0 until shelfList.size) {
            if (adapterPosition == count) {
                return i // Return shelf index if adapterPosition matches count
            }

            count++ // Increment count for the shelf itself

            if (!shelfList[i].drawers.isEmpty()) {
                count += shelfList[i].drawers.size // Increment count for each drawer in the shelf

                if (adapterPosition < count) {
                    return i // Return shelf index if adapterPosition is within the range of drawers
                }
            }
        }

        return -1 // Default return if adapterPosition is out of bounds
    }


    inner class ShelfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val shelfName: TextView = itemView.findViewById(R.id.shelfName)

        fun bind(shelf: Shelf) {
            shelfName.text = shelf.name
            itemView.setOnClickListener {
                // Handle shelf item click if needed
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

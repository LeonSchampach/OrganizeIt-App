package com.example.organizeit.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.DrawerDetailActivity
import com.example.organizeit.MainActivity
import com.example.organizeit.R
import com.example.organizeit.databinding.ItemShelfBinding
import com.example.organizeit.interfaces.OnDrawerClickListener
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.util.ConfigUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MoveItemAdapter(
    private val shelfList: MutableList<Shelf>,
    private val listener: OnDrawerClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ITEM_TYPE_SHELF = 0
        const val ITEM_TYPE_DRAWER = 1
    }

    fun setShelves(shelves: List<Shelf>) {
        shelfList.clear()
        shelfList.addAll(shelves)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_TYPE_SHELF) {
            val binding = ItemShelfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ShelfViewHolder(binding)
        } else {
            /*val binding = ItemDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            DrawerViewHolder(binding)*/
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawer, parent, false)
            DrawerViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemPosition = getItemPosition(position)
        if (holder is ShelfViewHolder) {
            holder.bind(shelfList[itemPosition])
        } else if (holder is DrawerViewHolder) {
            val shelfIndex = getShelfPosition(position)
            val drawerIndex = getDrawerPosition(position, shelfIndex)
            holder.bind(shelfList[shelfIndex].drawers[drawerIndex])
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        for (shelf in shelfList) {
            count++ // For the shelf itself
            if (shelf.isExpanded) {
                count += shelf.drawers.size // Add drawers if the shelf is expanded
            }
        }
        return count
    }

    override fun getItemViewType(position: Int): Int {
        return if (isShelf(position)) {
            ITEM_TYPE_SHELF
        } else {
            ITEM_TYPE_DRAWER
        }
    }

    private fun isShelf(position: Int): Boolean {
        var count = 0
        for (shelf in shelfList) {
            if (position == count) {
                return true
            }
            count++
            if (shelf.isExpanded) {
                count += shelf.drawers.size
            }
        }
        return false
    }

    private fun getShelfPosition(adapterPosition: Int): Int {
        var count = 0
        for (i in shelfList.indices) {
            //Log.d("ShelfAdapter", "Checking shelf position: $i, count: $count, adapterPosition: $adapterPosition")
            /*if (adapterPosition == count) {
                Log.d("ShelfAdapter", "Found shelf at position: $i")
                return i
            }
            */
            count++
            if (shelfList[i].isExpanded) {
                //count += shelfList[i].drawers.size

                for (j in shelfList[i].drawers.indices) {
                    if (adapterPosition == count){
                        return i;
                    }
                    count++
                }
            }
        }
        //Log.e("ShelfAdapter", "No shelf found for adapterPosition: $adapterPosition")
        return -1
    }

    private fun getItemPosition(adapterPosition: Int): Int {
        var count = 0
        for (i in shelfList.indices) {
            if (adapterPosition == count) {
                return i
            }
            count++
            if (shelfList[i].isExpanded) {
                count += shelfList[i].drawers.size
            }
        }
        return -1
    }

    private fun getDrawerPosition(adapterPosition: Int, shelfIndex: Int): Int {
        var count = 0
        for (i in 0 until shelfIndex) {
            count++
            if (shelfList[i].isExpanded) {
                count += shelfList[i].drawers.size
            }
        }
        return adapterPosition - count - 1
    }

    inner class ShelfViewHolder(private val binding: ItemShelfBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(shelf: Shelf) {
            binding.shelfName.text = shelf.name
            binding.shelfRoom.text = shelf.room
            binding.root.setOnClickListener {
                shelf.isExpanded = !shelf.isExpanded
                notifyDataSetChanged()
            }
        }
    }

    inner class DrawerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val drawerName: TextView = itemView.findViewById(R.id.drawerName)

        fun bind(drawer: Drawer) {
            drawerName.text = drawer.name
            itemView.setOnClickListener {
                listener.onDrawerClick(drawer)
            }
        }
    }

}
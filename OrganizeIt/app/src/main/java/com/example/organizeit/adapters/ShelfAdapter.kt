package com.example.organizeit.adapters

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
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.util.ConfigUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class ShelfAdapter(private val shelfList: MutableList<Shelf>, private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            holder.bind(shelfList[itemPosition], context, this)
        } else if (holder is DrawerViewHolder) {
            val shelfIndex = getShelfPosition(position)
            val drawerIndex = getDrawerPosition(position, shelfIndex)
            holder.bind(shelfList[shelfIndex].drawers[drawerIndex], context, this)
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

    fun addShelf(shelf: Shelf) {
        shelfList.add(shelf)
        var position = shelfList.size-1
        for (shelf2: Shelf in shelfList) {
            if (shelf2.isExpanded){
                position+=shelf2.drawers.size
            }
        }
        notifyItemInserted(position)
    }

    fun removeShelf(shelf: Shelf) {
        var position = 0
        for (i in shelfList.indices) {
            if (shelfList[i].isExpanded) {
                position += shelfList[i].drawers.size
            }
            if (shelfList[i].id == shelf.id) {
                shelfList.removeAt(i)
                //notifyItemRemoved(position)
                notifyDataSetChanged()
                return
            }
            position++
        }
    }

    fun removeDrawer(drawer: Drawer) {
        var position = 0
        for (shelf: Shelf in shelfList) {
            if (shelf.id == drawer.shelfId){
                val shelfIndex = shelfList.indexOf(shelf)
                val drawerIndex = shelfList[shelfIndex].drawers.indexOf(drawer)
                shelfList[shelfIndex].drawers.remove(drawer)
                notifyItemChanged(shelfIndex)
                notifyItemRemoved(position+(drawerIndex+1))
            }
            else if(shelf.isExpanded){
                position += shelf.drawers.size
            }
            position++
        }
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
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteShelf)

        fun bind(shelf: Shelf, context: Context, adapter: ShelfAdapter) {
            binding.shelfName.text = shelf.name
            binding.shelfRoom.text = shelf.room
            binding.root.setOnClickListener {
                shelf.isExpanded = !shelf.isExpanded
                notifyDataSetChanged()
            }

            deleteButton.setOnClickListener {
                deleteShelf(shelf, context, adapter)
            }
        }
    }

    inner class DrawerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val drawerName: TextView = itemView.findViewById(R.id.drawerName)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteDrawer)

        fun bind(drawer: Drawer, context: Context, adapter: ShelfAdapter) {
            drawerName.text = drawer.name
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, DrawerDetailActivity::class.java)
                intent.putExtra("drawer", drawer)
                itemView.context.startActivity(intent)
            }
            deleteButton.setOnClickListener {
                deleteDrawer(drawer, context, adapter)
            }
        }
    }

    private fun deleteShelf(shelf: Shelf, context: Context, adapter: ShelfAdapter) {
        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/api/deleteShelf?id=${shelf.id}"

        val request = Request.Builder()
            .url(apiUrl)
            .delete() // Empty DELETE body
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? MainActivity)?.runOnUiThread {
                    Toast.makeText(context, "Error deleting shelf", Toast.LENGTH_SHORT).show()
                    Log.e("ShelfAdapter", "Error deleting shelf", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    (context as? MainActivity)?.runOnUiThread {
                        Toast.makeText(context, "Error deleting shelf", Toast.LENGTH_SHORT).show()
                        Log.e("ShelfAdapter", "Error deleting shelf: ${response.code}")
                    }
                } else {
                    (context as? MainActivity)?.runOnUiThread {
                        Toast.makeText(context, "Shelf deleted successfully", Toast.LENGTH_SHORT).show()
                        adapter.removeShelf(shelf)
                    }
                }
            }
        })
    }

    private fun deleteDrawer(drawer: Drawer, context: Context, adapter: ShelfAdapter) {
        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/api/deleteDrawer?id=${drawer.id}"

        val request = Request.Builder()
            .url(apiUrl)
            .delete() // Empty DELETE body
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
                        adapter.removeDrawer(drawer)
                    }
                }
            }
        })
    }

}

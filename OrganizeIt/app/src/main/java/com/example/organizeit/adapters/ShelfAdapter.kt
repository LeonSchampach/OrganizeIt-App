package com.example.organizeit.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.DrawerDetailActivity
import com.example.organizeit.MainActivity
import com.example.organizeit.R
import com.example.organizeit.databinding.ItemShelfBinding
import com.example.organizeit.interfaces.MenuVisibilityListener
import com.example.organizeit.interfaces.ShelfSelectionListener
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.util.ConfigUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ShelfAdapter(
    private val shelfList: MutableList<Shelf>,
    private val context: Context,
    private val menuVisibilityListener: MenuVisibilityListener,
    private val shelfSelectionListener: ShelfSelectionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ITEM_TYPE_SHELF = 0
        const val ITEM_TYPE_DRAWER = 1
    }

    private val selectedShelves = mutableListOf<Shelf>()

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

    @SuppressLint("NotifyDataSetChanged")
    fun updateShelf(oldShelf: Shelf, newShelf: Shelf) {
        val index = shelfList.indexOf(oldShelf)
        shelfList[index] = newShelf
        //val fullIndex = fullItemList.indexOf(oldItem)
        //fullItemList[fullIndex] = newItem
        notifyItemChanged(index)

        //shelfList.sortBy { it.name }
        //fullItemList.sortBy { it.name }
        notifyDataSetChanged()
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
        private val checkBox: CheckBox = itemView.findViewById(R.id.shelfCheckBox)
        private val imageView = itemView.findViewById<ImageView>(R.id.imageView)

        fun bind(shelf: Shelf, context: Context, adapter: ShelfAdapter) {
            binding.shelfName.text = shelf.name
            if (shelf.room != "") {
                binding.shelfRoom.text = shelf.room
                binding.shelfRoom.visibility = View.VISIBLE
            }
            else
                binding.shelfRoom.visibility = View.GONE
            /*binding.root.setOnClickListener {
                shelf.isExpanded = !shelf.isExpanded
                notifyDataSetChanged()
            }*/

            if (shelf.checkboxVisible) {
                checkBox.visibility = View.VISIBLE
                itemView.setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked
                }
            }
            else {
                checkBox.visibility = View.GONE
                itemView.setOnClickListener {
                    shelf.isExpanded = !shelf.isExpanded

                    if (shelf.isExpanded)
                        imageView.rotation = 90f    // Rotate by 90 degrees
                    else
                        imageView.rotation = 0f     // Rotate back to the original position

                    notifyDataSetChanged()
                }
            }

            //checkBox.isChecked = selectedItems.contains(item)
            if (shelf.uncheck) {
                checkBox.isChecked = false
                shelf.uncheck = false
            }

            checkBox.setOnCheckedChangeListener(null)  // Disable listener to prevent conflicts
            checkBox.isChecked = selectedShelves.contains(shelf)  // Set initial state
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedShelves.add(shelf)
                } else {
                    selectedShelves.remove(shelf)
                }
                shelfSelectionListener.onShelvesSelected(selectedShelves)
            }

            itemView.setOnLongClickListener {
                setAllCheckboxesVisible(true)
                setIsExpandedFalse()
                menuVisibilityListener.showMenuItems()

                //checkBox.setOnCheckedChangeListener(null)  // Disable listener
                checkBox.isChecked = true  // Check the checkbox
                /*checkBox.setOnCheckedChangeListener { _, isChecked ->  // Re-enable listener
                    if (isChecked) {
                        selectedItems.add(item)
                    } else {
                        selectedItems.remove(item)
                    }
                    itemSelectionListener.onItemsSelected(selectedItems)
                }*/

                true
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAllCheckboxesVisible(visible: Boolean) {
        for (shelf in shelfList) {
            shelf.checkboxVisible = visible
            selectedShelves.remove(shelf)
        }
        notifyDataSetChanged() // Notify the adapter to refresh the views
    }

    private fun setIsExpandedFalse() {
        for (shelf in shelfList) {
            shelf.isExpanded = false
        }
        for (shelf in selectedShelves) {
            shelf.isExpanded = false
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun uncheckAllCheckboxes() {
        for (shelf in selectedShelves) {
            shelf.uncheck = true
        }
        notifyDataSetChanged() // Notify the adapter to refresh the views
    }

    inner class DrawerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val drawerName: TextView = itemView.findViewById(R.id.drawerNameInput)

        fun bind(drawer: Drawer, context: Context, adapter: ShelfAdapter) {
            drawerName.text = drawer.name
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, DrawerDetailActivity::class.java)
                intent.putExtra("drawer", drawer)
                itemView.context.startActivity(intent)
            }
        }
    }

    fun deleteShelf(shelf: Shelf, context: Context) {
        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/shelf/deleteShelf?id=${shelf.id}"

        selectedShelves.remove(shelf)
        shelfSelectionListener.onShelvesSelected(selectedShelves)

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
                        removeShelf(shelf)
                    }
                }
            }
        })
    }
}

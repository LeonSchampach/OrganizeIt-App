package com.example.organizeit.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.DrawerDetailActivity
import com.example.organizeit.R
import com.example.organizeit.interfaces.ItemSelectionListener
import com.example.organizeit.interfaces.MenuVisibilityListener
import com.example.organizeit.interfaces.OnItemLongClickListener
import com.example.organizeit.models.Item
import com.example.organizeit.ssl_certificate.TrustAllCertificates
import com.example.organizeit.ssl_certificate.TrustAllHostnames
import com.example.organizeit.ssl_certificate.TrustAllSSLSocketFactory
import com.example.organizeit.util.ConfigUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class ItemAdapter(
    private var itemList: MutableList<Item>,
    private val context: Context,
    private val menuVisibilityListener: MenuVisibilityListener,
    private val itemSelectionListener: ItemSelectionListener
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private val secure = ConfigUtil.isSecure(context)

    private var fullItemList: MutableList<Item> = itemList.toMutableList()
    private val selectedItems = mutableListOf<Item>()

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)
        private val itemDesc: TextView = itemView.findViewById(R.id.itemDesc)
        private val increaseQuantity: ImageButton = itemView.findViewById(R.id.increaseQuantity)
        private val decreaseQuantity: ImageButton = itemView.findViewById(R.id.decreaseQuantity)

        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

        fun bind(item: Item, context: Context, adapter: ItemAdapter) {
            itemName.text = item.name
            itemQuantity.text = formatNumber(item.quantity)
            itemDesc.text = if (item.desc.isEmpty()) {
                context.getString(R.string.noDescriptionAvailable)
            } else {
                String.format("%s %s", context.getString(R.string.description), item.desc)
            }

            if (item.checkboxVisible) {
                checkBox.visibility = View.VISIBLE
                itemView.setOnClickListener {
                    checkBox.isChecked = !checkBox.isChecked
                }
            }
            else {
                checkBox.visibility = View.GONE
                itemView.setOnClickListener {
                    itemDesc.visibility = if (itemDesc.visibility == View.GONE) View.VISIBLE else View.GONE
                }
            }

            //checkBox.isChecked = selectedItems.contains(item)
            if (item.uncheck) {
                checkBox.isChecked = false
                item.uncheck = false
            }

            checkBox.setOnCheckedChangeListener(null)  // Disable listener to prevent conflicts
            checkBox.isChecked = selectedItems.contains(item)  // Set initial state
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItems.add(item)
                } else {
                    selectedItems.remove(item)
                }
                itemSelectionListener.onItemsSelected(selectedItems)
            }

            itemView.setOnLongClickListener {
                setAllCheckboxesVisible(true)
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

            increaseQuantity.setOnClickListener {
                itemQuantity.text = formatNumber(item.quantity + 1)
                changeQuantity(item, 1, context, adapter)
            }

            decreaseQuantity.setOnClickListener {
                itemQuantity.text = formatNumber(item.quantity - 1)
                changeQuantity(item, -1, context, adapter)
            }
        }
    }

    private fun formatNumber(number: Double): String {
        return if (number == number.toLong().toDouble()) {
            String.format(Locale.getDefault(), "%d", number.toLong())
        } else {
            String.format(Locale.getDefault(), "%s", number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAllCheckboxesVisible(visible: Boolean) {
        for (item in itemList) {
            item.checkboxVisible = visible
            selectedItems.remove(item)
        }
        notifyDataSetChanged() // Notify the adapter to refresh the views
    }

    @SuppressLint("NotifyDataSetChanged")
    fun uncheckAllCheckboxes() {
        for (item in selectedItems) {
            item.uncheck = true
        }
        notifyDataSetChanged() // Notify the adapter to refresh the views
    }

    fun deleteItem(item: Item, context: Context, adapter: ItemAdapter) {
        val client = if (secure) OkHttpClient.Builder()
            .sslSocketFactory(TrustAllSSLSocketFactory.getSocketFactory(), TrustAllCertificates())
            .hostnameVerifier(TrustAllHostnames())
            .build() else OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/item/deleteItem?id=${item.id}"

        selectedItems.remove(item)
        itemSelectionListener.onItemsSelected(selectedItems)

        val request = Request.Builder()
            .url(apiUrl)
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? DrawerDetailActivity)?.runOnUiThread {
                    Toast.makeText(context, "Error deleting item", Toast.LENGTH_SHORT).show()
                    Log.e("ItemAdapter", "Error deleting item", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    (context as? DrawerDetailActivity)?.runOnUiThread {
                        Toast.makeText(context, "Error deleting item", Toast.LENGTH_SHORT).show()
                        Log.e("ItemAdapter", "Error deleting item: ${response.code}")
                    }
                } else {
                    (context as? DrawerDetailActivity)?.runOnUiThread {
                        adapter.removeItem(item)
                    }
                }
            }
        })
    }

    private fun changeQuantity(item: Item, incOrDec: Int, context: Context, adapter: ItemAdapter) {
        val jsonObject = JSONObject()
        jsonObject.put("id", item.id)
        jsonObject.put("name", item.name)
        jsonObject.put("desc", item.desc)
        jsonObject.put("quantity", item.quantity + incOrDec)
        jsonObject.put("drawerId", item.drawerId)

        val client = if (secure) OkHttpClient.Builder()
            .sslSocketFactory(TrustAllSSLSocketFactory.getSocketFactory(), TrustAllCertificates())
            .hostnameVerifier(TrustAllHostnames())
            .build() else OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/item/updateItem"
        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? DrawerDetailActivity)?.runOnUiThread {
                    Toast.makeText(context, "Error updating item", Toast.LENGTH_SHORT).show()
                    Log.e("ItemAdapter", "Error updating item", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    (context as? DrawerDetailActivity)?.runOnUiThread {
                        Toast.makeText(context, "Error updating item", Toast.LENGTH_SHORT).show()
                        Log.e("ItemAdapter", "Error updating item: ${response.code}")
                    }
                } else {
                    (context as? DrawerDetailActivity)?.runOnUiThread {
                        adapter.updateItemQuantity(item, incOrDec)
                    }
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawer_detail, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemList[position], context, this)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun setItems(newItems: List<Item>) {
        itemList.clear()
        itemList.addAll(newItems)
        fullItemList = itemList.toMutableList()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(newItem: Item) {
        itemList.add(newItem)
        fullItemList.add(newItem)
        //notifyItemInserted(itemList.lastIndex)

        itemList.sortBy { it.name }
        fullItemList.sortBy { it.name }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItem(oldItem: Item, newItem: Item) {
        val index = itemList.indexOf(oldItem)
        itemList[index] = newItem
        val fullIndex = fullItemList.indexOf(oldItem)
        fullItemList[fullIndex] = newItem
        notifyItemChanged(index)

        selectedItems.remove(oldItem)
        itemSelectionListener.onItemsSelected(selectedItems)

        itemList.sortBy { it.name }
        fullItemList.sortBy { it.name }
        notifyDataSetChanged()
    }

    fun updateItemQuantity(item: Item, incOrDec: Int) {
        val index = itemList.indexOf(item)
        if (index != -1) {
            val updatedItem = itemList[index].copy(quantity = item.quantity + incOrDec)
            itemList[index] = updatedItem

            val fullIndex = fullItemList.indexOf(item)
            if (fullIndex != -1) {
                fullItemList[fullIndex] = updatedItem
            }

            notifyItemChanged(index)
        }
    }

    fun removeItem(item: Item) {
        val index = itemList.indexOf(item)
        itemList.removeAt(index)
        val fullIndex = fullItemList.indexOf(item)
        fullItemList.removeAt(fullIndex)
        notifyItemRemoved(index)
    }

    fun filter(query: String) {
        itemList = if (query.isEmpty()) {
            fullItemList.toMutableList()
        } else {
            fullItemList.filter {
                it.name.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }
}

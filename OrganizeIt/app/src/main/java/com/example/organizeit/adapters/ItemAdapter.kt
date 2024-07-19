package com.example.organizeit.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.DrawerDetailActivity
import com.example.organizeit.R
import com.example.organizeit.models.Item
import com.example.organizeit.util.ConfigUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ItemAdapter(private val itemList: MutableList<Item>, private val context: Context) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)
        private val itemDesc: TextView = itemView.findViewById(R.id.itemDesc)
        private val increaseQuantity: ImageButton = itemView.findViewById(R.id.increaseQuantity)
        private val decreaseQuantity: ImageButton = itemView.findViewById(R.id.decreaseQuantity)
        private val deleteItem: ImageButton = itemView.findViewById(R.id.deleteItem)

        fun bind(item: Item, context: Context, adapter: ItemAdapter) {
            itemName.text = item.name
            itemQuantity.text = item.quantity.toString()
            itemDesc.text = item.desc

            itemView.setOnClickListener {
                if (itemDesc.visibility == View.GONE) {
                    itemDesc.visibility = View.VISIBLE
                } else {
                    itemDesc.visibility = View.GONE
                }
            }

            increaseQuantity.setOnClickListener {
                changeQuantity(item, 1, context, adapter)
            }

            decreaseQuantity.setOnClickListener {
                changeQuantity(item, -1, context, adapter)
            }

            deleteItem.setOnClickListener {
                deleteItem(item, context, adapter)
            }
        }
    }

    private fun deleteItem(item: Item, context: Context, adapter: ItemAdapter) {
        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/api/deleteItem?id=${item.id}"

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
                        Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                        adapter.removeItem(item)
                    }
                }
            }
        })
    }

    private fun changeQuantity(item: Item, incOrDec: Int, context: Context, adapter: ItemAdapter) {
        val jsonObject = JSONObject()
        jsonObject.put("name", item.name)
        jsonObject.put("desc", item.desc)
        jsonObject.put("quantity", item.quantity + incOrDec)
        jsonObject.put("drawerId", item.drawerId)

        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/api/updateItem"
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
                        adapter.updateItem(item, incOrDec)
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
        notifyDataSetChanged()
    }

    fun addItem(newItem: Item){
        itemList.add(newItem)
        notifyItemInserted(itemList.lastIndex)
    }

    fun updateItem(item: Item, incOrDec: Int) {
        val index = itemList.indexOf(item)
        itemList[index] = Item(item.id, item.name, item.desc, item.quantity + incOrDec, item.drawerId)
        notifyItemChanged(index)
    }

    fun removeItem(item: Item) {
        val index = itemList.indexOf(item)
        itemList.removeAt(index)
        notifyItemRemoved(index)
    }
}

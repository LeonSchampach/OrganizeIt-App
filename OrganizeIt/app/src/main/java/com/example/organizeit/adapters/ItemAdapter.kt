package com.example.organizeit.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.R
import com.example.organizeit.models.Item

class ItemAdapter(private val itemList: MutableList<Item>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)
        private val itemDesc: TextView = itemView.findViewById(R.id.itemDesc)
        private val increaseQuantity: Button = itemView.findViewById(R.id.increaseQuantity)
        private val decreaseQuantity: Button = itemView.findViewById(R.id.decreaseQuantity)
        private val deleteItem: Button = itemView.findViewById(R.id.deleteItem)

        fun bind(item: Item) {
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
                // Implement increase quantity logic
            }

            decreaseQuantity.setOnClickListener {
                // Implement decrease quantity logic
            }

            deleteItem.setOnClickListener {
                // Implement delete item logic
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawer_detail, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemList[position])
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
}

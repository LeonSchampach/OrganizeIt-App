package com.example.organizeit.interfaces

import com.example.organizeit.models.Item

interface ItemSelectionListener {
    fun onItemsSelected(selectedItems: List<Item>)
}

package com.example.organizeit.interfaces

import com.example.organizeit.models.Shelf

interface ShelfSelectionListener {
    fun onShelvesSelected(selectedShelves: List<Shelf>)
}
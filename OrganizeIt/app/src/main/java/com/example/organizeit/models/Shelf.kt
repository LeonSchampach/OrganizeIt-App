// Shelf.kt
package com.example.organizeit.models

data class Shelf(
    val id: Int? = null,
    val name: String,
    val room: String,
    val drawers: MutableList<Drawer>, // Assuming a list of drawers in a shelf
    var isExpanded: Boolean = false // Add this flag to track expansion state
)

// Shelf.kt
package com.example.organizeit.models

data class Shelf(
    val id: Long? = null,
    val name: String,
    val room: String,
    val drawers: MutableList<Drawer>, // Assuming a list of drawers in a shelf
    var isExpanded: Boolean = false, // Add this flag to track expansion state
    var checkboxVisible: Boolean = false,
    var imageViewVisible: Boolean = true,
    var uncheck: Boolean = false
)

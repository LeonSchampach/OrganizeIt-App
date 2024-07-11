package com.example.organizeit.models

data class ShelfRequest(
    val name: String,
    val room: String,
    val drawers: List<DrawerRequest>
)
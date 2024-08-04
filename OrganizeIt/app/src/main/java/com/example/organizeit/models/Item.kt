package com.example.organizeit.models

data class Item(
    val id: Int? = null,
    var name: String,
    var desc: String,
    var quantity: Double,
    val drawerId: Int
)
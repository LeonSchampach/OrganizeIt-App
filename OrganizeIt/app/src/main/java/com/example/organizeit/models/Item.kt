package com.example.organizeit.models

data class Item(
    val id: Int? = null,
    val name: String,
    val desc: String,
    val quantity: Int,
    val drawerId: Int
)
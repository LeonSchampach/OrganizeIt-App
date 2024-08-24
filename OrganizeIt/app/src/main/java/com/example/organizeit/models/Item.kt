package com.example.organizeit.models

data class Item(
    val id: Long? = null,
    var name: String,
    var desc: String,
    var quantity: Double,
    val drawerId: Long,
    var checkboxVisible: Boolean = false,
    var uncheck: Boolean = false
)
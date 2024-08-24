package com.example.organizeit.models

import java.io.Serializable

data class Drawer(
    val id: Long? = null,
    var name: String,
    val order: Int,
    val shelfId: Long? = null
) : Serializable
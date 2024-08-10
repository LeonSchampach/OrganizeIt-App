package com.example.organizeit.models

import java.io.Serializable

data class Drawer(
    val id: Int? = null,
    val name: String,
    val order: Int,
    val shelfId: Int? = null
) : Serializable
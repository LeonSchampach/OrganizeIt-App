package com.example.organizeit.network

import com.example.organizeit.models.ShelfRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/createShelf")
    fun createShelf(@Body shelfRequest: ShelfRequest): Call<Void>
}

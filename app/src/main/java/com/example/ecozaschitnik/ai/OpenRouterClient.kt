package com.example.ecozaschitnik.ai

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenRouterClient {
    private const val BASE_URL = "https://openrouter.ai/"

    val api: OpenRouterApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }
}

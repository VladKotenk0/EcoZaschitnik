package com.example.ecozaschitnik.ai

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenRouterApi {

    @Headers(
        "HTTP-Referer: https://vk.com/ecozaschitnik",
        "X-Title: EcoZaschitnik App"
    )
    @POST("api/v1/chat/completions")
    suspend fun generateReport(
        @Header("Authorization") auth: String,
        @Body body: ChatRequest
    ): ChatResponse
}

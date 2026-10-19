package com.example

interface AiClient {
    suspend fun reply(userText: String): MyraResult
}

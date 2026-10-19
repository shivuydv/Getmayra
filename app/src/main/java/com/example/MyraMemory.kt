package com.example

import android.content.Intent

data class MyraMessage(
    val id: Long = System.nanoTime(),
    val role: String, // "YOU" or "MYRA"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val actionUrl: String? = null,
    val actionIntent: Intent? = null
)

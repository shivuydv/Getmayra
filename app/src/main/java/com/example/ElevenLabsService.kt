package com.example

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object ElevenLabsService {

    private const val TAG = "ElevenLabsService"
    private var mediaPlayer: MediaPlayer? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // Model IDs tried in fallback order
    private val CANDIDATE_MODELS = listOf(
        "eleven_multilingual_v2",
        "eleven_turbo_v2",
        "eleven_flash_v2",
        "eleven_monolingual_v1"
    )

    suspend fun speakWithElevenLabs(
        context: Context,
        text: String,
        apiKey: String,
        voiceId: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            withContext(Dispatchers.Main) { onError("ElevenLabs API Key is missing. Enter key in Settings.") }
            return@withContext
        }

        val targetVoiceId = if (voiceId.isBlank()) "21m00Tcm4TlvDq8ikWAM" else voiceId.trim()
        var audioBytes: ByteArray? = null
        var lastErrorMessage = ""

        for (modelId in CANDIDATE_MODELS) {
            try {
                val url = "https://api.elevenlabs.io/v1/text-to-speech/$targetVoiceId?output_format=mp3_44100_128"

                val json = JSONObject().apply {
                    put("text", text)
                    put("model_id", modelId)
                    put("voice_settings", JSONObject().apply {
                        put("stability", 0.50)
                        put("similarity_boost", 0.80)
                        put("style", 0.35)
                        put("use_speaker_boost", true)
                    })
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("xi-api-key", cleanKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "audio/mpeg")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    audioBytes = response.body?.bytes()
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        Log.i(TAG, "ElevenLabs TTS success using model $modelId and voice $targetVoiceId")
                        break
                    }
                } else {
                    val errCode = response.code
                    val errBody = response.body?.string().orEmpty()
                    lastErrorMessage = "HTTP $errCode: $errBody"
                    Log.w(TAG, "ElevenLabs model $modelId failed ($errCode): $errBody")
                    // If Unauthorized (401), invalid API key - break immediately
                    if (errCode == 401) {
                        break
                    }
                }
            } catch (e: Exception) {
                lastErrorMessage = e.message ?: "Network error"
                Log.e(TAG, "ElevenLabs model $modelId exception: ${e.message}")
            }
        }

        if (audioBytes == null || audioBytes.isEmpty()) {
            withContext(Dispatchers.Main) {
                onError(lastErrorMessage.ifBlank { "ElevenLabs voice generation failed" })
            }
            return@withContext
        }

        try {
            val tempFile = File(context.cacheDir, "myra_voice_${System.currentTimeMillis()}.mp3")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            withContext(Dispatchers.Main) {
                playAudioFile(tempFile, onStart, onDone, onError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save/play ElevenLabs audio: ${e.message}")
            withContext(Dispatchers.Main) { onError(e.message ?: "Failed to play voice") }
        }
    }

    private fun playAudioFile(
        file: File,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            stopAudio()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    onStart()
                    it.start()
                }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    try { file.delete() } catch (_: Exception) {}
                    onDone()
                }
                setOnErrorListener { mp, what, extra ->
                    mp.release()
                    mediaPlayer = null
                    try { file.delete() } catch (_: Exception) {}
                    onError("MediaPlayer error $what / $extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            onError(e.message ?: "Failed to play audio")
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }
}

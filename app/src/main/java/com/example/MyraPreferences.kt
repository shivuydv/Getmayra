package com.example

import android.content.Context
import android.content.SharedPreferences

object MyraPreferences {
    private const val PREF_NAME = "myra_companion_prefs"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    private const val KEY_OPENAI_API_KEY = "openai_api_key"
    private const val KEY_AI_PROVIDER = "ai_provider" // "GEMINI", "OPENAI", "OFFLINE"
    private const val KEY_ELEVENLABS_API_KEY = "elevenlabs_api_key"
    private const val KEY_ELEVENLABS_VOICE_ID = "elevenlabs_voice_id"
    private const val KEY_PERSONALITY_MODE = "personality_mode" // "NAKHRE", "SWEET", "CARING"
    private const val KEY_VOICE_ENGINE = "voice_engine" // "ELEVENLABS" or "DEVICE_TTS"
    private const val KEY_USER_NAME = "user_name"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getGeminiApiKey(context: Context): String {
        val userKey = getPrefs(context).getString(KEY_GEMINI_API_KEY, "") ?: ""
        if (userKey.isNotBlank()) return userKey.trim()

        return try {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            val buildKey = field.get(null) as? String
            if (!buildKey.isNullOrBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey.trim() else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun setGeminiApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun getOpenAiApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_OPENAI_API_KEY, "")?.trim() ?: ""
    }

    fun setOpenAiApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_OPENAI_API_KEY, key.trim()).apply()
    }

    fun getAiProvider(context: Context): String {
        val provider = getPrefs(context).getString(KEY_AI_PROVIDER, "") ?: ""
        if (provider.isNotBlank()) return provider

        // Default to GEMINI if Gemini key is present, else OFFLINE
        return if (getGeminiApiKey(context).isNotBlank()) "GEMINI" else "OFFLINE"
    }

    fun setAiProvider(context: Context, provider: String) {
        getPrefs(context).edit().putString(KEY_AI_PROVIDER, provider).apply()
    }

    fun getElevenLabsApiKey(context: Context): String {
        val userKey = getPrefs(context).getString(KEY_ELEVENLABS_API_KEY, "")?.trim() ?: ""
        if (userKey.isNotBlank()) return userKey

        return try {
            val buildClass = com.example.BuildConfig::class.java
            val field = try {
                buildClass.getField("ELEVENLABS_API_KEY")
            } catch (_: Exception) {
                buildClass.getField("ELEVEN_LABS_API_KEY")
            }
            val buildKey = field.get(null) as? String
            if (!buildKey.isNullOrBlank() && !buildKey.startsWith("MY_")) buildKey.trim() else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun setElevenLabsApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_ELEVENLABS_API_KEY, key.trim()).apply()
    }

    fun getElevenLabsVoiceId(context: Context): String {
        val id = getPrefs(context).getString(KEY_ELEVENLABS_VOICE_ID, "")?.trim() ?: ""
        if (id.isNotBlank()) return id

        val buildVoice = try {
            val buildClass = com.example.BuildConfig::class.java
            val field = try {
                buildClass.getField("ELEVENLABS_VOICE_ID")
            } catch (_: Exception) {
                buildClass.getField("ELEVEN_LABS_VOICE_ID")
            }
            field.get(null) as? String
        } catch (_: Throwable) {
            null
        }

        if (!buildVoice.isNullOrBlank() && !buildVoice.startsWith("MY_")) {
            return buildVoice.trim()
        }

        return "21m00Tcm4TlvDq8ikWAM" // Default Rachel / Sweet voice
    }

    fun setElevenLabsVoiceId(context: Context, voiceId: String) {
        getPrefs(context).edit().putString(KEY_ELEVENLABS_VOICE_ID, voiceId.trim()).apply()
    }

    fun getPersonalityMode(context: Context): String {
        return getPrefs(context).getString(KEY_PERSONALITY_MODE, "NAKHRE") ?: "NAKHRE"
    }

    fun setPersonalityMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_PERSONALITY_MODE, mode).apply()
    }

    fun getVoiceEngine(context: Context): String {
        val engine = getPrefs(context).getString(KEY_VOICE_ENGINE, "") ?: ""
        if (engine.isNotBlank()) return engine
        return if (getElevenLabsApiKey(context).isNotBlank()) "ELEVENLABS" else "DEVICE_TTS"
    }

    fun setVoiceEngine(context: Context, engine: String) {
        getPrefs(context).edit().putString(KEY_VOICE_ENGINE, engine).apply()
    }

    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "Sweetheart") ?: "Sweetheart"
    }

    fun setUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun getDisplayMode(context: Context): String {
        return getPrefs(context).getString("display_mode", "ANIME") ?: "ANIME"
    }

    fun setDisplayMode(context: Context, mode: String) {
        getPrefs(context).edit().putString("display_mode", mode).apply()
    }
}

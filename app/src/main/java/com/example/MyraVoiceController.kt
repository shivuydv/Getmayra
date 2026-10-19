package com.example

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class MyraVoiceController(
    private val context: Context,
    private val onState: (MyraVoiceState, String) -> Unit,
    private val onTranscript: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit = {},
    private val onMessageGenerated: (role: String, text: String, actionType: String?, actionUrl: String?) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var scopeJob = Job()
    private var scope = CoroutineScope(Dispatchers.Main + scopeJob)

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var active = false
    private var isListening = false
    private var isSpeaking = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    var currentLanguageCode: String = "hi-IN"
        private set
    var ttsPitch: Float = 1.25f // Cute high-pitched girlfriend voice
        private set
    var ttsSpeed: Float = 1.05f
        private set

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initRecognizer()
        // Initialize WhatsApp Reader callback
        MayaWhatsAppReaderService.onWhatsAppMessageReceived = { sender, message ->
            if (active) {
                scope.launch {
                    val announcement = if (currentLanguageCode.startsWith("hi")) {
                        "$sender का WhatsApp मैसेज आया है: $message"
                    } else {
                        "New WhatsApp message from $sender: $message"
                    }
                    onMessageGenerated("MYRA", announcement, null, null)
                    speak(announcement)
                }
            }
        }
    }

    private fun initRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer?.setRecognitionListener(this)
            } else {
                Log.w("MyraVoiceController", "SpeechRecognizer service not available on device")
            }
        } catch (e: Exception) {
            Log.e("MyraVoiceController", "Failed to init SpeechRecognizer: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        ttsReady = (status == TextToSpeech.SUCCESS)
        if (ttsReady) {
            applyTtsLanguage()
            tts?.setPitch(ttsPitch)
            tts?.setSpeechRate(ttsSpeed)
            setupTtsProgressListener()
        }
    }

    private fun applyTtsLanguage() {
        if (!ttsReady || tts == null) return
        val targetLocale = when (currentLanguageCode) {
            "en-US" -> Locale.US
            "en-IN" -> Locale("en", "IN")
            else -> Locale("hi", "IN")
        }
        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.getDefault())
        }
    }

    private fun setupTtsProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                abandonFocus()
                if (active) {
                    mainHandler.postDelayed({
                        if (active && !isListening && !isSpeaking) startListening()
                    }, 400)
                }
            }

            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                abandonFocus()
                if (active) {
                    mainHandler.postDelayed({
                        if (active && !isListening) startListening()
                    }, 400)
                }
            }
        })
    }

    private fun requestFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioFocusRequest = req
                audioManager?.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (_: Exception) {}
    }

    private fun abandonFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
    }

    fun setLanguage(langCode: String) {
        currentLanguageCode = langCode
        applyTtsLanguage()
        if (active) {
            try {
                recognizer?.cancel()
            } catch (_: Exception) {}
            isListening = false
            startListening()
        }
    }

    fun setPitch(pitch: Float) {
        ttsPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(ttsPitch)
    }

    fun setSpeed(speed: Float) {
        ttsSpeed = speed.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(ttsSpeed)
    }

    fun startSession() {
        active = true
        isListening = false
        isSpeaking = false

        if (recognizer == null) {
            initRecognizer()
        }

        try {
            MayaSessionService.start(context)
        } catch (_: Exception) {}

        val listenMsg = if (currentLanguageCode.startsWith("hi")) {
            "मायरा सुन रही है… बोलिए"
        } else {
            "Myra is listening… Speak now"
        }
        onState(MyraVoiceState.LISTENING, listenMsg)
        startListening()
    }

    fun stopSession() {
        active = false
        isListening = false
        isSpeaking = false
        mainHandler.removeCallbacksAndMessages(null)
        scopeJob.cancel()
        scopeJob = Job()
        scope = CoroutineScope(Dispatchers.Main + scopeJob)

        try {
            MayaSessionService.stop(context)
        } catch (_: Exception) {}

        try {
            recognizer?.cancel()
        } catch (_: Exception) {}
        try {
            tts?.stop()
        } catch (_: Exception) {}
        ElevenLabsService.stopAudio()
        abandonFocus()

        onState(MyraVoiceState.IDLE, "Session बंद है। बात करने के लिए बटन दबाएं।")
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        ElevenLabsService.stopAudio()
        isSpeaking = false
        abandonFocus()
        if (active && !isListening) {
            startListening()
        }
    }

    fun retry() {
        if (active) {
            isListening = false
            isSpeaking = false
            startListening()
        } else {
            startSession()
        }
    }

    fun isSessionActive(): Boolean = active

    fun startListening() {
        if (!active || isListening || isSpeaking) return

        if (!SpeechRecognizer.isRecognitionAvailable(context) || recognizer == null) {
            // Speech recognizer service unavailable on emulator - keep session active for direct input
            onState(MyraVoiceState.LISTENING, "मायरा एक्टिव है! बोलें या नीचे लिखें")
            return
        }

        try {
            isListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            val listenMsg = if (currentLanguageCode.startsWith("hi")) "सुन रही हूँ… बोलिए" else "Listening… talk to me"
            onState(MyraVoiceState.LISTENING, listenMsg)
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            Log.w("MyraVoiceController", "startListening failed: ${e.message}")
            onState(MyraVoiceState.LISTENING, "मायरा एक्टिव है! बोलें या नीचे टाइप करें")
        }
    }

    override fun onResults(results: Bundle?) {
        isListening = false

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull().orEmpty().trim()

        if (text.isBlank()) {
            if (active && !isSpeaking) {
                mainHandler.postDelayed({ if (active && !isListening) startListening() }, 600)
            }
            return
        }

        onTranscript(text)
        processQuery(text)
    }

    fun processDirectText(text: String) {
        if (text.isBlank()) return
        onTranscript(text)
        processQuery(text)
    }

    private fun processQuery(text: String) {
        val procMsg = if (currentLanguageCode.startsWith("hi")) "सोच रही हूँ…" else "Thinking…"
        onState(MyraVoiceState.PROCESSING, procMsg)
        onMessageGenerated("YOU", text, null, null)

        // Wake word trigger ("हे मायरा", "hey myra")
        val lower = text.lowercase(Locale.ROOT)
        if (lower == "hey myra" || lower == "हे मायरा" || lower == "myra" || lower == "मायरा") {
            val userName = MyraPreferences.getUserName(context)
            val wakeReply = if (currentLanguageCode.startsWith("hi")) {
                "हाँ $userName! तुम्हारी मायरा यहीं है, कहो क्या हुक्म है?"
            } else {
                "Yes $userName! I am right here with you. What's on your mind?"
            }
            onMessageGenerated("MYRA", wakeReply, null, null)
            speak(wakeReply)
            return
        }

        // 1. Phone automations & system commands
        val cmdResult = MyraCommandRouter.execute(context, text, currentLanguageCode)

        if (cmdResult.handled) {
            onMessageGenerated("MYRA", cmdResult.replyText, cmdResult.actionType, cmdResult.actionUrl)
            speak(cmdResult.replyText)
            if (cmdResult.requestStop) {
                stopSession()
            }
        } else {
            // 2. Query Gemini / Girlfriend AI engine
            scope.launch {
                val reply = MyraAiEngine.getResponse(context, text, currentLanguageCode)
                onMessageGenerated("MYRA", reply, null, null)
                speak(reply)
            }
        }
    }

    fun speak(message: String) {
        val cleanMessage = MyraAiEngine.cleanForSpeech(message)
        val elevenLabsKey = MyraPreferences.getElevenLabsApiKey(context)
        val voiceEngine = MyraPreferences.getVoiceEngine(context)

        // If user configured ElevenLabs key and selected ElevenLabs voice engine
        if (voiceEngine == "ELEVENLABS" && elevenLabsKey.isNotBlank()) {
            val voiceId = MyraPreferences.getElevenLabsVoiceId(context)
            requestFocus()
            isSpeaking = true
            onState(MyraVoiceState.SPEAKING, message)
            scope.launch {
                ElevenLabsService.speakWithElevenLabs(
                    context = context,
                    text = cleanMessage,
                    apiKey = elevenLabsKey,
                    voiceId = voiceId,
                    onStart = {
                        isSpeaking = true
                        onState(MyraVoiceState.SPEAKING, message)
                    },
                    onDone = {
                        isSpeaking = false
                        abandonFocus()
                        if (active) {
                            mainHandler.postDelayed({
                                if (active && !isListening && !isSpeaking) startListening()
                            }, 400)
                        }
                    },
                    onError = { err ->
                        Log.w("MyraVoiceController", "ElevenLabs fallback: $err")
                        speakWithDeviceTts(cleanMessage)
                    }
                )
            }
            return
        }

        // Native High-Quality Device TTS
        speakWithDeviceTts(message)
    }

    private fun speakWithDeviceTts(message: String) {
        if (!ttsReady || tts == null) {
            onState(MyraVoiceState.LISTENING, "मायरा एक्टिव है")
            return
        }
        requestFocus()
        isSpeaking = true
        onState(MyraVoiceState.SPEAKING, message)
        
        val cleanMessage = MyraAiEngine.cleanForSpeech(message)
        val utteranceId = "myra-utterance-${System.currentTimeMillis()}"
        tts?.speak(cleanMessage, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        // Safety watchdog: ensure speech lock is released even if OS drops onDone
        val safeTimeoutMs = (message.length * 80L + 2200L).coerceIn(2200L, 8000L)
        mainHandler.postDelayed({
            if (isSpeaking) {
                isSpeaking = false
                abandonFocus()
                if (active && !isListening) {
                    startListening()
                }
            }
        }, safeTimeoutMs)
    }

    override fun onError(error: Int) {
        isListening = false
        if (!active) return

        // For silence or timeouts, don't break into an ugly error state! Keep session active and listening smoothly
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            onState(MyraVoiceState.LISTENING, "मायरा सुन रही है… बोलिए")
            mainHandler.postDelayed({
                if (active && !isListening && !isSpeaking) startListening()
            }, 800)
            return
        }

        // For other errors, log and keep active
        Log.w("MyraVoiceController", "Speech error code: $error")
        onState(MyraVoiceState.LISTENING, "मायरा एक्टिव है! बोलें या नीचे लिखें")
    }

    override fun onRmsChanged(rmsdB: Float) {
        onRmsChanged.invoke(rmsdB)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull().orEmpty()
        if (partial.isNotBlank()) {
            onTranscript(partial)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun release() {
        active = false
        isListening = false
        isSpeaking = false
        mainHandler.removeCallbacksAndMessages(null)
        scopeJob.cancel()
        abandonFocus()
        ElevenLabsService.stopAudio()

        try {
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null

        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }
}

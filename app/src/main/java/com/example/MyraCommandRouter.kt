package com.example

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommandResult(
    val handled: Boolean,
    val replyText: String,
    val actionType: String? = null,
    val actionUrl: String? = null,
    val actionIntent: Intent? = null,
    val requestStop: Boolean = false,
    val serviceResult: MyraResult? = null
)

object MyraCommandRouter {

    private var isTorchOn = false

    fun execute(context: Context, raw: String, languageCode: String = "hi-IN"): CommandResult {
        val text = raw.lowercase(Locale.ROOT).trim()
        val isHi = languageCode.startsWith("hi")

        // 1. Session stop commands
        if (text == "stop" || text == "बंद" || text == "चुप" || text == "शांत" ||
            text.contains("session बंद") || text.contains("stop session") ||
            text.contains("अलविदा") || text.contains("goodbye") || text.contains("bye myra")
        ) {
            MayaSirenPlayer.stopSiren()
            val reply = if (isHi) {
                "ठीक है, Session बंद कर रही हूँ। जब भी ज़रूरत हो, माइक दबाइए।"
            } else {
                "Goodbye! Stopping session now. Tap the mic whenever you need me."
            }
            return CommandResult(
                handled = true,
                replyText = reply,
                requestStop = true,
                serviceResult = MyraResult(true, "Session stopped by user")
            )
        }

        // Emergency Siren Sound
        if (text.contains("सायरन बंद") || text.contains("stop siren") || text.contains("mute siren")) {
            MayaSirenPlayer.stopSiren()
            val reply = if (isHi) "सायरन बंद कर दिया है।" else "Emergency siren stopped."
            return CommandResult(handled = true, replyText = reply, actionType = "SIREN_STOP", serviceResult = MyraResult(true, reply))
        }

        if (text.contains("सायरन") || text.contains("siren")) {
            MayaSirenPlayer.startSiren()
            val reply = if (isHi) {
                "इमरजेंसी सायरन बजा दिया है! इसे बंद करने के लिए 'सायरन बंद' बोलें।"
            } else {
                "Emergency siren activated! Say 'stop siren' to turn it off."
            }
            return CommandResult(handled = true, replyText = reply, actionType = "SIREN", serviceResult = MyraResult(true, reply))
        }

        // 2. Real-time Clock / Time
        if (text.contains("समय") || text.contains("time") || text.contains("कितने बजे") || text.contains("baje hai")) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            val reply = if (isHi) "अभी समय $currentTime है।" else "The current time is $currentTime."
            return CommandResult(
                handled = true,
                replyText = reply,
                actionType = "TIME",
                serviceResult = MyraResult(true, "Time reported: $currentTime")
            )
        }

        // 3. Date
        if (text.contains("तारीख") || text.contains("date") || text.contains("आज कौन सा दिन") || text.contains("today's date")) {
            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val reply = if (isHi) "आज $currentDate है।" else "Today is $currentDate."
            return CommandResult(
                handled = true,
                replyText = reply,
                actionType = "DATE",
                serviceResult = MyraResult(true, "Date reported: $currentDate")
            )
        }

        // 4. System Telemetry / Maya Left Panel Hardware Stats
        if (text.contains("सिस्टम") || text.contains("system status") || text.contains("phone info") ||
            text.contains("मेमोरी") || text.contains("ram") || text.contains("hardware") || text.contains("हार्डवेयर")
        ) {
            val summary = AndroidSystemMonitor.getVoiceSummary(context, languageCode)
            return CommandResult(
                handled = true,
                replyText = summary,
                actionType = "SYSTEM_TELEMETRY",
                serviceResult = MyraResult(true, summary)
            )
        }

        // Mode Switching ("मोड बदलो", "switch mode", "anime mode", "hud mode")
        if (text.contains("मोड बदलो") || text.contains("switch mode") || text.contains("hud mode") || text.contains("anime mode") || text.contains("अवतार बदलो")) {
            val currentMode = MyraPreferences.getDisplayMode(context)
            val newMode = if (currentMode == "ANIME") "CYBER_HUD" else "ANIME"
            MyraPreferences.setDisplayMode(context, newMode)
            val reply = if (isHi) {
                if (newMode == "CYBER_HUD") "माया साइबर HUD मोड चालू कर दिया है!" else "क्यूट एनिमे गर्लफ्रेंड मोड चालू कर दिया है!"
            } else {
                if (newMode == "CYBER_HUD") "Switched to Maya Cyber HUD Orb Mode!" else "Switched to Cute Anime Girlfriend Mode!"
            }
            return CommandResult(
                handled = true,
                replyText = reply,
                actionType = "MODE_SWITCH",
                serviceResult = MyraResult(true, newMode)
            )
        }

        // 5. Battery Status via BatteryManager
        if (text.contains("बैटरी") || text.contains("battery") || text.contains("चार्ज") || text.contains("charging")) {
            val batteryStatus = getBatteryInfo(context, languageCode)
            return CommandResult(
                handled = true,
                replyText = batteryStatus,
                actionType = "BATTERY",
                serviceResult = MyraResult(true, batteryStatus)
            )
        }

        // 5. Flashlight / Torch Toggle (Step 159 & 160)
        if (text.contains("टॉर्च") || text.contains("torch") || text.contains("फ्लैश") || text.contains("flashlight")) {
            val turnOn = !text.contains("बंद") && !text.contains("off")
            val torchResult = toggleTorch(context, turnOn)
            val reply = if (torchResult.success) {
                if (turnOn) {
                    if (isHi) "टॉर्च चालू कर दी है।" else "Flashlight turned on."
                } else {
                    if (isHi) "टॉर्च बंद कर दी है।" else "Flashlight turned off."
                }
            } else {
                if (isHi) "टॉर्च चालू नहीं हो सकी: ${torchResult.message}" else "Could not operate torch: ${torchResult.message}"
            }
            return CommandResult(
                handled = true,
                replyText = reply,
                actionType = "TORCH",
                serviceResult = torchResult
            )
        }

        // 6. Maps & Navigation (Step 154)
        if (text.startsWith("मैप") || text.contains("maps") || text.contains("नक्शा") || text.startsWith("रास्ता") || text.contains("directions to")) {
            val destination = extractAfter(raw, listOf("directions to", "रास्ता", "map of", "maps", "मैप", "नेविगेशन")).trim()
            val uriStr = if (destination.isNotBlank()) {
                "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(destination)
            } else {
                "https://maps.google.com"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (launched) {
                if (destination.isNotBlank()) {
                    if (isHi) "Maps पर '$destination' का रास्ता खोल रही हूँ।" else "Opening Maps for '$destination'."
                } else {
                    if (isHi) "Maps खोल रही हूँ।" else "Opening Google Maps."
                }
            } else {
                if (isHi) "Maps ऐप नहीं खोला जा सका।" else "Could not open Maps app."
            }
            return CommandResult(
                handled = true,
                replyText = reply,
                actionType = "MAPS",
                actionUrl = uriStr,
                actionIntent = intent,
                serviceResult = MyraResult(launched, reply)
            )
        }

        // 7. SMS Composer (Step 153) - Never silent sending, opens system composer for user review!
        if (text.startsWith("मैसेज") || text.startsWith("sms") || text.startsWith("संदेश")) {
            val clean = raw.substringAfter(' ').trim()
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                if (clean.isNotBlank()) {
                    putExtra("sms_body", clean)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launched = safeLaunch(context, intent)
            val reply = if (launched) {
                if (isHi) "SMS कंपोजर खोल रही हूँ। कृपया संदेश देखकर भेजें।" else "Opening SMS composer. Review your message before sending."
            } else {
                if (isHi) "मैसेजिंग ऐप नहीं खोला जा सका।" else "Could not open SMS composer."
            }
            return CommandResult(
                handled = true,
                replyText = reply,
                actionType = "SMS",
                actionIntent = intent,
                serviceResult = MyraResult(launched, reply)
            )
        }

        // 8. Settings navigation (Step 151)
        if (text.contains("वाईफाई") || text.contains("wifi") || text.contains("wi-fi")) {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "Wi-Fi सेटिंग खोल रही हूँ।" else "Opening Wi-Fi settings."
            return CommandResult(handled = true, replyText = reply, actionType = "SETTINGS_WIFI", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        if (text.contains("ब्लूटूथ") || text.contains("bluetooth")) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "Bluetooth सेटिंग खोल रही हूँ।" else "Opening Bluetooth settings."
            return CommandResult(handled = true, replyText = reply, actionType = "SETTINGS_BT", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // Hardware Volume Controls & Sound Settings
        if (text.contains("वॉल्यूम बढ़ाओ") || text.contains("volume up") || text.contains("आवाज़ बढ़ाओ") || text.contains("volume badhao")) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            val reply = if (isHi) "हाँ जी! वॉल्यूम बढ़ा दिया है।" else "Volume increased."
            return CommandResult(handled = true, replyText = reply, actionType = "VOLUME_UP", serviceResult = MyraResult(true, reply))
        }

        if (text.contains("वॉल्यूम कम करो") || text.contains("volume down") || text.contains("आवाज़ कम करो") || text.contains("volume kam karo")) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            val reply = if (isHi) "हाँ जी! वॉल्यूम कम कर दिया है।" else "Volume lowered."
            return CommandResult(handled = true, replyText = reply, actionType = "VOLUME_DOWN", serviceResult = MyraResult(true, reply))
        }

        if (text.contains("म्यूट") || text.contains("silent") || text.contains("mute") || text.contains("साइलेंट")) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            val reply = if (isHi) "फोन को म्यूट कर दिया है।" else "Muted audio."
            return CommandResult(handled = true, replyText = reply, actionType = "VOLUME_MUTE", serviceResult = MyraResult(true, reply))
        }

        if (text.contains("आवाज़") || text.contains("volume") || text.contains("sound setting")) {
            val intent = Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "साउंड और वॉल्यूम सेटिंग खोल रही हूँ।" else "Opening sound settings."
            return CommandResult(handled = true, replyText = reply, actionType = "SETTINGS_SOUND", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // Deep Android Phone Customization & File Manager
        if (text.contains("फाइल") || text.contains("files") || text.contains("फ़ाइल") || text.contains("document") || text.contains("download")) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "फ़ाइल मैनेजर और स्टोरेज खोल दिया है।" else "Opening File Manager & Documents."
            return CommandResult(handled = true, replyText = reply, actionType = "FILES", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        if (text.contains("स्टोरेज") || text.contains("storage") || text.contains("मेमोरी साफ़")) {
            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "फ़ोन स्टोरेज और मेमोरी सेटिंग्स खोल रही हूँ।" else "Opening Internal Storage settings."
            return CommandResult(handled = true, replyText = reply, actionType = "STORAGE", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // ML Kit Barcode & QR Scanner
        if (text.contains("स्कैन") || text.contains("scan") || text.contains("qr") || text.contains("क्युआर") || text.contains("barcode") || text.contains("बारकोड")) {
            val reply = if (isHi) {
                "हाँ जी! QR कोड या बारकोड स्कैन करने के लिए स्कैनर खोल रही हूँ। आप फोटो चुनकर या कैमरे से स्कैन कर सकते हैं।"
            } else {
                "Opening the QR & Barcode Scanner for you! You can scan from camera or pick a picture."
            }
            return CommandResult(handled = true, replyText = reply, actionType = "SCANNER", serviceResult = MyraResult(true, reply))
        }

        // Maya Guardian AI Call Screening & Voice Biometrics
        if (text.contains("guardian") || text.contains("गार्डियन") || text.contains("कॉल स्क्रीन") || text.contains("screen call") || text.contains("स्कैम") || text.contains("scam")) {
            val activeModel = MayaGuardianEngine.getActiveModel(context)
            val reply = if (isHi) {
                "Maya Guardian AI एक्टिव है! यह ${activeModel.label} मॉडल द्वारा वॉइस बायोमेट्रिक्स और स्कैम कॉल स्क्रीनिंग करता है।"
            } else {
                "Maya Guardian AI is active! Protecting against voice scams and robocalls using the ${activeModel.label} model."
            }
            return CommandResult(handled = true, replyText = reply, actionType = "GUARDIAN", serviceResult = MyraResult(true, reply))
        }

        // Maya Live Wallpaper on Home Screen
        if (text.contains("लाइव वॉलपेपर") || text.contains("live wallpaper") || text.contains("वॉलपेपर लगाओ") || text.contains("home screen wallpaper")) {
            val launched = MayaWallpaperService.launchLiveWallpaperPicker(context)
            val reply = if (isHi) {
                "Maya को आपकी होम स्क्रीन पर लाइव वॉलपेपर लगाने के लिए वॉलपेपर पिकर खोल दिया है! वह सांस लेगी, छूने पर रिएक्ट करेगी और स्क्रीन कवर होने पर अपने आप पॉज हो जाएगी।"
            } else {
                "Opening Live Wallpaper picker for Maya! She breathes, looks up, reacts to your touch, and pauses when the screen is covered."
            }
            return CommandResult(handled = true, replyText = reply, actionType = "LIVE_WALLPAPER", serviceResult = MyraResult(launched, reply))
        }

        // Maya Accessibility Screen Reader & App Controller
        if (text.contains("स्क्रीन पढ़ो") || text.contains("read screen") || text.contains("screen read") || text.contains("स्क्रीन पर क्या")) {
            val service = MayaAccessibilityService.instance
            if (service != null) {
                val screenText = service.readCurrentScreen()
                val reply = if (isHi) "स्क्रीन पर यह सामग्री है:\n$screenText" else "Here is what is on your screen:\n$screenText"
                return CommandResult(handled = true, replyText = reply, actionType = "SCREEN_READ", serviceResult = MyraResult(true, reply))
            } else {
                val reply = if (isHi) {
                    "स्क्रीन पढ़ने और ऐप्स कंट्रोल करने के लिए Maya Accessibility Service ऑन करें।"
                } else {
                    "Please turn on Maya Accessibility Service in Settings to read screen and control apps."
                }
                return CommandResult(handled = true, replyText = reply, actionType = "ACCESSIBILITY", serviceResult = MyraResult(false, reply))
            }
        }

        if (text.contains("एक्सेसिबिलिटी") || text.contains("accessibility")) {
            MayaAccessibilityService.openAccessibilitySettings(context)
            val reply = if (isHi) "Maya Accessibility सेटिंग्स खोल रही हूँ।" else "Opening Accessibility settings."
            return CommandResult(handled = true, replyText = reply, actionType = "ACCESSIBILITY", serviceResult = MyraResult(true, reply))
        }

        if (text.contains("वॉलपेपर") || text.contains("wallpaper") || text.contains("डिस्प्ले") || text.contains("display")) {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "डिस्प्ले और वॉलपेपर सेटिंग्स खोल रही हूँ।" else "Opening Display & Wallpaper settings."
            return CommandResult(handled = true, replyText = reply, actionType = "DISPLAY", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        if (text.contains("ऐप्स") || text.contains("apps") || text.contains("manage apps")) {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "ऐप मैनेजर सेटिंग्स खोल रही हूँ।" else "Opening Application settings."
            return CommandResult(handled = true, replyText = reply, actionType = "APPS_SETTINGS", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        if (text.contains("सेटिंग") || text.contains("settings")) {
            val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "फोन की सेटिंग खोल रही हूँ।" else "Opening system settings."
            return CommandResult(handled = true, replyText = reply, actionType = "SETTINGS", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // 9. Alarms & Timers (Step 163 & 164)
        if (text.contains("टाइमर") || text.contains("timer")) {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "टाइमर सेट करने के लिए टाइमर ऐप खोल दिया है।" else "Opening timer app."
            return CommandResult(handled = true, replyText = reply, actionType = "TIMER", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        if (text.contains("अलार्म") || text.contains("alarm")) {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) {
                "अलार्म ऐप खोल दिया है। आप मनचाहा समय सेट कर सकते हैं।"
            } else {
                "Opening alarm app. You can verify and set your alarm."
            }
            return CommandResult(handled = true, replyText = reply, actionType = "ALARM", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // 10. YouTube
        if (text.contains("यूट्यूब") || text.contains("youtube")) {
            val query = if (text.contains("खोजो") || text.contains("search") || text.contains("play")) {
                extractAfter(raw, listOf("यूट्यूब पर", "यूट्यूब में", "youtube पर", "search on youtube", "on youtube", "play"))
            } else ""

            val url = if (query.isNotBlank()) {
                "https://www.youtube.com/results?search_query=" + Uri.encode(query)
            } else {
                "https://www.youtube.com"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (query.isNotBlank()) {
                if (isHi) "YouTube पर '$query' खोज रही हूँ।" else "Searching YouTube for '$query'."
            } else {
                if (isHi) "YouTube खोल रही हूँ।" else "Opening YouTube."
            }
            return CommandResult(handled = true, replyText = reply, actionType = "YOUTUBE", actionUrl = url, actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // 11. Google / Web Search with safe URL validation (Step 173 & 174)
        if (text.startsWith("खोजो ") || text.startsWith("search ") || text.startsWith("गूगल पर खोजो ") || text.contains("google search")) {
            val query = extractAfter(raw, listOf("खोजो", "search", "गूगल पर खोजो", "google search")).trim()
            if (query.isNotBlank()) {
                val url = "https://www.google.com/search?q=" + Uri.encode(query)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val launched = safeLaunch(context, intent)
                val reply = if (isHi) "Google पर '$query' खोज रही हूँ।" else "Searching Google for '$query'."
                return CommandResult(handled = true, replyText = reply, actionType = "SEARCH", actionUrl = url, actionIntent = intent, serviceResult = MyraResult(launched, reply))
            }
        }

        if (text == "गूगल" || text == "google" || text.contains("गूगल खोलो") || text.contains("open google")) {
            val url = "https://www.google.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "Google खोल रही हूँ।" else "Opening Google."
            return CommandResult(handled = true, replyText = reply, actionType = "GOOGLE", actionUrl = url, actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // 12. Phone Dialer (Step 152) - User confirms and presses call, never silent dialing!
        if (text.contains("कॉल") || text.contains("dial") || text.contains("phone") || text.contains("डायल") || text.contains("call")) {
            val digits = raw.filter { it.isDigit() || it == '+' }
            val intent = if (digits.length >= 3) {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val launched = safeLaunch(context, intent)
            val reply = if (digits.length >= 3) {
                if (isHi) "डायल पैड पर $digits लोड कर दिया है। पुष्टि करके कॉल करें।" else "Dialer opened with $digits. Press call to connect."
            } else {
                if (isHi) "डायल पैड खोल रही हूँ।" else "Opening phone dialer."
            }
            return CommandResult(handled = true, replyText = reply, actionType = "DIAL", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // 13. Camera
        if (text.contains("कैमरा") || text.contains("camera") || text.contains("फोटो खींचो") || text.contains("take photo")) {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = safeLaunch(context, intent)
            val reply = if (isHi) "कैमरा खोल रही हूँ।" else "Opening camera."
            return CommandResult(handled = true, replyText = reply, actionType = "CAMERA", actionIntent = intent, serviceResult = MyraResult(launched, reply))
        }

        // 14. Simple On-device Math evaluation
        val mathResult = tryEvaluateMath(text)
        if (mathResult != null) {
            val reply = if (isHi) "इसका उत्तर $mathResult है।" else "The answer is $mathResult."
            return CommandResult(handled = true, replyText = reply, actionType = "CALCULATION", serviceResult = MyraResult(true, "Math result: $mathResult"))
        }

        // 15. Dynamic App Automation & Launching
        val autoResult = MyraAppAutomation.tryAutomateApp(context, raw, languageCode)
        if (autoResult != null && autoResult.success) {
            return CommandResult(
                handled = true,
                replyText = autoResult.message,
                actionType = "APP_LAUNCH",
                serviceResult = autoResult
            )
        }

        return CommandResult(handled = false, replyText = "")
    }

    private fun toggleTorch(context: Context, turnOn: Boolean): MyraResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return MyraResult(false, "CameraManager not available")

            var cameraIdWithFlash: String? = null
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) {
                    cameraIdWithFlash = id
                    break
                }
            }

            if (cameraIdWithFlash == null) {
                return MyraResult(false, "इस फोन में फ्लैशलाइट उपलब्ध नहीं है।")
            }

            cameraManager.setTorchMode(cameraIdWithFlash, turnOn)
            isTorchOn = turnOn
            MyraResult(true, if (turnOn) "Torch ON" else "Torch OFF")
        } catch (e: Exception) {
            MyraResult(false, e.message ?: "Torch error")
        }
    }

    private fun getBatteryInfo(context: Context, languageCode: String): String {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val pct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 0

            if (languageCode.startsWith("hi")) {
                val chargeMsg = if (isCharging) "और फोन चार्ज हो रहा है।" else "और फोन चार्ज नहीं हो रहा है।"
                "फोन की बैटरी $pct% है $chargeMsg"
            } else {
                val chargeMsg = if (isCharging) "and the device is charging." else "and not charging."
                "Battery level is $pct% $chargeMsg"
            }
        } catch (e: Exception) {
            if (languageCode.startsWith("hi")) "बैटरी की जानकारी नहीं मिल सकी।" else "Could not determine battery status."
        }
    }

    private fun tryEvaluateMath(input: String): String? {
        val clean = input.replace("what is", "")
            .replace("कितना होगा", "")
            .replace("calculate", "")
            .replace("plus", "+")
            .replace("धन", "+")
            .replace("जोड़", "+")
            .replace("minus", "-")
            .replace("घटाव", "-")
            .replace("times", "*")
            .replace("multiply", "*")
            .replace("गुना", "*")
            .replace("गुणा", "*")
            .replace("divided by", "/")
            .replace("divide", "/")
            .replace("भाग", "/")
            .trim()

        val pattern = Regex("""(\d+(?:\.\d+)?)\s*([\+\-\*\/])\s*(\d+(?:\.\d+)?)""")
        val match = pattern.find(clean) ?: return null

        val (num1Str, op, num2Str) = match.destructured
        val num1 = num1Str.toDoubleOrNull() ?: return null
        val num2 = num2Str.toDoubleOrNull() ?: return null

        val res = when (op) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "*" -> num1 * num2
            "/" -> if (num2 != 0.0) num1 / num2 else Double.NaN
            else -> return null
        }

        if (res.isNaN()) return "Undefined"
        return if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.2f", res)
    }

    private fun extractAfter(text: String, keywords: List<String>): String {
        var result = text
        for (kw in keywords) {
            val idx = result.indexOf(kw, ignoreCase = true)
            if (idx >= 0) {
                result = result.substring(idx + kw.length).trim()
            }
        }
        return result
    }

    private fun safeLaunch(context: Context, intent: Intent): Boolean {
        return try {
            // Strict scheme validation (Step 173)
            val uri = intent.data
            if (uri != null) {
                val scheme = uri.scheme?.lowercase(Locale.ROOT)
                if (scheme != null && scheme !in listOf("http", "https", "tel", "smsto", "geo")) {
                    return false
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

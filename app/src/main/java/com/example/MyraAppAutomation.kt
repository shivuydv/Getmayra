package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import java.util.Locale

object MyraAppAutomation {

    // Common app aliases mapping to standard package keywords
    private val APP_ALIASES = mapOf(
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "व्हाट्सएप" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "instagram" to listOf("com.instagram.android"),
        "इंस्टाग्राम" to listOf("com.instagram.android"),
        "insta" to listOf("com.instagram.android"),
        "spotify" to listOf("com.spotify.music"),
        "स्पॉटिफ़ाई" to listOf("com.spotify.music"),
        "gaana" to listOf("com.gaana", "com.spotify.music"),
        "telegram" to listOf("org.telegram.messenger"),
        "टेलीग्राम" to listOf("org.telegram.messenger"),
        "youtube" to listOf("com.google.android.youtube"),
        "यूट्यूब" to listOf("com.google.android.youtube"),
        "chrome" to listOf("com.android.chrome"),
        "क्रोम" to listOf("com.android.chrome"),
        "browser" to listOf("com.android.chrome"),
        "netflix" to listOf("com.netflix.mediaclient"),
        "नेटफ्लिक्स" to listOf("com.netflix.mediaclient"),
        "snapchat" to listOf("com.snapchat.android"),
        "स्नैपचैट" to listOf("com.snapchat.android"),
        "facebook" to listOf("com.facebook.katana"),
        "फेसबुक" to listOf("com.facebook.katana"),
        "twitter" to listOf("com.twitter.android"),
        "x" to listOf("com.twitter.android"),
        "ट्विटर" to listOf("com.twitter.android"),
        "gmail" to listOf("com.google.android.gm"),
        "जीमेल" to listOf("com.google.android.gm"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "कैलकुलेटर" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "camera" to listOf("com.google.android.GoogleCamera", "com.android.camera"),
        "कैमरा" to listOf("com.google.android.GoogleCamera", "com.android.camera"),
        "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
        "गैलरी" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
        "photos" to listOf("com.google.android.apps.photos"),
        "photo" to listOf("com.google.android.apps.photos"),
        "play store" to listOf("com.android.vending"),
        "प्ले स्टोर" to listOf("com.android.vending"),
        "maps" to listOf("com.google.android.apps.maps"),
        "मैप" to listOf("com.google.android.apps.maps"),
        "phone" to listOf("com.google.android.dialer", "com.android.dialer"),
        "contacts" to listOf("com.google.android.contacts", "com.android.contacts"),
        "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "files" to listOf("com.google.android.apps.nbu.files", "com.android.documentsui")
    )

    fun tryAutomateApp(context: Context, command: String, languageCode: String): MyraResult? {
        val clean = command.lowercase(Locale.ROOT).trim()

        // Check if command is asking to open or launch an app
        val isLaunchCommand = clean.contains("खोलो") || clean.contains("open") ||
                clean.contains("chalao") || clean.contains("चलाओ") ||
                clean.contains("launch") || clean.contains("start") ||
                clean.contains("bhejo") || clean.contains("send")

        if (!isLaunchCommand) return null

        val isHi = languageCode.startsWith("hi")

        // Check for WhatsApp message intent
        if ((clean.contains("whatsapp") || clean.contains("व्हाट्सएप")) && (clean.contains("message") || clean.contains("bhejo") || clean.contains("भेजो"))) {
            return launchWhatsAppComposer(context, isHi)
        }

        val targetName = clean
            .replace("open", "")
            .replace("खोलो", "")
            .replace("chalao", "")
            .replace("चलाओ", "")
            .replace("launch", "")
            .replace("start", "")
            .replace("app", "")
            .replace("ऐप", "")
            .trim()

        if (targetName.isBlank()) return null

        val pm = context.packageManager

        // 1. Check known alias packages
        val candidatePackages = APP_ALIASES[targetName]
        if (candidatePackages != null) {
            for (pkg in candidatePackages) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val msg = if (isHi) {
                        "हाँ जी! आपके लिए $targetName ऐप खोल दिया है।"
                    } else {
                        "Opening $targetName for you right now!"
                    }
                    return MyraResult(true, msg, payload = pkg)
                }
            }
        }

        // 2. Query all installed launcher activities dynamically
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)

            for (info in resolveInfos) {
                val appLabel = info.loadLabel(pm).toString().lowercase(Locale.ROOT)
                val packageName = info.activityInfo.packageName

                if (appLabel.contains(targetName) || targetName.contains(appLabel)) {
                    val launchIntent = pm.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        val displayLabel = info.loadLabel(pm).toString()
                        val msg = if (isHi) {
                            "हाँ जी! $displayLabel खोल दिया है।"
                        } else {
                            "Launching $displayLabel for you!"
                        }
                        return MyraResult(true, msg, payload = packageName)
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Search on Google Play Store or Web
        val webUrl = "https://www.google.com/search?q=" + Uri.encode(targetName)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            val msg = if (isHi) {
                "फ़ोन में $targetName ऐप नहीं मिला, इसलिए ब्राउज़र में खोल रही हूँ!"
            } else {
                "Couldn't find $targetName on device, searching on the web instead!"
            }
            MyraResult(true, msg, payload = webUrl)
        } catch (_: Exception) {
            null
        }
    }

    private fun launchWhatsAppComposer(context: Context, isHi: Boolean): MyraResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?text=")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val msg = if (isHi) {
                "WhatsApp खोल दिया है! जिसे चाहें संदेश भेज सकते हैं।"
            } else {
                "Opening WhatsApp composer for you!"
            }
            MyraResult(true, msg, payload = "com.whatsapp")
        } catch (e: Exception) {
            val msg = if (isHi) "WhatsApp खोलने में असमर्थ।" else "Unable to open WhatsApp."
            MyraResult(false, msg)
        }
    }
}

package com.example

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CardSurfaceElevated
import com.example.ui.theme.CoralError
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyraUi(autoStartSession: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var state by remember { mutableStateOf(MyraVoiceState.IDLE) }
    var statusText by remember { mutableStateOf("Session बंद है। माइक बटन दबाएं।") }
    var transcript by remember { mutableStateOf("") }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var sessionStarted by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showApiDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showGuardianDialog by remember { mutableStateOf(false) }

    var currentLang by remember { mutableStateOf("hi-IN") }
    var personalityMode by remember { mutableStateOf(MyraPreferences.getPersonalityMode(context)) }
    var aiProvider by remember { mutableStateOf(MyraPreferences.getAiProvider(context)) }
    var voiceEngine by remember { mutableStateOf(MyraPreferences.getVoiceEngine(context)) }
    var displayMode by remember { mutableStateOf(MyraPreferences.getDisplayMode(context)) } // "ANIME" vs "CYBER_HUD"
    var pitchVal by remember { mutableFloatStateOf(1.25f) }
    var speedVal by remember { mutableFloatStateOf(1.05f) }
    var latestSpeechText by remember { mutableStateOf("") }

    val messages = remember { mutableStateListOf<MyraMessage>() }
    var controller by remember { mutableStateOf<MyraVoiceController?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sessionStarted = true
            controller?.startSession()
        } else {
            statusText = "Microphone अनुमति आवश्यक है।"
            showPermissionSettingsDialog = true
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenList?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                controller?.processDirectText(spokenText)
            }
        }
    }

    val triggerVoiceInput: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            sessionStarted = true
            controller?.startSession()
        }
    }

    DisposableEffect(context) {
        val voiceCtrl = MyraVoiceController(
            context = context,
            onState = { newState, message ->
                state = newState
                statusText = message
                if (newState == MyraVoiceState.IDLE) {
                    sessionStarted = false
                }
            },
            onTranscript = { heard ->
                transcript = heard
            },
            onRmsChanged = { rms ->
                rmsLevel = rms.coerceAtLeast(0f)
            },
            onMessageGenerated = { role, text, actionType, actionUrl ->
                if (role == "MYRA") {
                    latestSpeechText = text
                    // Check if voice command asked to switch mode
                    if (actionType == "MODE_SWITCH") {
                        displayMode = MyraPreferences.getDisplayMode(context)
                    }
                }
                messages.add(
                    MyraMessage(
                        role = role,
                        text = text,
                        actionType = actionType,
                        actionUrl = actionUrl
                    )
                )
            }
        )
        controller = voiceCtrl

        if (autoStartSession) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                sessionStarted = true
                voiceCtrl.startSession()
            }
        }

        onDispose {
            voiceCtrl.release()
            controller = null
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // 1. Header Bar with Mode Toggle, State Badge & Menus
            MyraHeader(
                state = state,
                isOnline = MyraAiEngine.isDeviceOnline(context),
                aiProvider = aiProvider,
                personalityMode = personalityMode,
                displayMode = displayMode,
                onToggleDisplayMode = {
                    val next = if (displayMode == "ANIME") "CYBER_HUD" else "ANIME"
                    displayMode = next
                    MyraPreferences.setDisplayMode(context, next)
                },
                onApiKeysClick = { showApiDialog = true },
                onInfoClick = { showInfoDialog = true },
                onPrivacyClick = { showPrivacyDialog = true },
                onSettingsClick = { showSettingsSheet = true }
            )

            // 2. Real-Time System Telemetry Card (Maya HUD adaptation)
            SystemTelemetryCard(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                onExpandClick = { showInfoDialog = true }
            )

            // 3. Center Interactive Visualization: ANIME Girl or CYBER HUD ORB
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (displayMode == "CYBER_HUD") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Maya Sound-Reactive Holographic AI Orb
                        MayaOrbWidget(
                            voiceState = state,
                            rmsLevel = rmsLevel
                        )

                        Spacer(Modifier.height(4.dp))

                        // Dynamic Maya Mic Soundwave Spectrum Bars
                        MayaMicVisualizer(
                            rmsLevel = rmsLevel,
                            isActive = (state == MyraVoiceState.LISTENING || state == MyraVoiceState.SPEAKING)
                        )
                    }
                } else {
                    // Expressive Anime Girlfriend Avatar (Blinking, mouth flapping, hair sway)
                    MyraAnimeCompanion(
                        voiceState = state,
                        latestSpeech = latestSpeechText
                    )
                }
            }

            // 4. Status text & Stop/Retry Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    ),
                    color = if (state == MyraVoiceState.ERROR) CoralError else TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (state == MyraVoiceState.SPEAKING) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { controller?.stopSpeaking() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", fontSize = 11.sp)
                    }
                } else if (state == MyraVoiceState.ERROR) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { controller?.retry() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry", fontSize = 11.sp)
                    }
                }
            }

            // Live transcript bubble if speech detected
            AnimatedVisibility(
                visible = transcript.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "\"$transcript\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 5. Quick Actions & Automations (WhatsApp, Spotify, Insta, System, Mode toggle)
            MyraQuickActions(
                displayMode = displayMode,
                onToggleMode = {
                    val next = if (displayMode == "ANIME") "CYBER_HUD" else "ANIME"
                    displayMode = next
                    MyraPreferences.setDisplayMode(context, next)
                },
                onActionSelected = { command ->
                    if (sessionStarted) {
                        controller?.processDirectText(command)
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            sessionStarted = true
                            controller?.startSession()
                            controller?.processDirectText(command)
                        } else {
                            controller?.processDirectText(command)
                        }
                    }
                }
            )

            // 6. Conversation Timeline
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        EmptyConversationPlaceholder(displayMode = displayMode)
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        MyraMessageBubble(
                            message = msg,
                            onSpeakAgain = { controller?.speak(msg.text) },
                            onOpenAction = { url, actionType ->
                                try {
                                    if (!url.isNullOrBlank()) {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } else {
                                        val intent = when (actionType) {
                                            "LIVE_WALLPAPER" -> {
                                                MayaWallpaperService.launchLiveWallpaperPicker(context)
                                                null
                                            }
                                            "SCANNER" -> {
                                                showScannerDialog = true
                                                null
                                            }
                                            "GUARDIAN" -> {
                                                showGuardianDialog = true
                                                null
                                            }
                                            "ACCESSIBILITY" -> {
                                                MayaAccessibilityService.openAccessibilitySettings(context)
                                                null
                                            }
                                            "SCREEN_READ" -> {
                                                val text = MayaAccessibilityService.instance?.readCurrentScreen()
                                                if (text != null) {
                                                    controller?.processDirectText("स्क्रीन पढ़ो")
                                                } else {
                                                    MayaAccessibilityService.openAccessibilitySettings(context)
                                                }
                                                null
                                            }
                                            "FILES" -> Intent(Intent.ACTION_GET_CONTENT).apply {
                                                type = "*/*"
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                            }
                                            "STORAGE" -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                                            "DISPLAY" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                                            "APPS_SETTINGS" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
                                            "SETTINGS_WIFI" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                                            "SETTINGS_BT" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                            "SETTINGS_SOUND" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                                            "SETTINGS" -> Intent(Settings.ACTION_SETTINGS)
                                            else -> null
                                        }
                                        if (intent != null) {
                                            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Action unavailable", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // 7. Bottom Interactive Controls: Session Mic + Instant Text Input
            MyraBottomControls(
                sessionStarted = sessionStarted,
                state = state,
                textInput = textInput,
                onTextInputChange = { textInput = it },
                onSendText = {
                    val query = textInput.trim()
                    if (query.isNotEmpty()) {
                        controller?.processDirectText(query)
                        textInput = ""
                    }
                },
                onVoiceInputClick = triggerVoiceInput,
                onToggleSession = {
                    if (sessionStarted) {
                        controller?.stopSession()
                        sessionStarted = false
                    } else {
                        triggerVoiceInput()
                    }
                }
            )
        }
    }

    // AI & Voice API Key Management Dialog
    if (showApiDialog) {
        MyraApiKeyDialog(
            currentProvider = aiProvider,
            currentGeminiKey = MyraPreferences.getGeminiApiKey(context),
            currentOpenAiKey = MyraPreferences.getOpenAiApiKey(context),
            currentElevenLabsKey = MyraPreferences.getElevenLabsApiKey(context),
            currentVoiceId = MyraPreferences.getElevenLabsVoiceId(context),
            currentEngine = voiceEngine,
            currentUserName = MyraPreferences.getUserName(context),
            onSave = { provider, geminiKey, openAiKey, elevenLabsKey, voiceId, engine, name ->
                MyraPreferences.setAiProvider(context, provider)
                MyraPreferences.setGeminiApiKey(context, geminiKey)
                MyraPreferences.setOpenAiApiKey(context, openAiKey)
                MyraPreferences.setElevenLabsApiKey(context, elevenLabsKey)
                MyraPreferences.setElevenLabsVoiceId(context, voiceId)
                MyraPreferences.setVoiceEngine(context, engine)
                MyraPreferences.setUserName(context, name)
                aiProvider = provider
                voiceEngine = engine
                showApiDialog = false
                Toast.makeText(context, "API Keys & AI Engine Saved Successfully!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showApiDialog = false }
        )
    }

    // Settings Modal Sheet
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = CardSurface
        ) {
            MyraSettingsContent(
                currentLang = currentLang,
                personalityMode = personalityMode,
                displayMode = displayMode,
                pitch = pitchVal,
                speed = speedVal,
                onLangChange = { newLang ->
                    currentLang = newLang
                    controller?.setLanguage(newLang)
                },
                onPersonalityChange = { newMode ->
                    personalityMode = newMode
                    MyraPreferences.setPersonalityMode(context, newMode)
                },
                onDisplayModeChange = { newMode ->
                    displayMode = newMode
                    MyraPreferences.setDisplayMode(context, newMode)
                },
                onPitchChange = { newPitch ->
                    pitchVal = newPitch
                    controller?.setPitch(newPitch)
                },
                onSpeedChange = { newSpeed ->
                    speedVal = newSpeed
                    controller?.setSpeed(newSpeed)
                },
                onClearHistoryRequest = {
                    showClearConfirmDialog = true
                },
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showSettingsSheet = false }
                }
            )
        }
    }

    // Clear History Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Conversation?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("क्या आप पूरी बातचीत का इतिहास हटाना चाहते हैं?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        messages.clear()
                        showClearConfirmDialog = false
                        showSettingsSheet = false
                        Toast.makeText(context, "इतिहास साफ कर दिया गया।", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = CoralError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }

    // Permission Settings Dialog
    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            title = { Text("Microphone Permission Required", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "मायरा आपकी आवाज़ सुनकर बात करती है। कृपया Android Settings में जाकर Microphone अनुमति प्रदान करें।",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionSettingsDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:" + context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Open Settings", color = ObsidianBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurface
        )
    }

    // Diagnostics & Hardware Telemetry Dialog (Deep Phone Stats)
    if (showInfoDialog) {
        val t = AndroidSystemMonitor.getTelemetry(context)
        val speechAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val hasGemini = MyraPreferences.getGeminiApiKey(context).isNotBlank()
        val hasOpenAi = MyraPreferences.getOpenAiApiKey(context).isNotBlank()
        val hasElevenLabs = MyraPreferences.getElevenLabsApiKey(context).isNotBlank()

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Phone Hardware & HUD Stats", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("• Device: ${t.deviceModel} (${t.androidVersion})", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("• Battery: ${t.batteryPct}% (${t.chargingSource}) • Temp: ${t.batteryTempCelsius}°C • Health: ${t.batteryHealth}", color = TextSecondary, fontSize = 12.sp)
                    Text("• RAM: ${t.usedRamPct}% used (${String.format(Locale.US, "%.1f", t.availRamGb)} GB free of ${String.format(Locale.US, "%.1f", t.totalRamGb)} GB)", color = TextSecondary, fontSize = 12.sp)
                    Text("• Storage: ${t.usedStoragePct}% used (${String.format(Locale.US, "%.0f", t.availStorageGb)} GB free)", color = TextSecondary, fontSize = 12.sp)
                    Text("• Network: ${t.networkType} (Status: ${if (t.isOnline) "Connected" else "Offline"})", color = TextSecondary, fontSize = 12.sp)
                    Text("• Uptime: ${t.uptimeFormatted} • Active Mode: $displayMode", color = TextSecondary, fontSize = 12.sp)
                    Text("• Active AI: $aiProvider • Speech Recognizer: ${if (speechAvailable) "Online" else "Offline"}", color = TextSecondary, fontSize = 12.sp)
                    Text("• Voice Engine: ${if (hasElevenLabs && voiceEngine == "ELEVENLABS") "ElevenLabs Active" else "Device Cute TTS"}", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close", color = NeonCyan)
                }
            },
            containerColor = CardSurface
        )
    }

    // Privacy Notice Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldActive)
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy & Safety", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Private API Keys: Your Gemini, ChatGPT & ElevenLabs keys remain securely on your device.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Microphone: Listens only when you tap Start or during an active session.", color = TextSecondary, fontSize = 13.sp)
                    Text("• Safe Automations: App opening, calls and SMS always require system confirmation.", color = TextSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood", color = EmeraldActive)
                }
            },
            containerColor = CardSurface
        )
    }

    // Maya QR & Barcode Scanner Dialog
    if (showScannerDialog) {
        MayaScannerDialog(
            onDismiss = { showScannerDialog = false },
            onCodeDetected = { res ->
                showScannerDialog = false
                controller?.processDirectText(res.formattedSummary)
            }
        )
    }

    // Maya Guardian AI Call Screening Dialog
    if (showGuardianDialog) {
        MayaGuardianDialog(
            scope = scope,
            onDismiss = { showGuardianDialog = false },
            onScreenResult = { res ->
                showGuardianDialog = false
                controller?.processDirectText("कॉल स्क्रीनिंग परिणाम: ${res.verdict.name} - ${res.detailedAdvice}")
            }
        )
    }
}

@Composable
private fun MyraHeader(
    state: MyraVoiceState,
    isOnline: Boolean,
    aiProvider: String,
    personalityMode: String,
    displayMode: String,
    onToggleDisplayMode: () -> Unit,
    onApiKeysClick: () -> Unit,
    onInfoClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val stateColor by animateColorAsState(
        targetValue = when (state) {
            MyraVoiceState.LISTENING -> EmeraldActive
            MyraVoiceState.SPEAKING -> NeonCyan
            MyraVoiceState.PROCESSING -> ElectricViolet
            MyraVoiceState.CONNECTING -> NeonCyan
            MyraVoiceState.ERROR -> CoralError
            MyraVoiceState.IDLE -> TextTertiary
        },
        label = "stateColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MYRA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = TextPrimary
                )
                Spacer(Modifier.width(6.dp))
                // Animated Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(stateColor.copy(alpha = 0.15f))
                        .border(1.dp, stateColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(stateColor)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = state.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = if (isOnline) "Online" else "Offline",
                    tint = if (isOnline) NeonCyan.copy(alpha = 0.7f) else TextTertiary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = "$aiProvider • ${if (displayMode == "ANIME") "👧 ANIME GF" else "🔮 CYBER HUD"}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontSize = 10.sp),
                color = ElectricVioletLight
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Visual Mode Switch Button
            Surface(
                onClick = onToggleDisplayMode,
                shape = RoundedCornerShape(12.dp),
                color = if (displayMode == "CYBER_HUD") NeonCyan.copy(alpha = 0.2f) else ElectricViolet.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (displayMode == "CYBER_HUD") NeonCyan else ElectricVioletLight),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (displayMode == "ANIME") "🔮 HUD" else "👧 GF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (displayMode == "CYBER_HUD") NeonCyan else ElectricVioletLight
                    )
                }
            }

            IconButton(
                onClick = onApiKeysClick,
                modifier = Modifier.testTag("api_keys_button")
            ) {
                Icon(Icons.Default.Key, contentDescription = "API Keys", tint = NeonCyan, modifier = Modifier.size(19.dp))
            }
            IconButton(
                onClick = onPrivacyClick,
                modifier = Modifier.testTag("privacy_button")
            ) {
                Icon(Icons.Default.Security, contentDescription = "Privacy", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.testTag("info_button")
            ) {
                Icon(Icons.Default.Info, contentDescription = "Diagnostics", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("settings_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MyraQuickActions(
    displayMode: String,
    onToggleMode: () -> Unit,
    onActionSelected: (String) -> Unit
) {
    val actions = listOf(
        "🔮 ${if (displayMode == "ANIME") "Switch to HUD" else "Switch to GF"}" to "मोड बदलो",
        "🛡️ Guardian" to "guardian कॉल स्क्रीनिंग",
        "📷 Scan QR" to "qr कोड स्कैन करो",
        "🎀 Live Wallpaper" to "लाइव वॉलपेपर लगाओ",
        "📖 Read Screen" to "स्क्रीन पढ़ो",
        "🦾 Accessibility" to "एक्सेसिबिलिटी",
        "📂 Files & Storage" to "फ़ाइल्स खोलो",
        "💾 Storage Info" to "स्टोरेज खोलो",
        "🎨 Wallpaper & Display" to "वॉलपेपर बदलो",
        "📱 WhatsApp" to "WhatsApp खोलो",
        "📲 Manage Apps" to "ऐप्स खोलो",
        "⚙️ Phone Settings" to "सेटिंग खोलो",
        "💖 I Love You" to "मायरा आई लव यू",
        "🔥 Dikhao Nakhre" to "थोड़े नखरे दिखाओ",
        "🎵 Spotify" to "Spotify चलाओ",
        "🔊 Volume Up" to "वॉल्यूम बढ़ाओ",
        "🔉 Volume Down" to "वॉल्यूम कम करो",
        "🔦 Torch" to "टॉर्च चालू करो",
        "📷 Camera" to "कैमरा खोलो",
        "⚡ System Stats" to "सिस्टम स्टेटस बताओ",
        "📸 Instagram" to "Instagram खोलो",
        "▶️ YouTube" to "YouTube खोलो",
        "🗺️ Maps" to "मैप खोलो",
        "⏰ Alarms" to "अलार्म खोलो",
        "💡 Joke" to "चुटकुला सुनाओ"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        actions.forEach { (label, command) ->
            FilterChip(
                selected = false,
                onClick = {
                    if (command == "मोड बदलो") {
                        onToggleMode()
                    } else {
                        onActionSelected(command)
                    }
                },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CardSurface,
                    labelColor = TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = CardSurfaceElevated,
                    enabled = true,
                    selected = false
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun EmptyConversationPlaceholder(displayMode: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (displayMode == "CYBER_HUD") "MAYA HUD ONLINE • Tap Start or Type below" else "START SESSION दबाइए या बोलिए: \"हे मायरा\"",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "जैसे: \"WhatsApp खोलो\", \"सिस्टम स्टेटस\", \"YouTube खोलो\", \"आई लव यू\"",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MyraMessageBubble(
    message: MyraMessage,
    onSpeakAgain: () -> Unit,
    onOpenAction: (url: String?, type: String?) -> Unit
) {
    val context = LocalContext.current
    val isUser = message.role == "YOU"
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .testTag(if (isUser) "user_message_bubble" else "myra_message_bubble"),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) NeonCyan.copy(alpha = 0.16f) else CardSurface
            ),
            border = if (!isUser) {
                androidx.compose.foundation.BorderStroke(1.dp, CardSurfaceElevated)
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
            }
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header (Role & Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "YOU" else "MYRA 💕",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isUser) NeonCyan else ElectricVioletLight
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Message Text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )

                // Optional Action Button
                if (!message.actionUrl.isNullOrBlank() || message.actionType != null) {
                    val actionLabel = when (message.actionType) {
                        "YOUTUBE" -> "Open YouTube"
                        "SEARCH", "GOOGLE" -> "Open Search"
                        "MAPS" -> "Open Maps"
                        "SMS" -> "Review SMS"
                        "SETTINGS", "SETTINGS_WIFI", "SETTINGS_BT", "SETTINGS_SOUND" -> "Open Settings"
                        "ALARM" -> "Open Alarms"
                        "TIMER" -> "Open Timers"
                        "DIAL" -> "Open Phone"
                        "CAMERA" -> "Open Camera"
                        "TORCH" -> "Flashlight"
                        "SCANNER" -> "Open QR Scanner"
                        "GUARDIAN" -> "Maya Guardian Shield"
                        "LIVE_WALLPAPER" -> "Apply Live Wallpaper"
                        "ACCESSIBILITY" -> "Enable Accessibility"
                        "SCREEN_READ" -> "Read Screen Again"
                        "FILES" -> "Open Files & Storage"
                        "STORAGE" -> "Open Phone Storage"
                        "DISPLAY" -> "Display & Wallpaper"
                        "APPS_SETTINGS" -> "Manage Apps"
                        "APP_LAUNCH" -> "Launch App"
                        "SYSTEM_TELEMETRY" -> "View System Info"
                        "MODE_SWITCH" -> "Switch Avatar"
                        else -> "Open"
                    }

                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenAction(message.actionUrl, message.actionType) },
                        color = NeonCyan.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }
                }

                // Bubble actions: Re-speak and Copy
                if (!isUser) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSpeakAgain,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Repeat Response",
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("MYRA", message.text))
                                copied = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (copied) EmeraldActive else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyraBottomControls(
    sessionStarted: Boolean,
    state: MyraVoiceState,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSendText: () -> Unit,
    onVoiceInputClick: () -> Unit,
    onToggleSession: () -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = if (sessionStarted) CoralError else NeonCyan,
        label = "micButtonColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSurface)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Text Input Row with direct mic voice button & send button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextInputChange,
                placeholder = { Text("मायरा से बात करें या ऐप खोलने को कहें…", color = TextTertiary, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_input_field"),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    IconButton(
                        onClick = onVoiceInputClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CardSurfaceElevated,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSendText,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (textInput.isNotBlank()) NeonCyan else CardSurfaceElevated)
                    .testTag("send_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (textInput.isNotBlank()) ObsidianBg else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Big Main Mic / Session Button (Unblocked & 100% Responsive)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onToggleSession,
                shape = RoundedCornerShape(26.dp),
                color = buttonColor,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth(0.9f)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = buttonColor,
                        spotColor = buttonColor
                    )
                    .testTag("toggle_session_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (sessionStarted) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (sessionStarted) Color.White else ObsidianBg,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (sessionStarted) "END SESSION (बंद करें)" else "START SESSION (बात करें)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = if (sessionStarted) Color.White else ObsidianBg
                    )
                }
            }
        }
    }
}

@Composable
private fun MyraSettingsContent(
    currentLang: String,
    personalityMode: String,
    displayMode: String,
    pitch: Float,
    speed: Float,
    onLangChange: (String) -> Unit,
    onPersonalityChange: (String) -> Unit,
    onDisplayModeChange: (String) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onClearHistoryRequest: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MYRA Companion Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            TextButton(onClick = onClose) {
                Text("Done", color = NeonCyan)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Visual Display Mode Switcher
        Text(
            text = "Visual Interface Mode",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "ANIME" to "👧 Cute Anime GF",
                "CYBER_HUD" to "🔮 Maya Cyber HUD"
            ).forEach { (mode, label) ->
                val selected = displayMode == mode
                Surface(
                    onClick = { onDisplayModeChange(mode) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) NeonCyan.copy(alpha = 0.25f) else CardSurfaceElevated,
                    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, NeonCyan) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) NeonCyan else TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Personality mode selector
        Text(
            text = "Companion Personality",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "NAKHRE" to "🔥 Nakhre & Sassy",
                "SWEET" to "💖 Sweet & Loving",
                "CARING" to "🌸 Friendly"
            ).forEach { (mode, label) ->
                val selected = personalityMode == mode
                Surface(
                    onClick = { onPersonalityChange(mode) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) ElectricViolet.copy(alpha = 0.25f) else CardSurfaceElevated,
                    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, ElectricVioletLight) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) ElectricVioletLight else TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Language Selector
        Text(
            text = "Voice Language",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "hi-IN" to "हिन्दी",
                "en-IN" to "English (IN)",
                "en-US" to "English (US)"
            ).forEach { (code, label) ->
                val selected = currentLang == code
                Surface(
                    onClick = { onLangChange(code) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) NeonCyan.copy(alpha = 0.2f) else CardSurfaceElevated,
                    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, NeonCyan) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) NeonCyan else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Speech Pitch Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Voice Pitch", color = TextSecondary, fontSize = 13.sp)
            Text(String.format(Locale.US, "%.2fx", pitch), color = NeonCyan, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = pitch,
            onValueChange = onPitchChange,
            valueRange = 0.7f..1.8f,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = CardSurfaceElevated
            )
        )

        Spacer(Modifier.height(14.dp))

        // Deep Android Customization & Files
        val context = LocalContext.current
        Text(
            text = "Deep Android Phone Customization",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = NeonCyan
        )
        Text(
            text = "Direct hardware controls & phone file system customizers",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))

        val customActions = listOf(
            Triple("📂 Files & Docs", "Browse phone files & downloads") {
                try {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open files: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Triple("💾 Internal Storage", "Check free disk & clean storage") {
                try {
                    context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(context, "Storage settings unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            Triple("🎨 Wallpaper & Display", "Customize theme & screen") {
                try {
                    context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(context, "Display settings unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            Triple("🔊 Sound & Vibration", "Adjust ringtones & alerts") {
                try {
                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(context, "Sound settings unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            Triple("📲 Installed Apps", "Manage permissions & storage") {
                try {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(context, "Apps settings unavailable", Toast.LENGTH_SHORT).show()
                }
            },
            Triple("⚙️ All System Settings", "Full phone configuration") {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    Toast.makeText(context, "Settings unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            customActions.chunked(2).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowActions.forEach { item ->
                        Surface(
                            onClick = item.third,
                            shape = RoundedCornerShape(12.dp),
                            color = CardSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardSurfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item.first, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(item.second, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Maya Live Wallpaper Banner (Full Home Screen Companion)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ElectricViolet.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricVioletLight.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎀", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Maya Home Screen Companion", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    }
                    Text("LIVE", color = ElectricVioletLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Maya herself on your home screen. She breathes, looks up when you start talking to her, and reacts when you tap her. Pauses completely whenever the home screen is covered.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        MayaWallpaperService.launchLiveWallpaperPicker(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Live Wallpaper", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Maya Accessibility & WhatsApp Automation Banner
        val isAccessibilityOn = remember { MayaAccessibilityService.isServiceEnabled(context) }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isAccessibilityOn) EmeraldActive.copy(alpha = 0.12f) else CardSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAccessibilityOn) EmeraldActive.copy(alpha = 0.4f) else CardSurfaceElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🦾", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Maya App Controller & Reader", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    }
                    Text(
                        text = if (isAccessibilityOn) "ACTIVE" else "OFF",
                        color = if (isAccessibilityOn) EmeraldActive else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Maya uses this to control apps for you — like sending WhatsApp messages, making WhatsApp calls, playing YouTube videos and reading your screen when you ask her to. Maya only acts on your voice commands.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            MayaAccessibilityService.openAccessibilitySettings(context)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Configure Access", fontSize = 11.sp, color = NeonCyan)
                    }
                    Button(
                        onClick = {
                            if (MayaAccessibilityService.isRunning()) {
                                val text = MayaAccessibilityService.instance?.readCurrentScreen() ?: ""
                                Toast.makeText(context, text.take(100), Toast.LENGTH_LONG).show()
                            } else {
                                MayaAccessibilityService.openAccessibilitySettings(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Read Screen", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Maya WhatsApp Message Reader Banner
        val isNotificationAccessOn = remember { MayaWhatsAppReaderService.isNotificationAccessEnabled(context) }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isNotificationAccessOn) EmeraldActive.copy(alpha = 0.12f) else CardSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isNotificationAccessOn) EmeraldActive.copy(alpha = 0.4f) else CardSurfaceElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Maya WhatsApp Message Reader", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    }
                    Text(
                        text = if (isNotificationAccessOn) "ACTIVE" else "OFF",
                        color = if (isNotificationAccessOn) EmeraldActive else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Allows Maya to notify you and read incoming WhatsApp messages when they arrive. You can then ask her to reply or ignore.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        MayaWhatsAppReaderService.openNotificationAccessSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isNotificationAccessOn) EmeraldActive else ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isNotificationAccessOn) "Manage Access" else "Enable Notification Access",
                        color = if (isNotificationAccessOn) ObsidianBg else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Clear Chat History Button
        Surface(
            onClick = onClearHistoryRequest,
            shape = RoundedCornerShape(12.dp),
            color = CoralError.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoralError.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = CoralError, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear Conversation History", color = CoralError, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun MyraApiKeyDialog(
    currentProvider: String,
    currentGeminiKey: String,
    currentOpenAiKey: String,
    currentElevenLabsKey: String,
    currentVoiceId: String,
    currentEngine: String,
    currentUserName: String,
    onSave: (provider: String, geminiKey: String, openAiKey: String, elevenLabsKey: String, voiceId: String, engine: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var provider by remember { mutableStateOf(currentProvider) }
    var geminiKey by remember { mutableStateOf(currentGeminiKey) }
    var openAiKey by remember { mutableStateOf(currentOpenAiKey) }
    var elevenLabsKey by remember { mutableStateOf(currentElevenLabsKey) }
    var voiceId by remember { mutableStateOf(currentVoiceId) }
    var engine by remember { mutableStateOf(currentEngine) }
    var userName by remember { mutableStateOf(currentUserName) }

    var showGeminiSecret by remember { mutableStateOf(false) }
    var showOpenAiSecret by remember { mutableStateOf(false) }
    var showElevenSecret by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = NeonCyan)
                Spacer(Modifier.width(8.dp))
                Text("AI Engine & Voice Keys", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name input
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Your Name / Nickname", fontSize = 12.sp) },
                    placeholder = { Text("Jaan / Shivam", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // AI Provider Selector (Gemini vs ChatGPT vs Offline)
                Text("Primary AI Provider:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = provider == "GEMINI",
                        onClick = { provider = "GEMINI" },
                        label = { Text("Gemini", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CardSurfaceElevated,
                            selectedContainerColor = NeonCyan.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.weight(1f).testTag("provider_gemini")
                    )
                    FilterChip(
                        selected = provider == "OPENAI",
                        onClick = { provider = "OPENAI" },
                        label = { Text("ChatGPT", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CardSurfaceElevated,
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.weight(1f).testTag("provider_openai")
                    )
                    FilterChip(
                        selected = provider == "OFFLINE",
                        onClick = { provider = "OFFLINE" },
                        label = { Text("Offline", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CardSurfaceElevated,
                            selectedContainerColor = EmeraldActive.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Gemini Key
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("Gemini API Key", fontSize = 12.sp) },
                    placeholder = { Text("AIzaSy...", color = TextTertiary, fontSize = 12.sp) },
                    visualTransformation = if (showGeminiSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showGeminiSecret = !showGeminiSecret }) {
                            Text(if (showGeminiSecret) "Hide" else "Show", fontSize = 10.sp, color = NeonCyan)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val dialogScope = rememberCoroutineScope()
                var isTestingKey by remember { mutableStateOf(false) }
                var testStatus by remember { mutableStateOf<String?>(null) }
                var testSuccess by remember { mutableStateOf(false) }

                if (geminiKey.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTestingKey = true
                                testStatus = null
                                dialogScope.launch {
                                    val (ok, msg) = MyraAiEngine.testGeminiKey(geminiKey)
                                    isTestingKey = false
                                    testSuccess = ok
                                    testStatus = msg
                                }
                            },
                            enabled = !isTestingKey,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (isTestingKey) "Testing Gemini..." else "⚡ Test Gemini API Key", fontSize = 10.sp)
                        }
                    }
                    if (testStatus != null) {
                        Text(
                            text = testStatus!!,
                            fontSize = 11.sp,
                            color = if (testSuccess) EmeraldActive else CoralError,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // OpenAI / ChatGPT Key
                OutlinedTextField(
                    value = openAiKey,
                    onValueChange = { openAiKey = it },
                    label = { Text("OpenAI / ChatGPT API Key (Optional)", fontSize = 12.sp) },
                    placeholder = { Text("sk-proj-...", color = TextTertiary, fontSize = 12.sp) },
                    visualTransformation = if (showOpenAiSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showOpenAiSecret = !showOpenAiSecret }) {
                            Text(if (showOpenAiSecret) "Hide" else "Show", fontSize = 10.sp, color = ElectricVioletLight)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = CardSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ElevenLabs Key
                OutlinedTextField(
                    value = elevenLabsKey,
                    onValueChange = { elevenLabsKey = it },
                    label = { Text("ElevenLabs API Key (Ultra-Real Voice)", fontSize = 12.sp) },
                    visualTransformation = if (showElevenSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = CardSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Voice ID
                OutlinedTextField(
                    value = voiceId,
                    onValueChange = { voiceId = it },
                    label = { Text("ElevenLabs Voice ID", fontSize = 12.sp) },
                    placeholder = { Text("21m00Tcm4TlvDq8ikWAM", fontSize = 12.sp, color = TextTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricViolet,
                        unfocusedBorderColor = CardSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Voice Model Presets
                Text("Voice Model Presets:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "🌸 Rachel" to "21m00Tcm4TlvDq8ikWAM",
                        "🎀 Bella" to "EXAVITQu4vr4xnSDxMaL",
                        "💖 Jessica" to "cgSgspJ2msm6clMCkdW9",
                        "👧 Alice" to "Xb7hH8MSLwb3o6xL8d9X"
                    ).forEach { (presetLabel, presetId) ->
                        FilterChip(
                            selected = voiceId == presetId,
                            onClick = { voiceId = presetId },
                            label = { Text(presetLabel, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet.copy(alpha = 0.35f),
                                selectedLabelColor = ElectricVioletLight,
                                containerColor = CardSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("voice_preset_${presetId}")
                        )
                    }
                }

                if (elevenLabsKey.isNotBlank()) {
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            dialogScope.launch {
                                ElevenLabsService.speakWithElevenLabs(
                                    context = context,
                                    text = "Hello! Your ElevenLabs voice is connected to Myra!",
                                    apiKey = elevenLabsKey,
                                    voiceId = voiceId,
                                    onStart = {},
                                    onDone = {},
                                    onError = { Toast.makeText(context, "Voice error: $it", Toast.LENGTH_LONG).show() }
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricVioletLight),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("🔊 Test ElevenLabs Voice", fontSize = 10.sp)
                    }
                }

                // Engine toggle
                Text("Voice Engine:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = engine == "DEVICE_TTS",
                        onClick = { engine = "DEVICE_TTS" },
                        label = { Text("Device Cute TTS", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CardSurfaceElevated,
                            selectedContainerColor = NeonCyan.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("engine_device_tts")
                    )
                    FilterChip(
                        selected = engine == "ELEVENLABS",
                        onClick = { engine = "ELEVENLABS" },
                        label = { Text("ElevenLabs AI Voice", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CardSurfaceElevated,
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("engine_elevenlabs")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(provider, geminiKey, openAiKey, elevenLabsKey, voiceId, engine, userName) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Save All Settings", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardSurface
    )
}

@Composable
private fun MayaScannerDialog(
    onDismiss: () -> Unit,
    onCodeDetected: (MayaVisionScanner.ScanResult) -> Unit
) {
    val context = LocalContext.current
    var scanResult by remember { mutableStateOf<MayaVisionScanner.ScanResult?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            scanError = null
            scanResult = null
            MayaVisionScanner.scanUri(
                context = context,
                uri = uri,
                onSuccess = { res ->
                    isProcessing = false
                    scanResult = res
                    onCodeDetected(res)
                },
                onError = { err ->
                    isProcessing = false
                    scanError = err
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📷", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("Maya QR & Barcode Scanner", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Scan QR codes, website links, WiFi details, or product barcodes with Google ML Kit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (isProcessing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text("Scanning image with ML Kit…", color = NeonCyan, fontSize = 12.sp)
                    }
                }

                if (scanError != null) {
                    Text(scanError!!, color = CoralError, fontSize = 12.sp)
                }

                if (scanResult != null) {
                    val res = scanResult!!
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(res.typeLabel, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(res.rawValue, color = TextPrimary, fontSize = 11.sp)
                            if (!res.actionUrl.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(res.actionUrl))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Open Link", color = ObsidianBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Photo with QR / Barcode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = CardSurface
    )
}

@Composable
private fun MayaGuardianDialog(
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
    onScreenResult: (MayaGuardianEngine.ScreeningResult) -> Unit
) {
    val context = LocalContext.current
    var activeModel by remember { mutableStateOf(MayaGuardianEngine.getActiveModel(context)) }
    var screeningResult by remember { mutableStateOf<MayaGuardianEngine.ScreeningResult?>(null) }
    var isSimulating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛡️", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("Maya Guardian AI Shield", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AI Voice Biometrics & Call Screening. Identifies deepfake AI clones, robocalls, and financial fraud before you answer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Text("Biometric Model Selection", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 12.sp)

                MayaGuardianEngine.MODELS.forEach { model ->
                    val isSelected = activeModel.id == model.id
                    Surface(
                        onClick = {
                            activeModel = model
                            MayaGuardianEngine.setActiveModel(context, model.id)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ElectricViolet.copy(alpha = 0.25f) else CardSurfaceElevated,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ElectricVioletLight) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val isReady = remember(model.id) { GuardianModelStore.isModelReady(context, model.id) }
                            var downloadProgress by remember { mutableFloatStateOf(0f) }
                            var isDownloading by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${model.label} (${model.id})",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ElectricVioletLight else TextPrimary,
                                        fontSize = 12.sp
                                    )
                                    if (isReady) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Default.Check, contentDescription = "Ready", tint = EmeraldActive, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Text(
                                    text = "${model.windowSec}s window",
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = model.note,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                            if (!isReady) {
                                Spacer(Modifier.height(8.dp))
                                if (isDownloading) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = NeonCyan,
                                        trackColor = CardSurface
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            isDownloading = true
                                            scope.launch {
                                                val success = GuardianModelStore.downloadModelFiles(
                                                    context = context,
                                                    modelId = model.id,
                                                    tfliteUrl = "${MayaGuardianEngine.BASE_URL}${model.file}",
                                                    tfliteSha256 = model.sha256,
                                                    cohortUrl = "${MayaGuardianEngine.BASE_URL}${model.cohort}",
                                                    cohortSha256 = model.cohortSha256,
                                                    onProgress = { downloadProgress = it }
                                                )
                                                isDownloading = false
                                                if (success) {
                                                    Toast.makeText(context, "Model ${model.label} downloaded successfully", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to download ${model.label}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                    ) {
                                        Text("Download Model (${model.bytes / 1024 / 1024}MB)", color = ObsidianBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (screeningResult != null) {
                    val res = screeningResult!!
                    val badgeColor = when (res.verdict) {
                        MayaGuardianEngine.Verdict.VERIFIED_SAFE -> EmeraldActive
                        MayaGuardianEngine.Verdict.UNKNOWN_SUSPICIOUS -> AmberWarning
                        MayaGuardianEngine.Verdict.SCAM_AI_DETECTED -> CoralError
                    }
                    val badgeText = when (res.verdict) {
                        MayaGuardianEngine.Verdict.VERIFIED_SAFE -> "🟢 VERIFIED AUTHENTIC"
                        MayaGuardianEngine.Verdict.UNKNOWN_SUSPICIOUS -> "🟡 UNVERIFIED CALLER"
                        MayaGuardianEngine.Verdict.SCAM_AI_DETECTED -> "🔴 SUSPECTED AI SCAM"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(badgeText, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%.1f%% confidence", res.confidenceScore * 100),
                                    color = badgeColor,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(res.callerName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(res.callerNumber, color = TextSecondary, fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(res.detailedAdvice, color = TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
                        }
                    }
                }

                Text("Simulate Call Screening", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val res = MayaGuardianEngine.screenCall(
                                context = context,
                                callerNumber = "+91 14092-84110",
                                callerName = "Suspected Bank KYC Alert",
                                simulatedRawSimilarity = 0.28
                            )
                            screeningResult = res
                            onScreenResult(res)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Scam Call", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val res = MayaGuardianEngine.screenCall(
                                context = context,
                                callerNumber = "+91 98200-11223",
                                callerName = "Shivam (Verified Voice)",
                                simulatedRawSimilarity = 0.94
                            )
                            screeningResult = res
                            onScreenResult(res)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldActive),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test Safe Call", color = ObsidianBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = NeonCyan)
            }
        },
        containerColor = CardSurface
    )
}



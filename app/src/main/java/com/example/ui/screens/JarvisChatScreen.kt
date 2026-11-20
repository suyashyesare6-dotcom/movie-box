package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.JarvisVoiceController
import com.example.model.AiLanguage
import com.example.model.AiResponseState
import com.example.model.ChatMessage
import com.example.model.JarvisMode
import com.example.model.MemoryCategory
import com.example.model.MessageSender
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.JarvisArcOrb
import com.example.ui.components.JarvisModeSelector
import com.example.ui.components.StarkGlowText
import com.example.ui.components.starkPulseGlow
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanBright
import com.example.ui.theme.ArcCyanDim
import com.example.ui.theme.HudOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceBright
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StarkGold

@Composable
fun JarvisChatScreen(
    profile: UserProfile,
    messagesState: UiState<List<ChatMessage>>,
    aiState: AiResponseState,
    voiceController: JarvisVoiceController,
    isListening: Boolean,
    isSpeaking: Boolean,
    ttsStatus: com.example.core.TtsEngineStatus,
    partialTranscript: String,
    rmsLevel: Float = 0f,
    voiceStatusNote: String?,
    onSelectMode: (JarvisMode) -> Unit,
    onSelectLanguage: (AiLanguage) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onSaveAsMemory: (String) -> Unit,
    onSaveAsTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var micPermissionDenied by remember { mutableStateOf(false) }
    var forceMarathiMic by remember { mutableStateOf(true) }
    var autoSendVoice by remember { mutableStateOf(true) }
    val activeMode = profile.modeEnum()
    val activeLanguage = profile.languageEnum()
    val listState = rememberLazyListState()

    val effectiveSttLanguage = remember(forceMarathiMic, activeLanguage) {
        if (forceMarathiMic) AiLanguage.MARATHI else activeLanguage
    }

    fun handleRecognizedSpeech(spoken: String) {
        val clean = spoken.trim()
        if (clean.isEmpty()) return
        if (autoSendVoice) {
            onSendMessage(clean)
        } else {
            inputText = if (inputText.isBlank()) clean else "${inputText.trimEnd()} $clean"
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull { it.isNotBlank() }
                .orEmpty()
            handleRecognizedSpeech(spoken)
        }
    }

    fun beginSpeechCapture() {
        micPermissionDenied = false
        if (voiceController.isInlineSpeechAvailable()) {
            voiceController.startListening(languageOverride = effectiveSttLanguage) { spoken ->
                handleRecognizedSpeech(spoken)
            }
        } else {
            runCatching {
                systemSpeechLauncher.launch(voiceController.buildSpeechRecognitionIntent(effectiveSttLanguage))
            }.onFailure {
                voiceController.reportStatus("या डिव्हाइसवर स्पीच रेकग्निशन उपलब्ध नाही, बॉस. कृपया टाइप करा.")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionDenied = false
            voiceController.clearStatusNote()
            beginSpeechCapture()
        } else {
            micPermissionDenied = true
            voiceController.reportStatus("मायक्रोफोन परवानगी नाकारली गेली, बॉस. मराठीत बोलण्यासाठी परवानगी द्या.")
        }
    }

    fun triggerVoiceCapture() {
        if (isListening) {
            voiceController.stopListening()
            return
        }
        if (isSpeaking) {
            voiceController.stopSpeaking()
            return
        }
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasAudio) {
            beginSpeechCapture()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(messages.size, aiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        // Mode Switcher Header
        JarvisModeSelector(
            activeMode = activeMode,
            onModeSelected = onSelectMode
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Multi-Language AI Selector Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = "AI Language",
                tint = ArcCyan,
                modifier = Modifier.size(16.dp)
            )
            AiLanguage.entries.forEach { lang ->
                val selected = lang == activeLanguage
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) StarkGold.copy(alpha = 0.2f) else SlateSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (selected) StarkGold else HudOutline,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onSelectLanguage(lang) }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                        .testTag("chat_lang_${lang.name.lowercase()}")
                ) {
                    Text(
                        text = lang.nativeName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) StarkGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Arc Reactor Voice Header Card
        val headerActive = isListening || isSpeaking || aiState is AiResponseState.Loading || aiState is AiResponseState.Streaming
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.90f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .starkPulseGlow(
                    glowColor = if (isListening) StarkGold else ArcCyan,
                    cornerRadius = 20.dp,
                    enabled = headerActive
                )
                .border(
                    width = 1.dp,
                    color = if (headerActive) ArcCyan.copy(alpha = 0.7f) else HudOutline,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(14.dp)
            ) {
                JarvisArcOrb(
                    isListening = isListening,
                    isThinking = aiState is AiResponseState.Loading || aiState is AiResponseState.Streaming,
                    isSpeaking = isSpeaking,
                    onClick = ::triggerVoiceCapture,
                    size = 78.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    StarkGlowText(
                        text = "${activeMode.labelMarathi.uppercase()} • ${activeLanguage.nativeName} • ${ttsStatus.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeMode == JarvisMode.GOSSIP) StarkGold else ArcCyanBright,
                        glowColor = if (activeMode == JarvisMode.GOSSIP) StarkGold else ArcCyan
                    )
                    Text(
                        text = when {
                            isListening -> partialTranscript.ifBlank { stringResource(R.string.listening_now) }
                            aiState is AiResponseState.Loading -> stringResource(R.string.thinking_now)
                            aiState is AiResponseState.Streaming -> "JARVIS • Live"
                            isSpeaking -> stringResource(R.string.speaking_now)
                            activeLanguage == AiLanguage.MARATHI -> "${profile.preferredTitle}, बोला किंवा खाली टाइप करा."
                            else -> "${profile.preferredTitle}, speak or type in ${activeLanguage.englishName}."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = voiceStatusNote ?: activeMode.subtitleMarathi,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (voiceStatusNote != null) StarkGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSpeaking) {
                    IconButton(
                        onClick = { voiceController.stopSpeaking() },
                        modifier = Modifier.testTag("stop_tts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop speaking",
                            tint = StarkGold
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (profile.autoSpeakEnabled) voiceController.stopSpeaking()
                            onToggleAutoSpeak(!profile.autoSpeakEnabled)
                        },
                        modifier = Modifier.testTag("toggle_autospeak_button")
                    ) {
                        Icon(
                            imageVector = if (profile.autoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (profile.autoSpeakEnabled) "Mute auto-speak" else "Enable auto-speak",
                            tint = if (profile.autoSpeakEnabled) ArcCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Spoken Prompts per Mode & Language
        val prompts = remember(activeMode, activeLanguage) {
            when (activeLanguage) {
                AiLanguage.MARATHI -> when (activeMode) {
                    JarvisMode.GENERAL -> listOf(
                        "आजचं डेली ब्रीफिंग दे",
                        "माझ्या आजच्या कामांची यादी सांग",
                        "मला थोडं मोटिव्हेशन दे",
                        "आजचा दिवस कसा प्लॅन करू?"
                    )
                    JarvisMode.GOSSIP -> listOf(
                        "चल थोड्या गप्पा मारूया",
                        "माझ्या आवडींबद्दल काहीतरी आठव",
                        "आज थोडा कंटाळा आलाय यार",
                        "एक मजेशीर किस्सा सांग"
                    )
                    JarvisMode.STUDY -> listOf(
                        "माझी एक प्रश्नोत्तराची क्विझ घे",
                        "Recursion सोप्या मराठीत समजाव",
                        "अभ्यासाची रिव्हिजन ट्रिक सांग",
                        "फक्त हिंट दे, उत्तर नको"
                    )
                    JarvisMode.SECURITY -> listOf(
                        "AES-256 एन्क्रिप्शन सोप्या भाषेत समजाव",
                        "स्कॅम SMS कसा ओळखायचा?",
                        "2FA का महत्त्वाचे आहे?",
                        "माझा पासवर्ड मॅनेजर सल्ला दे"
                    )
                }
                AiLanguage.HINDI -> listOf(
                    "आज का डेली ब्रीफिंग सुनाओ",
                    "आज के काम और बिल बताओ",
                    "मुझे एक क्विज़ पूछो",
                    "2FA क्यों ज़रूरी है समझाओ"
                )
                AiLanguage.SPANISH -> listOf(
                    "Dame mi resumen diario",
                    "¿Qué tareas y facturas tengo hoy?",
                    "Hazme un quiz rápido de estudio",
                    "¿Cómo detectar mensajes phishing?"
                )
                AiLanguage.FRENCH -> listOf(
                    "Donne-moi mon briefing du jour",
                    "Quelles sont mes tâches et factures ?",
                    "Fais-moi un quiz d'étude rapide",
                    "Explique le chiffrement AES-256"
                )
                AiLanguage.GERMAN -> listOf(
                    "Gib mir mein Tages-Briefing",
                    "Welche Aufgaben und Rechnungen habe ich heute?",
                    "Mach ein kurzes Lern-Quiz mit mir",
                    "Wie erkenne ich Phishing-Links?"
                )
                AiLanguage.JAPANESE -> listOf(
                    "今日のデイリーブリーフィングを教えて",
                    "今日のタスクと請求書をまとめて",
                    "勉強のミニクイズを出して",
                    "2FAが重要な理由を教えて"
                )
                AiLanguage.ENGLISH,
                AiLanguage.AUTO -> when (activeMode) {
                    JarvisMode.GENERAL -> listOf(
                        "Give me today's daily briefing",
                        "Summarize my pending tasks and bills",
                        "Translate 'Good morning Boss' to Spanish",
                        "Help me plan today's priorities"
                    )
                    JarvisMode.GOSSIP -> listOf(
                        "Let's catch up like old friends",
                        "Roast my procrastination gently",
                        "Tell me a quick witty story",
                        "Bring up something from my interests"
                    )
                    JarvisMode.STUDY -> listOf(
                        "Quiz me one question at a time",
                        "Explain recursion simply with an analogy",
                        "Give me a revision trick, hints only",
                        "Build a 2-hour study sprint"
                    )
                    JarvisMode.SECURITY -> listOf(
                        "Explain AES-256 encryption simply",
                        "How do I spot phishing links?",
                        "Why are passkeys better than SMS OTP?",
                        "Give me a 3-step password hygiene check"
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            prompts.forEachIndexed { idx, promptText ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateSurfaceElevated)
                        .border(1.dp, HudOutline, RoundedCornerShape(12.dp))
                        .clickable { onSendMessage(promptText) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("quick_prompt_$idx")
                ) {
                    Text(
                        text = promptText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Messages List
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && aiState is AiResponseState.Idle) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HudOutline, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(18.dp)
                        ) {
                            StarkGlowText(
                                text = "JARVIS ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArcCyanBright,
                                glowColor = ArcCyan
                            )
                            Text(
                                text = "नमस्कार ${profile.preferredTitle}! मी JARVIS. वरचा ऑर्ब दाबून मराठीत बोला, किंवा खाली मेसेज पाठवा.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id.ifBlank { it.hashCode() } }) { msg ->
                val isUser = msg.senderEnum() == MessageSender.USER
                ChatBubble(
                    message = msg,
                    isUser = isUser,
                    onSpeak = { voiceController.speak(msg.text, profile.speechRate.toFloat()) },
                    onSaveMemory = { onSaveAsMemory(msg.text) },
                    onSaveTask = { onSaveAsTask(msg.text) }
                )
            }

            if (aiState is AiResponseState.Streaming && aiState.partialText.isNotBlank()) {
                item(key = "streaming_bubble") {
                    StreamingBubble(text = aiState.partialText)
                }
            } else if (aiState is AiResponseState.Error) {
                item(key = "error_bubble") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // Mic Audio Permission Denied Banner
        AnimatedVisibility(visible = micPermissionDenied) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AlertCrimson.copy(alpha = 0.14f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, AlertCrimson.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .testTag("mic_permission_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "मराठी व्हॉइस-टू-टेक्स्टसाठी ऑडिओ परवानगी आवश्यक आहे.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("mic_retry_permission_button")
                    ) {
                        Text("परवानगी द्या", color = ArcCyanBright, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("सेटिंग्ज", color = StarkGold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Live Marathi Voice-to-Text HUD Bar
        AnimatedVisibility(visible = isListening) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .starkPulseGlow(glowColor = StarkGold, cornerRadius = 18.dp)
                    .border(
                        width = 1.dp,
                        color = StarkGold.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    )
                    .testTag("voice_to_text_hud")
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VoiceRmsWaveform(level = rmsLevel)
                            StarkGlowText(
                                text = if (effectiveSttLanguage == AiLanguage.MARATHI) {
                                    "मराठी VOICE-TO-TEXT (mr-IN)"
                                } else {
                                    "${effectiveSttLanguage.nativeName.uppercase()} VOICE"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = StarkGold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(ArcCyan.copy(alpha = 0.16f))
                                    .border(1.dp, ArcCyan.copy(alpha = 0.55f), RoundedCornerShape(50))
                                    .clickable { autoSendVoice = !autoSendVoice }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("voice_send_mode_toggle")
                            ) {
                                Text(
                                    text = if (autoSendVoice) "थेट पाठवा" else "बॉक्समध्ये टाइप",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArcCyanBright
                                )
                            }

                            IconButton(
                                onClick = { voiceController.cancelListening() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("voice_cancel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel voice input",
                                    tint = AlertCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = partialTranscript.ifBlank {
                            if (effectiveSttLanguage == AiLanguage.MARATHI) {
                                "बॉस, मराठीत बोला… (उदा. 'आजची कामे सांग' किंवा 'मला एक क्विझ घे')"
                            } else {
                                "Listening… speak naturally, Boss."
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (partialTranscript.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("voice_live_transcript")
                    )
                }
            }
        }

        // Bottom Voice + Text Input Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface.copy(alpha = 0.94f))
                .border(1.dp, ArcCyan.copy(alpha = 0.25f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (forceMarathiMic) ArcCyanDim else SlateSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (forceMarathiMic) ArcCyan else HudOutline,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { forceMarathiMic = !forceMarathiMic }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("toggle_marathi_stt_chip")
                ) {
                    Text(
                        text = if (forceMarathiMic) "Mic: मराठी (mr-IN)" else "Mic: ${effectiveSttLanguage.nativeName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (forceMarathiMic) ArcCyanBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (!autoSendVoice) StarkGold.copy(alpha = 0.16f) else SlateSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (!autoSendVoice) StarkGold else HudOutline,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { autoSendVoice = !autoSendVoice }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("toggle_dictate_to_box_chip")
                ) {
                    Text(
                        text = if (autoSendVoice) "Voice: थेट कमांड" else "Voice: बॉक्समध्ये भरा",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!autoSendVoice) StarkGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = ::triggerVoiceCapture,
                    modifier = Modifier
                        .size(48.dp)
                        .starkPulseGlow(
                            glowColor = if (isListening) StarkGold else ArcCyan,
                            cornerRadius = 24.dp,
                            enabled = true
                        )
                        .clip(CircleShape)
                        .background(if (isListening) StarkGold.copy(alpha = 0.24f) else ArcCyanDim)
                        .border(1.2.dp, if (isListening) StarkGold else ArcCyan, CircleShape)
                        .testTag("chat_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop Marathi voice input" else "Speak Marathi command",
                        tint = if (isListening) StarkGold else ArcCyanBright
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isListening && partialTranscript.isNotBlank()) {
                                partialTranscript
                            } else {
                                stringResource(R.string.voice_hint)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = HudOutline,
                        focusedContainerColor = SlateSurfaceElevated,
                        unfocusedContainerColor = SlateSurfaceElevated
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                val canSend = inputText.isNotBlank() && aiState !is AiResponseState.Loading
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .starkPulseGlow(
                            glowColor = ArcCyan,
                            cornerRadius = 24.dp,
                            enabled = canSend
                        )
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) ArcCyan else SlateSurfaceBright)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isUser: Boolean,
    onSpeak: () -> Unit,
    onSaveMemory: () -> Unit,
    onSaveTask: () -> Unit
) {
    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) ArcCyanDim else SlateSurfaceElevated
            ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            modifier = Modifier
                .widthIn(max = 340.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) ArcCyan.copy(alpha = 0.5f) else HudOutline,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isUser) "तुम्ही" else "JARVIS • ${message.modeEnum().labelMarathi}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) ArcCyan else StarkGold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    if (!isUser) {
                        TextButton(
                            onClick = onSpeak,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak reply",
                                tint = ArcCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ऐका", style = MaterialTheme.typography.labelSmall, color = ArcCyan)
                        }
                    } else {
                        TextButton(
                            onClick = onSaveMemory,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Save to memory",
                                tint = StarkGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("आठवण", style = MaterialTheme.typography.labelSmall, color = StarkGold)
                        }
                        TextButton(
                            onClick = onSaveTask,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Save as task",
                                tint = ArcCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("टास्क", style = MaterialTheme.typography.labelSmall, color = ArcCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceRmsWaveform(level: Float) {
    val multipliers = listOf(0.45f, 0.85f, 1.0f, 0.75f, 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(18.dp)
    ) {
        multipliers.forEach { weight ->
            val targetHeight = (5f + (level * 13f * weight)).dp
            val barHeight by animateDpAsState(targetValue = targetHeight, label = "voice_rms_bar")
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(StarkGold)
            )
        }
    }
}

@Composable
private fun StreamingBubble(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .widthIn(max = 340.dp)
            .starkPulseGlow(glowColor = ArcCyan, cornerRadius = 18.dp)
            .border(1.dp, ArcCyan.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            StarkGlowText(
                text = "JARVIS • LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = ArcCyanBright,
                glowColor = ArcCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

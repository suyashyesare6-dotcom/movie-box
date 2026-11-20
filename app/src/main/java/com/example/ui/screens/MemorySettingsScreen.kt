package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.TtsEngineStatus
import com.example.core.TtsVoiceOption
import com.example.core.signOut
import com.example.model.AiLanguage
import com.example.model.MemoryCategory
import com.example.model.MemoryItem
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.StarkButtonLabel
import com.example.ui.components.StarkGlowButton
import com.example.ui.components.StarkGlowFab
import com.example.ui.components.StarkGlowText
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanBright
import com.example.ui.theme.ArcCyanDim
import com.example.ui.theme.HudOutline
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StarkGold

@Composable
fun MemorySettingsScreen(
    profile: UserProfile,
    memoriesState: UiState<List<MemoryItem>>,
    ttsStatus: TtsEngineStatus,
    availableVoices: List<TtsVoiceOption>,
    selectedVoiceName: String?,
    activeVoiceLabel: String,
    voicePitch: Float,
    onSelectVoice: (String) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSelectLanguage: (AiLanguage) -> Unit,
    onTestVoice: (String, Float, AiLanguage) -> Unit,
    onInstallVoiceData: () -> Unit,
    onUpdateSettings: (String, Boolean, Double, AiLanguage) -> Unit,
    onAddMemory: (String, String, MemoryCategory) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var preferredTitle by remember(profile.preferredTitle) { mutableStateOf(profile.preferredTitle) }
    var autoSpeak by remember(profile.autoSpeakEnabled) { mutableStateOf(profile.autoSpeakEnabled) }
    var speechRate by remember(profile.speechRate) { mutableFloatStateOf(profile.speechRate.toFloat()) }
    var selectedLanguage by remember(profile.preferredLanguage) { mutableStateOf(profile.languageEnum()) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }

    val memories = (memoriesState as? UiState.Success)?.data ?: emptyList()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Persona & Voice Preferences
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, HudOutline, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        StarkGlowText(
                            text = "JARVIS पसंती आणि आवाज सेटिंग्ज",
                            style = MaterialTheme.typography.titleMedium,
                            color = ArcCyanBright,
                            glowColor = ArcCyan
                        )
                        OutlinedTextField(
                            value = preferredTitle,
                            onValueChange = { preferredTitle = it },
                            label = { Text("JARVIS ने तुम्हाला काय म्हणावे? (उदा. Boss किंवा नाव)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preferred_title_input")
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("उत्तर आपोआप बोलून दाखवा (Auto-Speak)", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "बंद केल्यास तुम्ही 'ऐका' बटणावर टॅप करून ऐकू शकता.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoSpeak,
                                onCheckedChange = { autoSpeak = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = ArcCyan)
                            )
                        }

                        Column {
                            Text(
                                text = "आवाजाचा वेग: ${String.format("%.1fx", speechRate)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = StarkGold
                            )
                            Slider(
                                value = speechRate,
                                onValueChange = { speechRate = it },
                                valueRange = 0.6f..1.6f,
                                colors = SliderDefaults.colors(thumbColor = StarkGold, activeTrackColor = ArcCyan)
                            )
                        }

                        Column {
                            Text(
                                text = "आवाजाचा टोन (Pitch): ${String.format("%.2fx", voicePitch)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ArcCyan
                            )
                            Slider(
                                value = voicePitch,
                                onValueChange = onPitchChange,
                                valueRange = 0.75f..1.25f,
                                colors = SliderDefaults.colors(thumbColor = ArcCyan, activeTrackColor = StarkGold)
                            )
                        }

                        // AI Language Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "AI आणि व्हॉइस भाषा (Multi-Language AI):",
                                style = MaterialTheme.typography.labelMedium,
                                color = StarkGold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                AiLanguage.entries.forEach { lang ->
                                    val isSelected = selectedLanguage == lang
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (isSelected) StarkGold.copy(alpha = 0.2f)
                                                else SlateSurface
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) StarkGold else HudOutline,
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clickable {
                                                selectedLanguage = lang
                                                onSelectLanguage(lang)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("settings_lang_${lang.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = "${lang.nativeName} (${lang.englishName})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) StarkGold else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Multi-Language TextToSpeech Status & Voice Picker
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SlateSurface)
                                .border(1.dp, HudOutline, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = if (ttsStatus == TtsEngineStatus.READY) SecureEmerald else StarkGold
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${selectedLanguage.nativeName} • ${ttsStatus.label}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (ttsStatus == TtsEngineStatus.READY) SecureEmerald else StarkGold
                                        )
                                        Text(
                                            text = "सक्रिय व्हॉइस: $activeVoiceLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (availableVoices.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    ) {
                                        availableVoices.forEach { voice ->
                                            val selected = voice.name == selectedVoiceName
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (selected) ArcCyan.copy(alpha = 0.2f)
                                                        else SlateSurfaceElevated
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (selected) ArcCyan else HudOutline,
                                                        shape = RoundedCornerShape(50)
                                                    )
                                                    .clickable { onSelectVoice(voice.name) }
                                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = voice.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selected) ArcCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { onTestVoice(preferredTitle, speechRate, selectedLanguage) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("test_voice_tts_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = ArcCyan
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${selectedLanguage.nativeName} आवाज तपासा", color = ArcCyan)
                                    }
                                    if (ttsStatus == TtsEngineStatus.MISSING_DATA || ttsStatus == TtsEngineStatus.FALLBACK_LANGUAGE) {
                                        OutlinedButton(
                                            onClick = onInstallVoiceData,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = StarkGold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("व्हॉइस पॅक मिळवा", color = StarkGold)
                                        }
                                    }
                                }
                            }
                        }

                        StarkGlowButton(
                            onClick = { onUpdateSettings(preferredTitle, autoSpeak, speechRate.toDouble(), selectedLanguage) },
                            containerColor = ArcCyanDim,
                            contentColor = ArcCyanBright,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_settings_button")
                        ) {
                            StarkButtonLabel(
                                text = "सेटिंग्ज सेव्ह करा",
                                color = ArcCyanBright,
                                glowColor = ArcCyan
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onClearChat,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("चॅट साफ करा")
                            }
                            OutlinedButton(
                                onClick = { signOut(credentialManager, scope) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AlertCrimson)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.sign_out), color = AlertCrimson)
                            }
                        }
                    }
                }
            }

            // Memories & Interests Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = StarkGold)
                    Text(
                        text = "JARVIS च्या आठवणी व आवडी (${memories.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            when (memoriesState) {
                is UiState.Loading -> item { CircularProgressIndicator(color = ArcCyan) }
                is UiState.Error -> item { Text(memoriesState.message, color = AlertCrimson) }
                is UiState.Success -> {
                    if (memories.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HudOutline, RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = "तुमच्या आवडी, छंद किंवा महत्त्वाच्या गोष्टी इथे जोडा — गप्पा मोडमध्ये JARVIS त्या नैसर्गिकपणे लक्षात ठेवेल!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(memories, key = { it.id }) { mem ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HudOutline, RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${mem.categoryEnum().labelMarathi} • ${mem.topic}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StarkGold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(mem.detail, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    IconButton(onClick = { onDeleteMemory(mem.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete memory",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        StarkGlowFab(
            onClick = { showAddMemoryDialog = true },
            accentColor = ArcCyan,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_memory_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add memory")
        }
    }

    if (showAddMemoryDialog) {
        AddMemoryDialog(
            onDismiss = { showAddMemoryDialog = false },
            onConfirm = { topic, detail, cat ->
                onAddMemory(topic, detail, cat)
                showAddMemoryDialog = false
            }
        )
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, MemoryCategory) -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MemoryCategory.INTEREST) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        title = { Text("नवीन आठवण / आवड जोडा") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("विषय (उदा. क्रिकेट, कोडिंग, कॉफी)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("तपशील") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    MemoryCategory.entries.forEach { cat ->
                        val selected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) StarkGold.copy(alpha = 0.2f) else SlateSurface)
                                .border(1.dp, if (selected) StarkGold else HudOutline, RoundedCornerShape(50))
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat.labelMarathi,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) StarkGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(topic, detail, category) },
                enabled = topic.isNotBlank() && detail.isNotBlank()
            ) {
                Text("सेव्ह करा")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करा")
            }
        }
    )
}

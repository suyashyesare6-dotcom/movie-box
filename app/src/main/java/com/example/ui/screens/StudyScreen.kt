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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AiResponseState
import com.example.model.StudyNote
import com.example.model.StudyNoteType
import com.example.model.UiState
import com.example.ui.components.StarkButtonLabel
import com.example.ui.components.StarkGlowButton
import com.example.ui.components.StarkGlowFab
import com.example.ui.components.StarkGlowText
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanBright
import com.example.ui.theme.ArcCyanDim
import com.example.ui.theme.HudOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StarkGold

@Composable
fun StudyScreen(
    notesState: UiState<List<StudyNote>>,
    studyDraftState: AiResponseState,
    studyDraftText: String,
    onLaunchStudyPrompt: (String) -> Unit,
    onGenerateStudyNote: (String, String, StudyNoteType) -> Unit,
    onAddManualNote: (String, String, String, StudyNoteType) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAiGeneratorDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var quizTopic by remember { mutableStateOf("") }

    val notes = (notesState as? UiState.Success)?.data ?: emptyList()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Study Launcher Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ArcCyan.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = ArcCyanBright)
                            StarkGlowText(
                                text = "STUDY MODE & QUIZ LAB",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArcCyanBright,
                                glowColor = ArcCyan
                            )
                        }
                        Text(
                            text = "सोप्या मराठीत अभ्यास, हिंट्स आणि एक-एक प्रश्न क्विझ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = quizTopic,
                            onValueChange = { quizTopic = it },
                            label = { Text("विषय / प्रश्न टाका (उदा. Operating Systems Deadlock)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("study_topic_input")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StarkGlowButton(
                                onClick = {
                                    val topic = quizTopic.ifBlank { "General Science" }
                                    onLaunchStudyPrompt("बॉस, मला $topic या विषयावर स्टडी मोडमध्ये एक-एक प्रश्न विचारून क्विझ घे. आत्ता फक्त पहिला प्रश्न विचार.")
                                },
                                containerColor = ArcCyanDim,
                                contentColor = ArcCyanBright,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_quiz_button")
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = ArcCyanBright, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                StarkButtonLabel(text = "क्विझ सुरू करा", color = ArcCyanBright, glowColor = ArcCyan)
                            }
                            OutlinedButton(
                                onClick = {
                                    val topic = quizTopic.ifBlank { "माझा अवघड टॉपिक" }
                                    onLaunchStudyPrompt("$topic मध्ये मी अडकलोय, मला आधी फक्त हिंट दे, पूर्ण उत्तर देऊ नकोस.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hint_first_button")
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StarkGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("आधी हिंट द्या", color = StarkGold)
                            }
                        }
                        StarkGlowButton(
                            onClick = { showAiGeneratorDialog = true },
                            accentColor = StarkGold,
                            containerColor = StarkGold.copy(alpha = 0.16f),
                            contentColor = StarkGold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_study_note_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StarkGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            StarkButtonLabel(
                                text = "AI सारांश / वेळापत्रक / रिव्हिजन प्लॅन बनवा",
                                color = StarkGold
                            )
                        }
                    }
                }
            }

            if (studyDraftState is AiResponseState.Loading || studyDraftText.isNotBlank() || studyDraftState is AiResponseState.Error) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StarkGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("JARVIS तयार केलेली नोट", style = MaterialTheme.typography.labelMedium, color = StarkGold)
                                if (studyDraftText.isNotBlank()) {
                                    IconButton(onClick = { onSpeakText(studyDraftText) }) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak note", tint = ArcCyan)
                                    }
                                }
                            }
                            if (studyDraftState is AiResponseState.Loading) {
                                CircularProgressIndicator(color = ArcCyan, modifier = Modifier.size(22.dp))
                            } else if (studyDraftState is AiResponseState.Error) {
                                Text(studyDraftState.message, color = AlertCrimson)
                            }
                            if (studyDraftText.isNotBlank()) {
                                Text(studyDraftText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "सेव्ह केलेल्या स्टडी नोट्स (${notes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            when (notesState) {
                is UiState.Loading -> {
                    item { CircularProgressIndicator(color = ArcCyan) }
                }
                is UiState.Error -> {
                    item { Text(notesState.message, color = AlertCrimson) }
                }
                is UiState.Success -> {
                    if (notes.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HudOutline, RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    text = "अजून स्टडी नोट्स नाहीत, बॉस. वरून AI प्लॅन बनवा किंवा + दाबून तुमची नोट सेव्ह करा.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(notes, key = { it.id }) { note ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HudOutline, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${note.subject} • ${note.noteTypeEnum().labelMarathi}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ArcCyan
                                        )
                                        Row {
                                            IconButton(onClick = { onSpeakText(note.content) }) {
                                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = StarkGold)
                                            }
                                            IconButton(onClick = { onDeleteNote(note.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        StarkGlowFab(
            onClick = { showManualDialog = true },
            accentColor = ArcCyan,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_study_note_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add manual study note")
        }
    }

    if (showAiGeneratorDialog) {
        StudyNoteDialog(
            titleLabel = "AI स्टडी नोट / वेळापत्रक तयार करा",
            isAiMode = true,
            onDismiss = { showAiGeneratorDialog = false },
            onConfirm = { subject, title, _, noteType ->
                onGenerateStudyNote(subject, title, noteType)
                showAiGeneratorDialog = false
            }
        )
    }

    if (showManualDialog) {
        StudyNoteDialog(
            titleLabel = "स्वतःची स्टडी नोट जोडा",
            isAiMode = false,
            onDismiss = { showManualDialog = false },
            onConfirm = { subject, title, content, noteType ->
                onAddManualNote(subject, title, content, noteType)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun StudyNoteDialog(
    titleLabel: String,
    isAiMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, StudyNoteType) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var noteType by remember { mutableStateOf(StudyNoteType.SUMMARY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        title = { Text(titleLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("विषय (उदा. Physics, DSA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text(if (isAiMode) "टॉपिक किंवा परीक्षेची तारीख" else "शीर्षक") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isAiMode) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("नोट मजकूर") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudyNoteType.entries.forEach { t ->
                        val selected = noteType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) ArcCyan.copy(alpha = 0.2f) else SlateSurface)
                                .border(1.dp, if (selected) ArcCyan else HudOutline, RoundedCornerShape(50))
                                .clickable { noteType = t }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = t.labelMarathi,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) ArcCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(subject, topic, content, noteType) },
                enabled = subject.isNotBlank() && topic.isNotBlank() && (isAiMode || content.isNotBlank())
            ) {
                Text(if (isAiMode) "तयार करा" else "सेव्ह करा")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करा")
            }
        }
    )
}

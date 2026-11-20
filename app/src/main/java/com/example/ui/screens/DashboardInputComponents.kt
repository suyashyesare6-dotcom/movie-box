package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.DashboardDomain
import com.example.model.HousekeepingCategory
import com.example.model.PriorityLevel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.HudOutline
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StarkGold

@Composable
fun QuickAddTaskBillCard(
    activeDomain: DashboardDomain,
    isListening: Boolean,
    isParsingVoiceTask: Boolean,
    partialTranscript: String,
    voiceStatusNote: String?,
    onVoiceMicClick: (targetDomain: DashboardDomain) -> Unit,
    onAddQuickItem: (
        title: String,
        category: HousekeepingCategory,
        domain: DashboardDomain,
        priority: PriorityLevel
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var quickTitle by remember { mutableStateOf("") }
    var isBill by remember { mutableStateOf(false) }
    var quickDomain by remember(activeDomain) { mutableStateOf(activeDomain) }
    var quickPriority by remember { mutableStateOf(PriorityLevel.MEDIUM) }
    val accentColor = if (isBill) StarkGold else quickDomain.accentColor()

    fun submitQuickItem() {
        val clean = quickTitle.trim()
        if (clean.isEmpty()) return
        onAddQuickItem(
            clean,
            if (isBill) HousekeepingCategory.BILL else HousekeepingCategory.TASK,
            quickDomain,
            quickPriority
        )
        quickTitle = ""
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isListening) AlertCrimson.copy(alpha = 0.75f) else accentColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = quickTitle,
                    onValueChange = { quickTitle = it },
                    placeholder = {
                        Text(
                            if (isBill) "बिल लिहा किंवा मराठीत बोला..."
                            else "काम लिहा किंवा मराठीत बोला..."
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitQuickItem() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_add_title_input")
                )

                FilledIconButton(
                    onClick = { onVoiceMicClick(quickDomain) },
                    enabled = !isParsingVoiceTask,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isListening) AlertCrimson else SlateSurface,
                        contentColor = if (isListening) MaterialTheme.colorScheme.onError else ArcCyan
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .border(
                            width = 1.dp,
                            color = if (isListening) AlertCrimson else ArcCyan.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50)
                        )
                        .testTag("dashboard_voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop Marathi voice input" else "Add task or bill by Marathi voice"
                    )
                }

                FilledIconButton(
                    onClick = { submitQuickItem() },
                    enabled = quickTitle.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("quick_add_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add task or bill"
                    )
                }
            }

            AnimatedVisibility(visible = isListening || isParsingVoiceTask || voiceStatusNote != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateSurface)
                        .border(
                            width = 1.dp,
                            color = when {
                                isListening -> AlertCrimson.copy(alpha = 0.55f)
                                isParsingVoiceTask -> ArcCyan.copy(alpha = 0.55f)
                                else -> HudOutline
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (isParsingVoiceTask) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ArcCyan,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "JARVIS मराठी कमांड समजून Firestore मध्ये सेव्ह करतोय…",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArcCyan
                        )
                    } else if (isListening) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = AlertCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = partialTranscript.ifBlank {
                                "मराठीत बोला… (उदा. 'उद्या वीज बिल ५०० रुपये भरायचे आहे')"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (voiceStatusNote != null) {
                        Text(
                            text = voiceStatusNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = StarkGold
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                DashboardFilterChip(
                    label = "Task",
                    selected = !isBill,
                    accentColor = ArcCyan,
                    onClick = { isBill = false },
                    modifier = Modifier.testTag("quick_add_type_task")
                )
                DashboardFilterChip(
                    label = "Bill",
                    selected = isBill,
                    accentColor = StarkGold,
                    onClick = { isBill = true },
                    modifier = Modifier.testTag("quick_add_type_bill")
                )

                Spacer(modifier = Modifier.width(4.dp))

                DashboardDomain.entries.forEach { domain ->
                    DashboardFilterChip(
                        label = domain.labelEnglish,
                        selected = quickDomain == domain,
                        accentColor = domain.accentColor(),
                        onClick = { quickDomain = domain },
                        modifier = Modifier.testTag("quick_add_domain_${domain.name.lowercase()}")
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Priority",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Priority:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(PriorityLevel.HIGH, PriorityLevel.MEDIUM, PriorityLevel.LOW).forEach { level ->
                    DashboardFilterChip(
                        label = "${level.labelEnglish} (${level.labelMarathi})",
                        selected = quickPriority == level,
                        accentColor = level.accentColor(),
                        onClick = { quickPriority = level },
                        modifier = Modifier.testTag("quick_add_priority_${level.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
fun AddDashboardItemDialog(
    initialDomain: DashboardDomain,
    isListening: Boolean,
    onDictateTitle: (onTranscript: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        notes: String,
        category: HousekeepingCategory,
        domain: DashboardDomain,
        scheduleLabel: String,
        priority: PriorityLevel
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("आज") }
    var domain by remember(initialDomain) { mutableStateOf(initialDomain) }
    var category by remember { mutableStateOf(HousekeepingCategory.TASK) }
    var priority by remember { mutableStateOf(PriorityLevel.MEDIUM) }
    var showDetails by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        titleFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurfaceElevated,
        title = { Text("नवीन काम किंवा बिल जोडा") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(
                            if (category == HousekeepingCategory.BILL) {
                                "बिलाचे नाव (उदा. वीज बिल / Wi-Fi बिल)"
                            } else {
                                "कामाचे नाव (उदा. पासवर्ड अपडेट / उजळणी)"
                            }
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onDictateTitle { spoken ->
                                    title = spoken.take(120)
                                }
                            },
                            modifier = Modifier.testTag("dialog_voice_mic_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Dictate in Marathi",
                                tint = if (isListening) AlertCrimson else ArcCyan
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (title.isNotBlank()) {
                                onConfirm(title, notes, category, domain, schedule, priority)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .testTag("dashboard_title_input")
                )

                Text("प्रकार:", style = MaterialTheme.typography.labelMedium, color = StarkGold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    DashboardFilterChip(
                        label = "Task",
                        selected = category == HousekeepingCategory.TASK,
                        accentColor = ArcCyan,
                        onClick = { category = HousekeepingCategory.TASK },
                        modifier = Modifier.testTag("dialog_category_task")
                    )
                    DashboardFilterChip(
                        label = "Bill",
                        selected = category == HousekeepingCategory.BILL,
                        accentColor = StarkGold,
                        onClick = { category = HousekeepingCategory.BILL },
                        modifier = Modifier.testTag("dialog_category_bill")
                    )
                    DashboardFilterChip(
                        label = "Shopping",
                        selected = category == HousekeepingCategory.SHOPPING,
                        accentColor = ArcCyan,
                        onClick = { category = HousekeepingCategory.SHOPPING },
                        modifier = Modifier.testTag("dialog_category_shopping")
                    )
                    DashboardFilterChip(
                        label = "Routine",
                        selected = category == HousekeepingCategory.ROUTINE,
                        accentColor = ArcCyan,
                        onClick = { category = HousekeepingCategory.ROUTINE },
                        modifier = Modifier.testTag("dialog_category_routine")
                    )
                }

                Text("विभाग (Category):", style = MaterialTheme.typography.labelMedium, color = ArcCyan)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    DashboardDomain.entries.forEach { d ->
                        DashboardFilterChip(
                            label = d.labelEnglish,
                            selected = domain == d,
                            accentColor = d.accentColor(),
                            onClick = { domain = d },
                            modifier = Modifier.testTag("dialog_domain_${d.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "महत्त्व / Priority:",
                    style = MaterialTheme.typography.labelMedium,
                    color = quickPriorityTitleColor(priority)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(PriorityLevel.HIGH, PriorityLevel.MEDIUM, PriorityLevel.LOW).forEach { level ->
                        DashboardFilterChip(
                            label = "${level.labelEnglish} • ${level.labelMarathi}",
                            selected = priority == level,
                            accentColor = level.accentColor(),
                            onClick = { priority = level },
                            modifier = Modifier.testTag("dialog_priority_${level.name.lowercase()}")
                        )
                    }
                }

                TextButton(
                    onClick = { showDetails = !showDetails },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("toggle_more_details_button")
                ) {
                    Icon(
                        imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showDetails) "अधिक तपशील लपवा" else "अधिक तपशील (वेळ, रक्कम, नोट्स)")
                }

                AnimatedVisibility(visible = showDetails) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = schedule,
                            onValueChange = { schedule = it },
                            label = { Text("वेळ / रक्कम (उदा. आज संध्याकाळी 7 • ₹950)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_schedule_input")
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("अधिक माहिती (ऐच्छिक)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_notes_input")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, notes, category, domain, schedule, priority) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = domain.accentColor(),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("save_dashboard_item_button")
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

@Composable
private fun quickPriorityTitleColor(priority: PriorityLevel) = when (priority) {
    PriorityLevel.HIGH -> AlertCrimson
    PriorityLevel.MEDIUM -> StarkGold
    PriorityLevel.LOW -> SecureEmerald
}

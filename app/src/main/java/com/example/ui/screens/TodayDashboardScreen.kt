package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.JarvisVoiceController
import com.example.model.AiResponseState
import com.example.model.DashboardDomain
import com.example.model.DashboardSortMode
import com.example.model.HousekeepingCategory
import com.example.model.HousekeepingItem
import com.example.model.PriorityLevel
import com.example.model.UiState
import com.example.ui.components.StarkButtonLabel
import com.example.ui.components.StarkGlowButton
import com.example.ui.components.StarkGlowFab
import com.example.ui.components.StarkGlowText
import com.example.ui.components.starkPulseGlow
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
fun TodayDashboardScreen(
    itemsState: UiState<List<HousekeepingItem>>,
    briefingState: AiResponseState,
    briefingText: String,
    voiceController: JarvisVoiceController,
    isListening: Boolean,
    isParsingVoiceTask: Boolean,
    partialTranscript: String,
    voiceStatusNote: String?,
    onGenerateBriefing: () -> Unit,
    onSpeakText: (String) -> Unit,
    onAddFromVoice: (transcript: String, defaultDomain: DashboardDomain) -> Unit,
    onAddItem: (
        title: String,
        notes: String,
        category: HousekeepingCategory,
        domain: DashboardDomain,
        scheduleLabel: String,
        priority: PriorityLevel
    ) -> Unit,
    onToggleItem: (HousekeepingItem) -> Unit,
    onUpdatePriority: (HousekeepingItem, PriorityLevel) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf<DashboardDomain?>(null) }
    var selectedType by remember { mutableStateOf(DashboardTypeFilter.ALL) }
    var selectedPriorityFilter by remember { mutableStateOf<PriorityLevel?>(null) }
    var selectedSortMode by rememberSaveable { mutableStateOf(DashboardSortMode.CATEGORY_GROUPED) }
    var briefingExpanded by remember { mutableStateOf(false) }
    var addDialogDomain by remember { mutableStateOf<DashboardDomain?>(null) }
    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceController.clearStatusNote()
            pendingVoiceAction?.invoke()
        } else {
            voiceController.reportStatus("मायक्रोफोन परवानगी नाकारली गेली, बॉस. व्हॉइस कमांडसाठी ऑडिओ परवानगी द्या.")
        }
        pendingVoiceAction = null
    }

    fun launchWithMicPermission(onReady: () -> Unit) {
        if (isListening) {
            voiceController.stopListening()
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            onReady()
        } else {
            pendingVoiceAction = onReady
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val allItems = (itemsState as? UiState.Success)?.data ?: emptyList()

    val highPriorityPendingCount = remember(allItems) {
        allItems.count { !it.completed && it.priorityEnum() == PriorityLevel.HIGH }
    }
    val pendingTasksCount = remember(allItems) {
        allItems.count { !it.completed && it.categoryEnum() != HousekeepingCategory.BILL }
    }
    val pendingBillsCount = remember(allItems) {
        allItems.count { !it.completed && it.categoryEnum() == HousekeepingCategory.BILL }
    }
    val completedCount = remember(allItems) {
        allItems.count { it.completed }
    }
    val totalCount = allItems.size
    val progressRatio = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    val normalizedQuery = searchQuery.trim()
    val searchFilteredItems = remember(allItems, normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            allItems
        } else {
            allItems.filter { item ->
                val domain = item.domainEnum()
                val category = item.categoryEnum()
                val priority = item.priorityEnum()
                item.title.contains(normalizedQuery, ignoreCase = true) ||
                    domain.labelEnglish.contains(normalizedQuery, ignoreCase = true) ||
                    domain.labelMarathi.contains(normalizedQuery, ignoreCase = true) ||
                    domain.name.contains(normalizedQuery, ignoreCase = true) ||
                    category.shortEnglish.contains(normalizedQuery, ignoreCase = true) ||
                    category.labelMarathi.contains(normalizedQuery, ignoreCase = true) ||
                    category.name.contains(normalizedQuery, ignoreCase = true) ||
                    priority.labelEnglish.contains(normalizedQuery, ignoreCase = true) ||
                    priority.labelMarathi.contains(normalizedQuery, ignoreCase = true) ||
                    priority.name.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    val typeFilteredItems = remember(searchFilteredItems, selectedType) {
        when (selectedType) {
            DashboardTypeFilter.ALL -> searchFilteredItems
            DashboardTypeFilter.TASKS -> searchFilteredItems.filter { it.categoryEnum() != HousekeepingCategory.BILL }
            DashboardTypeFilter.BILLS -> searchFilteredItems.filter { it.categoryEnum() == HousekeepingCategory.BILL }
        }
    }

    val priorityFilteredItems = remember(typeFilteredItems, selectedPriorityFilter) {
        selectedPriorityFilter?.let { targetPriority ->
            typeFilteredItems.filter { it.priorityEnum() == targetPriority }
        } ?: typeFilteredItems
    }

    val activeComparator = remember(selectedSortMode) {
        when (selectedSortMode) {
            DashboardSortMode.IMPORTANCE_HIGH_FIRST,
            DashboardSortMode.CATEGORY_GROUPED -> compareBy<HousekeepingItem> { it.completed }
                .thenByDescending { it.priorityEnum().rank }
                .thenByDescending { it.createdAt?.seconds ?: 0L }

            DashboardSortMode.IMPORTANCE_LOW_FIRST -> compareBy<HousekeepingItem> { it.completed }
                .thenBy { it.priorityEnum().rank }
                .thenByDescending { it.createdAt?.seconds ?: 0L }

            DashboardSortMode.NEWEST_FIRST -> compareBy<HousekeepingItem> { it.completed }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
                .thenByDescending { it.priorityEnum().rank }
        }
    }

    val groupedByDomain = remember(priorityFilteredItems, activeComparator) {
        DashboardDomain.entries.associateWith { domain ->
            priorityFilteredItems
                .filter { it.domainEnum() == domain }
                .sortedWith(activeComparator)
        }
    }

    val flatSortedItems = remember(priorityFilteredItems, selectedDomain, activeComparator) {
        val domainScoped = selectedDomain?.let { target ->
            priorityFilteredItems.filter { it.domainEnum() == target }
        } ?: priorityFilteredItems
        domainScoped.sortedWith(activeComparator)
    }

    val visibleDomains = remember(selectedDomain) {
        selectedDomain?.let { listOf(it) } ?: DashboardDomain.entries
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pinned Real-Time Search Bar at the top of the Dashboard
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("कामे किंवा बिले शोधा (शीर्षक / विभाग)...")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search tasks and bills",
                            tint = ArcCyan
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("dashboard_search_clear")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateSurfaceElevated,
                        unfocusedContainerColor = SlateSurfaceElevated,
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = HudOutline
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_search_input")
                )

                AnimatedVisibility(visible = normalizedQuery.isNotEmpty()) {
                    val matchCount = flatSortedItems.size
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "“$normalizedQuery” साठी $matchCount परिणाम",
                            style = MaterialTheme.typography.labelMedium,
                            color = ArcCyan
                        )
                        TextButton(
                            onClick = { searchQuery = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("dashboard_search_reset_chip")
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("today_dashboard_list")
            ) {
            // Top Dashboard Header & Daily Progress
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StarkGlowText(
                                text = stringResource(R.string.dashboard_title),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                glowColor = ArcCyan
                            )
                            Text(
                                text = stringResource(R.string.dashboard_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DashboardKpiCard(
                            label = "High Priority",
                            count = highPriorityPendingCount,
                            accentColor = AlertCrimson,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardKpiCard(
                            label = "Tasks",
                            count = pendingTasksCount,
                            accentColor = ArcCyan,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardKpiCard(
                            label = "Bills Due",
                            count = pendingBillsCount,
                            accentColor = StarkGold,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardKpiCard(
                            label = "Done",
                            count = completedCount,
                            accentColor = SecureEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (totalCount > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "आजची प्रगती",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$completedCount / $totalCount पूर्ण",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SecureEmerald
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                color = SecureEmerald,
                                trackColor = SlateSurfaceElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }

            // Compact JARVIS Daily Briefing Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ArcCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ArcCyanDim.copy(alpha = 0.4f), SlateSurfaceElevated)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StarkGlowText(
                                    text = "JARVIS DAILY BRIEFING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArcCyanBright,
                                    glowColor = ArcCyan
                                )
                                Text(
                                    text = stringResource(R.string.daily_briefing_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (briefingText.isNotBlank()) {
                                    IconButton(
                                        onClick = { onSpeakText(briefingText) },
                                        modifier = Modifier.testTag("speak_briefing_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak daily briefing",
                                            tint = StarkGold
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { briefingExpanded = !briefingExpanded },
                                    modifier = Modifier.testTag("toggle_briefing_expand_button")
                                ) {
                                    Icon(
                                        imageVector = if (briefingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (briefingExpanded) "Collapse briefing" else "Expand briefing",
                                        tint = ArcCyan
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = briefingExpanded || briefingText.isNotBlank() || briefingState is AiResponseState.Loading || briefingState is AiResponseState.Error) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                when {
                                    briefingText.isNotBlank() -> {
                                        Text(
                                            text = briefingText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    briefingState is AiResponseState.Error -> {
                                        Text(
                                            text = briefingState.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AlertCrimson
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "बॉस, आजच्या हाऊसकीपिंग, अभ्यास व सुरक्षा कामांचा आणि बिलांचा थोडक्यात आढावा ऐका.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        StarkGlowButton(
                            onClick = {
                                briefingExpanded = true
                                onGenerateBriefing()
                            },
                            enabled = briefingState !is AiResponseState.Loading,
                            containerColor = ArcCyanDim,
                            contentColor = ArcCyanBright,
                            modifier = Modifier.testTag("generate_briefing_button")
                        ) {
                            if (briefingState is AiResponseState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = ArcCyanBright,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ArcCyanBright,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StarkButtonLabel(
                                    text = stringResource(R.string.daily_briefing_cta),
                                    color = ArcCyanBright,
                                    glowColor = ArcCyan
                                )
                            }
                        }
                    }
                }
            }

            // Domain, Type, Priority Filter & Sort By Importance Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        DashboardFilterChip(
                            label = "All (${priorityFilteredItems.size})",
                            selected = selectedDomain == null,
                            onClick = { selectedDomain = null },
                            modifier = Modifier.testTag("filter_domain_all")
                        )
                        DashboardDomain.entries.forEach { domain ->
                            val count = groupedByDomain[domain]?.size ?: 0
                            DashboardFilterChip(
                                label = "${domain.labelEnglish} ($count)",
                                selected = selectedDomain == domain,
                                accentColor = domain.accentColor(),
                                onClick = {
                                    selectedDomain = if (selectedDomain == domain) null else domain
                                },
                                modifier = Modifier.testTag("filter_domain_${domain.name.lowercase()}")
                            )
                        }
                    }

                    // Type & Priority Filter Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        DashboardTypeFilter.entries.forEach { filter ->
                            DashboardFilterChip(
                                label = filter.label,
                                selected = selectedType == filter,
                                accentColor = if (filter == DashboardTypeFilter.BILLS) StarkGold else ArcCyan,
                                onClick = { selectedType = filter },
                                modifier = Modifier.testTag("filter_type_${filter.name.lowercase()}")
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Filter by priority",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp)
                        )
                        listOf(PriorityLevel.HIGH, PriorityLevel.MEDIUM, PriorityLevel.LOW).forEach { level ->
                            val levelCount = typeFilteredItems.count { it.priorityEnum() == level }
                            DashboardFilterChip(
                                label = "${level.labelEnglish} ($levelCount)",
                                selected = selectedPriorityFilter == level,
                                accentColor = level.accentColor(),
                                onClick = {
                                    selectedPriorityFilter = if (selectedPriorityFilter == level) null else level
                                },
                                modifier = Modifier.testTag("filter_priority_${level.name.lowercase()}")
                            )
                        }
                    }

                    // Sort By Importance Control Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort tasks and bills",
                            tint = ArcCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sort:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DashboardSortMode.entries.forEach { sortMode ->
                            val chipAccent = when (sortMode) {
                                DashboardSortMode.IMPORTANCE_HIGH_FIRST -> AlertCrimson
                                DashboardSortMode.IMPORTANCE_LOW_FIRST -> SecureEmerald
                                else -> ArcCyan
                            }
                            DashboardFilterChip(
                                label = sortMode.labelEnglish,
                                selected = selectedSortMode == sortMode,
                                accentColor = chipAccent,
                                onClick = { selectedSortMode = sortMode },
                                modifier = Modifier.testTag("sort_mode_${sortMode.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Inline Quick-Add Input Field + Voice Command Mic for direct Firestore creation
            item {
                QuickAddTaskBillCard(
                    activeDomain = selectedDomain ?: DashboardDomain.HOUSEKEEPING,
                    isListening = isListening,
                    isParsingVoiceTask = isParsingVoiceTask,
                    partialTranscript = partialTranscript,
                    voiceStatusNote = voiceStatusNote,
                    onVoiceMicClick = { targetDomain ->
                        launchWithMicPermission {
                            voiceController.startListening { spoken ->
                                onAddFromVoice(spoken, targetDomain)
                            }
                        }
                    },
                    onAddQuickItem = { title, category, domain, priority ->
                        onAddItem(
                            title,
                            "",
                            category,
                            domain,
                            "आज",
                            priority
                        )
                    }
                )
            }

            // Data Loading / Error / Categorized Sections
            when (itemsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            CircularProgressIndicator(color = ArcCyan)
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AlertCrimson.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Text(
                                text = itemsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AlertCrimson,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (selectedSortMode == DashboardSortMode.CATEGORY_GROUPED) {
                        val domainsToRender = if ((normalizedQuery.isNotEmpty() || selectedPriorityFilter != null) && selectedDomain == null) {
                            visibleDomains.filter { groupedByDomain[it].orEmpty().isNotEmpty() }
                        } else {
                            visibleDomains
                        }

                        if ((normalizedQuery.isNotEmpty() || selectedPriorityFilter != null) &&
                            domainsToRender.all { groupedByDomain[it].isNullOrEmpty() }
                        ) {
                            item(key = "empty_search_results") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, HudOutline, RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(18.dp)
                                    ) {
                                        Text(
                                            text = if (normalizedQuery.isNotEmpty()) {
                                                "“$normalizedQuery” साठी कोणतेही काम किंवा बिल सापडले नाही, बॉस."
                                            } else {
                                                "निवडलेल्या प्राधान्याचे कोणतेही काम किंवा बिल नाही, बॉस."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = {
                                                searchQuery = ""
                                                selectedPriorityFilter = null
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Text("फिल्टर साफ करा", color = ArcCyan)
                                        }
                                    }
                                }
                            }
                        } else {
                            domainsToRender.forEach { domain ->
                                val sectionItems = groupedByDomain[domain].orEmpty()
                                val pendingInSection = sectionItems.count { !it.completed }

                                item(key = "header_${domain.name}") {
                                    DashboardSectionHeader(
                                        domain = domain,
                                        pendingCount = pendingInSection,
                                        totalCount = sectionItems.size,
                                        onQuickAdd = { addDialogDomain = domain }
                                    )
                                }

                                if (sectionItems.isEmpty()) {
                                    item(key = "empty_${domain.name}") {
                                        DashboardSectionEmptyCard(
                                            domain = domain,
                                            typeFilter = selectedType,
                                            onQuickAdd = { addDialogDomain = domain }
                                        )
                                    }
                                } else {
                                    items(
                                        items = sectionItems,
                                        key = { item -> "${domain.name}_${item.id}" }
                                    ) { item ->
                                        SwipeToDeleteDashboardItem(
                                            item = item,
                                            showDomainBadge = false,
                                            onToggle = { onToggleItem(item) },
                                            onPriorityChange = { newPriority ->
                                                onUpdatePriority(item, newPriority)
                                            },
                                            onDelete = { onDeleteItem(item.id) },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "flat_sort_header_${selectedSortMode.name}") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Sorted: ${selectedSortMode.labelEnglish}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${selectedSortMode.labelMarathi} • ${flatSortedItems.count { !it.completed }} बाकी (${flatSortedItems.size} एकूण)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (flatSortedItems.isEmpty()) {
                            item(key = "empty_flat_sorted") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, HudOutline, RoundedCornerShape(16.dp))
                                ) {
                                    Text(
                                        text = "या फिल्टरसाठी कोणतेही काम किंवा बिल सापडले नाही, बॉस.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(18.dp)
                                    )
                                }
                            }
                        } else {
                            items(
                                items = flatSortedItems,
                                key = { item -> "sorted_${item.id}" }
                            ) { item ->
                                SwipeToDeleteDashboardItem(
                                    item = item,
                                    showDomainBadge = true,
                                    onToggle = { onToggleItem(item) },
                                    onPriorityChange = { newPriority ->
                                        onUpdatePriority(item, newPriority)
                                    },
                                    onDelete = { onDeleteItem(item.id) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(76.dp)) }
        }
        }

        StarkGlowFab(
            onClick = { addDialogDomain = selectedDomain ?: DashboardDomain.HOUSEKEEPING },
            accentColor = ArcCyan,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_dashboard_item_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add task or bill")
        }
    }

    addDialogDomain?.let { initialDomain ->
        AddDashboardItemDialog(
            initialDomain = initialDomain,
            isListening = isListening,
            onDictateTitle = { onTranscript ->
                launchWithMicPermission {
                    voiceController.startListening { spoken ->
                        onTranscript(spoken)
                    }
                }
            },
            onDismiss = { addDialogDomain = null },
            onConfirm = { title, notes, category, domain, schedule, priority ->
                onAddItem(title, notes, category, domain, schedule, priority)
                addDialogDomain = null
            }
        )
    }
}

package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.core.JarvisVoiceController
import com.example.data.JarvisAiService
import com.example.data.JarvisRepository
import com.example.model.DashboardDomain
import com.example.model.HousekeepingCategory
import com.example.model.JarvisMode
import com.example.model.MemoryCategory
import com.example.model.PriorityLevel
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.StarkHudBackground
import com.example.ui.screens.JarvisChatScreen
import com.example.ui.screens.MemorySettingsScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.screens.TodayDashboardScreen
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanBright
import com.example.ui.theme.ArcCyanDim
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.viewmodel.JarvisViewModel

enum class JarvisTab(val labelRes: Int, val icon: ImageVector) {
    JARVIS(R.string.nav_jarvis, Icons.Default.GraphicEq),
    DASHBOARD(R.string.nav_dashboard, Icons.Default.Dashboard),
    STUDY(R.string.nav_study, Icons.Default.School),
    SECURITY(R.string.nav_security, Icons.Default.Shield),
    MEMORY(R.string.nav_memory, Icons.Default.Psychology)
}

@Composable
fun MainAppScaffold(
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: JarvisViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                JarvisViewModel(
                    repository = JarvisRepository(app),
                    aiService = JarvisAiService(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val voiceController = remember(context) { JarvisVoiceController(context) }
    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val memoriesState by viewModel.memoriesState.collectAsStateWithLifecycle()
    val housekeepingState by viewModel.housekeepingState.collectAsStateWithLifecycle()
    val studyNotesState by viewModel.studyNotesState.collectAsStateWithLifecycle()
    val securityChecksState by viewModel.securityChecksState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()

    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val briefingState by viewModel.briefingState.collectAsStateWithLifecycle()
    val briefingText by viewModel.briefingText.collectAsStateWithLifecycle()
    val securityAnalysisState by viewModel.securityAnalysisState.collectAsStateWithLifecycle()
    val securityAnalysisText by viewModel.securityAnalysisText.collectAsStateWithLifecycle()
    val studyDraftState by viewModel.studyDraftState.collectAsStateWithLifecycle()
    val studyDraftText by viewModel.studyDraftText.collectAsStateWithLifecycle()
    val isParsingVoiceTask by viewModel.isParsingVoiceTask.collectAsStateWithLifecycle()

    val isListening by voiceController.isListening.collectAsStateWithLifecycle()
    val isSpeaking by voiceController.isSpeaking.collectAsStateWithLifecycle()
    val partialTranscript by voiceController.partialTranscript.collectAsStateWithLifecycle()
    val rmsLevel by voiceController.rmsLevel.collectAsStateWithLifecycle()
    val voiceStatusNote by voiceController.voiceStatusNote.collectAsStateWithLifecycle()
    val ttsStatus by voiceController.ttsStatus.collectAsStateWithLifecycle()
    val availableVoices by voiceController.availableVoices.collectAsStateWithLifecycle()
    val selectedVoiceName by voiceController.selectedVoiceName.collectAsStateWithLifecycle()
    val activeVoiceLabel by voiceController.activeVoiceLabel.collectAsStateWithLifecycle()
    val voicePitch by voiceController.voicePitch.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
        ?: UserProfile(id = currentUserId, userId = currentUserId)
    val activeLanguage = profile.languageEnum()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(activeLanguage) {
        voiceController.setActiveLanguage(activeLanguage)
    }

    LaunchedEffect(viewModel, profile.speechRate) {
        viewModel.speakQueue.collect { payload ->
            voiceController.speak(payload.text, profile.speechRate.toFloat(), payload.language)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(JarvisTab.JARVIS) }

    BackHandler(enabled = selectedTab != JarvisTab.JARVIS) {
        selectedTab = JarvisTab.JARVIS
    }

    StarkHudBackground(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets.navigationBars,
                bottomBar = {
                    if (!useRail) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(ArcCyan.copy(alpha = 0.35f))
                            )
                            NavigationBar(
                                containerColor = SlateSurface.copy(alpha = 0.94f),
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                JarvisTab.entries.forEach { tab ->
                                    val selected = tab == selectedTab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ArcCyanBright,
                                            selectedTextColor = ArcCyan,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            indicatorColor = ArcCyanDim
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(containerColor = SlateSurface.copy(alpha = 0.94f)) {
                            JarvisTab.entries.forEach { tab ->
                                val selected = tab == selectedTab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ArcCyanBright,
                                        selectedTextColor = ArcCyan,
                                        indicatorColor = ArcCyanDim
                                    ),
                                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxSize()
                                .background(ArcCyan.copy(alpha = 0.3f))
                        )
                    }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                            .fillMaxSize()
                    ) {
                        when (selectedTab) {
                            JarvisTab.JARVIS -> JarvisChatScreen(
                                profile = profile,
                                messagesState = messagesState,
                                aiState = aiState,
                                voiceController = voiceController,
                                isListening = isListening,
                                isSpeaking = isSpeaking,
                                ttsStatus = ttsStatus,
                                partialTranscript = partialTranscript,
                                rmsLevel = rmsLevel,
                                voiceStatusNote = voiceStatusNote,
                                onSelectMode = viewModel::selectMode,
                                onSelectLanguage = viewModel::selectLanguage,
                                onToggleAutoSpeak = { enabled ->
                                    viewModel.updateSettings(
                                        preferredTitle = profile.preferredTitle,
                                        autoSpeakEnabled = enabled,
                                        speechRate = profile.speechRate,
                                        preferredLanguage = activeLanguage
                                    )
                                },
                                onSendMessage = { viewModel.sendMessage(it) },
                                onSaveAsMemory = { msg ->
                                    viewModel.addMemory(
                                        topic = msg.take(40),
                                        detail = msg.take(280),
                                        category = MemoryCategory.CONVERSATION
                                    )
                                },
                                onSaveAsTask = { msg ->
                                    val targetDomain = when (profile.modeEnum()) {
                                        JarvisMode.STUDY -> DashboardDomain.STUDY
                                        JarvisMode.SECURITY -> DashboardDomain.SECURITY
                                        else -> DashboardDomain.HOUSEKEEPING
                                    }
                                    viewModel.addHousekeepingItem(
                                        title = msg.take(100),
                                        notes = "चॅटमधून जोडलेले",
                                        category = HousekeepingCategory.TASK,
                                        domain = targetDomain,
                                        scheduleLabel = "आज",
                                        priority = PriorityLevel.MEDIUM
                                    )
                                }
                            )

                            JarvisTab.DASHBOARD -> TodayDashboardScreen(
                                itemsState = housekeepingState,
                                briefingState = briefingState,
                                briefingText = briefingText,
                                voiceController = voiceController,
                                isListening = isListening,
                                isParsingVoiceTask = isParsingVoiceTask,
                                partialTranscript = partialTranscript,
                                voiceStatusNote = voiceStatusNote,
                                onGenerateBriefing = viewModel::generateDailyBriefing,
                                onSpeakText = { voiceController.speak(it, profile.speechRate.toFloat()) },
                                onAddFromVoice = viewModel::addTaskOrBillFromVoice,
                                onAddItem = viewModel::addHousekeepingItem,
                                onToggleItem = viewModel::toggleHousekeepingItem,
                                onUpdatePriority = viewModel::updateHousekeepingPriority,
                                onDeleteItem = viewModel::deleteHousekeepingItem
                            )

                            JarvisTab.STUDY -> StudyScreen(
                                notesState = studyNotesState,
                                studyDraftState = studyDraftState,
                                studyDraftText = studyDraftText,
                                onLaunchStudyPrompt = { prompt ->
                                    selectedTab = JarvisTab.JARVIS
                                    viewModel.sendMessage(prompt, modeOverride = JarvisMode.STUDY)
                                },
                                onGenerateStudyNote = viewModel::generateStudyDraft,
                                onAddManualNote = viewModel::addManualStudyNote,
                                onDeleteNote = viewModel::deleteStudyNote,
                                onSpeakText = { voiceController.speak(it, profile.speechRate.toFloat()) }
                            )

                            JarvisTab.SECURITY -> SecurityScreen(
                                checksState = securityChecksState,
                                analysisState = securityAnalysisState,
                                analysisText = securityAnalysisText,
                                onAnalyzeSuspiciousText = viewModel::analyzeSuspiciousText,
                                onToggleCheck = viewModel::toggleSecurityCheck,
                                onSpeakText = { voiceController.speak(it, profile.speechRate.toFloat()) }
                            )

                            JarvisTab.MEMORY -> MemorySettingsScreen(
                                profile = profile,
                                memoriesState = memoriesState,
                                ttsStatus = ttsStatus,
                                availableVoices = availableVoices,
                                selectedVoiceName = selectedVoiceName,
                                activeVoiceLabel = activeVoiceLabel,
                                voicePitch = voicePitch,
                                onSelectVoice = voiceController::selectVoice,
                                onPitchChange = voiceController::setPitch,
                                onSelectLanguage = viewModel::selectLanguage,
                                onTestVoice = { title, rate, lang ->
                                    voiceController.testVoice(title, rate, lang)
                                },
                                onInstallVoiceData = {
                                    voiceController.openInstallVoiceData(context)
                                },
                                onUpdateSettings = viewModel::updateSettings,
                                onAddMemory = viewModel::addMemory,
                                onDeleteMemory = viewModel::deleteMemory,
                                onClearChat = viewModel::clearChatHistory
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

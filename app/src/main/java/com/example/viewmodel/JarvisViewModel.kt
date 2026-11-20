package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.JarvisAiService
import com.example.data.JarvisRepository
import com.example.model.AiLanguage
import com.example.model.AiResponseState
import com.example.model.ChatMessage
import com.example.model.DashboardDomain
import com.example.model.HousekeepingCategory
import com.example.model.HousekeepingItem
import com.example.model.JarvisMode
import com.example.model.MemoryCategory
import com.example.model.MemoryItem
import com.example.model.MessageSender
import com.example.model.PriorityLevel
import com.example.model.SecurityCategory
import com.example.model.SecurityCheck
import com.example.model.StudyNote
import com.example.model.StudyNoteType
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(
    private val repository: JarvisRepository,
    private val aiService: JarvisAiService,
    val currentUserId: String
) : ViewModel() {

    private fun unwrapFirestoreError(error: Throwable, contextTag: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error in $contextTag", rootError)
        return rootError.localizedMessage ?: "डेटा लोड करताना अडचण आली, बॉस."
    }

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeProfile(currentUserId)
        .map<UserProfile, UiState<UserProfile>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "profile"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val memoriesState: StateFlow<UiState<List<MemoryItem>>> = repository.observeMemories(currentUserId)
        .map<List<MemoryItem>, UiState<List<MemoryItem>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "memories"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val housekeepingState: StateFlow<UiState<List<HousekeepingItem>>> = repository.observeHousekeeping(currentUserId)
        .map<List<HousekeepingItem>, UiState<List<HousekeepingItem>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "housekeeping"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val studyNotesState: StateFlow<UiState<List<StudyNote>>> = repository.observeStudyNotes(currentUserId)
        .map<List<StudyNote>, UiState<List<StudyNote>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "studyNotes"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val securityChecksState: StateFlow<UiState<List<SecurityCheck>>> = repository.observeSecurityChecks(currentUserId)
        .map<List<SecurityCheck>, UiState<List<SecurityCheck>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "securityChecks"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = repository.observeMessages(currentUserId)
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "messages"))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _aiState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val aiState: StateFlow<AiResponseState> = _aiState.asStateFlow()

    private val _briefingState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val briefingState: StateFlow<AiResponseState> = _briefingState.asStateFlow()

    private val _briefingText = MutableStateFlow("")
    val briefingText: StateFlow<String> = _briefingText.asStateFlow()

    private val _securityAnalysisState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val securityAnalysisState: StateFlow<AiResponseState> = _securityAnalysisState.asStateFlow()

    private val _securityAnalysisText = MutableStateFlow("")
    val securityAnalysisText: StateFlow<String> = _securityAnalysisText.asStateFlow()

    private val _studyDraftState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val studyDraftState: StateFlow<AiResponseState> = _studyDraftState.asStateFlow()

    private val _studyDraftText = MutableStateFlow("")
    val studyDraftText: StateFlow<String> = _studyDraftText.asStateFlow()

data class SpeakPayload(
    val text: String,
    val language: AiLanguage
)

    private val _isParsingVoiceTask = MutableStateFlow(false)
    val isParsingVoiceTask: StateFlow<Boolean> = _isParsingVoiceTask.asStateFlow()

    private val _speakQueue = MutableSharedFlow<SpeakPayload>(extraBufferCapacity = 4)
    val speakQueue: SharedFlow<SpeakPayload> = _speakQueue.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureProfileInitialized(currentUserId)
            repository.seedDefaultSecurityChecksIfEmpty(currentUserId)
        }
    }

    private fun currentProfile(): UserProfile =
        (profileState.value as? UiState.Success)?.data ?: UserProfile(id = currentUserId, userId = currentUserId)

    private fun currentMemories(): List<MemoryItem> =
        (memoriesState.value as? UiState.Success)?.data ?: emptyList()

    private fun currentHousekeeping(): List<HousekeepingItem> =
        (housekeepingState.value as? UiState.Success)?.data ?: emptyList()

    private fun currentStudyNotes(): List<StudyNote> =
        (studyNotesState.value as? UiState.Success)?.data ?: emptyList()

    private fun currentMessages(): List<ChatMessage> =
        (messagesState.value as? UiState.Success)?.data ?: emptyList()

    fun selectMode(mode: JarvisMode) {
        viewModelScope.launch {
            repository.updateProfile(currentUserId, activeMode = mode).onFailure {
                _snackbarMessage.tryEmit("मोड बदलता आला नाही.")
            }
        }
    }

    fun selectLanguage(language: AiLanguage) {
        viewModelScope.launch {
            repository.updateProfile(currentUserId, preferredLanguage = language)
                .onSuccess {
                    _snackbarMessage.tryEmit("AI भाषा: ${language.nativeName} (${language.englishName})")
                }
                .onFailure {
                    _snackbarMessage.tryEmit("भाषा बदलता आली नाही.")
                }
        }
    }

    fun updateSettings(
        preferredTitle: String,
        autoSpeakEnabled: Boolean,
        speechRate: Double,
        preferredLanguage: AiLanguage? = null
    ) {
        viewModelScope.launch {
            repository.updateProfile(
                userId = currentUserId,
                preferredTitle = preferredTitle,
                autoSpeakEnabled = autoSpeakEnabled,
                speechRate = speechRate,
                preferredLanguage = preferredLanguage
            ).onSuccess {
                _snackbarMessage.tryEmit("सेटिंग्ज सेव्ह केल्या आहेत, बॉस.")
            }.onFailure {
                _snackbarMessage.tryEmit(it.localizedMessage ?: "सेटिंग्ज सेव्ह करता आल्या नाहीत.")
            }
        }
    }

    fun sendMessage(userText: String, modeOverride: JarvisMode? = null) {
        val cleanText = userText.trim()
        if (cleanText.isEmpty()) return

        val profile = currentProfile()
        val activeMode = modeOverride ?: profile.modeEnum()
        val activeLang = profile.languageEnum()
        val historySnapshot = currentMessages()

        viewModelScope.launch {
            if (modeOverride != null && modeOverride != profile.modeEnum()) {
                repository.updateProfile(currentUserId, activeMode = modeOverride)
            }
            repository.addMessage(currentUserId, MessageSender.USER, activeMode, cleanText)
                .onFailure {
                    _snackbarMessage.tryEmit("मेसेज सेव्ह करता आला नाही.")
                }

            _aiState.value = AiResponseState.Loading
            var finalReply = ""
            try {
                aiService.streamJarvisReply(
                    userMessage = cleanText,
                    history = historySnapshot,
                    preferredTitle = profile.preferredTitle,
                    mode = activeMode,
                    language = activeLang,
                    memories = currentMemories(),
                    housekeeping = currentHousekeeping(),
                    studyNotes = currentStudyNotes()
                ).collect { partial ->
                    finalReply = partial
                    _aiState.value = AiResponseState.Streaming(partial)
                }

                if (finalReply.isNotBlank()) {
                    repository.addMessage(currentUserId, MessageSender.JARVIS, activeMode, finalReply)
                    _aiState.value = AiResponseState.Idle
                    if (profile.autoSpeakEnabled) {
                        _speakQueue.tryEmit(SpeakPayload(finalReply, activeLang))
                    }
                } else {
                    _aiState.value = AiResponseState.Error("JARVIS कडून उत्तर रिकामे आले. पुन्हा प्रयत्न करा.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "JARVIS reply timed out", e)
                _aiState.value = AiResponseState.Error("प्रतिसादाला वेळ लागला, बॉस. पुन्हा प्रयत्न करा.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "JARVIS reply failed", e)
                _aiState.value = AiResponseState.Error(e.localizedMessage ?: "AI प्रतिसादात अडचण आली.")
            }
        }
    }

    fun generateDailyBriefing() {
        val profile = currentProfile()
        val activeLang = profile.languageEnum()
        viewModelScope.launch {
            _briefingState.value = AiResponseState.Loading
            _briefingText.value = ""
            var finalText = ""
            try {
                aiService.generateDailyBriefing(
                    preferredTitle = profile.preferredTitle,
                    language = activeLang,
                    housekeeping = currentHousekeeping(),
                    memories = currentMemories()
                ).collect { partial ->
                    finalText = partial
                    _briefingText.value = partial
                    _briefingState.value = AiResponseState.Streaming(partial)
                }
                if (finalText.isNotBlank()) {
                    _briefingState.value = AiResponseState.Idle
                    if (profile.autoSpeakEnabled) {
                        _speakQueue.tryEmit(SpeakPayload(finalText, activeLang))
                    }
                } else {
                    _briefingState.value = AiResponseState.Error("ब्रीफिंग तयार करता आले नाही.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Daily briefing timed out", e)
                _briefingState.value = AiResponseState.Error("ब्रीफिंगला वेळ लागला. पुन्हा प्रयत्न करा.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Daily briefing failed", e)
                _briefingState.value = AiResponseState.Error(e.localizedMessage ?: "ब्रीफिंग अयशस्वी झाले.")
            }
        }
    }

    fun analyzeSuspiciousText(input: String) {
        val clean = input.trim()
        if (clean.isEmpty()) return
        val profile = currentProfile()
        val activeLang = profile.languageEnum()
        viewModelScope.launch {
            _securityAnalysisState.value = AiResponseState.Loading
            _securityAnalysisText.value = ""
            var finalText = ""
            try {
                aiService.analyzeSuspiciousMessage(
                    preferredTitle = profile.preferredTitle,
                    language = activeLang,
                    suspiciousText = clean
                ).collect { partial ->
                    finalText = partial
                    _securityAnalysisText.value = partial
                    _securityAnalysisState.value = AiResponseState.Streaming(partial)
                }
                if (finalText.isNotBlank()) {
                    _securityAnalysisState.value = AiResponseState.Idle
                    if (profile.autoSpeakEnabled) {
                        _speakQueue.tryEmit(SpeakPayload(finalText, activeLang))
                    }
                } else {
                    _securityAnalysisState.value = AiResponseState.Error("तपासणी करता आली नाही.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Security analysis timed out", e)
                _securityAnalysisState.value = AiResponseState.Error("वेळ संपली, पुन्हा तपासा.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Security analysis failed", e)
                _securityAnalysisState.value = AiResponseState.Error(e.localizedMessage ?: "विश्लेषण अयशस्वी झाले.")
            }
        }
    }

    fun generateStudyDraft(subject: String, topic: String, noteType: StudyNoteType) {
        val cleanSubject = subject.trim()
        val cleanTopic = topic.trim()
        if (cleanSubject.isEmpty() || cleanTopic.isEmpty()) return
        val profile = currentProfile()
        val activeLang = profile.languageEnum()
        viewModelScope.launch {
            _studyDraftState.value = AiResponseState.Loading
            _studyDraftText.value = ""
            var finalText = ""
            try {
                aiService.generateStudyArtifact(
                    preferredTitle = profile.preferredTitle,
                    language = activeLang,
                    subject = cleanSubject,
                    topicOrGoal = cleanTopic,
                    artifactKind = noteType.labelMarathi
                ).collect { partial ->
                    finalText = partial
                    _studyDraftText.value = partial
                    _studyDraftState.value = AiResponseState.Streaming(partial)
                }
                if (finalText.isNotBlank()) {
                    _studyDraftState.value = AiResponseState.Idle
                    repository.addStudyNote(
                        userId = currentUserId,
                        subject = cleanSubject,
                        title = cleanTopic,
                        content = finalText,
                        noteType = noteType
                    ).onSuccess {
                        _snackbarMessage.tryEmit("स्टडी नोट सेव्ह केली आहे, बॉस.")
                    }
                } else {
                    _studyDraftState.value = AiResponseState.Error("नोट तयार करता आली नाही.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Study draft timed out", e)
                _studyDraftState.value = AiResponseState.Error("वेळ संपली, पुन्हा प्रयत्न करा.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Study draft failed", e)
                _studyDraftState.value = AiResponseState.Error(e.localizedMessage ?: "स्टडी नोट अयशस्वी.")
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearMessages(currentUserId)
                .onSuccess { _snackbarMessage.tryEmit("चॅट इतिहास साफ केला आहे.") }
                .onFailure { _snackbarMessage.tryEmit("चॅट साफ करता आला नाही.") }
        }
    }

    // --- Housekeeping & Dashboard CRUD ---
    fun addHousekeepingItem(
        title: String,
        notes: String,
        category: HousekeepingCategory,
        domain: DashboardDomain = DashboardDomain.HOUSEKEEPING,
        scheduleLabel: String,
        priority: PriorityLevel
    ) {
        viewModelScope.launch {
            repository.addHousekeepingItem(currentUserId, title, notes, category, domain, scheduleLabel, priority)
                .onSuccess { _snackbarMessage.tryEmit("डॅशबोर्डवर जोडले आहे, बॉस.") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "काम जोडता आले नाही.") }
        }
    }

    fun addTaskOrBillFromVoice(
        transcript: String,
        defaultDomain: DashboardDomain = DashboardDomain.HOUSEKEEPING
    ) {
        val cleanTranscript = transcript.trim()
        if (cleanTranscript.isEmpty()) return
        val profile = currentProfile()
        val activeLang = profile.languageEnum()

        viewModelScope.launch {
            _isParsingVoiceTask.value = true
            try {
                val parsed = aiService.parseVoiceTaskOrBill(
                    transcript = cleanTranscript,
                    preferredTitle = profile.preferredTitle,
                    language = activeLang,
                    defaultDomain = defaultDomain
                )
                repository.addHousekeepingItem(
                    userId = currentUserId,
                    title = parsed.title,
                    notes = parsed.notes,
                    category = parsed.category,
                    domain = parsed.domain,
                    scheduleLabel = parsed.scheduleLabel,
                    priority = parsed.priority
                ).onSuccess {
                    _snackbarMessage.tryEmit(
                        "${parsed.domain.labelEnglish} • ${parsed.category.shortEnglish}: ${parsed.title}"
                    )
                    if (profile.autoSpeakEnabled) {
                        _speakQueue.tryEmit(SpeakPayload(parsed.confirmationMessage, activeLang))
                    }
                }.onFailure {
                    _snackbarMessage.tryEmit(it.localizedMessage ?: "व्हॉइस काम सेव्ह करता आले नाही.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice command task creation failed", e)
                _snackbarMessage.tryEmit("व्हॉइस कमांड प्रोसेस करता आली नाही, बॉस.")
            } finally {
                _isParsingVoiceTask.value = false
            }
        }
    }

    fun toggleHousekeepingItem(item: HousekeepingItem) {
        viewModelScope.launch {
            repository.toggleHousekeepingCompleted(currentUserId, item.id, !item.completed)
                .onFailure { _snackbarMessage.tryEmit("स्थिती बदलता आली नाही.") }
        }
    }

    fun updateHousekeepingPriority(item: HousekeepingItem, priority: PriorityLevel) {
        if (item.priorityEnum() == priority) return
        viewModelScope.launch {
            repository.updateHousekeepingPriority(currentUserId, item.id, priority)
                .onSuccess {
                    _snackbarMessage.tryEmit("प्राधान्य अपडेट केले: ${priority.labelEnglish} (${priority.labelMarathi})")
                }
                .onFailure { _snackbarMessage.tryEmit("प्राधान्य बदलता आले नाही.") }
        }
    }

    fun deleteHousekeepingItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteHousekeepingItem(currentUserId, itemId)
                .onFailure { _snackbarMessage.tryEmit("काढता आले नाही.") }
        }
    }

    // --- Study Notes CRUD ---
    fun addManualStudyNote(subject: String, title: String, content: String, noteType: StudyNoteType) {
        viewModelScope.launch {
            repository.addStudyNote(currentUserId, subject, title, content, noteType)
                .onSuccess { _snackbarMessage.tryEmit("नोट सेव्ह झाली.") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "नोट सेव्ह करता आली नाही.") }
        }
    }

    fun deleteStudyNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteStudyNote(currentUserId, noteId)
                .onFailure { _snackbarMessage.tryEmit("नोट काढता आली नाही.") }
        }
    }

    // --- Memories CRUD ---
    fun addMemory(topic: String, detail: String, category: MemoryCategory) {
        viewModelScope.launch {
            repository.addMemory(currentUserId, topic, detail, category)
                .onSuccess { _snackbarMessage.tryEmit("आठवण सेव्ह केली आहे, बॉस.") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "आठवण सेव्ह करता आली नाही.") }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            repository.deleteMemory(currentUserId, memoryId)
                .onFailure { _snackbarMessage.tryEmit("आठवण काढता आली नाही.") }
        }
    }

    // --- Security Checklist ---
    fun toggleSecurityCheck(check: SecurityCheck) {
        viewModelScope.launch {
            repository.toggleSecurityCheck(currentUserId, check.id, !check.completed)
                .onFailure { _snackbarMessage.tryEmit("चेकलिस्ट अपडेट करता आली नाही.") }
        }
    }

    fun addSecurityCheck(title: String, guidance: String, category: SecurityCategory) {
        viewModelScope.launch {
            repository.addSecurityCheck(currentUserId, title, guidance, category)
                .onSuccess { _snackbarMessage.tryEmit("सुरक्षा नियम जोडला आहे.") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "जोडता आले नाही.") }
        }
    }

    private companion object {
        const val TAG = "JarvisVM"
    }
}

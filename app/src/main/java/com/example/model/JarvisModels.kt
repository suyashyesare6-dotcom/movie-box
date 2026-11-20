package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class JarvisMode(val labelMarathi: String, val subtitleMarathi: String) {
    GENERAL("बॉस मोड", "शांत, हुशार आणि टोमणेबाज सहाय्यक"),
    GOSSIP("गप्पा मोड", "मित्रांसारख्या गप्पा आणि आठवणी"),
    STUDY("स्टडी मोड", "सोप्या मराठीत अभ्यास, हिंट्स आणि क्विझ"),
    SECURITY("सिक्युरिटी", "सायबर सुरक्षा, स्कॅम तपासणी आणि एन्क्रिप्शन");

    companion object {
        fun fromRaw(raw: String?): JarvisMode =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

enum class AiLanguage(
    val localeTag: String,
    val nativeName: String,
    val englishName: String,
    val speechRecognizerTag: String
) {
    AUTO("auto", "Auto", "Auto-Detect", "en-US"),
    MARATHI("mr-IN", "मराठी", "Marathi", "mr-IN"),
    ENGLISH("en-US", "English", "English", "en-US"),
    HINDI("hi-IN", "हिन्दी", "Hindi", "hi-IN"),
    SPANISH("es-ES", "Español", "Spanish", "es-ES"),
    FRENCH("fr-FR", "Français", "French", "fr-FR"),
    GERMAN("de-DE", "Deutsch", "German", "de-DE"),
    JAPANESE("ja-JP", "日本語", "Japanese", "ja-JP");

    companion object {
        fun fromRaw(raw: String?): AiLanguage =
            entries.firstOrNull { it.name == raw } ?: MARATHI
    }
}

enum class MemoryCategory(val labelMarathi: String) {
    INTEREST("आवड"),
    CONVERSATION("गप्पा आठवण"),
    PREFERENCE("पसंती"),
    GOAL("ध्येय");

    companion object {
        fun fromRaw(raw: String?): MemoryCategory =
            entries.firstOrNull { it.name == raw } ?: INTEREST
    }
}

enum class DashboardDomain(val labelEnglish: String, val labelMarathi: String) {
    HOUSEKEEPING("Housekeeping", "हाऊसकीपिंग"),
    STUDY("Study", "अभ्यास"),
    SECURITY("Security", "सुरक्षा");

    companion object {
        fun fromRaw(raw: String?): DashboardDomain =
            entries.firstOrNull { it.name == raw } ?: HOUSEKEEPING
    }
}

enum class HousekeepingCategory(val labelMarathi: String, val shortEnglish: String) {
    TASK("काम / रिमाइंडर", "Task"),
    SHOPPING("खरेदी यादी", "Shopping"),
    BILL("बिल / वेळापत्रक", "Bill"),
    ROUTINE("स्वच्छता / रूटीन", "Routine");

    companion object {
        fun fromRaw(raw: String?): HousekeepingCategory =
            entries.firstOrNull { it.name == raw } ?: TASK
    }
}

enum class PriorityLevel(
    val labelMarathi: String,
    val labelEnglish: String,
    val rank: Int
) {
    LOW("कमी", "Low", 1),
    MEDIUM("मध्यम", "Medium", 2),
    HIGH("महत्त्वाचे", "High", 3);

    companion object {
        fun fromRaw(raw: String?): PriorityLevel =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class DashboardSortMode(val labelEnglish: String, val labelMarathi: String) {
    CATEGORY_GROUPED("By Category", "विभागानुसार"),
    IMPORTANCE_HIGH_FIRST("High → Low", "जास्त → कमी"),
    IMPORTANCE_LOW_FIRST("Low → High", "कमी → जास्त"),
    NEWEST_FIRST("Newest", "नवीन प्रथम")
}

enum class StudyNoteType(val labelMarathi: String) {
    SUMMARY("सारांश"),
    TIMETABLE("वेळापत्रक"),
    REVISION("उजळणी"),
    QUIZ_LOG("क्विझ");

    companion object {
        fun fromRaw(raw: String?): StudyNoteType =
            entries.firstOrNull { it.name == raw } ?: SUMMARY
    }
}

enum class SecurityCategory(val labelMarathi: String) {
    AUTH_2FA("2FA सुरक्षा"),
    PASSWORDS("पासवर्ड मॅनेजर"),
    UPDATES("सॉफ्टवेअर अपडेट्स"),
    BACKUPS("बॅकअप"),
    NETWORK("नेटवर्क व डिव्हाइस");

    companion object {
        fun fromRaw(raw: String?): SecurityCategory =
            entries.firstOrNull { it.name == raw } ?: AUTH_2FA
    }
}

enum class MessageSender {
    USER,
    JARVIS;

    companion object {
        fun fromRaw(raw: String?): MessageSender =
            entries.firstOrNull { it.name == raw } ?: JARVIS
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val preferredTitle: String = "Boss",
    val autoSpeakEnabled: Boolean = true,
    val speechRate: Double = 1.0,
    val activeMode: String = JarvisMode.GENERAL.name,
    val preferredLanguage: String = AiLanguage.MARATHI.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun modeEnum(): JarvisMode = JarvisMode.fromRaw(activeMode)
    fun languageEnum(): AiLanguage = AiLanguage.fromRaw(preferredLanguage)
}

@Keep
@IgnoreExtraProperties
data class MemoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val topic: String = "",
    val detail: String = "",
    val category: String = MemoryCategory.INTEREST.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): MemoryCategory = MemoryCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class HousekeepingItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String = "",
    val category: String = HousekeepingCategory.TASK.name,
    val domain: String = DashboardDomain.HOUSEKEEPING.name,
    val scheduleLabel: String = "",
    val priority: String = PriorityLevel.MEDIUM.name,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): HousekeepingCategory = HousekeepingCategory.fromRaw(category)
    fun domainEnum(): DashboardDomain = DashboardDomain.fromRaw(domain)
    fun priorityEnum(): PriorityLevel = PriorityLevel.fromRaw(priority)
}

@Keep
@IgnoreExtraProperties
data class StudyNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val title: String = "",
    val content: String = "",
    val noteType: String = StudyNoteType.SUMMARY.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun noteTypeEnum(): StudyNoteType = StudyNoteType.fromRaw(noteType)
}

@Keep
@IgnoreExtraProperties
data class SecurityCheck(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val guidance: String = "",
    val category: String = SecurityCategory.AUTH_2FA.name,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): SecurityCategory = SecurityCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = MessageSender.JARVIS.name,
    val mode: String = JarvisMode.GENERAL.name,
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun senderEnum(): MessageSender = MessageSender.fromRaw(sender)
    fun modeEnum(): JarvisMode = JarvisMode.fromRaw(mode)
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiResponseState {
    data object Idle : AiResponseState
    data object Loading : AiResponseState
    data class Streaming(val partialText: String) : AiResponseState
    data class Error(val message: String) : AiResponseState
}

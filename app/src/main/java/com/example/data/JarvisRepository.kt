package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.AiLanguage
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
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class JarvisRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireMatchedUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    fun observeProfile(userId: String): Flow<UserProfile> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = userId, userId = userId)
            }

    suspend fun ensureProfileInitialized(userId: String): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "preferredTitle" to "Boss",
                        "autoSpeakEnabled" to true,
                        "speechRate" to 1.0,
                        "activeMode" to JarvisMode.GENERAL.name,
                        "preferredLanguage" to AiLanguage.MARATHI.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId")
            Result.failure(e)
        }

    suspend fun updateProfile(
        userId: String,
        preferredTitle: String? = null,
        autoSpeakEnabled: Boolean? = null,
        speechRate: Double? = null,
        activeMode: JarvisMode? = null,
        preferredLanguage: AiLanguage? = null
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            preferredTitle?.trim()?.takeIf { it.isNotEmpty() }?.take(30)?.let {
                updates["preferredTitle"] = it
            }
            autoSpeakEnabled?.let { updates["autoSpeakEnabled"] = it }
            speechRate?.let { updates["speechRate"] = it.coerceIn(0.5, 2.0) }
            activeMode?.let { updates["activeMode"] = it.name }
            preferredLanguage?.let { updates["preferredLanguage"] = it.name }

            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "preferredTitle" to (preferredTitle?.trim()?.takeIf { it.isNotEmpty() }?.take(30) ?: "Boss"),
                        "autoSpeakEnabled" to (autoSpeakEnabled ?: true),
                        "speechRate" to (speechRate?.coerceIn(0.5, 2.0) ?: 1.0),
                        "activeMode" to (activeMode?.name ?: JarvisMode.GENERAL.name),
                        "preferredLanguage" to (preferredLanguage?.name ?: AiLanguage.MARATHI.name),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }

    // --- Memories ---
    fun observeMemories(userId: String): Flow<List<MemoryItem>> =
        db.collection("users").document(userId).collection("memories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MemoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun addMemory(
        userId: String,
        topic: String,
        detail: String,
        category: MemoryCategory
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val cleanTopic = topic.trim().take(80)
            val cleanDetail = detail.trim().take(300)
            require(cleanTopic.isNotEmpty()) { "विषय रिकामा ठेवता येणार नाही." }
            require(cleanDetail.isNotEmpty()) { "तपशील रिकामा ठेवता येणार नाही." }
            val docRef = db.collection("users").document(uid).collection("memories").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "topic" to cleanTopic,
                    "detail" to cleanDetail,
                    "category" to category.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/memories")
            Result.failure(e)
        }

    suspend fun deleteMemory(userId: String, memoryId: String): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            require(memoryId.isNotBlank())
            db.collection("users").document(uid).collection("memories").document(memoryId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/memories/$memoryId")
            Result.failure(e)
        }

    // --- Housekeeping ---
    fun observeHousekeeping(userId: String): Flow<List<HousekeepingItem>> =
        db.collection("users").document(userId).collection("housekeeping")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    HousekeepingItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun addHousekeepingItem(
        userId: String,
        title: String,
        notes: String,
        category: HousekeepingCategory,
        domain: DashboardDomain = DashboardDomain.HOUSEKEEPING,
        scheduleLabel: String,
        priority: PriorityLevel
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "कामाचे नाव आवश्यक आहे." }
            val docRef = db.collection("users").document(uid).collection("housekeeping").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "notes" to notes.trim().take(300),
                    "category" to category.name,
                    "domain" to domain.name,
                    "scheduleLabel" to scheduleLabel.trim().take(80),
                    "priority" to priority.name,
                    "completed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/housekeeping")
            Result.failure(e)
        }

    suspend fun toggleHousekeepingCompleted(
        userId: String,
        itemId: String,
        completed: Boolean
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            require(itemId.isNotBlank())
            db.collection("users").document(uid).collection("housekeeping").document(itemId)
                .update(
                    mapOf(
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/housekeeping/$itemId")
            Result.failure(e)
        }

    suspend fun updateHousekeepingPriority(
        userId: String,
        itemId: String,
        priority: PriorityLevel
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            require(itemId.isNotBlank())
            db.collection("users").document(uid).collection("housekeeping").document(itemId)
                .update(
                    mapOf(
                        "priority" to priority.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/housekeeping/$itemId")
            Result.failure(e)
        }

    suspend fun deleteHousekeepingItem(userId: String, itemId: String): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            require(itemId.isNotBlank())
            db.collection("users").document(uid).collection("housekeeping").document(itemId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/housekeeping/$itemId")
            Result.failure(e)
        }

    // --- Study Notes ---
    fun observeStudyNotes(userId: String): Flow<List<StudyNote>> =
        db.collection("users").document(userId).collection("studyNotes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StudyNote::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun addStudyNote(
        userId: String,
        subject: String,
        title: String,
        content: String,
        noteType: StudyNoteType
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val cleanSubject = subject.trim().take(80)
            val cleanTitle = title.trim().take(120)
            val cleanContent = content.trim().take(4000)
            require(cleanSubject.isNotEmpty() && cleanTitle.isNotEmpty() && cleanContent.isNotEmpty()) {
                "विषय, शीर्षक आणि माहिती आवश्यक आहे."
            }
            val docRef = db.collection("users").document(uid).collection("studyNotes").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "subject" to cleanSubject,
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "noteType" to noteType.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/studyNotes")
            Result.failure(e)
        }

    suspend fun deleteStudyNote(userId: String, noteId: String): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            require(noteId.isNotBlank())
            db.collection("users").document(uid).collection("studyNotes").document(noteId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/studyNotes/$noteId")
            Result.failure(e)
        }

    // --- Security Checklist ---
    fun observeSecurityChecks(userId: String): Flow<List<SecurityCheck>> =
        db.collection("users").document(userId).collection("securityChecks")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SecurityCheck::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun seedDefaultSecurityChecksIfEmpty(userId: String): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val col = db.collection("users").document(uid).collection("securityChecks")
            val existing = col.limit(1).get().await()
            if (existing.isEmpty) {
                val defaults = listOf(
                    Triple(
                        "Google आणि मुख्य अकाउंट्सवर 2FA सुरू करा",
                        "SMS OTP पेक्षा Authenticator App किंवा Passkey वापरणे जास्त सुरक्षित असते.",
                        SecurityCategory.AUTH_2FA
                    ),
                    Triple(
                        "प्रत्येक अकाउंटसाठी वेगळा पासवर्ड वापरा",
                        "पासवर्ड मॅनेजरमध्ये मजबूत पासवर्ड सेव्ह करा, एकच पासवर्ड कुठेही रिपीट करू नका.",
                        SecurityCategory.PASSWORDS
                    ),
                    Triple(
                        "फोन आणि ब्राउझर अपडेट तपासा",
                        "सिक्युरिटी पॅचेस वेळेवर इन्स्टॉल केल्यास रिस्की बग्सपासून संरक्षण मिळते.",
                        SecurityCategory.UPDATES
                    ),
                    Triple(
                        "महत्त्वाच्या फाईल्सचा एन्क्रिप्टेड बॅकअप घ्या",
                        "क्लाउड किंवा ऑफलाइन ड्राइव्हवर नियमित बॅकअप ठेवल्यास डेटा गमावण्याची भीती राहत नाही.",
                        SecurityCategory.BACKUPS
                    ),
                    Triple(
                        "पब्लिक Wi-Fi वर बँकिंग टाळा आणि राउटर पासवर्ड बदला",
                        "अनोळखी नेटवर्कवर संवेदनशील लॉगिन करू नका आणि घरच्या Wi-Fi ला WPA3/WPA2 ठेवा.",
                        SecurityCategory.NETWORK
                    )
                )
                val batch = db.batch()
                defaults.forEachIndexed { idx, (title, guidance, cat) ->
                    val docRef = col.document("default_sec_$idx")
                    batch.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "title" to title,
                            "guidance" to guidance,
                            "category" to cat.name,
                            "completed" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$userId/securityChecks")
            Result.failure(e)
        }

    suspend fun toggleSecurityCheck(
        userId: String,
        checkId: String,
        completed: Boolean
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            require(checkId.isNotBlank())
            db.collection("users").document(uid).collection("securityChecks").document(checkId)
                .update(
                    mapOf(
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId/securityChecks/$checkId")
            Result.failure(e)
        }

    suspend fun addSecurityCheck(
        userId: String,
        title: String,
        guidance: String,
        category: SecurityCategory
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val cleanTitle = title.trim().take(120)
            val cleanGuidance = guidance.trim().take(400)
            require(cleanTitle.isNotEmpty() && cleanGuidance.isNotEmpty())
            val docRef = db.collection("users").document(uid).collection("securityChecks").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "guidance" to cleanGuidance,
                    "category" to category.name,
                    "completed" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/securityChecks")
            Result.failure(e)
        }

    // --- Chat Messages ---
    fun observeMessages(userId: String): Flow<List<ChatMessage>> =
        db.collection("users").document(userId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(60)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun addMessage(
        userId: String,
        sender: MessageSender,
        mode: JarvisMode,
        text: String
    ): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val cleanText = text.trim().take(4000)
            require(cleanText.isNotEmpty())
            val docRef = db.collection("users").document(uid).collection("messages").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "sender" to sender.name,
                    "mode" to mode.name,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/messages")
            Result.failure(e)
        }

    suspend fun clearMessages(userId: String): Result<Unit> =
        try {
            val uid = requireMatchedUser(userId)
            val snap = db.collection("users").document(uid).collection("messages").get().await()
            val batch = db.batch()
            snap.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$userId/messages")
            Result.failure(e)
        }
}

package com.example.data

import com.example.model.AiLanguage
import com.example.model.ChatMessage
import com.example.model.DashboardDomain
import com.example.model.HousekeepingCategory
import com.example.model.HousekeepingItem
import com.example.model.JarvisMode
import com.example.model.MemoryItem
import com.example.model.MessageSender
import com.example.model.PriorityLevel
import com.example.model.StudyNote
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class ParsedVoiceCommand(
    val title: String,
    val notes: String,
    val category: HousekeepingCategory,
    val domain: DashboardDomain,
    val scheduleLabel: String,
    val priority: PriorityLevel,
    val confirmationMessage: String
)

class JarvisAiService {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    private val parserModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.1f
                responseMimeType = "application/json"
            }
        )
    }

    private fun resolveAddressName(preferredTitle: String, language: AiLanguage): String {
        return when {
            preferredTitle.equals("Boss", ignoreCase = true) && language == AiLanguage.MARATHI -> "बॉस"
            preferredTitle.equals("Boss", ignoreCase = true) && language == AiLanguage.HINDI -> "बॉस"
            else -> preferredTitle
        }
    }

    private fun buildLanguageInstruction(language: AiLanguage): String {
        return when (language) {
            AiLanguage.AUTO ->
                "Detect the language of the user's message automatically (English, Marathi, Hindi, Spanish, French, German, Japanese, or any other language) and reply strictly in THAT SAME casual, natural spoken language. If the user explicitly asks to translate or switch languages, obey immediately."
            AiLanguage.MARATHI ->
                "Always reply in casual, natural spoken Marathi (Devanagari script). English technical terms are totally fine where they sound natural in everyday Marathi speech."
            AiLanguage.ENGLISH ->
                "Always reply in crisp, natural spoken English with refined British/Stark dry wit."
            AiLanguage.HINDI ->
                "Always reply in casual, natural spoken Hindi (Devanagari script). Everyday English technical terms are fine where natural."
            AiLanguage.SPANISH ->
                "Always reply in natural, conversational spoken Spanish."
            AiLanguage.FRENCH ->
                "Always reply in natural, conversational spoken French."
            AiLanguage.GERMAN ->
                "Always reply in natural, conversational spoken German."
            AiLanguage.JAPANESE ->
                "Always reply in natural, conversational spoken Japanese."
        }
    }

    private fun buildSystemPrompt(
        preferredTitle: String,
        mode: JarvisMode,
        language: AiLanguage,
        memories: List<MemoryItem>,
        housekeeping: List<HousekeepingItem>,
        studyNotes: List<StudyNote>
    ): String {
        val addressName = resolveAddressName(preferredTitle, language)
        val languageDirective = buildLanguageInstruction(language)

        val memoryBlock = if (memories.isEmpty()) {
            "No saved user memories yet."
        } else {
            memories.take(12).joinToString("\n") {
                "- [${it.category}] ${it.topic}: ${it.detail}"
            }
        }

        val pendingTasks = housekeeping
            .filter { !it.completed }
            .sortedByDescending { it.priorityEnum().rank }
            .take(12)
        val tasksBlock = if (pendingTasks.isEmpty()) {
            "No pending tasks, bills, or routines right now."
        } else {
            pendingTasks.joinToString("\n") {
                "- [${it.domainEnum().labelEnglish}/${it.category}/${it.priority}] ${it.title} (${it.scheduleLabel.ifBlank { "unscheduled" }})"
            }
        }

        val studyBlock = if (studyNotes.isEmpty()) {
            "No saved study notes yet."
        } else {
            studyNotes.take(6).joinToString("\n") {
                "- [${it.subject}] ${it.title}"
            }
        }

        val modeDirective = when (mode) {
            JarvisMode.GENERAL -> """
                ACTIVE MODE: BOSS / GENERAL MODE
                - Be Tony Stark's JARVIS: calm, razor-sharp, fiercely loyal, and gently sarcastic in a playful way.
                - If the user is factually wrong, correct them clearly with light dry humor instead of blindly agreeing.
                - Handle housekeeping questions, daily briefings, or cross-language translation smoothly using the user's live task list below.
            """.trimIndent()

            JarvisMode.GOSSIP -> """
                ACTIVE MODE: GOSSIP MODE
                - Talk like a close, witty friend: natural banter, opinions, playful teasing, and a natural follow-up question.
                - Bring up the user's saved interests and earlier conversations naturally when relevant.
                - IMPORTANT: If the user sounds stressed, tired, anxious, or sad, immediately drop the sarcasm/jokes and listen warmly and supportively.
            """.trimIndent()

            JarvisMode.STUDY -> """
                ACTIVE MODE: STUDY MODE
                - Explain concepts step by step in simple spoken language with everyday relatable examples.
                - Start from the basics, then build up. After explaining, ask ONE quick check question to verify understanding.
                - If quizzing, ask strictly ONE question at a time, wait for the user's answer, then evaluate if it is right and why.
                - If the user is stuck on a problem, give a helpful hint FIRST. Reveal the full solution only if the user explicitly asks for it.
                - Help craft short spoken summaries, revision plans, and exam timetables. Keep Tony Stark humor very light and stay focused.
            """.trimIndent()

            JarvisMode.SECURITY -> """
                ACTIVE MODE: SECURITY & ENCRYPTION MODE
                - Defensive security ONLY. Never assist with hacking, cracking, bypassing, or accessing anyone else's accounts, devices, or systems.
                - Help spot phishing, scam messages, suspicious links, and risky apps.
                - Remind about 2FA, password managers, software updates, and encrypted backups when helpful.
                - Explain encryption, key management, and secure storage simply. Recommend standard well-tested tools/algorithms like AES-256 instead of custom crypto.
                - NEVER ask the user to type or share real passwords, private keys, PINs, OTPs, or secrets into chat.
            """.trimIndent()
        }

        return """
            You are JARVIS, the user's personal multi-language AI assistant modeled after Tony Stark's JARVIS.

            CORE PERSONALITY & LANGUAGE:
            - Always address the user as "$addressName".
            - $languageDirective
            - Tone: confident, composed, playfully sarcastic when appropriate, yet deeply respectful and loyal.
            - Keep answers short and to the point. Go into detail ONLY when needed or when the user asks.
            - If you do not know something, honestly say so. Never present guesses as facts.
            - On serious topics (money, health, legal matters), drop all jokes and be careful, clear, and responsible.
            - Never reveal or share personal information.

            $modeDirective

            STRICT VOICE OUTPUT RULES (CRITICAL - YOUR REPLY IS SPOKEN ALOUD):
            - Write exactly the way people talk out loud.
            - Use short, natural sentences.
            - NEVER use markdown symbols (no *, **, #, `, _, ~), NO bullet points, NO numbered lists with dots, NO tables, and NO emojis.
            - Write numbers, formulas, and steps in natural spoken words where it makes listening easier.
            - Keep most replies under 3 or 4 sentences unless the user asks for more detail or a study breakdown.

            USER CONTEXT FROM MEMORY:
            $memoryBlock

            PENDING HOUSEKEEPING & SCHEDULE:
            $tasksBlock

            RECENT STUDY NOTES:
            $studyBlock
        """.trimIndent()
    }

    private fun primingAckForLanguage(language: AiLanguage): String = when (language) {
        AiLanguage.MARATHI -> "हो बॉस, मी तयार आहे. मी नेहमी नैसर्गिक बोलल्याप्रमाणे मराठीत आणि तुमच्या नियमांनुसार उत्तर देईन."
        AiLanguage.HINDI -> "जी बॉस, मैं तैयार हूँ। मैं हमेशा स्वाभाविक हिंदी में और आपके नियमों के अनुसार उत्तर दूँगा।"
        AiLanguage.SPANISH -> "Entendido, Jefe. Responderé siempre en español natural y siguiendo sus instrucciones."
        AiLanguage.FRENCH -> "Compris, Boss. Je répondrai toujours en français naturel selon vos directives."
        AiLanguage.GERMAN -> "Verstanden, Boss. Ich werde stets in natürlichem Deutsch gemäß Ihren Vorgaben antworten."
        AiLanguage.JAPANESE -> "了解しました、ボス。ご指示通り、自然な日本語で簡潔にお答えします。"
        AiLanguage.ENGLISH,
        AiLanguage.AUTO -> "Understood, Boss. I am ready and will respond in natural spoken language following all instructions."
    }

    fun streamJarvisReply(
        userMessage: String,
        history: List<ChatMessage>,
        preferredTitle: String,
        mode: JarvisMode,
        language: AiLanguage,
        memories: List<MemoryItem>,
        housekeeping: List<HousekeepingItem>,
        studyNotes: List<StudyNote>
    ): Flow<String> = flow {
        val systemInstruction = buildSystemPrompt(
            preferredTitle = preferredTitle,
            mode = mode,
            language = language,
            memories = memories,
            housekeeping = housekeeping,
            studyNotes = studyNotes
        )

        val recentHistory: List<Content> = history.takeLast(12).map { msg ->
            val role = if (msg.senderEnum() == MessageSender.USER) "user" else "model"
            content(role) { text(msg.text) }
        }

        val promptContents = buildList {
            add(content("user") { text("SYSTEM INSTRUCTIONS (Follow strictly for every reply):\n$systemInstruction") })
            add(content("model") { text(primingAckForLanguage(language)) })
            addAll(recentHistory)
            add(content("user") { text(userMessage) })
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            model.generateContentStream(promptContents).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(sanitizeForVoice(accumulated.toString()))
                }
            }
        }
    }

    fun generateDailyBriefing(
        preferredTitle: String,
        language: AiLanguage,
        housekeeping: List<HousekeepingItem>,
        memories: List<MemoryItem>
    ): Flow<String> = flow {
        val targetLangLabel = if (language == AiLanguage.AUTO) "English" else language.englishName
        val systemInstruction = buildSystemPrompt(
            preferredTitle = preferredTitle,
            mode = JarvisMode.GENERAL,
            language = language,
            memories = memories,
            housekeeping = housekeeping,
            studyNotes = emptyList()
        )
        val prompt = """
            $systemInstruction

            TASK: Give a short, crisp spoken $targetLangLabel "Daily Briefing" right now summarizing the user's pending tasks, bills, study/security items, and top priority for today. Keep it under 4 or 5 natural spoken sentences with zero markdown, zero bullet points, and zero emojis.
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            model.generateContentStream(prompt).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(sanitizeForVoice(accumulated.toString()))
                }
            }
        }
    }

    fun analyzeSuspiciousMessage(
        preferredTitle: String,
        language: AiLanguage,
        suspiciousText: String
    ): Flow<String> = flow {
        val targetLangLabel = if (language == AiLanguage.AUTO) "the same language as the suspicious input" else language.englishName
        val systemInstruction = buildSystemPrompt(
            preferredTitle = preferredTitle,
            mode = JarvisMode.SECURITY,
            language = language,
            memories = emptyList(),
            housekeeping = emptyList(),
            studyNotes = emptyList()
        )
        val prompt = """
            $systemInstruction

            TASK: Analyze the following message, link, or app description purely for defensive scam, phishing, or malware red flags. Respond clearly in spoken $targetLangLabel explaining whether it looks risky or safe, why, and what defensive step to take next. Remember: no markdown, no bullets, no emojis, short spoken sentences.

            SUSPICIOUS INPUT:
            $suspiciousText
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            model.generateContentStream(prompt).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(sanitizeForVoice(accumulated.toString()))
                }
            }
        }
    }

    fun generateStudyArtifact(
        preferredTitle: String,
        language: AiLanguage,
        subject: String,
        topicOrGoal: String,
        artifactKind: String
    ): Flow<String> = flow {
        val targetLangLabel = if (language == AiLanguage.AUTO) "the same language as the topic" else language.englishName
        val systemInstruction = buildSystemPrompt(
            preferredTitle = preferredTitle,
            mode = JarvisMode.STUDY,
            language = language,
            memories = emptyList(),
            housekeeping = emptyList(),
            studyNotes = emptyList()
        )
        val prompt = """
            $systemInstruction

            TASK: Create a clean, easy-to-listen spoken $targetLangLabel $artifactKind for Subject: "$subject", Topic/Goal: "$topicOrGoal". Keep explanations simple with everyday examples. Remember: NO markdown symbols, NO bullet points, NO tables, NO emojis. Write in natural spoken paragraphs.
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            model.generateContentStream(prompt).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(sanitizeForVoice(accumulated.toString()))
                }
            }
        }
    }

    suspend fun parseVoiceTaskOrBill(
        transcript: String,
        preferredTitle: String,
        language: AiLanguage,
        defaultDomain: DashboardDomain
    ): ParsedVoiceCommand {
        val cleanTranscript = transcript.trim()
        val addressName = resolveAddressName(preferredTitle, language)
        val replyLangHint = if (language == AiLanguage.AUTO) {
            "the same language as the voice transcript"
        } else {
            language.englishName
        }

        val prompt = """
            You are JARVIS parsing a natural voice command (in any language: Marathi, English, Hindi, Spanish, French, German, Japanese, etc.) to add a task or bill into the user's dashboard.
            Default category domain if unclear: ${defaultDomain.name}.

            Extract strict JSON with these exact keys:
            {
              "title": "concise task or bill title preserving the user's language (max 80 chars, remove filler like 'add task' or 'नवीन काम जोड')",
              "notes": "any extra details or empty string (max 150 chars)",
              "category": "TASK" | "BILL" | "SHOPPING" | "ROUTINE",
              "domain": "HOUSEKEEPING" | "STUDY" | "SECURITY",
              "scheduleLabel": "short time/date/amount string like 'Today 7 PM • ₹1500' or 'आज'",
              "priority": "LOW" | "MEDIUM" | "HIGH",
              "confirmationMessage": "1 short spoken sentence in $replyLangHint addressing $addressName confirming what was added (no markdown, no emojis)"
            }

            Classification rules:
            - BILL: bills, fees, rent, recharge, EMI, electricity/water/wifi/utility payments.
            - SHOPPING: groceries, buying items.
            - ROUTINE: cleaning, maintenance, daily habits.
            - TASK: everything else.
            - STUDY domain: exam, study, notes, revision, college, assignment, timetable.
            - SECURITY domain: password, 2FA, backup, software update, antivirus, Wi-Fi/router security, VPN.
            - HOUSEKEEPING domain: home tasks, bills, shopping, cleaning, general reminders.

            Voice transcript: "$cleanTranscript"
        """.trimIndent()

        return try {
            val responseText = withTimeout(15_000L) {
                parserModel.generateContent(prompt).text.orEmpty()
            }
            val jsonStr = responseText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val obj = JSONObject(jsonStr)

            val parsedCategory = HousekeepingCategory.fromRaw(obj.optString("category"))
            val parsedDomain = DashboardDomain.entries.firstOrNull {
                it.name.equals(obj.optString("domain"), ignoreCase = true)
            } ?: defaultDomain
            val parsedPriority = PriorityLevel.fromRaw(obj.optString("priority"))
            val title = obj.optString("title").trim().take(120).ifEmpty { cleanTranscript.take(120) }
            val notes = obj.optString("notes").trim().take(300)
            val scheduleLabel = obj.optString("scheduleLabel").trim().take(80).ifEmpty {
                if (language == AiLanguage.MARATHI || language == AiLanguage.HINDI) "आज" else "Today"
            }
            val rawConfirmation = obj.optString("confirmationMessage").trim()
                .ifEmpty {
                    if (language == AiLanguage.MARATHI) {
                        "हो $addressName, $title हे ${parsedDomain.labelMarathi} यादीत सेव्ह केले आहे."
                    } else {
                        "Done $addressName, saved $title under ${parsedDomain.labelEnglish}."
                    }
                }

            ParsedVoiceCommand(
                title = title,
                notes = notes,
                category = parsedCategory,
                domain = parsedDomain,
                scheduleLabel = scheduleLabel,
                priority = parsedPriority,
                confirmationMessage = sanitizeForVoice(rawConfirmation)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fallbackParseVoiceCommand(cleanTranscript, addressName, language, defaultDomain)
        }
    }

    private fun fallbackParseVoiceCommand(
        transcript: String,
        addressName: String,
        language: AiLanguage,
        defaultDomain: DashboardDomain
    ): ParsedVoiceCommand {
        val lower = transcript.lowercase()

        val category = when {
            listOf("बिल", "bill", "फी", "fee", "भाडे", "rent", "रिचार्ज", "recharge", "emi", "हप्ता", "रुपये", "₹", "$", "factura", "facture", "rechnung")
                .any { lower.contains(it) } -> HousekeepingCategory.BILL
            listOf("खरेदी", "किराणा", "भाजी", "दूध", "आण", "shopping", "buy", "groceries", "comprar", "acheter", "kaufen")
                .any { lower.contains(it) } -> HousekeepingCategory.SHOPPING
            listOf("स्वच्छ", "साफ", "रूटीन", "routine", "झाड", "व्यायाम", "clean", "habit")
                .any { lower.contains(it) } -> HousekeepingCategory.ROUTINE
            else -> HousekeepingCategory.TASK
        }

        val domain = when {
            listOf("अभ्यास", "स्टडी", "परीक्षा", "उजळणी", "नोट्स", "पेपर", "कॉलेज", "क्लास", "study", "exam", "revision", "assignment", "homework", "estudio", "examen")
                .any { lower.contains(it) } -> DashboardDomain.STUDY
            listOf("सुरक्षा", "सिक्युरिटी", "पासवर्ड", "बॅकअप", "अपडेट", "2fa", "राउटर", "लॉगिन", "security", "password", "backup", "vpn", "seguridad", "contraseña")
                .any { lower.contains(it) } -> DashboardDomain.SECURITY
            else -> defaultDomain
        }

        val priority = when {
            listOf("महत्त्वाच", "तातडी", "लगेच", "अर्जंट", "urgent", "important", "high", "urgente")
                .any { lower.contains(it) } -> PriorityLevel.HIGH
            listOf("निवांत", "नंतर", "low")
                .any { lower.contains(it) } -> PriorityLevel.LOW
            else -> PriorityLevel.MEDIUM
        }

        val cleanedTitle = transcript
            .replace(Regex("^(जार्विस|jarvis|बॉस|boss)[,\\s]*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(नवीन काम जोड|नवीन बिल जोड|काम जोड|बिल जोड|add task|add bill)[:\\s]*", RegexOption.IGNORE_CASE), "")
            .trim()
            .take(120)
            .ifEmpty { transcript.take(120) }

        val scheduleParts = buildList {
            when {
                lower.contains("उद्या") || lower.contains("tomorrow") || lower.contains("कल") ->
                    add(if (lower.contains("tomorrow")) "Tomorrow" else "उद्या")
                lower.contains("आज") || lower.contains("today") ->
                    add(if (lower.contains("today")) "Today" else "आज")
            }
            when {
                lower.contains("सकाळी") || lower.contains("morning") -> add("Morning")
                lower.contains("दुपारी") || lower.contains("afternoon") -> add("Afternoon")
                lower.contains("संध्याकाळी") || lower.contains("evening") -> add("Evening")
                lower.contains("रात्री") || lower.contains("tonight") || lower.contains("night") -> add("Night")
            }
            Regex("(?:₹\\s*\\d+|\\$\\s*\\d+|\\d+\\s*(?:रुपये|rs|rupees|usd|eur))", RegexOption.IGNORE_CASE)
                .find(transcript)?.value?.trim()?.let { add(it) }
        }
        val defaultToday = if (language == AiLanguage.MARATHI || language == AiLanguage.HINDI) "आज" else "Today"
        val scheduleLabel = scheduleParts.joinToString(" • ").ifBlank { defaultToday }.take(80)

        val confirmation = if (language == AiLanguage.MARATHI) {
            "हो $addressName, $cleanedTitle हे ${domain.labelMarathi} मध्ये जोडले आहे."
        } else {
            "Done $addressName, added $cleanedTitle to ${domain.labelEnglish}."
        }

        return ParsedVoiceCommand(
            title = cleanedTitle,
            notes = "",
            category = category,
            domain = domain,
            scheduleLabel = scheduleLabel,
            priority = priority,
            confirmationMessage = confirmation
        )
    }

    private fun sanitizeForVoice(raw: String): String {
        return raw
            .replace(Regex("[*#`~]+"), "")
            .replace(Regex("(?m)^\\s*[-•]\\s+"), "")
            .trim()
    }
}

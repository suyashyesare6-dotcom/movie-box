package com.example.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.model.AiLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

enum class TtsEngineStatus(val label: String) {
    INITIALIZING("व्हॉइस इंजिन सुरू होत आहे…"),
    READY("व्हॉइस इंजिन सक्रिय"),
    FALLBACK_LANGUAGE("बॅकअप आवाज सक्रिय (पसंतीचा पॅक उपलब्ध नाही)"),
    MISSING_DATA("या भाषेचा व्हॉइस पॅक डाउनलोड आवश्यक"),
    UNAVAILABLE("TextToSpeech उपलब्ध नाही")
}

data class TtsVoiceOption(
    val name: String,
    val label: String,
    val isOffline: Boolean,
    val isHighQuality: Boolean
)

class JarvisVoiceController(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _voiceStatusNote = MutableStateFlow<String?>(null)
    val voiceStatusNote: StateFlow<String?> = _voiceStatusNote.asStateFlow()

    private val _ttsStatus = MutableStateFlow(TtsEngineStatus.INITIALIZING)
    val ttsStatus: StateFlow<TtsEngineStatus> = _ttsStatus.asStateFlow()

    private val _activeLanguage = MutableStateFlow(AiLanguage.MARATHI)
    val activeLanguage: StateFlow<AiLanguage> = _activeLanguage.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceOption>> = _availableVoices.asStateFlow()

    private val _selectedVoiceName = MutableStateFlow<String?>(null)
    val selectedVoiceName: StateFlow<String?> = _selectedVoiceName.asStateFlow()

    private val _activeVoiceLabel = MutableStateFlow("Default")
    val activeVoiceLabel: StateFlow<String> = _activeVoiceLabel.asStateFlow()

    private val _voicePitch = MutableStateFlow(prefs.getFloat(KEY_VOICE_PITCH, DEFAULT_PITCH))
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private var ttsReady = false
    private var pendingSpeech: Triple<String, Float, AiLanguage?>? = null
    private var lastUtteranceId: String? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var onFinalResultCallback: ((String) -> Unit)? = null

    private var tts: TextToSpeech? = TextToSpeech(appContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            configureAudioAttributes()
            attachUtteranceListener()
            ttsReady = true
            configureLanguageAndVoices(_activeLanguage.value)
            pendingSpeech?.let { (text, rate, lang) ->
                pendingSpeech = null
                speak(text, rate, lang)
            }
        } else {
            Log.w(TAG, "TextToSpeech initialization failed with status $status")
            _ttsStatus.value = TtsEngineStatus.UNAVAILABLE
        }
    }

    private fun configureAudioAttributes() {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(attrs)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to set TTS AudioAttributes", e)
        }
    }

    private fun attachUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null || utteranceId == lastUtteranceId) {
                    _isSpeaking.value = false
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == null || utteranceId == lastUtteranceId) {
                    _isSpeaking.value = false
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "TTS utterance error code=$errorCode id=$utteranceId")
                if (utteranceId == null || utteranceId == lastUtteranceId) {
                    _isSpeaking.value = false
                }
            }
        })
    }

    fun setActiveLanguage(language: AiLanguage) {
        if (_activeLanguage.value == language && ttsReady && _availableVoices.value.isNotEmpty()) {
            return
        }
        _activeLanguage.value = language
        if (ttsReady) {
            configureLanguageAndVoices(language)
        }
    }

    private fun effectiveLocaleTag(language: AiLanguage, sampleText: String? = null): String {
        if (language != AiLanguage.AUTO) return language.localeTag
        if (!sampleText.isNullOrBlank()) {
            return when {
                // Japanese Hiragana / Katakana / CJK Unified Ideographs
                sampleText.any { it in '\u3040'..'\u30FF' || it in '\u4E00'..'\u9FFF' } -> "ja-JP"
                // Devanagari (Marathi / Hindi)
                sampleText.any { it in '\u0900'..'\u097F' } -> "mr-IN"
                // Spanish / French / German accents heuristic
                sampleText.any { it in "ñ¿¡" } -> "es-ES"
                sampleText.any { it in "ßäöü" } -> "de-DE"
                sampleText.any { it in "çœàèùêîôû" } -> "fr-FR"
                else -> "en-US"
            }
        }
        return "en-US"
    }

    private fun configureLanguageAndVoices(language: AiLanguage, sampleText: String? = null) {
        val engine = tts ?: return
        val tag = effectiveLocaleTag(language, sampleText)
        val targetLocale = Locale.forLanguageTag(tag)
        val langResult = engine.setLanguage(targetLocale)

        val voices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        val matchingVoices = voices
            .filter { voice ->
                voice.locale.language.equals(targetLocale.language, ignoreCase = true) &&
                    !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            .sortedWith(
                compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                    .thenByDescending { it.quality }
            )

        _availableVoices.value = matchingVoices.mapIndexed { idx, voice ->
            val modeLabel = if (voice.isNetworkConnectionRequired) "Online" else "Offline"
            val qualityLabel = if (voice.quality >= Voice.QUALITY_HIGH) "HD" else "Standard"
            TtsVoiceOption(
                name = voice.name,
                label = "${targetLocale.displayLanguage} ${idx + 1} ($qualityLabel • $modeLabel)",
                isOffline = !voice.isNetworkConnectionRequired,
                isHighQuality = voice.quality >= Voice.QUALITY_HIGH
            )
        }

        val savedName = prefs.getString("$KEY_VOICE_PREFIX${targetLocale.language}", null)
        val chosenVoice = matchingVoices.firstOrNull { it.name == savedName } ?: matchingVoices.firstOrNull()

        if (chosenVoice != null) {
            engine.voice = chosenVoice
            _selectedVoiceName.value = chosenVoice.name
            _activeVoiceLabel.value = "${targetLocale.displayLanguage} • ${chosenVoice.name}"
            _ttsStatus.value = TtsEngineStatus.READY
            return
        }

        when (langResult) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                _selectedVoiceName.value = engine.voice?.name
                _activeVoiceLabel.value = engine.voice?.name ?: tag
                _ttsStatus.value = TtsEngineStatus.READY
            }
            TextToSpeech.LANG_MISSING_DATA -> {
                _ttsStatus.value = TtsEngineStatus.MISSING_DATA
                fallbackToAvailableLocale(engine, targetLocale)
            }
            else -> {
                _ttsStatus.value = TtsEngineStatus.FALLBACK_LANGUAGE
                fallbackToAvailableLocale(engine, targetLocale)
            }
        }
    }

    private fun fallbackToAvailableLocale(engine: TextToSpeech, failedLocale: Locale) {
        if (failedLocale.language == "mr") {
            val hiResult = engine.setLanguage(Locale.forLanguageTag("hi-IN"))
            if (hiResult != TextToSpeech.LANG_MISSING_DATA && hiResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                _activeVoiceLabel.value = engine.voice?.name ?: "hi-IN (Fallback)"
                return
            }
        }
        val enResult = engine.setLanguage(Locale.US)
        if (enResult == TextToSpeech.LANG_MISSING_DATA || enResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.getDefault()
        }
        _activeVoiceLabel.value = engine.voice?.name ?: engine.language?.displayName ?: "Fallback"
    }

    fun selectVoice(voiceName: String) {
        val engine = tts ?: return
        val match = runCatching {
            engine.voices?.firstOrNull { it.name == voiceName }
        }.getOrNull() ?: return

        engine.voice = match
        _selectedVoiceName.value = match.name
        _activeVoiceLabel.value = "${match.locale.displayLanguage} • ${match.name}"
        _ttsStatus.value = TtsEngineStatus.READY
        prefs.edit().putString("$KEY_VOICE_PREFIX${match.locale.language}", match.name).apply()
    }

    fun setPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.75f, 1.25f)
        _voicePitch.value = clamped
        prefs.edit().putFloat(KEY_VOICE_PITCH, clamped).apply()
    }

    fun openInstallVoiceData(context: Context) {
        try {
            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No TTS data installer activity available", e)
            _voiceStatusNote.value = "डिव्हाइसच्या TTS सेटिंग्जमध्ये जाऊन भाषा पॅक डाउनलोड करा."
        }
    }

    fun testVoice(preferredTitle: String, speechRate: Float = 1.0f, language: AiLanguage = _activeLanguage.value) {
        val title = if (preferredTitle.equals("Boss", ignoreCase = true) &&
            (language == AiLanguage.MARATHI || language == AiLanguage.HINDI)
        ) "बॉस" else preferredTitle

        val sample = when (language) {
            AiLanguage.MARATHI -> "नमस्कार $title, मी जार्विस बोलतोय. मराठी व्हॉइस सिस्टीम तयार आहे."
            AiLanguage.HINDI -> "नमस्ते $title, मैं जार्विस बोल रहा हूँ। हिंदी वॉयस सिस्टम तैयार है।"
            AiLanguage.SPANISH -> "Hola $title, soy Jarvis. El sistema de voz en español está listo."
            AiLanguage.FRENCH -> "Bonjour $title, je suis Jarvis. Le système vocal français est prêt."
            AiLanguage.GERMAN -> "Hallo $title, ich bin Jarvis. Das deutsche Sprachsystem ist bereit."
            AiLanguage.JAPANESE -> "こんにちは、$title。ジャービスです。日本語音声システムは準備完了です。"
            AiLanguage.ENGLISH,
            AiLanguage.AUTO -> "Hello $title, this is JARVIS. Multi-language voice systems are online."
        }
        speak(sample, speechRate, language)
    }

    fun speak(text: String, speechRate: Float = 1.0f, languageOverride: AiLanguage? = null) {
        val cleanText = sanitizeSpeechText(text)
        if (cleanText.isEmpty()) return

        val targetLang = languageOverride ?: _activeLanguage.value
        if (!ttsReady) {
            pendingSpeech = Triple(cleanText, speechRate, targetLang)
            return
        }

        val engine = tts ?: return
        try {
            if (targetLang == AiLanguage.AUTO) {
                configureLanguageAndVoices(AiLanguage.AUTO, cleanText)
            } else if (languageOverride != null && languageOverride != _activeLanguage.value) {
                configureLanguageAndVoices(languageOverride, cleanText)
            }

            engine.setSpeechRate(speechRate.coerceIn(0.6f, 1.8f))
            engine.setPitch(_voicePitch.value)

            val chunks = splitIntoSpeechChunks(cleanText, TextToSpeech.getMaxSpeechInputLength().coerceAtMost(900))
            if (chunks.isEmpty()) return

            val lastId = UUID.randomUUID().toString()
            lastUtteranceId = lastId

            chunks.forEachIndexed { index, chunk ->
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val utteranceId = if (index == chunks.lastIndex) lastId else UUID.randomUUID().toString()
                engine.speak(chunk, queueMode, null, utteranceId)
            }
            _isSpeaking.value = true
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
            _isSpeaking.value = false
        }
    }

    private fun sanitizeSpeechText(raw: String): String {
        return raw
            .replace(Regex("[*#`~_]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun splitIntoSpeechChunks(text: String, maxChunkSize: Int): List<String> {
        if (text.length <= maxChunkSize) return listOf(text)
        val sentences = text.split(Regex("(?<=[।.?!。\\n])\\s*"))
        val chunks = mutableListOf<String>()
        val current = StringBuilder()

        for (sentence in sentences) {
            val trimmed = sentence.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.length > maxChunkSize) {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                }
                var start = 0
                while (start < trimmed.length) {
                    val end = (start + maxChunkSize).coerceAtMost(trimmed.length)
                    chunks.add(trimmed.substring(start, end))
                    start = end
                }
            } else if (current.length + trimmed.length + 1 > maxChunkSize) {
                chunks.add(current.toString().trim())
                current.clear()
                current.append(trimmed)
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(trimmed)
            }
        }
        if (current.isNotEmpty()) {
            chunks.add(current.toString().trim())
        }
        return chunks
    }

    fun stopSpeaking() {
        pendingSpeech = null
        lastUtteranceId = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        } finally {
            _isSpeaking.value = false
        }
    }

    fun isInlineSpeechAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(appContext)

    fun buildSpeechRecognitionIntent(languageOverride: AiLanguage? = null): Intent {
        val activeLang = languageOverride ?: _activeLanguage.value
        val recognitionLocale = if (activeLang == AiLanguage.AUTO) {
            val deviceTag = Locale.getDefault().toLanguageTag()
            if (deviceTag.startsWith("mr") || deviceTag.startsWith("hi")) deviceTag else "mr-IN"
        } else {
            activeLang.speechRecognizerTag
        }
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, recognitionLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, recognitionLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (recognitionLocale.startsWith("mr")) "बॉस, मराठीत बोला…" else "Speak now, Boss…"
            )
        }
    }

    fun startListening(languageOverride: AiLanguage? = null, onResult: (String) -> Unit) {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _voiceStatusNote.value = "या डिव्हाइसवर स्पीच रेकग्निशन उपलब्ध नाही, बॉस. कृपया टाइप करा."
            return
        }

        onFinalResultCallback = onResult
        _voiceStatusNote.value = null
        _partialTranscript.value = ""
        _rmsLevel.value = 0f

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        _partialTranscript.value = ""
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "ऑडिओ रेकॉर्डिंगमध्ये अडचण आली."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "मायक्रोफोनची परवानगी आवश्यक आहे."
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "स्पीच सर्व्हिससाठी नेटवर्क तपासा."
                            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "मराठी व्हॉइस रेकग्निशन पॅक उपलब्ध नाही; डिव्हाइस सेटिंग्ज तपासा."
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "आवाज स्पष्ट आला नाही. पुन्हा प्रयत्न करा."
                            else -> "व्हॉइस इनपुट थांबला. पुन्हा प्रयत्न करा."
                        }
                        _voiceStatusNote.value = message
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                        _partialTranscript.value = ""
                        if (spoken.isNotEmpty()) {
                            onFinalResultCallback?.invoke(spoken)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull { it.isNotBlank() }.orEmpty()
                        if (partial.isNotBlank()) {
                            _partialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
            }

            val intent = buildSpeechRecognitionIntent(languageOverride)
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            _rmsLevel.value = 0f
            _voiceStatusNote.value = "मायक्रोफोन सुरू करता आला नाही, बॉस."
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        } finally {
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }

    fun cancelListening() {
        onFinalResultCallback = null
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {
        } finally {
            _isListening.value = false
            _rmsLevel.value = 0f
            _partialTranscript.value = ""
        }
    }

    fun reportStatus(message: String?) {
        _voiceStatusNote.value = message
    }

    fun clearStatusNote() {
        _voiceStatusNote.value = null
    }

    fun release() {
        pendingSpeech = null
        lastUtteranceId = null
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TAG = "JarvisVoice"
        const val PREFS_NAME = "jarvis_voice_prefs"
        const val KEY_VOICE_PREFIX = "selected_voice_"
        const val KEY_VOICE_PITCH = "marathi_voice_pitch"
        const val DEFAULT_PITCH = 0.95f
    }
}

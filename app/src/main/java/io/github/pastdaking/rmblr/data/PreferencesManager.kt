package io.github.pastdaking.rmblr.data

import android.content.Context
import android.content.SharedPreferences
import io.github.pastdaking.rmblr.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ilight_keyboard_prefs", Context.MODE_PRIVATE)

    private val _apiKeyFlow = MutableStateFlow(getEffectiveApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    private val _modelFlow = MutableStateFlow(getSelectedModel())
    val modelFlow: StateFlow<String> = _modelFlow.asStateFlow()

    private val _engineFlow = MutableStateFlow(getEngine())
    val engineFlow: StateFlow<TranscriptionEngine> = _engineFlow.asStateFlow()

    /** The key belonging to whichever engine is selected right now. */
    private val _groqKeyFlow = MutableStateFlow(getProviderKey(getEngine().provider))
    val groqKeyFlow: StateFlow<String> = _groqKeyFlow.asStateFlow()

    private val _languageFlow = MutableStateFlow(getSpokenLanguage())
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    private val _dictationLanguageFlow = MutableStateFlow(getDictationLanguage())
    val dictationLanguageFlow: StateFlow<String> = _dictationLanguageFlow.asStateFlow()

    private val _translateEnabledFlow = MutableStateFlow(isTranslateEnabled())
    val translateEnabledFlow: StateFlow<Boolean> = _translateEnabledFlow.asStateFlow()

    private val _translateTargetFlow = MutableStateFlow(getTranslateTarget())
    val translateTargetFlow: StateFlow<String> = _translateTargetFlow.asStateFlow()

    private val _textProviderFlow = MutableStateFlow(getTextProvider())
    val textProviderFlow: StateFlow<TextProvider> = _textProviderFlow.asStateFlow()

    private val _textKeyFlow = MutableStateFlow(getTextApiKey())
    val textKeyFlow: StateFlow<String> = _textKeyFlow.asStateFlow()

    private val _transcriptionModeFlow = MutableStateFlow(getTranscriptionMode())
    val transcriptionModeFlow: StateFlow<TranscriptionMode> = _transcriptionModeFlow.asStateFlow()

    private val _presetFlow = MutableStateFlow(getCleanupPreset())
    val presetFlow: StateFlow<CleanupPreset> = _presetFlow.asStateFlow()

    private val _customPromptFlow = MutableStateFlow(getCustomPrompt())
    val customPromptFlow: StateFlow<String> = _customPromptFlow.asStateFlow()

    private val _floatingEnabledFlow = MutableStateFlow(isFloatingAssistantEnabled())
    val floatingEnabledFlow: StateFlow<Boolean> = _floatingEnabledFlow.asStateFlow()

    fun getUserApiKey(): String {
        return prefs.getString("user_api_key", "") ?: ""
    }

    fun setUserApiKey(key: String) {
        prefs.edit().putString("user_api_key", key.trim()).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    fun getEffectiveApiKey(): String {
        val userKey = getUserApiKey()
        if (userKey.isNotBlank()) return userKey
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun getSelectedModel(): String = getEngine().id

    fun setSelectedModel(model: String) {
        setEngine(TranscriptionEngine.from(model))
    }

    // ---- engine ---------------------------------------------------------
    //
    // Which thing actually does the listening. This replaced a plain model-id string:
    // Groq is not a Gemini model, and "streams while you talk" is a property of the
    // engine rather than of the name, so the choice needs somewhere richer to live.

    fun getEngine(): TranscriptionEngine =
        TranscriptionEngine.from(prefs.getString("engine", null) ?: prefs.getString("gemini_model", null))

    fun setEngine(engine: TranscriptionEngine) {
        prefs.edit().putString("engine", engine.name).apply()
        _engineFlow.value = engine
        _modelFlow.value = engine.id
        _groqKeyFlow.value = getEngineKey(engine)
    }

    // ---- per-engine keys and models -------------------------------------
    //
    // Groq, Mistral and OpenRouter each bring their own key, and two of them front many
    // models. Keys are stored per engine so switching between them does not make you
    // paste the previous one back in.

    /**
     * The key for a PROVIDER, shared by everything that provider does.
     *
     * Keys used to be stored per feature, so a Mistral key pasted into dictation did
     * nothing for translation and had to be typed again. An account is an account.
     *
     * Older per-feature keys are read forward the first time each provider is asked for,
     * so nobody has to re-enter anything after upgrading.
     */
    fun getProviderKey(provider: Provider): String {
        if (!provider.needsKey) return getEffectiveApiKey()
        prefs.getString("provider_key_${provider.name}", null)?.let { return it }
        // Where this provider's key used to live, newest scheme first.
        val legacy = listOfNotNull(
            TranscriptionEngine.values().firstOrNull { it.provider == provider }
                ?.let { prefs.getString("engine_key_${it.name}", null) },
            TextProvider.values().firstOrNull { it.provider == provider }
                ?.let { prefs.getString("text_key_${it.name}", null) },
            if (provider == Provider.GROQ) prefs.getString("groq_api_key", null) else null
        )
        return legacy.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    fun setProviderKey(provider: Provider, key: String) {
        if (!provider.needsKey) {
            setUserApiKey(key)
            return
        }
        prefs.edit().putString("provider_key_${provider.name}", key.trim()).apply()
        _groqKeyFlow.value = key.trim()
        _textKeyFlow.value = key.trim()
    }

    /**
     * Where a provider's API lives.
     *
     * Every provider but CUSTOM knows its own address, so this only ever needs typing
     * into for a self-hosted or in-house endpoint — and like the key, it is stored once
     * per provider and shared by dictation and translation.
     */
    fun getProviderBaseUrl(provider: Provider): String {
        val stored = prefs.getString("provider_base_${provider.name}", "") ?: ""
        return stored.ifBlank { provider.baseUrl }
    }

    fun setProviderBaseUrl(provider: Provider, url: String) {
        prefs.edit().putString("provider_base_${provider.name}", url.trim()).apply()
    }

    fun getEngineKey(engine: TranscriptionEngine): String = getProviderKey(engine.provider)

    fun setEngineKey(engine: TranscriptionEngine, key: String) = setProviderKey(engine.provider, key)

    /** Blank falls back to the engine's own default, so only the open-ended ones need typing into. */
    fun getEngineModel(engine: TranscriptionEngine): String {
        val stored = prefs.getString("engine_model_${engine.name}", "") ?: ""
        return stored.ifBlank { engine.id }
    }

    fun setEngineModel(engine: TranscriptionEngine, model: String) {
        prefs.edit().putString("engine_model_${engine.name}", model.trim()).apply()
    }

    fun getGroqApiKey(): String = getProviderKey(Provider.GROQ)

    fun setGroqApiKey(key: String) = setProviderKey(Provider.GROQ, key)

    // ---- spoken language ------------------------------------------------
    //
    // Blank means "work it out", which is the default and what almost everyone should
    // leave it on. Naming a language only helps when you know the transcriber is
    // guessing wrong.

    /**
     * The language, as a NAME the user typed, or blank for auto.
     *
     * It used to be a code chosen from a fixed strip of chips, which is fine right up
     * until your language is not one of the fifteen someone else picked. It is only ever
     * a word handed to a model, so any word will do.
     */
    fun getSpokenLanguage(): String = prefs.getString("language_name", "") ?: ""

    fun setSpokenLanguage(name: String) {
        prefs.edit().putString("language_name", name.trim()).apply()
        _languageFlow.value = name.trim()
    }

    /**
     * The ISO code, when the typed name happens to be one we recognise.
     *
     * Only the OpenAI-shaped transcribers want a code, and they are all happy without
     * one. So an unrecognised name is not an error — the name still reaches the model as
     * context, it just does not also become a `language` field.
     */
    fun getLanguageCode(): String = languageCodeFor(getSpokenLanguage())

    fun setLanguageCode(code: String) {
        setSpokenLanguage(languageFor(code).label.takeIf { code.isNotBlank() } ?: "")
    }

    /**
     * Write the dictation in a DIFFERENT language from the one it was spoken in.
     *
     * Blank means "whatever I said", which is what almost everyone wants. Set it and
     * every dictation is translated on its way into the field — speak English, type
     * Japanese. It reuses the translation provider, so it costs one extra call and only
     * when it is switched on.
     */
    fun getDictationLanguage(): String = prefs.getString("dictation_out_language", "") ?: ""

    fun setDictationLanguage(name: String) {
        prefs.edit().putString("dictation_out_language", name.trim()).apply()
        _dictationLanguageFlow.value = name.trim()
    }

    fun getTranscriptionMode(): TranscriptionMode {
        val name = prefs.getString("transcription_mode", TranscriptionMode.POST_PROCESS_CLEANUP.name)
        return try {
            TranscriptionMode.valueOf(name ?: TranscriptionMode.POST_PROCESS_CLEANUP.name)
        } catch (e: Exception) {
            TranscriptionMode.POST_PROCESS_CLEANUP
        }
    }

    fun setTranscriptionMode(mode: TranscriptionMode) {
        prefs.edit().putString("transcription_mode", mode.name).apply()
        _transcriptionModeFlow.value = mode
    }

    fun getCleanupPreset(): CleanupPreset {
        val name = prefs.getString("cleanup_preset", CleanupPreset.SMART_CLEAN.name)
        return try {
            CleanupPreset.valueOf(name ?: CleanupPreset.SMART_CLEAN.name)
        } catch (e: Exception) {
            CleanupPreset.SMART_CLEAN
        }
    }

    fun setCleanupPreset(preset: CleanupPreset) {
        prefs.edit().putString("cleanup_preset", preset.name).apply()
        _presetFlow.value = preset
    }

    fun getCustomPrompt(): String {
        return prefs.getString("custom_prompt", "Rewrite clearly and remove filler words, maintaining natural flow.") ?: ""
    }

    fun setCustomPrompt(prompt: String) {
        prefs.edit().putString("custom_prompt", prompt).apply()
        _customPromptFlow.value = prompt
    }

    // ---- translation ----------------------------------------------------
    //
    // Off until it is switched on, and it says why on the switch. Reading the clipboard
    // is the kind of permission people are right to be suspicious of, so nothing here
    // happens to anyone who did not ask for it.

    fun isTranslateEnabled(): Boolean = prefs.getBoolean("translate_enabled", false)

    fun setTranslateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("translate_enabled", enabled).apply()
        _translateEnabledFlow.value = enabled
    }

    /** Also a typed name, for the same reason. */
    fun getTranslateTarget(): String = prefs.getString("translate_target_name", "English") ?: "English"

    fun setTranslateTarget(name: String) {
        val cleaned = name.trim().ifBlank { "English" }
        prefs.edit().putString("translate_target_name", cleaned).apply()
        _translateTargetFlow.value = cleaned
    }

    fun getTranslateTargetCode(): String = getTranslateTarget()

    fun setTranslateTargetCode(code: String) = setTranslateTarget(languageFor(code).label)

    // ---- text provider --------------------------------------------------

    fun getTextProvider(): TextProvider = TextProvider.from(prefs.getString("text_provider", null))

    fun setTextProvider(provider: TextProvider) {
        prefs.edit().putString("text_provider", provider.name).apply()
        _textProviderFlow.value = provider
    }

    fun getTextApiKey(): String = getProviderKey(getTextProvider().provider)

    fun setTextApiKey(key: String) = setProviderKey(getTextProvider().provider, key)

    /** Blank falls back to the provider's own, so only CUSTOM ever needs typing into. */
    fun getTextBaseUrl(): String = getProviderBaseUrl(getTextProvider().provider)

    fun setTextBaseUrl(url: String) = setProviderBaseUrl(getTextProvider().provider, url)

    fun getTextModel(): String {
        val provider = getTextProvider()
        val stored = prefs.getString("text_model_${provider.name}", "") ?: ""
        return stored.ifBlank { provider.defaultModel }
    }

    fun setTextModel(model: String) {
        prefs.edit().putString("text_model_${getTextProvider().name}", model.trim()).apply()
    }

    fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_feedback", true)
    fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean("haptic_feedback", enabled).apply()

    fun isFloatingAssistantEnabled(): Boolean = prefs.getBoolean("floating_assistant_enabled", false)
    fun setFloatingAssistantEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("floating_assistant_enabled", enabled).apply()
        _floatingEnabledFlow.value = enabled
    }

    fun isAutoCapitalizeEnabled(): Boolean = prefs.getBoolean("auto_capitalize", true)
    fun setAutoCapitalizeEnabled(enabled: Boolean) = prefs.edit().putBoolean("auto_capitalize", enabled).apply()

    companion object {
        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

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

    fun getSelectedModel(): String {
        return prefs.getString("gemini_model", "gemini-3.1-flash-live-preview") ?: "gemini-3.1-flash-live-preview"
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString("gemini_model", model).apply()
        _modelFlow.value = model
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

    fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_feedback", true)
    fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean("haptic_feedback", enabled).apply()

    fun isSoundEnabled(): Boolean = prefs.getBoolean("sound_effects", false)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean("sound_effects", enabled).apply()

    fun isFloatingAssistantEnabled(): Boolean = prefs.getBoolean("floating_assistant_enabled", false)
    fun setFloatingAssistantEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("floating_assistant_enabled", enabled).apply()
        _floatingEnabledFlow.value = enabled
    }

    fun isAutoCapitalizeEnabled(): Boolean = prefs.getBoolean("auto_capitalize", true)
    fun setAutoCapitalizeEnabled(enabled: Boolean) = prefs.edit().putBoolean("auto_capitalize", enabled).apply()

    fun getKeyboardTheme(): String = prefs.getString("keyboard_theme", "dark_futuristic") ?: "dark_futuristic"
    fun setKeyboardTheme(theme: String) = prefs.edit().putString("keyboard_theme", theme).apply()

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

package io.github.pastdaking.rmblr.ui.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.pastdaking.rmblr.ai.DictationController
import io.github.pastdaking.rmblr.audio.AudioRecorderManager
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.data.DictationHistoryItem
import io.github.pastdaking.rmblr.data.DictionaryRepository
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.TranscriptionMode
import io.github.pastdaking.rmblr.ui.m3.WaveformBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the in-app dictation is. */
sealed interface HomePhase {
    data object Idle : HomePhase
    data object Recording : HomePhase
    data object Working : HomePhase
    data class Result(val item: DictationHistoryItem) : HomePhase
}

/**
 * The tone's instruction for a dictation, or null for "just type what I said".
 * Verbatim mode never rewrites; a custom tone hands down its own prompt.
 */
fun instructionFor(mode: TranscriptionMode, preset: CleanupPreset, customPrompt: String): String? = when {
    mode == TranscriptionMode.DIRECT_VERBATIM -> null
    preset == CleanupPreset.CUSTOM -> customPrompt.takeIf { it.isNotBlank() }
    else -> preset.systemPrompt
}

/**
 * Runs one in-app dictation and exposes what the Home, Recording and Result screens draw.
 * It drives the same [DictationController] the orb uses, so the engines, fallbacks and tones
 * behave identically in both places.
 */
class HomeSession(
    context: Context,
    private val prefs: PreferencesManager,
    private val history: HistoryRepository,
    private val scope: CoroutineScope
) {
    private val recorder = AudioRecorderManager(context.applicationContext)
    private val dictation = DictationController(
        prefs, recorder, DictionaryRepository.getInstance(context), history
    )

    var phase by mutableStateOf<HomePhase>(HomePhase.Idle)
        private set
    var elapsedSeconds by mutableIntStateOf(0)
        private set
    var levels by mutableStateOf(List(48) { 0f })
        private set
    var liveText by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)

    /** True when the engine in use sends text before you stop talking. */
    var streaming by mutableStateOf(false)
        private set

    private val wave = WaveformBuffer(48)
    private var ticker: Job? = null

    fun start() {
        if (phase != HomePhase.Idle) return
        error = null
        liveText = ""
        elapsedSeconds = 0
        dictation.begin()
        streaming = dictation.isStreaming
        phase = HomePhase.Recording
        val startedAt = System.currentTimeMillis()
        ticker = scope.launch {
            while (isActive && phase == HomePhase.Recording) {
                wave.push(recorder.audioAmplitude.value)
                levels = wave.snapshot()
                liveText = dictation.liveText.value
                elapsedSeconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                delay(80)
            }
        }
    }

    fun stop() {
        if (phase != HomePhase.Recording) return
        phase = HomePhase.Working
        val seconds = elapsedSeconds
        scope.launch {
            val mode = prefs.getTranscriptionMode()
            val preset = prefs.getCleanupPreset()
            val instruction = instructionFor(mode, preset, prefs.getCustomPrompt())
            val result = withContext(Dispatchers.IO) { dictation.finish(instruction) }
            result.onSuccess { (raw, cleaned) ->
                val item = DictationHistoryItem(
                    rawText = raw, cleanedText = cleaned, mode = mode, preset = preset,
                    durationSeconds = seconds
                )
                history.addHistoryItem(item)
                phase = HomePhase.Result(item)
            }.onFailure {
                error = it.message ?: "That didn't work."
                phase = HomePhase.Idle
            }
        }
    }

    fun cancel() {
        if (phase != HomePhase.Recording) return
        dictation.cancel()
        phase = HomePhase.Idle
    }

    /** Back to the start screen from the result. */
    fun reset() {
        if (phase is HomePhase.Result || phase == HomePhase.Idle) phase = HomePhase.Idle
    }

    /** Swap the text shown on the result after a rewrite or translation. */
    fun replaceResult(item: DictationHistoryItem) {
        if (phase is HomePhase.Result) phase = HomePhase.Result(item)
        history.updateHistoryItem(item)
    }
}

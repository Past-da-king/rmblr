package io.github.pastdaking.rmblr.ui.screens

import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.Provider
import io.github.pastdaking.rmblr.data.TextProvider
import io.github.pastdaking.rmblr.data.TranscriptionEngine

/**
 * The walkthrough's rules, apart from its drawing.
 *
 * Five steps: what it does, where it appears, how to use it, which key, and the four
 * permissions Android makes the person switch on for themselves.
 */
object OnboardingFlow {
    const val STEPS = 5
    const val KEY_STEP = 3
    const val PERMISSIONS_STEP = 4

    /** The label on the big button at the foot of a step. */
    fun primaryLabel(step: Int): String = when (step) {
        KEY_STEP -> "Continue"
        PERMISSIONS_STEP -> "Finish setup"
        else -> "Next"
    }

    /**
     * Finish is only live once all four are on. Skip is always there for anyone who would
     * rather do it later, so this can be strict without trapping anybody.
     */
    fun canFinish(state: PermissionState): Boolean = state.allGranted

    fun isLast(step: Int): Boolean = step >= STEPS - 1

    /** Never past the ends. */
    fun clamp(step: Int): Int = step.coerceIn(0, STEPS - 1)
}

/**
 * Choosing a provider on the key step, and pasting its key, should leave the app able to
 * use it. The walkthrough used to take a Gemini key only; with six providers offered here,
 * a Groq key that was saved but never selected would sit unused while dictation kept
 * asking for Gemini.
 */
fun useProviderForEverything(prefs: PreferencesManager, provider: Provider) {
    TranscriptionEngine.values().firstOrNull { it.provider == provider }?.let {
        if (prefs.getEngine().provider != provider) prefs.setEngine(it)
    }
    TextProvider.values().firstOrNull { it.provider == provider }?.let {
        if (prefs.getTextProvider().provider != provider) prefs.setTextProvider(it)
    }
}

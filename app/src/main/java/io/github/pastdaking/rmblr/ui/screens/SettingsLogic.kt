package io.github.pastdaking.rmblr.ui.screens

import android.content.Context
import io.github.pastdaking.rmblr.ai.GeminiApiClient
import io.github.pastdaking.rmblr.ai.SpeechToTextClient
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.Provider

/**
 * The parts of Settings that are decisions rather than drawing, kept here so they can be
 * tested without a screen.
 */

/**
 * A stored key, shown without giving it away.
 *
 * The first four and last four characters are enough to tell two keys apart and to
 * recognise the one you pasted; everything between is dots. A key too short to hide
 * anything by trimming is shown as dots alone, so a short secret is never half-printed.
 */
fun maskKey(key: String): String {
    val k = key.trim()
    if (k.isEmpty()) return ""
    if (k.length <= 8) return "•".repeat(k.length)
    val dots = (k.length - 8).coerceIn(3, 12)
    return k.take(4) + "•".repeat(dots) + k.takeLast(4)
}

/**
 * "Fade when idle" as one slider.
 *
 * The orb has two stored values, whether it fades at all and how many seconds of being
 * left alone it waits. One slider is a kinder control, so its left end is "never" and
 * the rest of the track runs from a long wait (60 s) down to a short one (2 s).
 */
object IdleFadeScale {
    const val MIN_SECONDS = 2
    const val MAX_SECONDS = 60

    private const val OFF_BELOW = 0.02f

    /** Slider position 0..1 for the stored values. */
    fun position(fades: Boolean, afterSeconds: Int): Float {
        if (!fades) return 0f
        val s = afterSeconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
        return OFF_BELOW + (MAX_SECONDS - s).toFloat() / (MAX_SECONDS - MIN_SECONDS) * (1f - OFF_BELOW)
    }

    /** The stored values for a slider position. Position 0 means it never fades. */
    fun fromPosition(position: Float): Pair<Boolean, Int> {
        val p = position.coerceIn(0f, 1f)
        if (p < OFF_BELOW) return false to MAX_SECONDS
        val seconds = Math.round(MAX_SECONDS - (p - OFF_BELOW) / (1f - OFF_BELOW) * (MAX_SECONDS - MIN_SECONDS))
        return true to seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
    }

    fun label(fades: Boolean, afterSeconds: Int): String =
        if (fades) "After $afterSeconds s" else "Never"
}

/** What a key looks like to the person looking at it. */
sealed interface KeyStatus {
    /** Nothing saved for this provider. */
    data object NotSet : KeyStatus

    /** A key is saved and has not been proved. */
    data object Untested : KeyStatus

    /** The saved key was checked against the provider and answered. */
    data object Connected : KeyStatus

    /** The last check was refused or could not reach the provider. */
    data class Failed(val message: String) : KeyStatus
}

/**
 * Remembers which key passed its check, without keeping the key.
 *
 * Only a fingerprint of the key is stored, so changing the key makes it "untested" again
 * on its own, and nothing secret is written a second time.
 */
class KeyChecks(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rmblr_key_checks", Context.MODE_PRIVATE)

    fun passed(provider: Provider, key: String) {
        prefs.edit().putString(provider.name, fingerprint(key)).apply()
    }

    fun clear(provider: Provider) {
        prefs.edit().remove(provider.name).apply()
    }

    fun isPassing(provider: Provider, key: String): Boolean =
        key.isNotBlank() && prefs.getString(provider.name, null) == fingerprint(key)

    companion object {
        fun fingerprint(key: String): String {
            val k = key.trim()
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(k.toByteArray())
            return digest.take(8).joinToString("") { "%02x".format(it) }
        }
    }
}

fun keyStatusOf(provider: Provider, key: String, checks: KeyChecks, failure: String? = null): KeyStatus = when {
    key.isBlank() -> KeyStatus.NotSet
    failure != null -> KeyStatus.Failed(failure)
    checks.isPassing(provider, key) -> KeyStatus.Connected
    else -> KeyStatus.Untested
}

/** The outcome of a real call with a key. */
data class KeyTestResult(val ok: Boolean, val message: String)

/**
 * Prove a key by using it.
 *
 * A blank key is refused before anything is sent, which is the bug the two clients each
 * had a private fix for: a test that can say "works" with nothing typed in is worse than
 * no test. Gemini is checked with a short generate call, and every other provider with
 * a list-models call on its own base URL, so a wrong key, a wrong address and no
 * connection all come back as different words.
 */
object KeyTester {

    fun blankMessage(provider: Provider) = "Paste a ${provider.label} key first."

    suspend fun test(provider: Provider, key: String, baseUrl: String): KeyTestResult {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return KeyTestResult(false, blankMessage(provider))
        if (provider.needsKey && baseUrl.isBlank()) {
            return KeyTestResult(false, "Add the base URL first, for example https://api.example.com/v1.")
        }
        val result = if (provider.needsKey) {
            SpeechToTextClient.testConnection(baseUrl, trimmed, provider.label)
        } else {
            GeminiApiClient.testConnection(trimmed)
        }
        return result.fold(
            onSuccess = { KeyTestResult(true, it) },
            onFailure = { KeyTestResult(false, it.message ?: "Could not reach ${provider.label}.") }
        )
    }
}

/** Where to get a key, for the "Get a key" button. Blank when the provider has no fixed place. */
fun keyConsoleUrl(provider: Provider): String = when (provider) {
    Provider.GEMINI -> "https://aistudio.google.com/apikey"
    Provider.GROQ -> "https://console.groq.com/keys"
    Provider.MISTRAL -> "https://console.mistral.ai/api-keys"
    Provider.OPENROUTER -> "https://openrouter.ai/keys"
    Provider.OPENAI -> "https://platform.openai.com/api-keys"
    Provider.CUSTOM -> ""
}

/** Helpers over [PreferencesManager] that the Settings screen leans on. */
fun PreferencesManager.keyFor(provider: Provider): String = getProviderKey(provider)

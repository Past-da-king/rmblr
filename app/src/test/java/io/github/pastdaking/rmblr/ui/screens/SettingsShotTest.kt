package io.github.pastdaking.rmblr.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import io.github.pastdaking.rmblr.data.LanguageSelection
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.Provider
import io.github.pastdaking.rmblr.update.UpdateRepository
import io.github.pastdaking.rmblr.update.UpdateStatus
import org.junit.Test

/**
 * Screenshots of the Settings page and its sheets, light and dark. They are written to
 * `app/build/m3-proof/` and compared by eye against `design/m3-mocks/png/10-*`, `11-*`.
 */
class SettingsShotTest : ProofShotBase() {

    private fun seed(): PreferencesManager {
        val prefs = PreferencesManager.getInstance(context)
        // Obviously fake keys: these only ever appear in a test image.
        prefs.setProviderKey(Provider.GEMINI, "AIzaSyD-example-key-0000-3f9a")
        prefs.setProviderKey(Provider.GROQ, "gsk_exampleexampleexample_k2Qz")
        KeyChecks(context).passed(Provider.GEMINI, "AIzaSyD-example-key-0000-3f9a")
        prefs.setLanguageSelection(LanguageSelection(auto = true, names = listOf("French", "Spanish")))
        // A fresh check on record means the page does not go to GitHub while it is drawn.
        context.getSharedPreferences("rmblr_updates", android.content.Context.MODE_PRIVATE)
            .edit().putLong("last_check_at", System.currentTimeMillis()).commit()
        UpdateRepository.getInstance(context).setStatusForTest(UpdateStatus.UpToDate(System.currentTimeMillis()))
        return prefs
    }

    /** Scroll so the text sits [topDp] below the top edge, the way the mock frames are cut. */
    private fun ComposeContentTestRule.scrollTextTo(text: String, topDp: Float) {
        val density = 3f
        onNodeWithText(text).performScrollTo()
        waitForIdle()
        repeat(2) {
            val top = onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top
            onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, top - topDp * density) }
            waitForIdle()
        }
    }

    @Test fun settingsListeningAndRewriting() {
        val prefs = seed()
        proof("10-settings") { SettingsScreen(prefs) }
    }

    @Test fun settingsKeysAndLanguages() {
        val prefs = seed()
        proof("10-settings-2", prepare = { scrollTextTo("API keys", 112f) }) { SettingsScreen(prefs) }
    }

    @Test fun settingsOrbAndAppearance() {
        val prefs = seed()
        proof("10-settings-3", prepare = { scrollTextTo("Orb behaviour", 112f) }) { SettingsScreen(prefs) }
    }

    @Test fun settingsPrivacyTypingAbout() {
        val prefs = seed()
        proof("10-settings-4", prepare = { scrollTextTo("Private by design", 80f) }) { SettingsScreen(prefs) }
    }

    @Test fun apiKeySheetShowsAWorkingKey() {
        val prefs = seed()
        val checks = KeyChecks(context)
        proof("11-settings-keys") {
            SettingsScreen(prefs)
            ApiKeySheet(
                provider = Provider.GROQ,
                prefs = prefs,
                checks = checks,
                onDismiss = {},
                initialResult = KeyTestResult(true, "whisper-large-v3-turbo answered.")
            )
        }
    }

    @Test fun apiKeySheetShowsAFailure() {
        val prefs = seed()
        val checks = KeyChecks(context)
        proof("11-settings-keys-failed") {
            SettingsScreen(prefs)
            ApiKeySheet(
                provider = Provider.MISTRAL,
                prefs = prefs,
                checks = checks,
                onDismiss = {},
                initialResult = KeyTestResult(false, "Mistral rejected the key (401).")
            )
        }
    }
}

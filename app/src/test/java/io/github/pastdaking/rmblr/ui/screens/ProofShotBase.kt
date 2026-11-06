package io.github.pastdaking.rmblr.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.MyApplicationTheme
import io.github.pastdaking.rmblr.ui.theme.ThemeChoice
import io.github.pastdaking.rmblr.ui.theme.ThemeState
import io.github.pastdaking.rmblr.update.UpdateRepository
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot base for the settings-side screens.
 *
 * The same 412 x 915 dp frame and the same light-then-dark capture as the shared one in
 * `ui/m3`, with a step in between for scrolling a long page to the part being proved, and
 * a clean slate of preferences for every test so one screen's fixtures never leak into
 * the next.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
abstract class ProofShotBase {

    @get:Rule
    val rule = createComposeRule()

    protected val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun freshSingletons() {
        // Both are process-wide singletons holding the first test's context.
        listOf(PreferencesManager::class.java, UpdateRepository::class.java).forEach { type ->
            runCatching {
                type.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
            }
        }
    }

    /**
     * Draw [content], run [prepare] (scroll, open a menu), and write
     * `build/m3-proof/<name>-light.png` and `-dark.png`.
     */
    protected fun proof(
        name: String,
        prepare: ComposeContentTestRule.() -> Unit = {},
        content: @Composable () -> Unit
    ) {
        ThemeState.dynamic = false
        ThemeState.choice = ThemeChoice.LIGHT
        rule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().background(Ink)) { content() }
            }
        }
        rule.waitForIdle()
        rule.prepare()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/m3-proof/$name-light.png")
        rule.runOnIdle { ThemeState.choice = ThemeChoice.DARK }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/m3-proof/$name-dark.png")
    }
}

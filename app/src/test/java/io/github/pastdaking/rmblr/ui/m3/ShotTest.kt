package io.github.pastdaking.rmblr.ui.m3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.MyApplicationTheme
import io.github.pastdaking.rmblr.ui.theme.ThemeChoice
import io.github.pastdaking.rmblr.ui.theme.ThemeState
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Base class for screenshot tests. A test calls [shot] with a screen; the screen is drawn
 * at 412 x 915 dp in the light skin and then the dark skin, and the PNGs land in
 * `app/build/m3-proof/<name>-light.png` and `-dark.png`. Run them with
 * `./gradlew :app:recordRoborazziDebug` (copy the PNGs to docs/m3-build/proof/ afterwards).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
abstract class ShotTest {

    @get:Rule
    val rule = createComposeRule()

    protected fun shot(name: String, content: @Composable () -> Unit) {
        ThemeState.dynamic = false
        ThemeState.choice = ThemeChoice.LIGHT
        rule.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize().background(Ink)) { content() }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/m3-proof/$name-light.png")
        rule.runOnIdle { ThemeState.choice = ThemeChoice.DARK }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/m3-proof/$name-dark.png")
    }
}

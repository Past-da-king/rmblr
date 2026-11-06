package io.github.pastdaking.rmblr.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.data.Provider
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.AssistChip
import io.github.pastdaking.rmblr.ui.m3.Eyebrow
import io.github.pastdaking.rmblr.ui.m3.ExpressiveShapes
import io.github.pastdaking.rmblr.ui.m3.M3Chip
import io.github.pastdaking.rmblr.ui.m3.Radial
import io.github.pastdaking.rmblr.ui.m3.RadialShape
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalTextField
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentText
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.GoodWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.RaisedHighest
import io.github.pastdaking.rmblr.ui.theme.SecondaryWash
import io.github.pastdaking.rmblr.ui.theme.TealWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlinx.coroutines.launch

/** Whether the walkthrough has been seen. Read once at start and written when it ends. */
class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("rmblr_onboarding", Context.MODE_PRIVATE)
    var done: Boolean
        get() = prefs.getBoolean("done", false)
        set(value) = prefs.edit().putBoolean("done", value).apply()
}

/**
 * The walkthrough: five steps, the last of which is the four permissions with a live
 * granted or needed state for each.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val pager = rememberPagerState(pageCount = { OnboardingFlow.STEPS })
    val scope = rememberCoroutineScope()
    val permissions by rememberPermissionState()

    OnboardingContent(
        step = pager.currentPage,
        permissions = permissions,
        onSkip = onDone,
        onBack = { scope.launch { pager.animateScrollToPage(OnboardingFlow.clamp(pager.currentPage - 1)) } },
        onNext = {
            if (OnboardingFlow.isLast(pager.currentPage)) onDone()
            else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
        },
        pageContent = {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
                OnboardingPage(index, prefs)
            }
        }
    )
}

/** The chrome around the pages: progress, Skip, Back and the big button. */
@Composable
fun OnboardingContent(
    step: Int,
    permissions: PermissionState,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    pageContent: @Composable () -> Unit
) {
    val last = OnboardingFlow.isLast(step)
    val enabled = !last || OnboardingFlow.canFinish(permissions)
    Column(Modifier.fillMaxSize().background(Ink).testTag("onboarding_screen")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.xl).padding(top = Space.xxl, bottom = Space.sm)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.weight(1f)) {
                repeat(OnboardingFlow.STEPS) { index ->
                    Box(
                        Modifier
                            .weight(if (index == step) 3f else 1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    index == step -> Accent
                                    index < step -> Accent.copy(alpha = 0.4f)
                                    else -> RaisedHighest
                                }
                            )
                    )
                }
            }
            Spacer(Modifier.width(Space.lg))
            Text(
                "Skip",
                color = AccentText,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.pressable(onClick = onSkip).padding(vertical = 8.dp).testTag("onboarding_skip")
            )
        }

        Box(Modifier.weight(1f)) { pageContent() }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg).padding(bottom = Space.xl, top = Space.sm)
        ) {
            if (step > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(RaisedHighest)
                        .pressable(onClick = onBack)
                        .testTag("onboarding_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextHigh)
                }
                Spacer(Modifier.width(Space.md))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (enabled) Accent else RaisedHighest)
                    .pressable(enabled = enabled, onClick = onNext)
                    .testTag("onboarding_next")
            ) {
                Text(
                    OnboardingFlow.primaryLabel(step),
                    color = if (enabled) OnAccent else TextMid,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!last) {
                    Spacer(Modifier.width(Space.sm))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = OnAccent, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** One step: its picture, its headline and what it says. Scrolls, so nothing is ever cut off. */
@Composable
fun OnboardingPage(index: Int, prefs: PreferencesManager) {
    when (index) {
        0 -> PageFrame("Speak. It writes.", "Say it out loud and the words land in whatever app you are typing in, cleaned up and ready to send.") { SpeakArt() }
        1 -> PageFrame("It shows up when you type", "Tap into any text field and a small orb appears beside it. When you are not typing, it is not there.") { ShowsUpArt() }
        2 -> PageFrame("Tap, hold, flick", "Tap to dictate. Hold to fan out your tones, then flick toward one. Double tap to translate.", art = { ArcArt() }) { GestureCards() }
        3 -> KeyPage(prefs)
        else -> PermissionsPage()
    }
}

@Composable
private fun PageFrame(
    title: String,
    body: String,
    art: (@Composable () -> Unit)? = null,
    extra: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
    ) {
        // With an explicit art slot the picture is that, and the trailing lambda is the card row
        // under the text; otherwise the trailing lambda is the picture.
        Box(Modifier.fillMaxWidth().height(310.dp), contentAlignment = Alignment.Center) {
            if (art != null) art() else extra()
        }
        Spacer(Modifier.height(Space.lg))
        Text(title, color = TextHigh, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(Space.md))
        Text(body, color = TextMid, style = MaterialTheme.typography.bodyLarge)
        if (art != null) {
            Spacer(Modifier.height(Space.lg))
            extra()
        }
        Spacer(Modifier.height(Space.xl))
    }
}

// ------------------------------------------------------------------ pictures

@Composable
private fun Blob(shape: Radial, size: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(color, RadialShape(shape)))
}

@Composable
private fun SpeakArt() {
    Box(Modifier.fillMaxSize()) {
        Blob(ExpressiveShapes.SoftBurst, 270.dp, AccentWash.copy(alpha = 0.55f), Modifier.align(Alignment.Center))
        Blob(ExpressiveShapes.Cookie9, 200.dp, AccentWash, Modifier.align(Alignment.Center))
        Blob(ExpressiveShapes.Cookie9, 128.dp, Accent, Modifier.align(Alignment.Center).offset(y = (-24).dp))
        Icon(Icons.Default.Mic, null, tint = OnAccent, modifier = Modifier.align(Alignment.Center).offset(y = (-24).dp).size(58.dp))
        Blob(ExpressiveShapes.Cookie9, 52.dp, TealWash, Modifier.align(Alignment.TopEnd))
        Blob(ExpressiveShapes.Clover4, 40.dp, GoodWash, Modifier.align(Alignment.CenterStart).offset(y = (-50).dp))
        Column(
            Modifier
                .align(Alignment.Center)
                .offset(x = 36.dp, y = 62.dp)
                .shadow(6.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(RaisedHigh)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text("Running ten minutes late,", color = TextHigh, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("start without me", color = TextLow, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(2.dp))
                Box(Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
            }
        }
        Bars(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
    }
}

/** A row of voice bars, tallest in the middle and fading to dots at the edges. */
@Composable
private fun Bars(modifier: Modifier = Modifier) {
    val heights = remember { listOf(0.1f, 0.1f, 0.14f, 0.2f, 0.32f, 0.5f, 0.45f, 0.7f, 0.62f, 0.85f, 1f, 0.8f, 0.9f, 0.6f, 0.75f, 0.4f, 0.55f, 0.3f, 0.2f, 0.14f, 0.1f, 0.1f) }
    Row(modifier.height(48.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        heights.forEachIndexed { i, h ->
            val edge = (i < 3 || i > heights.size - 4)
            Box(
                Modifier
                    .width(if (edge) 5.dp else 6.dp)
                    .height((48 * h).dp.coerceAtLeast(5.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(Accent.copy(alpha = if (edge) 0.35f else 0.85f))
            )
        }
    }
}

@Composable
private fun ShowsUpArt() {
    Box(Modifier.fillMaxSize()) {
        Blob(ExpressiveShapes.Flower, 96.dp, TealWash, Modifier.align(Alignment.TopEnd).offset(x = 0.dp, y = (-10).dp))
        Column(Modifier.fillMaxWidth(0.84f).align(Alignment.TopStart)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(36.dp))
                    .background(Raised)
                    .padding(Space.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(AccentWash, RadialShape(ExpressiveShapes.Cookie9)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Chat, null, tint = OnAccentWash, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(Space.md))
                    Column {
                        Box(Modifier.width(120.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(TextMid))
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.width(52.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(TextLow))
                    }
                }
                Spacer(Modifier.height(Space.md))
                Box(Modifier.width(150.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(RaisedHigh))
                Spacer(Modifier.height(Space.sm))
                Box(Modifier.align(Alignment.End).width(150.dp).height(28.dp).clip(RoundedCornerShape(14.dp)).background(AccentWash))
            }
            Spacer(Modifier.height(Space.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(RaisedHigh)
                    .border(2.dp, Accent, RoundedCornerShape(28.dp))
                    .padding(start = Space.lg)
            ) {
                Text("Message", color = TextMid, style = MaterialTheme.typography.bodyLarge)
                Box(Modifier.padding(start = 2.dp).width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
            }
            Spacer(Modifier.height(Space.md))
            KeyboardRows()
        }
        // The orb, hanging off the right end of the field.
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = (-12).dp, y = 148.dp).size(76.dp).background(Accent.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(58.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GraphicEq, null, tint = OnAccent, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun KeyboardRows() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(10, 9).forEach { count ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)) {
                repeat(count) { Box(Modifier.width(26.dp).height(38.dp).clip(RoundedCornerShape(9.dp)).background(Raised)) }
            }
        }
    }
}

@Composable
private fun ArcArt() {
    val tones = listOf(
        Triple("Summary", -6f, -122f),
        Triple("Clean up", -112f, -78f),
        Triple("Professional", 96f, -66f),
        Triple("Casual", -128f, -8f),
        Triple("Bullets", 114f, -8f)
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(300.dp, 250.dp)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 14f))
            drawArc(
                color = Color(0x66_5B45F0).copy(alpha = 0.35f),
                startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(size.width * 0.05f, size.height * 0.02f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.9f, size.height * 0.95f),
                style = Stroke(width = 4f, pathEffect = dash, cap = StrokeCap.Round)
            )
        }
        tones.forEach { (label, x, y) ->
            val selected = label == "Professional"
            Text(
                label,
                color = if (selected) OnAccent else TextHigh,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .then(if (selected) Modifier.shadow(8.dp, RoundedCornerShape(24.dp)) else Modifier)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (selected) Accent else RaisedHighest)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
        Box(Modifier.offset(y = 76.dp).size(90.dp).background(Accent.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GraphicEq, null, tint = OnAccent, modifier = Modifier.size(34.dp))
            }
        }
        Blob(ExpressiveShapes.Cookie9, 36.dp, TealWash, Modifier.align(Alignment.TopStart).offset(x = (-12).dp, y = (-6).dp))
        Blob(ExpressiveShapes.Flower, 30.dp, GoodWash, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp))
    }
}

@Composable
private fun GestureCards() {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        GestureCard(Icons.Default.TouchApp, "Tap", "Dictate", Modifier.weight(1f))
        GestureCard(Icons.Default.PanTool, "Hold", "Fan the tones", Modifier.weight(1f))
        GestureCard(Icons.Default.Swipe, "Flick", "Fire one", Modifier.weight(1f))
    }
}

@Composable
private fun GestureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, meaning: String, modifier: Modifier) {
    TonalCard(modifier = modifier, padding = PaddingValues(Space.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AccentText, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(4.dp))
        Text(meaning, color = TextMid, style = MaterialTheme.typography.bodyMedium)
    }
}

// ------------------------------------------------------------------ key step

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyPage(prefs: PreferencesManager) {
    val clipboard = LocalClipboardManager.current
    var provider by remember { mutableStateOf(prefs.getEngine().provider) }
    var key by remember(provider) { mutableStateOf(prefs.getProviderKey(provider)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
    ) {
        Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
            Blob(ExpressiveShapes.SoftBurst, 210.dp, TealWash, Modifier.align(Alignment.Center))
            Blob(ExpressiveShapes.Gem, 122.dp, Accent, Modifier.align(Alignment.Center))
            Icon(Icons.Default.Key, null, tint = OnAccent, modifier = Modifier.align(Alignment.Center).size(56.dp))
            Blob(ExpressiveShapes.Cookie9, 44.dp, AccentWash, Modifier.align(Alignment.TopStart).offset(x = 8.dp, y = 4.dp))
            Blob(ExpressiveShapes.Clover4, 40.dp, GoodWash, Modifier.align(Alignment.BottomEnd).offset(x = (-14).dp, y = (-8).dp))
        }
        Text("Bring your own key", color = TextHigh, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(Space.md))
        Text("Paste a key from the provider you like. No account with us, ever.", color = TextMid, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(Space.xl))
        Eyebrow("Provider")
        Spacer(Modifier.height(Space.sm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Provider.values().forEach { p ->
                M3Chip(
                    label = if (p == Provider.CUSTOM) "Custom" else p.label,
                    selected = p == provider,
                    onClick = { provider = p }
                )
            }
        }
        Spacer(Modifier.height(Space.xl))
        TonalTextField(
            value = key,
            onValueChange = {
                key = it.trim()
                prefs.setProviderKey(provider, key)
                if (key.isNotBlank()) useProviderForEverything(prefs, provider)
            },
            placeholder = provider.keyHint,
            label = "${provider.label} API key",
            visualTransformation = VisualTransformation.None,
            trailing = {
                AssistChip(
                    label = "Paste",
                    icon = Icons.Default.ContentPaste,
                    onClick = {
                        clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let {
                            key = it
                            prefs.setProviderKey(provider, it)
                            useProviderForEverything(prefs, provider)
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("onboarding_key")
        )
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = Space.sm)) {
            Icon(Icons.Default.Lock, null, tint = TextMid, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(Space.md))
            Text(
                provider.note.substringBefore(". ").trimEnd('.') + ". The key lives on this phone and calls the provider directly.",
                color = TextMid,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(Modifier.height(Space.xl))
    }
}

// ------------------------------------------------------------------ permissions

@Composable
private fun PermissionsPage() {
    val permissions by rememberPermissionState()
    val action = rememberPermissionAction()
    PermissionsPageContent(permissions, action)
}

@Composable
fun PermissionsPageContent(permissions: PermissionState, onAction: (Permission) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
    ) {
        Spacer(Modifier.height(Space.lg))
        Text("Four permissions", color = TextHigh, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(Space.md))
        Text(
            "Android asks you to switch these on yourself. Each one is for a single job.",
            color = TextMid,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(Space.xl))
        PermissionCards(permissions, onAction)
        Spacer(Modifier.height(Space.xl))
    }
}

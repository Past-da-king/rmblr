package io.github.pastdaking.rmblr.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.offset
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.orb.FieldWatcherService
import io.github.pastdaking.rmblr.orb.WaveMark
import io.github.pastdaking.rmblr.ui.components.PrimaryAction
import io.github.pastdaking.rmblr.ui.components.QuietAction
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Alert
import io.github.pastdaking.rmblr.ui.theme.Good
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Line
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Whether the operator has been through onboarding. */
class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("rmblr_onboarding", Context.MODE_PRIVATE)
    var done: Boolean
        get() = prefs.getBoolean("done", false)
        set(value) = prefs.edit().putBoolean("done", value).apply()
}

/**
 * First run.
 *
 * Every page shows the real component doing the real thing rather than describing it: the
 * actual orb, the actual arc, the actual waveform. A screenshot of a gesture teaches
 * nothing; watching the thing move does.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(pageCount = { 7 })

    Column(modifier = Modifier.fillMaxSize().background(Ink)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> Page(
                    art = { SpeakItWritesArt() },
                    title = "Speak. It writes.",
                    body = "RMBLR turns what you say into clean, finished text, straight into whatever you were typing in. It can translate as it goes. No copying, no pasting."
                )
                1 -> Page(
                    art = { OrbArt() },
                    title = "It shows up when you type",
                    body = "Tap any text box in any app and the orb appears next to it. Nowhere else. It is never sitting on your home screen getting in the way."
                )
                2 -> Page(
                    art = { GestureArt(cycling = true) },
                    title = "Tap, hold, flick",
                    body = "Tap to dictate. Hold to choose a tone from the arc. Flick straight at one to skip the menu. Double tap to switch it between writing and translating. Drag it anywhere and it stays put."
                )
                3 -> Page(
                    art = { ProfileArt() },
                    title = "It knows where you are",
                    body = "WhatsApp gets casual. Gmail gets professional. Slack gets somewhere in between. Pick the apps and the tones once, in Profiles."
                )
                4 -> Page(
                    art = { TranslateArt() },
                    title = "Say it in one language, write it in another",
                    body = "Double tap the orb and it turns into a translator. Hold it and the arc shows your languages instead of your tones — flick at one and what you say lands there. Copy something in an app and tap the orb to read it back in your language."
                )
                5 -> PermissionsPage()
                6 -> ApiKeyPage()
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl)
                .padding(bottom = Space.xxl, top = Space.lg)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.weight(1f)) {
                repeat(7) { index ->
                    Box(
                        modifier = Modifier
                            .size(width = if (index == pager.currentPage) 20.dp else 7.dp, height = 7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (index == pager.currentPage) Accent else Line)
                    )
                }
            }

            if (pager.currentPage < 6) {
                QuietAction("Skip", onClick = onDone)
                Spacer(Modifier.width(Space.sm))
                PrimaryAction("Next", onClick = {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                })
            } else {
                PrimaryAction("Start using it", onClick = onDone)
            }
        }
    }
}

@Composable
private fun Page(art: @Composable () -> Unit, title: String, body: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.xl)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { art() }

        Text(
            text = title,
            color = TextHigh,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Space.md))
        Text(
            text = body,
            color = TextMid,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Space.xxl)
        )
    }
}

/** The mark, alive: speech arriving as bars. */
@Composable
private fun WaveformArt() {
    val motion = rememberInfiniteTransition(label = "wave")
    val t by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "wave_t"
    )

    Canvas(modifier = Modifier.size(width = 220.dp, height = 120.dp)) {
        val bars = 9
        val gap = size.width / (bars * 2f)
        val barWidth = gap
        for (i in 0 until bars) {
            val centreBias = 1f - kotlin.math.abs(i - bars / 2f) / (bars / 2f)
            val wobble = kotlin.math.sin((t * 6.28f) + i).toFloat() * 0.25f + 0.75f
            val h = size.height * (0.18f + centreBias * 0.7f * wobble)
            val x = gap + i * (barWidth + gap)
            drawLine(
                color = if (i == bars / 2) Accent else Accent.copy(alpha = 0.55f),
                start = Offset(x, size.height / 2 - h / 2),
                end = Offset(x, size.height / 2 + h / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/** The orb sitting beside a text field, which is the whole promise of the app. */
@Composable
private fun OrbArt() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .width(180.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Surface)
                    .padding(horizontal = Space.lg)
            ) {
                Box(modifier = Modifier.size(width = 2.dp, height = 20.dp).background(Accent))
            }
            Spacer(Modifier.width(Space.md))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(Raised)
            ) {
                WaveMark(size = 24.dp, tint = TextHigh)
            }
        }
    }
}

/**
 * The arc, laid out the way it really opens.
 *
 * Positioned from a single centre point with trigonometry rather than hand-tuned padding,
 * which is what made the first version collide with itself: every chip was nudged
 * independently and they piled up on the right.
 */
@Composable
private fun GestureArt(cycling: Boolean = false) {
    // The arc is the same arc either way, which is the point: a double tap swaps what is
    // on it. Showing tones only, as this page used to, taught half the gesture.
    var translating by remember { mutableStateOf(false) }
    if (cycling) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(2600)
                translating = !translating
            }
        }
    }

    val labels = if (translating) LANGUAGES_ON_ARC else listOf("Professional", "Clean up", "Casual", "Bullets", "Summary")
    val radius = 118f
    val chipWidth = 112.dp
    val chipHeight = 32.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(width = 320.dp, height = 300.dp)) {
        labels.forEachIndexed { index, label ->
            val degrees = -72f + index * 36f
            val radians = Math.toRadians(degrees.toDouble())
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(
                        x = (radius * kotlin.math.cos(radians)).dp,
                        y = (radius * kotlin.math.sin(radians)).dp
                    )
                    .width(chipWidth)
                    .height(chipHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (index == 2) Accent else Surface)
            ) {
                Crossfade(targetState = label, label = "arc_chip") { shown ->
                    Text(
                        text = shown,
                        color = if (index == 2) OnAccent else TextMid,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }

        Crossfade(targetState = translating, label = "arc_centre") { translate ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).clip(CircleShape).background(if (translate) Accent else Alert)
            ) {
                if (translate) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = OnAccent,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    WaveMark(size = 26.dp, tint = OnAccent)
                }
            }
        }
    }
}

/** Two apps, two tones: the profiles idea in one picture. */
@Composable
private fun ProfileArt() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.md)) {
        ProfileRow("Chat apps", "Casual", Accent)
        ProfileRow("Email", "Professional", Good)
        ProfileRow("Work chat", "Clean up", TextMid)
    }
}

/** The languages the arc and the first page cycle through. */
private val LANGUAGES_ON_ARC = listOf("Espa\u00f1ol", "\u65e5\u672c\u8a9e", "Fran\u00e7ais", "isiZulu", "\u4e2d\u6587")

/**
 * Speech going in, and the writing coming out in a language that keeps changing.
 *
 * The first page used to promise transcription and nothing else, which made translation
 * read as an afterthought bolted on at the end of onboarding. The same sentence rewriting
 * itself in six languages says the whole app in one glance, before a word of body copy.
 */
@Composable
private fun SpeakItWritesArt() {
    val written = listOf(
        "Good morning" to TextHigh,
        "Buenos d\u00edas" to Accent,
        "\u304a\u306f\u3088\u3046" to Accent,
        "Bonjour" to Accent,
        "Sawubona" to Accent,
        "\u65e9\u4e0a\u597d" to Accent
    )
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1700)
            index = (index + 1) % written.size
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WaveformArt()
        Spacer(Modifier.height(Space.xl))
        Crossfade(targetState = index, label = "written") { i ->
            val (text, tint) = written[i]
            Text(
                text = text,
                color = tint,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .padding(horizontal = Space.xl, vertical = Space.md)
            )
        }
    }
}

/**
 * The orb between two languages.
 *
 * Drawn rather than described because the point of translate mode is that it is the same
 * orb doing a second job, and a picture of one orb with a language either side says that
 * faster than a paragraph can.
 */
@Composable
private fun TranslateArt() {
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            index = (index + 1) % LANGUAGES_ON_ARC.size
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        LanguageChip("English", TextMid, Surface)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Accent)
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = OnAccent,
                modifier = Modifier.size(30.dp)
            )
        }
        Crossfade(targetState = index, label = "target_language") { i ->
            LanguageChip(LANGUAGES_ON_ARC[i], OnAccent, Accent)
        }
    }
}

@Composable
private fun LanguageChip(label: String, tint: androidx.compose.ui.graphics.Color, fill: androidx.compose.ui.graphics.Color) {
    Text(
        text = label,
        color = tint,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .padding(horizontal = Space.lg, vertical = Space.sm)
    )
}

@Composable
private fun ProfileRow(app: String, tone: String, tint: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(Space.lg)
    ) {
        Text(app, color = TextHigh, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = tone,
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AccentWash)
                .padding(horizontal = Space.sm, vertical = 4.dp)
        )
    }
}

/** The last page does the setup rather than telling you to go and find it. */
@Composable
private fun PermissionsPage() {
    val context = LocalContext.current
    var canOverlay by remember { mutableStateOf(false) }
    var canWatch by remember { mutableStateOf(false) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                canWatch = accessibilityOn(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(horizontal = Space.xl)
    ) {
        Spacer(Modifier.height(Space.xxl))
        Text("Two switches", color = TextHigh, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Space.md))
        Text(
            text = "Android will not let any app know you are typing, or write into another app's text box, without these. Nothing works until they are on.",
            color = TextMid,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Space.xxl))

        PermissionCard(
            title = "Draw over other apps",
            body = "Lets the orb sit above whatever you are typing in.",
            granted = canOverlay,
            onOpen = {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            }
        )

        Spacer(Modifier.height(Space.md))

        PermissionCard(
            title = "RMBLR orb, under Accessibility",
            body = "Lets it notice a text field and write your words into it.",
            granted = canWatch,
            onOpen = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        )
    }
}

@Composable
private fun PermissionCard(title: String, body: String, granted: Boolean, onOpen: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable(enabled = !granted) { onOpen() }
            .padding(Space.lg)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
            Text(body, color = TextMid, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(Space.md))
        if (granted) {
            Icon(Icons.Default.CheckCircle, contentDescription = "On", tint = Good, modifier = Modifier.size(24.dp))
        } else {
            Text("Open", color = Accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun accessibilityOn(context: Context): Boolean {
    val expected = "${context.packageName}/${FieldWatcherService::class.java.name}"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

/**
 * The last thing, because nothing works without it.
 *
 * The app talks to Gemini directly on your own key rather than through a server of ours,
 * which means no account, no subscription and no one else holding your dictation. It also
 * means we have to actually ask for the key instead of hoping it is already there.
 */
@Composable
private fun ApiKeyPage() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    var key by remember { mutableStateOf(prefs.getUserApiKey()) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
    ) {
        Spacer(Modifier.height(Space.xxl))

        WaveMark(size = 48.dp, tint = Accent)

        Spacer(Modifier.height(Space.xl))

        Text("One key to finish", color = TextHigh, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(Space.md))
        Text(
            text = "RMBLR speaks to Gemini on your own API key. Nothing runs through a server of ours, so there is no account and no subscription, and your dictation is between you and the model. Groq, Mistral, OpenRouter and anything OpenAI-shaped work too — set those up later in Models and keys.",
            color = TextMid,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(Space.xl))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .padding(Space.lg)
        ) {
            Text("Where to get one", color = TextHigh, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "Open Google AI Studio, sign in, then Get API key. It is free to start and takes about a minute.",
                color = TextMid,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(Space.md))
            PrimaryAction(
                text = "Open AI Studio",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(Space.lg))

        OutlinedTextField(
            value = key,
            onValueChange = {
                key = it.trim()
                prefs.setUserApiKey(key)
                saved = key.isNotBlank()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Line,
                focusedTextColor = TextHigh,
                unfocusedTextColor = TextHigh,
                cursorColor = Accent,
                focusedContainerColor = Raised,
                unfocusedContainerColor = Raised
            ),
            placeholder = { Text("Paste your key, AIzaSy...", color = TextLow, style = MaterialTheme.typography.bodyMedium) }
        )

        Spacer(Modifier.height(Space.sm))
        Text(
            text = if (saved) "Saved. You can change it any time in Settings." else "You can add it later in Settings, but nothing will transcribe until you do.",
            color = if (saved) Good else TextLow,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(Space.xxl))
    }
}

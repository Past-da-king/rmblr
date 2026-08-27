package io.github.pastdaking.rmblr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import io.github.pastdaking.rmblr.orb.OrbOverlayService
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import io.github.pastdaking.rmblr.ui.components.Radius
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.theme.Line
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.pastdaking.rmblr.data.HistoryRepository
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.ui.components.PrimaryAction
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.screens.HistoryScreen
import io.github.pastdaking.rmblr.ui.screens.HomeScreen
import io.github.pastdaking.rmblr.ui.screens.SettingsScreen
import io.github.pastdaking.rmblr.ui.screens.OnboardingPrefs
import io.github.pastdaking.rmblr.ui.screens.OnboardingScreen
import io.github.pastdaking.rmblr.ui.screens.ProfilesScreen
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.MyApplicationTheme
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var historyRepo: HistoryRepository

    /**
     * Start the orb again if the switch says it should be running.
     *
     * The switch stores a preference and starts the service in the same breath, which
     * looks correct until the process dies — a reinstall, a reboot, Android reclaiming
     * memory. The preference survives, the service does not, so the switch sat there
     * reading ON while nothing was listening, and the only way back was to toggle it off
     * and on again. Reconciling the two every time the app is opened costs nothing:
     * starting a foreground service that is already running is a no-op.
     */
    private fun syncOrbService() {
        if (!prefsManager.isFloatingAssistantEnabled()) return
        val canOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(this)
        if (!canOverlay) return
        val intent = Intent(this, OrbOverlayService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        syncOrbService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefsManager = PreferencesManager.getInstance(this)
        historyRepo = HistoryRepository.getInstance(this)

        setContent {
            MyApplicationTheme {
                val onboardingPrefs = remember { OnboardingPrefs(this@MainActivity) }
                var onboarded by remember { mutableStateOf(onboardingPrefs.done) }
                if (!onboarded) {
                    OnboardingScreen(onDone = {
                        onboardingPrefs.done = true
                        onboarded = true
                    })
                    return@MyApplicationTheme
                }

                var selectedTabIndex by remember { mutableIntStateOf(0) }
                var hasMicPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    hasMicPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
                }

                LaunchedEffect(Unit) {
                    val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        needed.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val notGranted = needed.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (notGranted.isNotEmpty()) {
                        permissionLauncher.launch(notGranted.toTypedArray())
                    }
                }

                val navItems = listOf(
                    NavItem("Orb", Icons.Default.Layers, "nav_tab_hub"),
                    NavItem("Profiles", Icons.Default.Tune, "nav_tab_studio"),
                    NavItem("History", Icons.Default.History, "nav_tab_history"),
                    NavItem("Settings", Icons.Default.Settings, "nav_tab_settings")
                )

                Scaffold(
                    containerColor = Ink,
                    bottomBar = {
                        FloatingNav(
                            items = navItems,
                            selected = selectedTabIndex,
                            onSelect = { selectedTabIndex = it }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Only the TOP inset is applied. Taking the bottom one as well
                    // ended the content in a hard horizontal edge with the bar sitting
                    // below it on a bare strip — which is exactly what a floating bar
                    // must not look like. The screens run full height and pass behind
                    // it instead; each one carries Space.navClear at the end of its own
                    // scroll so nothing is stranded underneath.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Ink)
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        if (!hasMicPermission) {
                            MicPermissionBanner(
                                onGrant = {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTabIndex) {
                                0 -> HomeScreen(
                                    prefsManager = prefsManager,
                                    historyRepo = historyRepo,
                                    onNavigateToSettings = { selectedTabIndex = 3 }
                                )
                                1 -> ProfilesScreen()
                                2 -> HistoryScreen(historyRepo = historyRepo)
                                3 -> SettingsScreen(prefsManager = prefsManager)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The bottom navigation, as a bar that floats rather than one bolted to the edge.
 *
 * A full-bleed navigation bar reads as chrome — the part of the screen the app did not
 * design. Lifting it off the edges, rounding it fully and letting the selected tab
 * spread into a filled pill is the Material 3 Expressive treatment, and it is the single
 * change that does most to stop this looking like a settings app.
 *
 * The selected tab is the only one that carries its label. Four words competing at the
 * bottom of every screen is noise; one word telling you where you are is navigation.
 */
@Composable
private fun FloatingNav(
    items: List<NavItem>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            modifier = Modifier
                .clip(RoundedCornerShape(Radius.pill))
                .background(Surface)
                .border(1.dp, Line, RoundedCornerShape(Radius.pill))
                .padding(Space.sm)
                .testTag("main_bottom_nav")
        ) {
            items.forEachIndexed { index, item ->
                NavPill(
                    item = item,
                    selected = selected == index,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun NavPill(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    // Springs, not curves: the pill overshoots a touch as it takes the label in, which
    // is what makes the switch feel like a physical thing moving rather than a redraw.
    val weight by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "nav_pill"
    )
    val tint = lerp(TextLow, OnAccent, weight)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.pill))
            .background(lerp(Surface, Accent, weight))
            .padding(horizontal = (12 + 6 * weight).dp, vertical = 12.dp)
            .testTag(item.testTag)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        if (weight > 0.01f) {
            Spacer(Modifier.width((6 * weight).dp))
            Text(
                text = item.title,
                color = tint,
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.graphicsLayer { alpha = weight }
            )
        }
    }
}

@Composable
private fun MicPermissionBanner(onGrant: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(AccentWash)
            .padding(horizontal = Space.lg, vertical = Space.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Space.md))
            Text(
                text = "RMBLR needs the microphone to dictate.",
                color = TextHigh,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.width(Space.md))
        PrimaryAction(text = "Allow", onClick = onGrant)
    }
}

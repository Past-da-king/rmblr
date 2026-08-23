package io.github.pastdaking.rmblr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
                        NavigationBar(
                            containerColor = Surface,
                            contentColor = TextHigh,
                            tonalElevation = 0.dp,
                            modifier = Modifier.testTag("main_bottom_nav")
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val isSelected = selectedTabIndex == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTabIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Accent,
                                        selectedTextColor = Accent,
                                        unselectedIconColor = TextLow,
                                        unselectedTextColor = TextLow,
                                        indicatorColor = AccentWash
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Ink)
                            .padding(innerPadding)
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
                                    onNavigateToVoiceStudio = { selectedTabIndex = 1 },
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

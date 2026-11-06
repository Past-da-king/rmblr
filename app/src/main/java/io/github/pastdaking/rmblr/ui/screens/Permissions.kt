package io.github.pastdaking.rmblr.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.pastdaking.rmblr.orb.FieldWatcherService
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.ExpressiveShapes
import io.github.pastdaking.rmblr.ui.m3.Radial
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.AlertWash
import io.github.pastdaking.rmblr.ui.theme.GoodWash
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.OnAlertWash
import io.github.pastdaking.rmblr.ui.theme.OnGoodWash
import io.github.pastdaking.rmblr.ui.theme.OnSecondaryWash
import io.github.pastdaking.rmblr.ui.theme.SecondaryWash
import io.github.pastdaking.rmblr.ui.theme.TealWash
import io.github.pastdaking.rmblr.ui.theme.Teal
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid

/** The four things Android makes the person switch on for themselves. */
enum class Permission { MICROPHONE, OVERLAY, ACCESSIBILITY, NOTIFICATIONS }

/** Which of them are on right now. A plain value, so the rules about it can be tested. */
data class PermissionState(
    val microphone: Boolean,
    val overlay: Boolean,
    val accessibility: Boolean,
    val notifications: Boolean
) {
    fun has(p: Permission): Boolean = when (p) {
        Permission.MICROPHONE -> microphone
        Permission.OVERLAY -> overlay
        Permission.ACCESSIBILITY -> accessibility
        Permission.NOTIFICATIONS -> notifications
    }

    /** Everything the orb needs to work end to end. */
    val allGranted: Boolean get() = microphone && overlay && accessibility && notifications

    /** What is still to do, in the order the screen lists them. */
    val missing: List<Permission> get() = Permission.values().filterNot { has(it) }

    /** The orb can only be placed and write text back with these two. */
    val orbReady: Boolean get() = overlay && accessibility
}

/** Whether the person has switched our accessibility service on in Android settings. */
fun isAccessibilityOn(context: Context): Boolean {
    val expected = "${context.packageName}/${FieldWatcherService::class.java.name}"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

fun readPermissions(context: Context): PermissionState {
    val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val overlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    return PermissionState(mic, overlay, isAccessibilityOn(context), notifications)
}

/**
 * The live state of the four permissions.
 *
 * Two of them are switched on inside Android's own settings, so the person leaves the app
 * to do it and comes back. Re-reading every time the screen resumes is what makes a
 * "Needed" turn into "Granted" the moment they return, with nothing to press.
 */
@Composable
fun rememberPermissionState(): State<PermissionState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(readPermissions(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.value = readPermissions(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return state
}

/** Android's own page for this app, for a permission it will no longer ask about in a dialog. */
private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/**
 * What tapping a permission's button does.
 *
 * Microphone and notifications are asked for in Android's own dialog; if the person has
 * already refused twice Android stops showing it, so the same button then opens this
 * app's page in system settings. Draw-over-apps and accessibility have no dialog at all,
 * only a settings page, which is where they are sent.
 */
@Composable
fun rememberPermissionAction(refresh: () -> Unit = {}): (Permission) -> Unit {
    val context = LocalContext.current
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context as? Activity
        if (!granted && activity != null &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        ) {
            openAppSettings(context)
        }
        refresh()
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context as? Activity
        if (!granted && activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            openAppSettings(context)
        }
        refresh()
    }
    return { permission ->
        when (permission) {
            Permission.MICROPHONE -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            Permission.NOTIFICATIONS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            Permission.OVERLAY -> runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            Permission.ACCESSIBILITY -> runCatching {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

private class PermissionLook(
    val title: String,
    val body: String,
    val action: String,
    val icon: ImageVector,
    val shape: Radial,
    val container: Color,
    val tint: Color
)

@Composable
private fun lookOf(p: Permission): PermissionLook = when (p) {
    Permission.MICROPHONE -> PermissionLook(
        "Microphone", "To hear you. Audio goes only to the provider you chose.", "Grant",
        Icons.Default.Mic, ExpressiveShapes.Flower, AccentWash, OnAccentWash
    )
    Permission.OVERLAY -> PermissionLook(
        "Draw over other apps", "To float the orb above the app you are typing in.", "Open",
        Icons.Default.Layers, ExpressiveShapes.Clover4, TealWash, Teal
    )
    Permission.ACCESSIBILITY -> PermissionLook(
        "Accessibility", "Service named RMBLR orb. Notices a focused text field and writes your words back into it.", "Open",
        Icons.Default.Accessibility, ExpressiveShapes.Gem, AccentWash, OnAccentWash
    )
    Permission.NOTIFICATIONS -> PermissionLook(
        "Notifications", "To show a Stop button while it is listening.", "Grant",
        Icons.Default.NotificationsActive, ExpressiveShapes.Puffy, GoodWash, OnGoodWash
    )
}

/** The four permission cards, each with its live state and a real action. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PermissionCards(
    state: PermissionState,
    onAction: (Permission) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Permission.values().forEach { p ->
            val look = lookOf(p)
            val granted = state.has(p)
            TonalCard(
                shape = RoundedCornerShape(28.dp),
                padding = androidx.compose.foundation.layout.PaddingValues(Space.lg)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShapedIcon(look.icon, look.shape, look.container, look.tint, size = 56.dp)
                    Spacer(Modifier.width(Space.lg))
                    Column(Modifier.weight(1f)) {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(look.title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                            PermissionChip(granted)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(look.body, color = TextMid, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!granted) {
                        Spacer(Modifier.width(Space.md))
                        Text(
                            look.action,
                            color = OnAccent,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .pressable(onClick = { onAction(p) })
                                .clip(RoundedCornerShape(999.dp))
                                .background(Accent)
                                .padding(horizontal = 22.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionChip(granted: Boolean) {
    val container = if (granted) GoodWash else AlertWash
    val content = if (granted) OnGoodWash else OnAlertWash
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        if (granted) {
            Icon(Icons.Default.Check, null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(if (granted) "Granted" else "Needed", color = content, style = MaterialTheme.typography.labelLarge)
    }
}

package io.github.pastdaking.rmblr.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.orb.AppProfile
import io.github.pastdaking.rmblr.orb.AppProfileStore
import io.github.pastdaking.rmblr.orb.Tone
import io.github.pastdaking.rmblr.orb.ToneStore
import io.github.pastdaking.rmblr.ui.components.Hairline
import io.github.pastdaking.rmblr.ui.components.Panel
import io.github.pastdaking.rmblr.ui.components.PrimaryAction
import io.github.pastdaking.rmblr.ui.components.QuietAction
import io.github.pastdaking.rmblr.ui.components.Radius
import io.github.pastdaking.rmblr.ui.components.SectionLabel
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.Raised
import io.github.pastdaking.rmblr.ui.theme.Surface
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextLow
import io.github.pastdaking.rmblr.ui.theme.TextMid

private data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap?
)

/** The real launcher icon, so a list of apps looks like your phone rather than a list of ids. */
private fun iconFor(context: Context, packageName: String): ImageBitmap? = runCatching {
    val drawable = context.packageManager.getApplicationIcon(packageName)
    drawable.toBitmap(width = 96, height = 96).asImageBitmap()
}.getOrNull()

/** Apps you could plausibly be typing in: anything with a launcher entry. */
private fun installedApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return runCatching {
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolved ->
                val info: ApplicationInfo = resolved.activityInfo?.applicationInfo ?: return@mapNotNull null
                InstalledApp(
                    label = pm.getApplicationLabel(info).toString(),
                    packageName = info.packageName,
                    icon = null
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}

/**
 * One app icon, decoded when its row is actually on screen.
 *
 * Decoding ninety of them up front is what made opening Profiles stutter: it was all
 * happening on the main thread before the first frame could be drawn.
 */
@Composable
private fun rememberIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = withContext(Dispatchers.IO) { iconFor(context, packageName) }
    }
    return icon
}

@Composable
private fun AppIcon(icon: ImageBitmap?, modifier: Modifier = Modifier) {
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = modifier.size(26.dp))
    } else {
        Box(
            modifier = modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Raised)
        )
    }
}

/**
 * Where the per-app behaviour is actually configured.
 *
 * A profile is a set of apps plus the five tones the orb offers in them. Slack and Gmail
 * should not hand you the same options as WhatsApp, and this is where you say so.
 */
@Composable
fun ProfilesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { AppProfileStore(context) }
    val toneStore = remember { ToneStore(context) }
    var profiles by remember { mutableStateOf(store.load()) }
    var tones by remember { mutableStateOf(toneStore.load()) }
    var editing by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var addingProfile by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var addingTone by remember { mutableStateOf(false) }
    var newToneName by remember { mutableStateOf("") }
    var newTonePrompt by remember { mutableStateOf("") }

    // Reading the launcher list off the main thread keeps the tab switch instant.
    var installedList by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    LaunchedEffect(Unit) {
        installedList = withContext(Dispatchers.IO) { installedApps(context) }
    }

    fun update(changed: AppProfile) {
        profiles = profiles.map { if (it.id == changed.id) changed else it }
        store.save(profiles)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl)
            .padding(top = Space.xl, bottom = Space.xxl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Profiles",
                color = TextHigh,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (adding) Accent else Raised)
                    .clickable { adding = !adding }
            ) {
                Icon(
                    imageVector = if (adding) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Add",
                    tint = if (adding) io.github.pastdaking.rmblr.ui.theme.OnAccent else TextHigh,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            text = "Pick the apps, then pick the five tones the orb offers in them.",
            color = TextMid,
            style = MaterialTheme.typography.bodyMedium
        )

        if (adding) {
            Spacer(Modifier.height(Space.lg))
            Panel {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.control))
                        .clickable { adding = false; addingProfile = true; addingTone = false }
                        .padding(Space.md)
                ) {
                    Column {
                        Text("New profile", color = TextHigh, style = MaterialTheme.typography.bodyMedium)
                        Text("A set of apps and the tones they get", color = TextMid, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Hairline(Modifier.padding(vertical = Space.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.control))
                        .clickable { adding = false; addingTone = true; addingProfile = false }
                        .padding(Space.md)
                ) {
                    Column {
                        Text("New tone", color = TextHigh, style = MaterialTheme.typography.bodyMedium)
                        Text("An instruction for what to do with your words", color = TextMid, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (addingTone) {
            Panel {
                Text("New tone", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "A tone is the instruction given to the model. Write it as an order: what to do with the words you just said.",
                    color = TextMid,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(Space.md))
                PlainField(newToneName, { newToneName = it }, "Name, e.g. LinkedIn post")
                Spacer(Modifier.height(Space.sm))
                PlainField(
                    value = newTonePrompt,
                    onValueChange = { newTonePrompt = it },
                    placeholder = "Rewrite this as a short LinkedIn post. Confident, no hashtags. Output only the post.",
                    height = 110.dp
                )
                Spacer(Modifier.height(Space.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    PrimaryAction(
                        text = "Save tone",
                        enabled = newToneName.isNotBlank() && newTonePrompt.isNotBlank(),
                        onClick = {
                            toneStore.add(newToneName, newTonePrompt)
                            tones = toneStore.load()
                            newToneName = ""
                            newTonePrompt = ""
                            addingTone = false
                        }
                    )
                    QuietAction("Cancel", onClick = { addingTone = false })
                }
            }
            Spacer(Modifier.height(Space.lg))
        }

        if (addingProfile) {
            Panel {
                Text("New profile", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Space.sm))
                PlainField(newProfileName, { newProfileName = it }, "Name, e.g. Clients")
                Spacer(Modifier.height(Space.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    PrimaryAction("Create", enabled = newProfileName.isNotBlank(), onClick = {
                        store.add(newProfileName)
                        profiles = store.load()
                        newProfileName = ""
                        addingProfile = false
                    })
                    QuietAction("Cancel", onClick = { addingProfile = false })
                }
            }
            Spacer(Modifier.height(Space.lg))
        }

        profiles.forEachIndexed { index, profile ->
            if (index > 0) Spacer(Modifier.height(Space.md))
            ProfileCard(
                profile = profile,
                expanded = editing == profile.id,
                onToggle = { editing = if (editing == profile.id) null else profile.id },
                onChange = ::update,
                installed = installedList,
                tones = tones,
                onDelete = {
                    store.delete(profile.id)
                    profiles = store.load()
                    editing = null
                },
                onNewTone = { addingTone = true }
            )
        }


    }
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    height: androidx.compose.ui.unit.Dp? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier),
        shape = RoundedCornerShape(Radius.control),
        singleLine = height == null,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = io.github.pastdaking.rmblr.ui.theme.Line,
            focusedTextColor = TextHigh,
            unfocusedTextColor = TextHigh,
            cursorColor = Accent,
            focusedContainerColor = Raised,
            unfocusedContainerColor = Raised
        ),
        placeholder = { Text(placeholder, color = TextLow, style = MaterialTheme.typography.bodyMedium) }
    )
}

@Composable
private fun ProfileCard(
    profile: AppProfile,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (AppProfile) -> Unit,
    installed: List<InstalledApp>,
    tones: List<Tone>,
    onDelete: () -> Unit,
    onNewTone: () -> Unit
) {
    val context = LocalContext.current
    var pickingApps by remember { mutableStateOf(false) }
    var slotBeingSet by remember { mutableStateOf<String?>(null) }

    Panel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (profile.packages.isEmpty()) {
                        "Used everywhere you have not assigned"
                    } else {
                        "${profile.packages.size} app${if (profile.packages.size == 1) "" else "s"}"
                    },
                    color = TextMid,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = if (expanded) "Done" else "Edit",
                color = Accent,
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (!expanded) return@Panel

        Spacer(Modifier.height(Space.lg))
        Hairline()
        Spacer(Modifier.height(Space.lg))

        Text("Apps", color = TextLow, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(Space.sm))

        if (profile.packages.isEmpty()) {
            Text(
                text = "No apps. This is the fallback.",
                color = TextMid,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            profile.packages.forEach { pkg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                ) {
                    AppIcon(rememberIcon(pkg))
                    Spacer(Modifier.width(Space.md))
                    Text(
                        text = labelFor(context, pkg),
                        color = TextHigh,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TextLow,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onChange(profile.copy(packages = profile.packages - pkg)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.md))
        QuietAction("Add apps", onClick = { pickingApps = true }, icon = Icons.Default.Add)

        Spacer(Modifier.height(Space.lg))
        Hairline()
        Spacer(Modifier.height(Space.lg))

        Text("Tones on the arc", color = TextLow, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(Space.sm))

        SlotRow("Top", toneName(tones, profile.up)) { slotBeingSet = "up" }
        SlotRow("Upper", toneName(tones, profile.left)) { slotBeingSet = "left" }
        SlotRow("Middle", toneName(tones, profile.tap)) { slotBeingSet = "tap" }
        SlotRow("Lower", toneName(tones, profile.right)) { slotBeingSet = "right" }
        SlotRow("Bottom", toneName(tones, profile.down)) { slotBeingSet = "down" }

        Spacer(Modifier.height(Space.sm))
        Text(
            text = "Middle is also what a plain tap uses.",
            color = TextLow,
            style = MaterialTheme.typography.bodySmall
        )

        if (profile.packages.isNotEmpty()) {
            Spacer(Modifier.height(Space.lg))
            QuietAction("Delete this profile", onClick = onDelete, icon = Icons.Default.Close)
        }
    }

    if (pickingApps) {
        AppPicker(
            apps = installed,
            chosen = profile.packages.toSet(),
            onDone = { pickingApps = false },
            onToggle = { pkg ->
                val next = if (pkg in profile.packages) profile.packages - pkg else profile.packages + pkg
                onChange(profile.copy(packages = next))
            }
        )
    }

    slotBeingSet?.let { slot ->
        TonePicker(
            tones = tones,
            onNewTone = { slotBeingSet = null; onNewTone() },
            onDismiss = { slotBeingSet = null },
            onPick = { preset ->
                onChange(
                    when (slot) {
                        "up" -> profile.copy(up = preset.id)
                        "left" -> profile.copy(left = preset.id)
                        "right" -> profile.copy(right = preset.id)
                        "down" -> profile.copy(down = preset.id)
                        else -> profile.copy(tap = preset.id)
                    }
                )
                slotBeingSet = null
            }
        )
    }
}

private fun labelFor(context: Context, pkg: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
}.getOrDefault(pkg)

@Composable
private fun toneName(tones: List<Tone>, id: String): String =
    tones.firstOrNull { it.id == id }?.name ?: "Clean & Smooth"

@Composable
private fun SlotRow(position: String, toneLabel: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.control))
            .clickable { onClick() }
            .padding(vertical = Space.sm)
    ) {
        Text(position, color = TextLow, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp))
        Text(toneLabel, color = TextHigh, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("Change", color = Accent, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TonePicker(
    tones: List<Tone>,
    onNewTone: () -> Unit,
    onDismiss: () -> Unit,
    onPick: (Tone) -> Unit
) {
    Sheet(title = "Pick a tone", onDismiss = onDismiss) {
        tones.forEach { tone ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.control))
                    .clickable { onPick(tone) }
                    .padding(Space.md)
            ) {
                Text(tone.name, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
                Text(tone.description, color = TextMid, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(Space.sm))
        QuietAction("Write a new tone", onClick = onNewTone, icon = Icons.Default.Add)
    }
}

@Composable
private fun AppPicker(
    apps: List<InstalledApp>,
    chosen: Set<String>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit
) {
    Sheet(title = "Choose apps", onDismiss = onDone) {
        LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp)) {
            items(apps) { app ->
                val selected = app.packageName in chosen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.control))
                        .clickable { onToggle(app.packageName) }
                        .padding(Space.md)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Accent else Raised)
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.width(Space.md))
                    AppIcon(rememberIcon(app.packageName))
                    Spacer(Modifier.width(Space.md))
                    Text(
                        app.label,
                        color = TextHigh,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.md))
        PrimaryAction("Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * A plain in-place panel rather than a dialog: the app is one column, keep it that way.
 *
 * Deliberately NOT scrollable. The whole screen already scrolls, and a vertically
 * scrolling child inside a vertically scrolling parent is measured with infinite height,
 * which Compose refuses outright: that is what crashed the app the moment a tone picker
 * opened.
 */
@Composable
private fun Sheet(
    title: String,
    onDismiss: () -> Unit,
    scrollable: Boolean = false,
    content: @Composable () -> Unit
) {
    Spacer(Modifier.height(Space.md))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.panel))
            .background(AccentWash)
            .padding(Space.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = TextMid,
                modifier = Modifier.size(20.dp).clickable { onDismiss() }
            )
        }
        Spacer(Modifier.height(Space.sm))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}

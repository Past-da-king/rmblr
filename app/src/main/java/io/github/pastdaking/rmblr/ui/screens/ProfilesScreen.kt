package io.github.pastdaking.rmblr.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.pastdaking.rmblr.orb.AppProfile
import io.github.pastdaking.rmblr.orb.AppProfileStore
import io.github.pastdaking.rmblr.orb.Tone
import io.github.pastdaking.rmblr.orb.ToneStore
import io.github.pastdaking.rmblr.ui.components.RmblrSheet
import io.github.pastdaking.rmblr.ui.components.ScreenHeader
import io.github.pastdaking.rmblr.ui.components.SheetOption
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.m3.ConfirmSheet
import io.github.pastdaking.rmblr.ui.m3.ExpressiveShapes
import io.github.pastdaking.rmblr.ui.m3.GroupedRow
import io.github.pastdaking.rmblr.ui.m3.Radial
import io.github.pastdaking.rmblr.ui.m3.RadialShape
import io.github.pastdaking.rmblr.ui.m3.SheetButton
import io.github.pastdaking.rmblr.ui.m3.TonalCard
import io.github.pastdaking.rmblr.ui.m3.TonalTextField
import io.github.pastdaking.rmblr.ui.m3.ToneTag
import io.github.pastdaking.rmblr.ui.theme.Accent
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.GoodWash
import io.github.pastdaking.rmblr.ui.theme.Ink
import io.github.pastdaking.rmblr.ui.theme.OnAccent
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash
import io.github.pastdaking.rmblr.ui.theme.OnGoodWash
import io.github.pastdaking.rmblr.ui.theme.RaisedHigh
import io.github.pastdaking.rmblr.ui.theme.RaisedHighest
import io.github.pastdaking.rmblr.ui.theme.Teal
import io.github.pastdaking.rmblr.ui.theme.TealWash
import io.github.pastdaking.rmblr.ui.theme.TextHigh
import io.github.pastdaking.rmblr.ui.theme.TextMid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The real launcher icon, so a list of apps looks like your phone rather than a list of ids. */
private fun iconFor(context: Context, packageName: String): ImageBitmap? = runCatching {
    context.packageManager.getApplicationIcon(packageName).toBitmap(width = 96, height = 96).asImageBitmap()
}.getOrNull()

/** Apps you could plausibly be typing in: anything with a launcher entry. */
private fun installedApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return runCatching {
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolved ->
                val info: ApplicationInfo = resolved.activityInfo?.applicationInfo ?: return@mapNotNull null
                AppEntry(pm.getApplicationLabel(info).toString(), info.packageName)
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}

private fun labelFor(context: Context, pkg: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
}.getOrDefault(pkg)

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

/** An app's icon, or a soft shape with its first letter when the phone has no such app. */
@Composable
private fun AppBadge(label: String, packageName: String, size: androidx.compose.ui.unit.Dp, shape: Radial) {
    val icon = rememberIcon(packageName)
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4)))
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size).background(RaisedHighest, RadialShape(shape))
        ) {
            Text(label.take(1).uppercase(), color = TextHigh, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private val badgeShapes = listOf(ExpressiveShapes.Flower, ExpressiveShapes.Gem, ExpressiveShapes.Puffy, ExpressiveShapes.Clover4)

private sealed interface ProfilesSheet {
    data object AddMenu : ProfilesSheet
    data object NewProfile : ProfilesSheet
    data class NewTone(val forSlot: Pair<String, String>?) : ProfilesSheet
    data class Edit(val id: String, val page: EditPage) : ProfilesSheet
    data class Delete(val id: String) : ProfilesSheet
}

private sealed interface EditPage {
    data object Main : EditPage
    data class Slot(val slot: String) : EditPage
}

/** The five places a tone can sit on the arc, in the words the screen uses. */
private val slotNames = listOf("up" to "Top", "left" to "Upper", "tap" to "Middle", "right" to "Lower", "down" to "Bottom")

private fun AppProfile.toneIdFor(slot: String): String = when (slot) {
    "up" -> up
    "left" -> left
    "right" -> right
    "down" -> down
    else -> tap
}

private fun AppProfile.withTone(slot: String, id: String): AppProfile = when (slot) {
    "up" -> copy(up = id)
    "left" -> copy(left = id)
    "right" -> copy(right = id)
    "down" -> copy(down = id)
    else -> copy(tap = id)
}

/**
 * Per-app tones: which profile an app belongs to, and the five tones the orb offers there.
 *
 * Slack and Gmail should not hand you the same options as WhatsApp, and this is where you
 * say so. The profiles are stored exactly as before, so nobody's setup moves.
 */
@Composable
fun ProfilesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { AppProfileStore(context) }
    val toneStore = remember { ToneStore(context) }
    var profiles by remember { mutableStateOf(store.load()) }
    var tones by remember { mutableStateOf(toneStore.load()) }
    var sheet by remember { mutableStateOf<ProfilesSheet?>(null) }

    // Reading the launcher list off the main thread keeps the tab switch instant.
    var installed by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    LaunchedEffect(Unit) { installed = withContext(Dispatchers.IO) { installedApps(context) } }

    ProfilesContent(
        profiles = profiles,
        tones = tones,
        installed = installed,
        sheet = sheet,
        onSheet = { sheet = it },
        onChange = { changed ->
            profiles = profiles.map { if (it.id == changed.id) changed else it }
            store.save(profiles)
        },
        onAddProfile = { name ->
            val created = store.add(name)
            profiles = store.load()
            sheet = ProfilesSheet.Edit(created.id, EditPage.Main)
        },
        onAddTone = { name, prompt ->
            val tone = toneStore.add(name, prompt)
            tones = toneStore.load()
            tone
        },
        onDelete = { id ->
            store.delete(id)
            profiles = store.load()
        },
        modifier = modifier
    )
}

/** The screen without its storage, so it can be drawn and tested on its own. */
@Composable
private fun ProfilesContent(
    profiles: List<AppProfile>,
    tones: List<Tone>,
    installed: List<AppEntry>,
    sheet: ProfilesSheet?,
    onSheet: (ProfilesSheet?) -> Unit,
    onChange: (AppProfile) -> Unit,
    onAddProfile: (String) -> Unit,
    onAddTone: (String, String) -> Tone,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(Ink)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Space.md),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.lg)
                .padding(top = Space.xl, bottom = 200.dp)
                .testTag("profiles_screen")
        ) {
            ScreenHeader(
                title = "Profiles",
                subtitle = "A different tone for each kind of app.",
                modifier = Modifier.padding(horizontal = Space.sm),
                trailing = {
                    ShapedIcon(Icons.Default.Layers, ExpressiveShapes.Flower, AccentWash, OnAccentWash, size = 72.dp)
                }
            )
            profiles.forEachIndexed { index, profile ->
                ProfileCard(profile, tones, index) { onSheet(ProfilesSheet.Edit(profile.id, EditPage.Main)) }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Space.lg, bottom = 132.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(AccentWash)
                .pressable(onClick = { onSheet(ProfilesSheet.AddMenu) })
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .testTag("add_profile")
        ) {
            Icon(Icons.Default.Add, null, tint = OnAccentWash)
            Spacer(Modifier.width(Space.md))
            Text("Add profile", color = OnAccentWash, style = MaterialTheme.typography.titleMedium)
        }
    }

    when (val open = sheet) {
        ProfilesSheet.AddMenu -> RmblrSheet(title = "Add", onDismiss = { onSheet(null) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GroupedRow(0, 2, onClick = { onSheet(ProfilesSheet.NewProfile) }) {
                    MenuLine("New profile", "A set of apps and the tones they get")
                }
                GroupedRow(1, 2, onClick = { onSheet(ProfilesSheet.NewTone(null)) }) {
                    MenuLine("New tone", "An instruction for what to do with your words")
                }
            }
        }

        ProfilesSheet.NewProfile -> {
            var name by remember { mutableStateOf("") }
            RmblrSheet(title = "New profile", onDismiss = { onSheet(null) }) {
                TonalTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Name, for example Clients",
                    modifier = Modifier.fillMaxWidth().testTag("new_profile_name")
                )
                Spacer(Modifier.height(Space.xl))
                SheetButton(
                    "Create",
                    { if (name.isNotBlank()) onAddProfile(name) },
                    if (name.isNotBlank()) Accent else RaisedHigh,
                    if (name.isNotBlank()) OnAccent else TextMid,
                    Modifier.fillMaxWidth()
                )
            }
        }

        is ProfilesSheet.NewTone -> {
            var name by remember { mutableStateOf("") }
            var prompt by remember { mutableStateOf("") }
            val ready = name.isNotBlank() && prompt.isNotBlank()
            RmblrSheet(title = "New tone", onDismiss = { onSheet(open.forSlot?.let { ProfilesSheet.Edit(it.first, EditPage.Main) }) }) {
                Text(
                    "A tone is the instruction given to the model. Write it as an order: what to do with the words you just said.",
                    color = TextMid,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(Space.lg))
                TonalTextField(name, { name = it }, placeholder = "Name, for example LinkedIn post", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Space.md))
                TonalTextField(
                    prompt, { prompt = it },
                    placeholder = "Rewrite this as a short LinkedIn post. Confident, no hashtags. Output only the post.",
                    minLines = 4, maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.xl))
                SheetButton(
                    "Save tone",
                    {
                        if (ready) {
                            val tone = onAddTone(name, prompt)
                            val back = open.forSlot
                            if (back != null) {
                                profiles.firstOrNull { it.id == back.first }?.let { onChange(it.withTone(back.second, tone.id)) }
                                onSheet(ProfilesSheet.Edit(back.first, EditPage.Main))
                            } else onSheet(null)
                        }
                    },
                    if (ready) Accent else RaisedHigh,
                    if (ready) OnAccent else TextMid,
                    Modifier.fillMaxWidth()
                )
            }
        }

        is ProfilesSheet.Edit -> {
            val profile = profiles.firstOrNull { it.id == open.id }
            if (profile == null) {
                LaunchedEffect(open.id) { onSheet(null) }
            } else when (val page = open.page) {
                EditPage.Main -> EditProfileSheet(
                    profile = profile,
                    tones = tones,
                    installed = installed,
                    canDelete = canDeleteProfile(profile, profiles),
                    onChange = onChange,
                    onPickSlot = { onSheet(ProfilesSheet.Edit(profile.id, EditPage.Slot(it))) },
                    onDelete = { onSheet(ProfilesSheet.Delete(profile.id)) },
                    onDismiss = { onSheet(null) }
                )
                is EditPage.Slot -> TonePickerSheet(
                    tones = tones,
                    currentId = profile.toneIdFor(page.slot),
                    onPick = { tone ->
                        onChange(profile.withTone(page.slot, tone.id))
                        onSheet(ProfilesSheet.Edit(profile.id, EditPage.Main))
                    },
                    onNewTone = { onSheet(ProfilesSheet.NewTone(profile.id to page.slot)) },
                    onDismiss = { onSheet(ProfilesSheet.Edit(profile.id, EditPage.Main)) }
                )
            }
        }

        is ProfilesSheet.Delete -> {
            val profile = profiles.firstOrNull { it.id == open.id }
            ConfirmSheet(
                title = "Delete ${profile?.name ?: "this profile"}?",
                body = "Its apps fall back to the profile for every app not listed. Your tones are not touched.",
                confirmLabel = "Delete",
                destructive = true,
                icon = Icons.Default.Delete,
                onConfirm = { onDelete(open.id); onSheet(null) },
                onDismiss = { onSheet(ProfilesSheet.Edit(open.id, EditPage.Main)) }
            )
        }

        null -> Unit
    }
}

@Composable
private fun MenuLine(title: String, supporting: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
            Text(supporting, color = TextMid, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextMid)
    }
}

@Composable
private fun toneName(tones: List<Tone>, id: String): String =
    tones.firstOrNull { it.id == id }?.name ?: "Clean & Smooth"

private fun glyphIcon(glyph: ProfileGlyph): ImageVector = when (glyph) {
    ProfileGlyph.CHAT -> Icons.Default.Chat
    ProfileGlyph.EMAIL -> Icons.Default.Email
    ProfileGlyph.WORK -> Icons.Default.Work
    ProfileGlyph.ANYWHERE -> Icons.Default.Public
    ProfileGlyph.GENERIC -> Icons.Default.Layers
}

@Composable
private fun glyphLook(glyph: ProfileGlyph): Triple<Radial, Color, Color> = when (glyph) {
    ProfileGlyph.CHAT -> Triple(ExpressiveShapes.Cookie9, AccentWash, OnAccentWash)
    ProfileGlyph.EMAIL -> Triple(ExpressiveShapes.Clover4, TealWash, Teal)
    ProfileGlyph.WORK -> Triple(ExpressiveShapes.Gem, AccentWash, OnAccentWash)
    ProfileGlyph.ANYWHERE -> Triple(ExpressiveShapes.Puffy, GoodWash, OnGoodWash)
    ProfileGlyph.GENERIC -> Triple(ExpressiveShapes.Sunny, TealWash, Teal)
}

/** One profile: its glyph, name, apps, the tone a tap uses and how many the arc offers. */
@Composable
private fun ProfileCard(profile: AppProfile, tones: List<Tone>, index: Int, onClick: () -> Unit) {
    val fallback = isFallbackProfile(profile)
    val glyph = glyphFor(profile, fallback)
    val (shape, container, tint) = glyphLook(glyph)
    TonalCard(onClick = onClick, padding = PaddingValues(Space.lg), modifier = Modifier.testTag("profile_${profile.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShapedIcon(glyphIcon(glyph), shape, container, tint, size = 64.dp)
            Spacer(Modifier.width(Space.lg))
            Column(Modifier.weight(1f)) {
                Text(profile.name, color = TextHigh, style = MaterialTheme.typography.titleLarge)
                Text(appCountLabel(profile), color = TextMid, style = MaterialTheme.typography.bodyLarge)
            }
            if (fallback) {
                Text(
                    "Fallback",
                    color = TextMid,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(RaisedHighest).padding(horizontal = 16.dp, vertical = 10.dp)
                )
            } else {
                IconStack(profile.packages.take(3), labels = profile.packages.take(3))
            }
        }
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            ToneTag(toneName(tones, profile.tap), icon = Icons.Default.AutoFixHigh)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(RaisedHighest).padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.TouchApp, null, tint = TextHigh, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Space.sm))
                Text(holdLabel(holdToneCount(profile)), color = TextHigh, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Up to three app icons, each overlapping the one before. */
@Composable
private fun IconStack(packages: List<String>, labels: List<String>) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        packages.forEachIndexed { i, pkg ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(RaisedHigh)
            ) {
                AppBadge(labelFor(context, labels[i]), pkg, 36.dp, badgeShapes[i % badgeShapes.size])
            }
        }
    }
}

/** The profile's settings: its apps and the five tones. Everything here saves as you change it. */
@Composable
private fun EditProfileSheet(
    profile: AppProfile,
    tones: List<Tone>,
    installed: List<AppEntry>,
    canDelete: Boolean,
    onChange: (AppProfile) -> Unit,
    onPickSlot: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var choosingApps by remember { mutableStateOf(false) }

    if (choosingApps) {
        ChooseAppsSheet(
            profileName = profile.name,
            apps = installed.ifEmpty { profile.packages.map { AppEntry(labelFor(context, it), it) } },
            chosen = profile.packages.toSet(),
            onToggle = { pkg -> onChange(togglePackage(profile, pkg)) },
            onClear = { onChange(profile.copy(packages = emptyList())) },
            onDone = { choosingApps = false }
        )
        return
    }

    RmblrSheet(title = profile.name, onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GroupedRow(0, 1, onClick = { choosingApps = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                        Icon(Icons.Default.Apps, null, tint = TextHigh)
                        Spacer(Modifier.width(Space.md))
                        Column(Modifier.weight(1f)) {
                            Text("Apps", color = TextHigh, style = MaterialTheme.typography.titleMedium)
                            Text(appCountLabel(profile), color = TextMid, style = MaterialTheme.typography.bodyMedium)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = TextMid)
                    }
                }
            }
            Spacer(Modifier.height(Space.lg))
            Text("Tones on the arc", color = TextHigh, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Space.sm))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                slotNames.forEachIndexed { i, (slot, label) ->
                    GroupedRow(i, slotNames.size, onClick = { onPickSlot(slot) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                            Text(label, color = TextMid, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp))
                            Text(toneName(tones, profile.toneIdFor(slot)), color = TextHigh, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = TextMid)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.sm))
            Text("Middle is also what a plain tap uses.", color = TextMid, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(Space.xl))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), modifier = Modifier.fillMaxWidth()) {
                if (canDelete) {
                    SheetButton("Delete", onDelete, AccentWash, OnAccentWash, Modifier.weight(1f))
                }
                SheetButton("Done", onDismiss, Accent, OnAccent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TonePickerSheet(
    tones: List<Tone>,
    currentId: String,
    onPick: (Tone) -> Unit,
    onNewTone: () -> Unit,
    onDismiss: () -> Unit
) {
    RmblrSheet(title = "Pick a tone", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            tones.forEach { tone ->
                SheetOption(
                    title = tone.name,
                    supporting = tone.description,
                    selected = tone.id == currentId,
                    onClick = { onPick(tone) }
                )
            }
            Spacer(Modifier.height(Space.sm))
            SheetButton("Write a new tone", onNewTone, AccentWash, OnAccentWash, Modifier.fillMaxWidth())
        }
    }
}

/**
 * Choose which apps a profile covers: a search box, a count, and every app with a launcher
 * icon on the phone. The ones already chosen come first.
 */
@Composable
fun ChooseAppsSheet(
    profileName: String,
    apps: List<AppEntry>,
    chosen: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val openedWith = remember { chosen }
    val ordered = remember(apps) { orderForPicker(apps, openedWith) }
    val shown = filterApps(ordered, query)

    RmblrSheet(title = "", onDismiss = onDone) {
        Column(Modifier.fillMaxWidth().testTag("choose_apps_sheet")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Choose apps", color = TextHigh, style = MaterialTheme.typography.headlineSmall)
                    Text("$profileName tones apply in these apps", color = TextMid, style = MaterialTheme.typography.bodyLarge)
                }
                InfoPill(selectedLabel(chosen.size))
            }
            Spacer(Modifier.height(Space.lg))
            TonalTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search installed apps",
                leadingIcon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth().testTag("app_search")
            )
            Spacer(Modifier.height(Space.md))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 372.dp)) {
                items(shown, key = { it.packageName }) { app ->
                    val selected = app.packageName in chosen
                    AppRow(app, selected, shapeIndex = apps.indexOf(app)) { onToggle(app.packageName) }
                }
            }
            Spacer(Modifier.height(Space.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), modifier = Modifier.fillMaxWidth()) {
                SheetButton("Clear", onClear, AccentWash, OnAccentWash, Modifier.weight(1f))
                SheetButton("Done", onDone, Accent, OnAccent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AppRow(app: AppEntry, selected: Boolean, shapeIndex: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressable(pressedScale = 0.985f, onClick = onClick)
            .clip(RoundedCornerShape(if (selected) 28.dp else 12.dp))
            .background(if (selected) AccentWash else Color.Transparent)
            .padding(horizontal = Space.lg, vertical = Space.sm)
    ) {
        AppBadge(app.label, app.packageName, 44.dp, badgeShapes[shapeIndex.coerceAtLeast(0) % badgeShapes.size])
        Spacer(Modifier.width(Space.lg))
        Text(
            app.label,
            color = if (selected) OnAccentWash else TextHigh,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (selected) Accent else Color.Transparent)
                .then(if (selected) Modifier else Modifier.border(2.dp, TextMid, CircleShape))
        ) {
            if (selected) Icon(Icons.Default.Check, null, tint = OnAccent, modifier = Modifier.size(18.dp))
        }
    }
}

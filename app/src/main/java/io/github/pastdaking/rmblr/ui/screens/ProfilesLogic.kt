package io.github.pastdaking.rmblr.ui.screens

import io.github.pastdaking.rmblr.orb.AppProfile

/**
 * The decisions behind the Profiles screen, apart from drawing it.
 */

/** One app that could be put in a profile. */
data class AppEntry(val label: String, val packageName: String)

/** Which glyph a profile wears. Chosen from its name, so a profile you add yourself gets a sensible one. */
enum class ProfileGlyph { CHAT, EMAIL, WORK, ANYWHERE, GENERIC }

fun glyphFor(profile: AppProfile, isFallback: Boolean): ProfileGlyph {
    if (isFallback) return ProfileGlyph.ANYWHERE
    val n = profile.name.lowercase()
    return when {
        listOf("chat", "message", "social", "friends", "whatsapp").any { n.contains(it) } -> ProfileGlyph.CHAT
        listOf("mail", "inbox").any { n.contains(it) } -> ProfileGlyph.EMAIL
        listOf("work", "office", "business", "client", "job").any { n.contains(it) } -> ProfileGlyph.WORK
        else -> ProfileGlyph.GENERIC
    }
}

/** The profile that catches every app nobody has claimed: the one with no apps. */
fun isFallbackProfile(profile: AppProfile): Boolean = profile.packages.isEmpty()

/**
 * "Hold for 4 tones": how many different tones the arc offers in this profile. The arc has
 * five places, and a profile that gives two of them the same tone offers one fewer.
 */
fun holdToneCount(profile: AppProfile): Int =
    listOf(profile.tap, profile.up, profile.down, profile.left, profile.right).distinct().size

fun holdLabel(count: Int): String = when (count) {
    1 -> "Hold for 1 tone"
    else -> "Hold for $count tones"
}

fun appCountLabel(profile: AppProfile): String = when {
    isFallbackProfile(profile) -> "Every app not listed"
    profile.packages.size == 1 -> "1 app"
    else -> "${profile.packages.size} apps"
}

/**
 * Whether a profile may be deleted.
 *
 * Something has to catch the apps nobody has claimed, so the last profile without apps
 * stays. Everything else can go.
 */
fun canDeleteProfile(profile: AppProfile, all: List<AppProfile>): Boolean =
    !(isFallbackProfile(profile) && all.count { isFallbackProfile(it) } <= 1)

/** Search the installed apps by name or package, ignoring case. A blank query keeps them all. */
fun filterApps(apps: List<AppEntry>, query: String): List<AppEntry> {
    val q = query.trim()
    if (q.isEmpty()) return apps
    return apps.filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
}

/**
 * The order the app list opens in: apps already in the profile first, then the rest, each
 * group alphabetical. Computed once when the sheet opens, so rows do not jump about under
 * a finger while somebody is ticking them.
 */
fun orderForPicker(apps: List<AppEntry>, chosenAtOpen: Set<String>): List<AppEntry> =
    apps.sortedWith(compareBy<AppEntry> { it.packageName !in chosenAtOpen }.thenBy { it.label.lowercase() })

fun selectedLabel(count: Int): String = "$count selected"

/** Toggle one package in a profile's app list. */
fun togglePackage(profile: AppProfile, packageName: String): AppProfile =
    profile.copy(
        packages = if (packageName in profile.packages) profile.packages - packageName else profile.packages + packageName
    )

package io.github.pastdaking.rmblr.orb

import android.content.Context
import io.github.pastdaking.rmblr.data.CleanupPreset
import org.json.JSONArray
import org.json.JSONObject

/**
 * What the orb offers depends on where you are.
 *
 * Typing in Gmail almost always means an email; typing in WhatsApp almost never does. The
 * four actions behind a hold were global, which meant the same four compromises everywhere.
 * A profile binds a set of apps to a set of actions, so holding the orb in Slack offers
 * work tones and holding it in Instagram offers casual ones, with no switching to do.
 */
data class AppProfile(
    val id: String,
    val name: String,
    val packages: List<String>,
    /** Tone ids, resolved through ToneStore. Ids rather than an enum so tones stay editable. */
    val tap: String,
    val up: String,
    val down: String,
    val left: String,
    val right: String
)

class AppProfileStore(private val context: Context) {

    private companion object {
        /** Bump to re-seed the starter profiles once on the next launch. */
        const val SEED_VERSION = 3
    }

    private val prefs = context.getSharedPreferences("rmblr_profiles", Context.MODE_PRIVATE)

    fun load(): List<AppProfile> {
        // The first seed ran before the manifest could see other apps, so it matched
        // almost nothing. Bumping this re-seeds once, without touching anything else.
        if (prefs.getInt("seed_version", 0) < SEED_VERSION) return seed()
        val raw = prefs.getString("profiles", null) ?: return seed()
        return runCatching { parse(raw) }.getOrDefault(seed()).ifEmpty { seed() }
    }

    /**
     * First run only: the starter profiles, with every app that is not actually on this
     * phone stripped out.
     *
     * The suggested package lists are guesses about what someone might have installed.
     * Showing "Proton Mail" to somebody who deleted Proton Mail is just wrong, so they get
     * filtered against the real launcher list and the result is saved immediately, which
     * also means the guessing only ever happens once.
     */
    private fun seed(): List<AppProfile> {
        val present = installedPackages()
        val trimmed = defaults().map { profile ->
            profile.copy(packages = profile.packages.filter { it in present })
        }
        save(trimmed)
        prefs.edit().putInt("seed_version", SEED_VERSION).apply()
        return trimmed
    }

    private fun installedPackages(): Set<String> = runCatching {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        context.packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }.getOrDefault(emptySet())

    fun add(name: String): AppProfile {
        val profile = AppProfile(
            id = "p_${java.util.UUID.randomUUID().toString().take(8)}",
            name = name.trim().ifBlank { "New profile" },
            packages = emptyList(),
            tap = CleanupPreset.SMART_CLEAN.name,
            up = CleanupPreset.FORMAL_EMAIL.name,
            down = CleanupPreset.BULLET_POINTS.name,
            left = CleanupPreset.CASUAL_CHAT.name,
            right = CleanupPreset.SMART_CLEAN.name
        )
        // Ahead of the fallback, which must stay last so it keeps catching everything else.
        val all = load().toMutableList()
        all.add(maxOf(all.size - 1, 0), profile)
        save(all)
        return profile
    }

    /** The fallback cannot be deleted: something has to catch unassigned apps. */
    fun delete(id: String) {
        val all = load()
        val target = all.firstOrNull { it.id == id } ?: return
        if (target.packages.isEmpty() && all.count { it.packages.isEmpty() } <= 1) return
        save(all.filterNot { it.id == id })
    }

    fun save(profiles: List<AppProfile>) {
        val array = JSONArray()
        profiles.forEach { p ->
            array.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("packages", JSONArray(p.packages))
                    .put("tap", p.tap)
                    .put("up", p.up)
                    .put("down", p.down)
                    .put("left", p.left)
                    .put("right", p.right)
            )
        }
        prefs.edit().putString("profiles", array.toString()).apply()
    }

    /**
     * The profile covering [packageName], or the fallback.
     *
     * The fallback is the profile with no apps attached, which is also what a brand new
     * install uses everywhere until apps get assigned.
     */
    fun profileFor(packageName: String?): AppProfile {
        val all = load()
        if (packageName != null) {
            all.firstOrNull { packageName in it.packages }?.let { return it }
        }
        return all.firstOrNull { it.packages.isEmpty() } ?: all.first()
    }

    private fun parse(raw: String): List<AppProfile> {
        val array = JSONArray(raw)
        val out = ArrayList<AppProfile>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val packagesArray = o.optJSONArray("packages") ?: JSONArray()
            val packages = (0 until packagesArray.length()).map { packagesArray.getString(it) }
            out.add(
                AppProfile(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    packages = packages,
                    tap = o.optString("tap", CleanupPreset.SMART_CLEAN.name),
                    up = o.optString("up", CleanupPreset.FORMAL_EMAIL.name),
                    down = o.optString("down", CleanupPreset.BULLET_POINTS.name),
                    left = o.optString("left", CleanupPreset.CASUAL_CHAT.name),
                    right = o.optString("right", CleanupPreset.SMART_CLEAN.name)
                )
            )
        }
        return out
    }


    /**
     * Sensible out of the box, so profiles are useful before anyone configures anything.
     * The package lists are the obvious suspects; anything unlisted lands on Anywhere.
     */
    private fun defaults(): List<AppProfile> = listOf(
        AppProfile(
            id = "chat",
            name = "Chat",
            packages = listOf(
                "com.whatsapp", "com.whatsapp.w4b",
                "org.telegram.messenger", "com.facebook.orca",
                "com.instagram.android", "org.thoughtcrime.securesms",
                "com.snapchat.android", "com.google.android.apps.messaging"
            ),
            tap = CleanupPreset.CASUAL_CHAT.name,
            up = CleanupPreset.SMART_CLEAN.name,
            down = CleanupPreset.CONCISE_SUMMARY.name,
            left = CleanupPreset.CASUAL_CHAT.name,
            right = CleanupPreset.FIX_GRAMMAR.name
        ),
        AppProfile(
            id = "email",
            name = "Email",
            packages = listOf(
                "com.google.android.gm", "com.microsoft.office.outlook",
                "com.samsung.android.email.provider", "ch.protonmail.android"
            ),
            tap = CleanupPreset.FORMAL_EMAIL.name,
            up = CleanupPreset.FORMAL_EMAIL.name,
            down = CleanupPreset.BULLET_POINTS.name,
            left = CleanupPreset.CONCISE_SUMMARY.name,
            right = CleanupPreset.SMART_CLEAN.name
        ),
        AppProfile(
            id = "work",
            name = "Work",
            packages = listOf(
                "com.Slack", "com.microsoft.teams", "com.atlassian.android.jira.core",
                "com.linkedin.android", "com.google.android.apps.dynamite"
            ),
            tap = CleanupPreset.SMART_CLEAN.name,
            up = CleanupPreset.FORMAL_EMAIL.name,
            down = CleanupPreset.BULLET_POINTS.name,
            left = CleanupPreset.SMART_CLEAN.name,
            right = CleanupPreset.CONCISE_SUMMARY.name
        ),
        AppProfile(
            id = "default",
            name = "Anywhere else",
            packages = emptyList(),
            tap = CleanupPreset.SMART_CLEAN.name,
            up = CleanupPreset.FORMAL_EMAIL.name,
            down = CleanupPreset.BULLET_POINTS.name,
            left = CleanupPreset.CASUAL_CHAT.name,
            right = CleanupPreset.SMART_CLEAN.name
        )
    )
}

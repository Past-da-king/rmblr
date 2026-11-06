package io.github.pastdaking.rmblr.ui.screens

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import io.github.pastdaking.rmblr.data.PreferencesManager
import io.github.pastdaking.rmblr.orb.AppProfile
import io.github.pastdaking.rmblr.orb.AppProfileStore
import io.github.pastdaking.rmblr.data.CleanupPreset
import io.github.pastdaking.rmblr.update.Changelog
import io.github.pastdaking.rmblr.update.ReleaseInfo
import io.github.pastdaking.rmblr.update.UpdateRepository
import io.github.pastdaking.rmblr.update.UpdateStatus
import org.junit.Test
import org.robolectric.Shadows.shadowOf

/**
 * Screenshots of Profiles, the walkthrough, the update sheets and Feedback, light and dark,
 * compared by eye against `design/m3-mocks/png/12-*` to `17-*`.
 */
class FlowShotTest : ProofShotBase() {

    // ------------------------------------------------------------------ profiles

    private val apps = listOf(
        "com.example.messages" to "Messages",
        "com.example.notes" to "Notes",
        "com.example.browser" to "Browser",
        "com.example.mail" to "Mail",
        "com.example.docs" to "Docs"
    )

    /** Put a few launcher apps on the pretend phone, so the lists show names and not ids. */
    private fun installApps() {
        val pm = shadowOf(context.packageManager)
        apps.forEach { (pkg, label) ->
            val info = PackageInfo().apply {
                packageName = pkg
                applicationInfo = ApplicationInfo().apply {
                    packageName = pkg
                    nonLocalizedLabel = label
                }
            }
            pm.installPackage(info)
            val component = android.content.ComponentName(pkg, "$pkg.Main")
            pm.addOrUpdateActivity(ActivityInfo().apply {
                packageName = pkg
                name = "$pkg.Main"
                applicationInfo = info.applicationInfo
                nonLocalizedLabel = label
            })
            pm.addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            )
        }
    }

    private fun seedProfiles() {
        installApps()
        context.getSharedPreferences("rmblr_profiles", android.content.Context.MODE_PRIVATE).edit().putInt("seed_version", 3).commit()
        val store = AppProfileStore(context)
        val tone = CleanupPreset.values()
        store.save(
            listOf(
                AppProfile("chat", "Chat", listOf("com.example.messages", "com.example.notes", "com.example.browser"),
                    CleanupPreset.CASUAL_CHAT.name, CleanupPreset.SMART_CLEAN.name, CleanupPreset.CONCISE_SUMMARY.name, CleanupPreset.CASUAL_CHAT.name, CleanupPreset.FIX_GRAMMAR.name),
                AppProfile("email", "Email", listOf("com.example.mail", "com.example.docs"),
                    CleanupPreset.FORMAL_EMAIL.name, CleanupPreset.FORMAL_EMAIL.name, CleanupPreset.BULLET_POINTS.name, CleanupPreset.CONCISE_SUMMARY.name, CleanupPreset.SMART_CLEAN.name),
                AppProfile("work", "Work", listOf("com.example.docs", "com.example.notes", "com.example.messages"),
                    CleanupPreset.SMART_CLEAN.name, CleanupPreset.FORMAL_EMAIL.name, CleanupPreset.BULLET_POINTS.name, CleanupPreset.SMART_CLEAN.name, CleanupPreset.CONCISE_SUMMARY.name),
                AppProfile("default", "Anywhere else", emptyList(),
                    CleanupPreset.SMART_CLEAN.name, CleanupPreset.FORMAL_EMAIL.name, CleanupPreset.BULLET_POINTS.name, CleanupPreset.CASUAL_CHAT.name, CleanupPreset.SMART_CLEAN.name)
            )
        )
        check(tone.isNotEmpty())
    }

    @Test fun profiles() {
        seedProfiles()
        proof("12-profiles") { ProfilesScreen() }
    }

    @Test fun chooseAppsSheet() {
        seedProfiles()
        val entries = apps.map { AppEntry(it.second, it.first) }
        proof("13-profile-apps-sheet") {
            ProfilesScreen()
            ChooseAppsSheet(
                profileName = "Chat",
                apps = entries,
                chosen = setOf("com.example.messages", "com.example.notes", "com.example.browser"),
                onToggle = {},
                onClear = {},
                onDone = {}
            )
        }
    }

    // ------------------------------------------------------------------ walkthrough

    private fun onboarding(step: Int, name: String, granted: PermissionState = PermissionState(false, false, false, false)) {
        val prefs = PreferencesManager.getInstance(context)
        proof(name) {
            OnboardingContent(
                step = step,
                permissions = granted,
                onSkip = {}, onBack = {}, onNext = {},
                pageContent = {
                    if (step == OnboardingFlow.PERMISSIONS_STEP) PermissionsPageContent(granted) {}
                    else OnboardingPage(step, prefs)
                }
            )
        }
    }

    @Test fun onboardingSpeak() = onboarding(0, "14-onboarding-1")
    @Test fun onboardingShowsUp() = onboarding(1, "14-onboarding-2")
    @Test fun onboardingGestures() = onboarding(2, "14-onboarding-3")
    @Test fun onboardingKey() = onboarding(3, "14-onboarding-4")
    @Test fun permissions() = onboarding(4, "15-permissions", PermissionState(true, true, false, false))
    @Test fun permissionsAllGranted() = onboarding(4, "15-permissions-done", PermissionState(true, true, true, true))

    // ------------------------------------------------------------------ updates

    private val release = ReleaseInfo(
        tag = "v2.7.0",
        title = "v2.7.0",
        notes = Changelog.entries.first().notes,
        apkUrl = "https://example.invalid/RMBLR-2.7.0.apk",
        pageUrl = "https://example.invalid/releases/v2.7.0",
        apkBytes = 14L * 1024 * 1024
    )

    @Test fun updateSheet() {
        val prefs = PreferencesManager.getInstance(context)
        context.getSharedPreferences("rmblr_updates", android.content.Context.MODE_PRIVATE)
            .edit().putLong("last_check_at", System.currentTimeMillis()).commit()
        UpdateRepository.getInstance(context).setStatusForTest(UpdateStatus.Available(release, System.currentTimeMillis()))
        proof("16-update-sheet") {
            SettingsScreen(prefs)
            UpdateAvailableSheet(release = release, onDownload = {}, onSkip = {}, onDismiss = {})
        }
    }

    @Test fun whatsNewSheet() {
        val prefs = PreferencesManager.getInstance(context)
        context.getSharedPreferences("rmblr_updates", android.content.Context.MODE_PRIVATE)
            .edit().putLong("last_check_at", System.currentTimeMillis()).commit()
        proof("16-whats-new-sheet") {
            SettingsScreen(prefs)
            WhatsNewSheet(entry = Changelog.entries.first(), onDismiss = {})
        }
    }

    // ------------------------------------------------------------------ feedback

    @Test fun feedback() {
        val prefs = PreferencesManager.getInstance(context)
        proof(
            "17-feedback",
            prepare = {
                onNodeWithTag("feedback_body")
                    .performTextInput("The orb does not appear in my browser's address bar. It works in every other text field I tried.")
            }
        ) { FeedbackScreen(prefs, onBack = {}) }
    }
}

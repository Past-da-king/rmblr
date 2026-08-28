package io.github.pastdaking.rmblr.update

import android.content.Context
import android.content.SharedPreferences
import io.github.pastdaking.rmblr.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the last check found. */
sealed interface UpdateStatus {
    /** Never checked on this device, or the answer has been cleared. */
    data object Unknown : UpdateStatus

    /** Checked, and this is the newest release there is. */
    data class UpToDate(val checkedAt: Long) : UpdateStatus

    /** Checked, and there is something newer than what is installed. */
    data class Available(val release: ReleaseInfo, val checkedAt: Long) : UpdateStatus

    /** The check itself failed. [message] is safe to show someone. */
    data class Failed(val message: String) : UpdateStatus
}

/**
 * Everything the app knows about versions other than the one it is.
 *
 * Two jobs that look like one. It answers "is there a newer RMBLR" — on demand from the
 * Updates screen, and by itself roughly once a day so a notification can be posted. And
 * it remembers which version was last *seen*, which is how the What's New sheet knows to
 * appear exactly once after an update rather than every single launch.
 *
 * There is no WorkManager here on purpose. The check needs an app that is running, and
 * this one already is: the orb is a foreground service that most people leave on all day,
 * and it starts the check when it starts. Someone who has the orb off and never opens the
 * app is someone who is not using RMBLR, and waking their phone on a schedule to tell
 * them about a version of it would be rude rather than helpful.
 */
class UpdateRepository private constructor(context: Context) {

    private val app = context.applicationContext
    private val prefs: SharedPreferences =
        app.getSharedPreferences("rmblr_updates", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Unknown)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val _autoCheck = MutableStateFlow(prefs.getBoolean(KEY_AUTO, true))
    val autoCheck: StateFlow<Boolean> = _autoCheck.asStateFlow()

    /** The version this APK is, e.g. "2.5.3". */
    val installedVersion: String get() = BuildConfig.VERSION_NAME

    // ------------------------------------------------------------ preferences

    fun setAutoCheck(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO, enabled).apply()
        _autoCheck.value = enabled
    }

    /** Stop offering this particular version. A later one will still be offered. */
    fun skip(version: String) {
        prefs.edit().putString(KEY_SKIPPED, version).apply()
        // Drop it out of the current answer too, so the row stops nagging immediately
        // instead of at the next check.
        val current = _status.value
        if (current is UpdateStatus.Available && current.release.version == version) {
            _status.value = UpdateStatus.UpToDate(System.currentTimeMillis())
        }
    }

    private fun isSkipped(version: String): Boolean =
        prefs.getString(KEY_SKIPPED, null) == version

    /** Millis of the last successful check, or 0. */
    fun lastCheckedAt(): Long = prefs.getLong(KEY_LAST_CHECK, 0L)

    // ------------------------------------------------------------ the check

    /**
     * Ask GitHub, and record the answer.
     *
     * [force] is what the Check now button passes; without it the call returns the
     * existing status untouched if the last check was recent. Cheap enough to call from
     * anywhere.
     */
    suspend fun check(force: Boolean = false): UpdateStatus {
        if (!force) {
            if (!_autoCheck.value) return _status.value
            val since = System.currentTimeMillis() - lastCheckedAt()
            if (since in 0 until CHECK_INTERVAL_MS) return _status.value
        }
        if (_checking.value) return _status.value

        _checking.value = true
        val result = try {
            GitHubReleases.latest()
        } finally {
            _checking.value = false
        }

        val now = System.currentTimeMillis()
        val status = result.fold(
            onSuccess = { release ->
                prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
                if (isNewerVersion(release.version, installedVersion)) {
                    UpdateStatus.Available(release, now)
                } else {
                    UpdateStatus.UpToDate(now)
                }
            },
            onFailure = { error ->
                UpdateStatus.Failed(error.message ?: "Could not reach GitHub.")
            }
        )
        _status.value = status
        return status
    }

    /**
     * The once-a-day check, plus the notification if it finds something.
     *
     * Notifies at most once per released version: someone who has already been told about
     * 2.6.0 and chose not to install it does not need telling again tomorrow, and the day
     * after. Skipped versions are never announced at all.
     */
    suspend fun checkAndNotify() {
        if (!_autoCheck.value) return
        val status = check(force = false)
        if (status !is UpdateStatus.Available) return

        val version = status.release.version
        if (isSkipped(version)) return
        if (prefs.getString(KEY_NOTIFIED, null) == version) return

        prefs.edit().putString(KEY_NOTIFIED, version).apply()
        UpdateNotifier.notify(app, status.release)
    }

    // ------------------------------------------------------------ what's new

    /**
     * The notes for the version running right now, the first time it runs — and null
     * every time after that.
     *
     * Keyed on versionCode rather than versionName because it is the one number
     * guaranteed to move on every build. Calling this consumes it: the caller is expected
     * to show the sheet, and asking twice would mean showing it twice.
     */
    fun consumeWhatsNew(): ChangelogEntry? {
        val seen = prefs.getInt(KEY_SEEN_CODE, 0)
        val current = BuildConfig.VERSION_CODE
        if (seen >= current) return null

        prefs.edit().putInt(KEY_SEEN_CODE, current).apply()

        // A fresh install is not an update. Someone who has just met the app does not
        // want a list of what changed in a version they never had — the walkthrough is
        // what greets them, and this would be a second greeting arguing with it.
        if (seen == 0) return null

        return Changelog.forVersion(BuildConfig.VERSION_NAME)
    }

    /** For the Updates screen, which shows the notes whether or not they are new. */
    fun currentNotes(): ChangelogEntry? = Changelog.forVersion(BuildConfig.VERSION_NAME)

    companion object {
        private const val KEY_AUTO = "auto_check"
        private const val KEY_LAST_CHECK = "last_check_at"
        private const val KEY_SEEN_CODE = "last_seen_version_code"
        private const val KEY_SKIPPED = "skipped_version"
        private const val KEY_NOTIFIED = "notified_version"

        private const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

        @Volatile
        private var instance: UpdateRepository? = null

        fun getInstance(context: Context): UpdateRepository =
            instance ?: synchronized(this) {
                instance ?: UpdateRepository(context).also { instance = it }
            }
    }
}

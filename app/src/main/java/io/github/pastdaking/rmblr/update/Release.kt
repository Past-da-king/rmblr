package io.github.pastdaking.rmblr.update

import io.github.pastdaking.rmblr.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * One published release, reduced to the five things the app has any use for.
 *
 * The GitHub payload is ninety fields deep and almost all of it is about the repository
 * rather than the download. Parsing it into this by hand with [org.json] keeps the app
 * free of a Moshi adapter and a KSP round trip for a single request that runs at most
 * once a day.
 */
data class ReleaseInfo(
    /** The tag as published, e.g. "v2.5.3". */
    val tag: String,
    /** The release headline, e.g. "v2.5.3 — translating is the app now". */
    val title: String,
    /** The release notes, in the markdown they were written in. */
    val notes: String,
    /** Direct link to the signed APK, or null if the release has no APK attached. */
    val apkUrl: String?,
    /** The release page, used when there is no APK to point at. */
    val pageUrl: String,
    /** Size of the APK in bytes, 0 when unknown. */
    val apkBytes: Long
) {
    /** "2.5.3" — the tag with its decorative v removed. */
    val version: String get() = tag.trimStart('v', 'V')

    /** Where the download button should send someone. Prefers the APK itself. */
    val downloadUrl: String get() = apkUrl ?: pageUrl

    /** "17.5 MB", or null when GitHub did not tell us. */
    val apkSize: String?
        get() = if (apkBytes <= 0) null
        else String.format("%.1f MB", apkBytes / 1_048_576.0)
}

/**
 * Is [candidate] a later version than [current]?
 *
 * Compares the numbers and nothing else: "v2.10.0" beats "2.9.4", which a string
 * comparison gets backwards, and a suffix like "-beta" is ignored rather than guessed
 * at. Anything unparseable returns false, so a malformed tag can never nag someone into
 * downloading a release that is actually older than what they are running.
 */
fun isNewerVersion(candidate: String, current: String): Boolean {
    val a = versionParts(candidate)
    val b = versionParts(current)
    if (a.isEmpty() || b.isEmpty()) return false
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

private fun versionParts(raw: String): List<Int> =
    raw.trimStart('v', 'V')
        .substringBefore('-')
        .split('.')
        .mapNotNull { part -> part.takeWhile { it.isDigit() }.toIntOrNull() }

/**
 * Asks GitHub what the newest release is.
 *
 * Unauthenticated, because the repository is public and the alternative is shipping a
 * token inside an APK that anyone can unzip. That caps us at 60 requests an hour per IP,
 * which is roughly sixty times more than a once-a-day check will ever need.
 */
object GitHubReleases {

    const val OWNER = "Past-da-king"
    const val REPO = "rmblr"

    const val RELEASES_URL = "https://github.com/$OWNER/$REPO/releases"
    const val ISSUES_URL = "https://github.com/$OWNER/$REPO/issues"
    const val NEW_ISSUE_URL = "https://github.com/$OWNER/$REPO/issues/new"

    private const val LATEST_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fetch the latest release, or fail with a message worth showing someone.
     *
     * Runs on IO. Every failure — no network, rate limit, a repository with no releases
     * yet — comes back as a failed Result rather than an exception, because the caller is
     * usually a background check that must not take the app down with it.
     */
    suspend fun latest(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "RMBLR/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) error("No releases published yet.")
                if (response.code == 403) error("GitHub is rate limiting us. Try again later.")
                if (!response.isSuccessful) error("GitHub returned ${response.code}.")

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) error("GitHub returned an empty response.")

                parse(JSONObject(body))
            }
        }
    }

    /** Visible for tests: turn one release object into a [ReleaseInfo]. */
    fun parse(json: JSONObject): ReleaseInfo {
        val tag = json.optString("tag_name").ifBlank { error("Release has no tag.") }
        val assets = json.optJSONArray("assets")

        var apkUrl: String? = null
        var apkBytes = 0L
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url").ifBlank { null }
                    apkBytes = asset.optLong("size", 0L)
                    break
                }
            }
        }

        return ReleaseInfo(
            tag = tag,
            title = json.optString("name").ifBlank { tag },
            notes = json.optString("body").trim(),
            apkUrl = apkUrl,
            pageUrl = json.optString("html_url").ifBlank { "$RELEASES_URL/tag/$tag" },
            apkBytes = apkBytes
        )
    }
}

package io.github.pastdaking.rmblr.update

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The two pieces of update logic that can be wrong without anyone noticing.
 *
 * A version comparison that gets 2.10 vs 2.9 backwards produces an app that nags people
 * to "upgrade" to something older than what they have, forever, and it would look like a
 * server problem rather than a string problem. A parser that trips over a release with no
 * APK attached takes the whole background check down. Neither is visible in a screenshot,
 * so they are tested.
 *
 * Robolectric because org.json is an Android class: the stub on the plain unit-test
 * classpath throws on every method rather than parsing anything.
 */
@RunWith(RobolectricTestRunner::class)
class ReleaseTest {

    @Test
    fun `a higher patch is newer`() {
        assertTrue(isNewerVersion("2.5.4", "2.5.3"))
    }

    @Test
    fun `the same version is not newer`() {
        assertFalse(isNewerVersion("2.5.3", "2.5.3"))
    }

    @Test
    fun `an older version is not newer`() {
        assertFalse(isNewerVersion("2.5.2", "2.5.3"))
    }

    @Test
    fun `a leading v is decoration and is ignored`() {
        assertTrue(isNewerVersion("v2.6.0", "2.5.3"))
        assertFalse(isNewerVersion("v2.5.3", "2.5.3"))
    }

    @Test
    fun `ten is greater than nine, which a string comparison gets backwards`() {
        assertTrue(isNewerVersion("2.10.0", "2.9.4"))
        assertFalse(isNewerVersion("2.9.4", "2.10.0"))
    }

    @Test
    fun `a shorter version is padded rather than treated as smaller`() {
        assertTrue(isNewerVersion("3.0", "2.9.9"))
        assertFalse(isNewerVersion("2.5", "2.5.0"))
    }

    @Test
    fun `a suffix is dropped, not guessed at`() {
        assertTrue(isNewerVersion("2.6.0-beta", "2.5.3"))
        assertFalse(isNewerVersion("2.5.3-beta", "2.5.3"))
    }

    @Test
    fun `nonsense never offers an update`() {
        assertFalse(isNewerVersion("nightly", "2.5.3"))
        assertFalse(isNewerVersion("", "2.5.3"))
    }

    @Test
    fun `a release with an apk yields a direct download`() {
        val release = GitHubReleases.parse(
            JSONObject(
                """
                {
                  "tag_name": "v2.5.2",
                  "name": "v2.5.2 — translating is the app now",
                  "body": "Some notes.",
                  "html_url": "https://github.com/Past-da-king/rmblr/releases/tag/v2.5.2",
                  "assets": [
                    {"name": "rmblr-v2.5.2.apk",
                     "browser_download_url": "https://example.invalid/rmblr.apk",
                     "size": 17588973}
                  ]
                }
                """.trimIndent()
            )
        )

        assertEquals("2.5.2", release.version)
        assertEquals("https://example.invalid/rmblr.apk", release.downloadUrl)
        assertEquals("16.8 MB", release.apkSize)
    }

    @Test
    fun `a release with no apk falls back to its page`() {
        val release = GitHubReleases.parse(
            JSONObject(
                """
                {
                  "tag_name": "v2.5.2",
                  "html_url": "https://github.com/Past-da-king/rmblr/releases/tag/v2.5.2",
                  "assets": []
                }
                """.trimIndent()
            )
        )

        assertNull(release.apkSize)
        assertEquals(
            "https://github.com/Past-da-king/rmblr/releases/tag/v2.5.2",
            release.downloadUrl
        )
        // A release with no name falls back to the tag rather than showing an empty line.
        assertEquals("v2.5.2", release.title)
    }

    @Test
    fun `every bundled changelog entry has notes`() {
        // A version listed with an empty body would show an empty What's New sheet, which
        // is worse than not showing one at all.
        Changelog.entries.forEach { entry ->
            assertTrue(
                "${entry.version} has no headline",
                entry.headline.isNotBlank()
            )
            assertTrue("${entry.version} has no notes", entry.notes.isNotBlank())
        }
    }

    @Test
    fun `the shipping version has a changelog entry`() {
        // The one that breaks silently: bump versionName in build.gradle.kts, forget to
        // add the entry, and the update ships with no What's New sheet at all.
        assertTrue(
            "No Changelog entry for ${io.github.pastdaking.rmblr.BuildConfig.VERSION_NAME}",
            Changelog.forVersion(io.github.pastdaking.rmblr.BuildConfig.VERSION_NAME) != null
        )
    }
}

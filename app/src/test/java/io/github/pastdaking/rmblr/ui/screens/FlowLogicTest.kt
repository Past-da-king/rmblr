package io.github.pastdaking.rmblr.ui.screens

import io.github.pastdaking.rmblr.orb.AppProfile
import io.github.pastdaking.rmblr.ui.components.isBoldHeading
import io.github.pastdaking.rmblr.update.Changelog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowLogicTest {

    private fun profile(id: String, packages: List<String>, tap: String = "A", up: String = "B", down: String = "C", left: String = "D", right: String = "E") =
        AppProfile(id, id.replaceFirstChar { it.uppercase() }, packages, tap, up, down, left, right)

    @Test fun `hold count is the number of different tones on the arc`() {
        assertEquals(5, holdToneCount(profile("x", listOf("a"))))
        assertEquals(4, holdToneCount(profile("x", listOf("a"), left = "A")))
        assertEquals(1, holdToneCount(profile("x", listOf("a"), "A", "A", "A", "A", "A")))
    }

    @Test fun `labels read naturally`() {
        assertEquals("Hold for 1 tone", holdLabel(1))
        assertEquals("Hold for 4 tones", holdLabel(4))
        assertEquals("1 app", appCountLabel(profile("x", listOf("a"))))
        assertEquals("3 apps", appCountLabel(profile("x", listOf("a", "b", "c"))))
        assertEquals("Every app not listed", appCountLabel(profile("x", emptyList())))
    }

    @Test fun `the last profile without apps cannot be deleted`() {
        val chat = profile("chat", listOf("a"))
        val fallback = profile("default", emptyList())
        assertFalse(canDeleteProfile(fallback, listOf(chat, fallback)))
        assertTrue(canDeleteProfile(chat, listOf(chat, fallback)))
        assertTrue(canDeleteProfile(fallback, listOf(chat, fallback, profile("other", emptyList()))))
    }

    @Test fun `glyph follows the name and the fallback wins`() {
        assertEquals(ProfileGlyph.CHAT, glyphFor(profile("x", listOf("a")).copy(name = "Group chat"), false))
        assertEquals(ProfileGlyph.EMAIL, glyphFor(profile("x", listOf("a")).copy(name = "Email"), false))
        assertEquals(ProfileGlyph.WORK, glyphFor(profile("x", listOf("a")).copy(name = "Clients"), false))
        assertEquals(ProfileGlyph.GENERIC, glyphFor(profile("x", listOf("a")).copy(name = "Zebra"), false))
        assertEquals(ProfileGlyph.ANYWHERE, glyphFor(profile("x", emptyList()).copy(name = "Email"), true))
    }

    private val apps = listOf(AppEntry("Notes", "com.n"), AppEntry("Browser", "com.b"), AppEntry("mail", "com.m"))

    @Test fun `search matches name or package, any case`() {
        assertEquals(listOf("Browser"), filterApps(apps, "brow").map { it.label })
        assertEquals(listOf("mail"), filterApps(apps, "COM.M").map { it.label })
        assertEquals(3, filterApps(apps, "  ").size)
    }

    @Test fun `chosen apps come first and each group is alphabetical`() {
        assertEquals(listOf("Notes", "Browser", "mail"), orderForPicker(apps, setOf("com.n")).map { it.label }.let { listOf(it[0]) + it.drop(1) })
        assertEquals(listOf("Browser", "mail", "Notes"), orderForPicker(apps, emptySet()).map { it.label })
    }

    @Test fun `toggling a package adds then removes it`() {
        val p = profile("x", listOf("a"))
        val added = togglePackage(p, "b")
        assertEquals(listOf("a", "b"), added.packages)
        assertEquals(listOf("a"), togglePackage(added, "b").packages)
    }

    @Test fun `walkthrough has five steps and finishing needs all four permissions`() {
        assertEquals(5, OnboardingFlow.STEPS)
        assertEquals("Next", OnboardingFlow.primaryLabel(0))
        assertEquals("Continue", OnboardingFlow.primaryLabel(3))
        assertEquals("Finish setup", OnboardingFlow.primaryLabel(4))
        assertTrue(OnboardingFlow.isLast(4))
        assertEquals(0, OnboardingFlow.clamp(-3))
        assertFalse(OnboardingFlow.canFinish(PermissionState(true, true, false, true)))
        assertTrue(OnboardingFlow.canFinish(PermissionState(true, true, true, true)))
    }

    @Test fun `permission state lists what is missing in order`() {
        val s = PermissionState(microphone = true, overlay = false, accessibility = false, notifications = true)
        assertEquals(listOf(Permission.OVERLAY, Permission.ACCESSIBILITY), s.missing)
        assertFalse(s.orbReady)
        assertTrue(PermissionState(false, true, true, false).orbReady)
    }

    @Test fun `a bold-only line is a heading`() {
        assertTrue(isBoldHeading("**New**"))
        assertFalse(isBoldHeading("**Bold** and more"))
        assertFalse(isBoldHeading("plain"))
    }

    @Test fun `the 2_7_0 entry exists, leads the list and names no tools`() {
        val entry = Changelog.entries.first()
        assertEquals("2.7.0", entry.version)
        assertTrue(entry.notes.contains("**New**") && entry.notes.contains("**Better**") && entry.notes.contains("**Fixed**"))
        val text = (entry.headline + entry.notes).lowercase()
        listOf("claude", "gpt", "generated", "assistant", "todo", "lorem").forEach { assertFalse(it, text.contains(it)) }
    }
}

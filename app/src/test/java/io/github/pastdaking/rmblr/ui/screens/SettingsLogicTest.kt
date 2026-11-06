package io.github.pastdaking.rmblr.ui.screens

import io.github.pastdaking.rmblr.data.Provider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {

    @Test fun `a long key keeps its first and last four characters`() {
        val masked = maskKey("AIzaSyD-example-key-3f9a")
        assertTrue(masked.startsWith("AIza"))
        assertTrue(masked.endsWith("3f9a"))
        assertTrue(masked.contains('•'))
        assertFalse(masked.contains("example"))
    }

    @Test fun `a short key is all dots`() {
        assertEquals("•••••", maskKey("abcde"))
    }

    @Test fun `a blank key masks to nothing`() {
        assertEquals("", maskKey("   "))
    }

    @Test fun `masking never changes length by more than the dots allow`() {
        val masked = maskKey("gsk_" + "x".repeat(40) + "k2Qz")
        assertTrue(masked.length <= 20)
    }

    @Test fun `the left end of the fade slider means never`() {
        assertEquals(false to 60, IdleFadeScale.fromPosition(0f))
        assertEquals(0f, IdleFadeScale.position(false, 5), 0f)
    }

    @Test fun `the right end of the fade slider is the shortest wait`() {
        assertEquals(true to 2, IdleFadeScale.fromPosition(1f))
    }

    @Test fun `fade slider round trips the stored seconds`() {
        for (s in listOf(2, 3, 10, 30, 45, 60)) {
            val back = IdleFadeScale.fromPosition(IdleFadeScale.position(true, s))
            assertTrue("fades for $s", back.first)
            assertEquals(s, back.second)
        }
    }

    @Test fun `fade label names the wait`() {
        assertEquals("After 3 s", IdleFadeScale.label(true, 3))
        assertEquals("Never", IdleFadeScale.label(false, 3))
    }

    @Test fun `fingerprints differ when the key differs and ignore stray spaces`() {
        assertNotEquals(KeyChecks.fingerprint("abc12345"), KeyChecks.fingerprint("abc12346"))
        assertEquals(KeyChecks.fingerprint("abc12345"), KeyChecks.fingerprint(" abc12345 "))
    }

    @Test fun `fingerprint does not contain the key`() {
        assertFalse(KeyChecks.fingerprint("supersecretkey").contains("secret"))
    }

    @Test fun `a blank key is refused before any request is made`() = runBlocking {
        val result = KeyTester.test(Provider.GROQ, "   ", "https://api.groq.com/openai/v1")
        assertFalse(result.ok)
        assertEquals(KeyTester.blankMessage(Provider.GROQ), result.message)
    }

    @Test fun `a custom provider with no address is refused before any request is made`() = runBlocking {
        val result = KeyTester.test(Provider.CUSTOM, "sk-123456", "")
        assertFalse(result.ok)
        assertTrue(result.message.contains("base URL"))
    }
}

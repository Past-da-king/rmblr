package io.github.pastdaking.rmblr.update

/**
 * One entry in the app's own history.
 *
 * [notes] is the same markdown-lite the GitHub release bodies are written in — a blank
 * line between paragraphs, `**bold**` for the lead of a point, and `- ` for a bullet.
 */
data class ChangelogEntry(
    val version: String,
    val headline: String,
    val notes: String
)

/**
 * What has been done, kept in the app rather than fetched.
 *
 * There are two different questions and they need two different sources. "What is in the
 * version I could install?" can only be answered by GitHub, because that release does not
 * exist on this phone yet. "What changed in the version I just installed?" must be
 * answerable with no network at all — it is shown the first time the app opens after an
 * update, which is very often the moment somebody is on a train with one bar. So the
 * notes for shipped versions are compiled in, and the notes for the next one are
 * downloaded.
 *
 * Adding a version: put it at the TOP, and bump versionName in app/build.gradle.kts to
 * match. [current] finds itself by name, so a mismatch simply shows nothing rather than
 * showing the wrong release's notes.
 */
object Changelog {

    /**
     * Where this is going.
     *
     * Shown under the release notes on the What's New sheet. It is the answer to "why
     * should I keep this installed" — a changelog on its own only ever says what was
     * fixed, never what it is for.
     */
    const val VISION = """
RMBLR is meant to be the fastest way to get a thought out of your head and into a text box, in any app, in any language — and to stay yours while doing it.

**Three things that will not change**
- **No account.** There is nothing to sign up for and nothing to sign in to. The app has no server of its own and never will.
- **No subscription.** It is free, and the code is open. You bring your own API key and pay whoever is doing the listening, directly, at their price.
- **Nothing of yours leaves the phone** except the audio you deliberately dictate, and that goes straight to the provider you picked. There is no analytics, no telemetry, and no middle.

**What is being worked on**
- Fewer taps between wanting to say something and it being written down.
- More engines, including ones that run on the phone itself with no key and no network.
- Tones and profiles that get better at guessing which one you meant.
- Getting it onto F-Droid, so updating does not mean downloading an APK by hand.

If something on that list matters to you more than the rest of it, say so — Feedback is in Settings, and it goes straight to the person building this.
"""

    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "2.6.0",
            headline = "The orb can get out of the way",
            notes = """
All of this came from the first issue anybody opened on the repo. Nothing here changes by itself — every switch below starts off, so the orb looks and behaves exactly as it did until you say otherwise.

**Fade when idle**
- Leave the orb alone and it **dims and shrinks** back out of the way. Touch it and it is instantly solid again.
- **Settings on the home screen → Size and place** — turn it on, choose how long it waits, and how faint it gets.
- It only fades what you see. The orb stays exactly as easy to hit while it is small.

**Translucency as a setting**
- A **Solid** slider in the same place, from fully opaque down to barely there. It applies all the time, not just when idle, and the preview above the slider moves with it.

**Only with the keyboard**
- A new switch next to *Keep it on screen*. With it on, the orb waits for the **keyboard to actually be up** rather than appearing the moment a text box takes the cursor.
- It needs the accessibility service that RMBLR already uses to know you are in a text box — no new permission is asked for.
"""
        ),
        ChangelogEntry(
            version = "2.5.3",
            headline = "It can tell you when there is a new one",
            notes = """
Until now the only way to find out RMBLR had been updated was to go and look.

**Updates**
- **Settings → Updates** asks GitHub what the newest release is, tells you what is in it, and downloads it in your browser.
- It checks by itself about once a day and **notifies you** when there is something new. Tap the notification to read what changed; tap Download to go straight to the APK.
- You can turn the checking off entirely, or skip a single version and be left alone until the one after it.

**What's new, on first open**
- After an update, the app opens once on a sheet saying what changed and where the thing is going. It appears once per version and never again.

**Feedback**
- **Settings → Feedback** — say what is broken or what is missing. It opens a prefilled GitHub issue with the version and device already filled in, or copies the report if you would rather send it another way.

**A fix**
- The walkthrough's illustrations lost their outlines in light mode, leaving cards floating on the background with no edge.
"""
        ),
        ChangelogEntry(
            version = "2.5.2",
            headline = "Translating is the app now, not a setting at the bottom",
            notes = """
The last release could translate. Nothing about the app admitted it.

**Translating moved to the front**
- **Translate what you copy** sits in the first card, and warns you when the provider behind it has no key instead of failing silently on your first copy.
- The **languages on the arc** are chips — type, press the plus, tap the cross to remove one.
- The two scattered test boxes are **one Try it panel** holding both the mic and the translation.

**The walkthrough**
- A new page for translating, and it no longer pretends Gemini is the only provider it can talk to.
- **Settings can replay it**, because a walkthrough seen once is one you can never go back to.

**Two fixes**
- **The last words of a dictation were being clipped.** A transcript now has to land after you release the microphone before any completion counts.
- **The orb switch read ON after the app was killed** while nothing was listening. It is reconciled every time the app comes back to the front.
"""
        ),
        ChangelogEntry(
            version = "2.5.0",
            headline = "Stop losing everything said after a pause",
            notes = """
**The pause bug**
- A gap in the middle of a sentence could settle the session early and throw away everything after it. It waits properly now.

**Six ways to dictate**
- Gemini, Groq, Mistral, OpenRouter, OpenAI and any OpenAI-compatible endpoint you host yourself.
"""
        ),
        ChangelogEntry(
            version = "2.3.0",
            headline = "Double tap the orb to translate",
            notes = """
**Translate as you speak**
- Double tap switches the orb into translating. The arc fills with your languages instead of your tones — say it in English, have it land in Japanese, in one call.
"""
        ),
        ChangelogEntry(
            version = "2.2.0",
            headline = "History you can search, and snippets that do something",
            notes = """
**History**
- Search everything you have dictated, and send any of it back through a different tone.

**Snippets**
- Saved phrases you can drop into a field without saying them.
"""
        )
    )

    /** The notes for a given version name, or null if that version was never listed. */
    fun forVersion(version: String): ChangelogEntry? =
        entries.firstOrNull { it.version == version }
}

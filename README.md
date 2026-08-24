![RMBLR](docs/banner.png)

# RMBLR

Speak into any Android app. RMBLR writes it down, tidies it up, and puts the text
straight into the box you were typing in.

No account. No subscription. Your own Gemini API key, talking directly to Google.

**[Download the latest APK →](https://github.com/Past-da-king/rmblr/releases/latest)**

---

## What it does

A small orb appears **only when a text field has the cursor** — never sitting on your
home screen. From there:

| Gesture | What happens |
|---|---|
| **Tap** | Dictate. The transcript is written into the field. |
| **Hold** | An arc of tones fans out. Slide to one, release, speak. |
| **Flick** | Fires that direction's tone immediately, skipping the menu. |
| **Drag** | Park it anywhere. It remembers. |

### Per-app profiles

The tones on the arc change with the app you are in. WhatsApp offers casual ones,
Gmail offers professional ones, Slack sits in between. Pick the apps and the tones
once, in the Profiles tab.

### Your own tones

A tone is a name and a system prompt, nothing more. The built-ins are a starting
point — write your own ("Rewrite this as a short LinkedIn post, no hashtags") and it
appears on the arc alongside them.

---

## What it looks like

| The arc, over whatever you were typing in | Size, placement, permissions | Per-app profiles |
|---|---|---|
| ![arc](docs/screenshot-arc.png) | ![home](docs/screenshot-home.png) | ![profiles](docs/screenshot-profiles.png) |

---

## Setup

1. **Draw over other apps** — so the orb can sit above what you are typing in.
2. **Accessibility → RMBLR orb** — the only Android API that can tell an app a text
   field has focus, and write text back into another app's field. Without it the orb
   cannot appear on cue and can only copy to the clipboard.
3. **A Gemini API key** — free to start, from
   [Google AI Studio](https://aistudio.google.com/apikey). Paste it on the last
   onboarding page or in Settings.

The key is stored on your device and used to call Google directly. Nothing is routed
through any server of ours, because there isn't one.

---

## Models

Transcription runs on `gemini-3.1-flash-live-preview` over the Live WebSocket API,
which handles isiZulu and code-switching noticeably better than the REST models. If it
is unavailable or out of quota, the client walks down a fallback chain
(`gemini-3.5-flash`, then `gemini-3.1-flash-lite`) rather than losing what you said.

---

## Building

```bash
git clone https://github.com/Past-da-king/rmblr.git
cd rmblr
./gradlew assembleDebug
```

A debug build needs no configuration. For a signed release, create
`keystore.properties` in the project root:

```properties
storeFile=your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Both that file and any `*.jks` are gitignored. Without it, `assembleRelease` simply
produces an unsigned APK.

**Requirements:** Android 7.0+ (API 24). Built with Kotlin and Jetpack Compose.

---

## Licence

MIT. See [LICENSE](LICENSE).

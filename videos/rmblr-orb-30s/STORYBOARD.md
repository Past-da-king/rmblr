---
format: 1920x1080
duration: 30s
message: "One orb, five gestures — it appears when you type, and writes into whatever app you were typing in."
arc: The orb appears because you typed → Tap → Hold → Flick (Gmail) → Drag (Slack) → Double tap to translate → What it costs you
audience: Android users and developers meeting RMBLR cold on Reddit, Product Hunt and GitHub
mode: collaborative
music: none
fps: 60
---

## Video direction

**The one idea — "One thumb, four verbs."** RMBLR's subject is a gesture, not a screen.
So the film is a gesture vocabulary demonstrated, and it is built on one law:

> ⭐ **THE ANCHOR CONTRACT. The orb's centre sits at frame pixel `(1000, 540)` and its size
> never changes, for all thirty seconds.** It does not drift, breathe, slide, scale or get
> chased. Everything else — the app behind it, the thumb, the arc, the text, the language —
> changes around it. The stillness of the orb *is* the claim: everything changes, this stays.
> There is **no camera move anywhere in this film.** No push-in, no pan, no parallax drift,
> not "for energy". Adding one breaks the contract and the film with it.

This is `fixed-anchor-cycle` applied at film scale, so it also answers *follow the thumb*
in the strongest way available: the camera is parked **on** the orb permanently, and every
touch arrives into a locked frame where you cannot miss it.

### The stage — identical in every frame, never re-derived

| Thing | Value |
|---|---|
| Canvas | 1920 × 1080, 60fps |
| `--dp` | `2.1px` (see `frame.md` § The unit) |
| Phone screen | 393 × 851dp → **x 260 → 1085**, full-bleed vertically (the 851dp screen is 1787px tall, so the frame shows a 1080px vertical slice of it — it bleeds off the top and bottom edges, deliberately) |
| Screen edge | 1px `--line` hairline down x=260 and x=1085; no shadow, no rounded device bezel, no phone chrome, no notch |
| **Orb centre** | **(1000, 540)** — pinned, 52dp box = 109px |
| The arc | opens **LEFT**, away from the parked edge. Radius 116dp = 244px; chips 269 × 71px |
| Claim column | x **1180 → 1810**, left-aligned. Type only — never a card, never a panel |
| Caption keep-out | nothing meaningful below **y = 880** |

Left of the screen (x 0 → 260) is the film's own ground, `--bg`, carrying one very faint
`--accent-wash` radial bloom centred behind the orb at about 12% — the only thing in the
film that is decorative, and it never moves either.

### Palette — from `frame.md`, never invented

The film's ground is the app's ground: `--bg #0A0B0D`. App surfaces `--surface #15171B`,
`--raised #1E2127`, hairlines `--line #2B2F37`. The claim column uses the three-step text
ladder `--text #EDEFF3` → `--text-muted #9AA1AD` → `--text-light #666D7A`. **`--primary
#7C6CFF` is the only accent and it is rationed**: it appears on the selected arc chip, the
working orb, the caret, and the wordmark — nowhere else. `--negative #E05561` means
recording and only recording; `--positive #4FBF87` means done and only done.

### Motion grammar and the reveal model

- **There is no narration.** The film is cut on a hard **120 BPM grid — beat = 0.5s, bar =
  2s** — so a track drops in later with no re-edit. Every reveal, gesture and cut lands on a
  beat. Where the method says "pace reveals to the voiceover", here it means **pace reveals
  to the beat grid**, and the anti-front-loading rule stands unchanged: at t=0 a frame shows
  only what it is saying at t=0, and the rest arrives across the shot, especially the back half.
- Long-tail eases, `power3` by default. Smooth over bouncy. The single exception is the
  arc's springing chips, which carry a restrained overshoot because the app's own fan does.
- **The gesture is the motion.** Every beat's movement is caused by the thumb: a touch
  arrives, and the UI answers it. Nothing on screen ever moves for decoration.
- **The thumb.** A soft-edged circular contact patch, 36dp, `--text` at 18% with a 1px
  `--text` ring at 40% — a fingertip's footprint, not a mouse cursor and not a hand
  illustration. It **enters from the bottom of the frame**, does its verb at the orb, and
  leaves the same way. It is never on screen when it is not doing something.
- **The gesture label** (TAP / HOLD / FLICK / DOUBLE TAP) is the one piece of type allowed
  to punch: it hard-cuts in on the beat the touch lands, in `--text`, and holds still.

### Rhythm — where the film breathes

Frames 1 and 6 are **held beats**: content resolves early and reads still. Frames 2, 3 and
5 each run one gesture to its consequence. Frame 4 is the busiest and the film's climax —
it earns it by being the only frame that changes the world twice.

### Negative list — what never appears

Camera moves of any kind · a mouse cursor or a drawn hand · a phone bezel, notch, status
bar or device mockup · a microphone glyph in place of the wave mark · the green Material
You accent from `docs/screenshot-arc.png` · drop shadows · floating bokeh or purple-blue
"AI" gradients · nav bars, footers or scrollbars · **"X, not Y" antithesis or any sentence
beginning "Most apps"** · snippet shortcuts, sound settings or keyboard themes (half-built
or dead — never on screen) · both motion failure modes: the slideshow (everything dumped at
t=0 then frozen) and the screensaver (things drifting for no reason).

---

> ⚠ **This board describes v2.** v1 was rejected — "low-grade slop… I like the concept,
> it's just a horrible execution." The concept survived; the execution was rebuilt. See
> `m_0f2fcbf08666` for his three notes verbatim. The three changes that matter:
> **(1)** the apps are real recreations of WhatsApp, Gmail and Slack, not neutral surfaces;
> **(2)** RMBLR does **not** stream text into the field — it records, loads, and then the
> finished string **lands whole at the cursor**, because it writes into another app through
> accessibility and cannot type incrementally; **(3)** **drag** is in, and it is the turn.
> v1's absolute camera lock is also gone: the camera pulls wide to establish each app and
> pushes in for each gesture, because the arc was illegible at rest.

## Frame 1 — WhatsApp · it appears because a field took the cursor
- scene: real WhatsApp, keyboard up, empty composer; the caret starts and the orb arrives beside it
- duration: 3.5s
- transition_in: cut
- status: animated
- src: index.html (monolith — see below)

## Frame 2 — TAP
- scene: the thumb taps; the orb records, then loads, then the sentence LANDS WHOLE in the composer
- duration: 4.5s
- status: animated

## Frame 3 — HOLD
- scene: the thumb holds; the arc of tones fans out over a scrimmed, defocused WhatsApp; sliding lights each chip
- duration: 5.5s
- status: animated

## Frame 4 — Gmail · FLICK
- scene: the app is Gmail, established at full width; a flick fires a tone with no menu, and the arc's tones have changed with the app
- duration: 4.5s
- status: animated

## Frame 5 — Slack · DRAG
- scene: the app is Slack; the thumb picks the orb up and parks it somewhere else — the first time it has moved — then holds for a third set of tones
- duration: 5.0s
- status: animated

## Frame 6 — DOUBLE TAP
- scene: back in WhatsApp, the orb wears 文A; said in English, it lands in isiZulu
- duration: 3.5s
- status: animated

## Frame 7 — The lockup
- scene: the app stops being in the way and the orb, which has been the mark all along, grows into it
- duration: 3.5s
- status: animated

---

## A note on the build

This is **one monolithic `index.html`, generated by `build.py` + `apps.py`** — not the
workflow's per-frame sub-agent fan-out. The orb and the three app surfaces have to be
pixel-identical across every cut, and parallel workers would each invent their own. `lint`'s
`composition_file_too_large` is expected and accepted. Rendering is remote only
(`hf-render-remote`), never on this laptop.

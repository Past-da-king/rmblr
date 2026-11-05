# Asset inventory — RMBLR

All assets are the app's own shipped material from `/home/past9/rmblr`. No web capture was
run: the source of truth for this film is the Kotlin itself, so the orb, the arc, the tone
chips and the translation bubble are **rebuilt in HTML from `orb/OrbUi.kt` and
`ui/theme/Color.kt`**, not composited from screenshots. The screenshots below are reference
and, in two places, real on-screen plates.

| File | What it is | Use in the film |
|---|---|---|
| `screenshot-home.png` | Onboarding page 2, OLED black, Material You green accent. An empty rounded text field with a blinking caret and the orb beside it, under "It shows up when you type". | **Reference for the cold open.** Rebuild, do not composite — the green is a wallpaper accent, not the brand. |
| `screenshot-arc.png` | Onboarding page 3, OLED black. The arc open: Professional / Clean up / Casual (highlighted) / Bullets / Summary, orb in its red RECORDING state. | **The canonical arc reference.** Exact chip labels and stacking come from here. |
| `screenshot-profiles.png` | The Profiles tab, light theme, true indigo accent #5A46F0. Cards: Chat 3 apps, Email 2 apps, Work 3 apps, Anywhere else. Bottom bar: Orb / Profiles / History / Settings. | Reference for the per-app beat if a real screen is needed. Preferred: state per-app through the arc itself changing. |
| `screenshot-speak.png` | The in-app dictation screen. | Reference only. |
| `screenshot-translate.png` | The translate flow. | Reference for the translation bubble. |
| `banner.png` | The README banner / wordmark. | Candidate for the end lockup. |
| `rmblr-icon.webp` | The launcher icon (xxhdpi round). Five bars rising to the middle — the same WaveMark drawn inside the orb. | End lockup. |

## Colour truth (from `ui/theme/Color.kt`)
Dark default: Ink `#0A0B0D` · Surface `#15171B` · Raised `#1E2127` · Line `#2B2F37` ·
TextHigh `#EDEFF3` · TextMid `#9AA1AD` · TextLow `#666D7A` · Accent `#7C6CFF` ·
AccentWash `#23204A` · Alert `#E05561` · Good `#4FBF87` · OnAccent `#FFFFFF`.
**Scrim `#07080A`, never derived from the theme** — 0.42 alpha behind the arc, 0.58 behind
the translation bubble.

## Orb geometry truth (from `orb/OrbUi.kt`)
Orb 52dp; body circle 0.84x; WaveMark 0.46x. WaveMark = five round-capped bars at relative
heights 0.34 / 0.66 / 1.0 / 0.72 / 0.42, gap = width / (5 × 2.2), bar width = gap.
Arc: radius 116dp, chips 128 × 34dp, 17dp corner radius, 150° of spread, 0° points right
and grows clockwise, and the fan always opens *away* from the parked edge.
Phases: RECORDING = Alert + a 0.22-alpha halo scaling 1.0→1.35 with mic amplitude;
WORKING = Accent + a spinning arc (360° per 900ms, sweep 40°→260° per 1100ms reversing);
DONE = Good; IDLE = Surface. Idle fade: scale 0.72 and fade over 600ms out, 140ms back.

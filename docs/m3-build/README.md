# Material 3 Expressive build: foundation

Branch `feat/m3-expressive`. Reference design: `design/m3-mocks` (mocks, tokens, notes).

## Theme (`ui/theme`)

Existing token names are kept, retuned to the design, so every screen that already writes `Ink`, `TextHigh`, `Accent` follows the new palette and still follows light, dark, OLED black and dynamic colour (`Skin`).

| Token | Role |
|---|---|
| `Ink` | surface (page background) |
| `Surface` | container-low (sheets, nav track) |
| `Raised` | container (cards, rows, panels) |
| `RaisedHigh`, `RaisedHighest` | container-high, container-highest (fields, chips, nested cards) |
| `Line` | outline-variant (outlines only where a tone will not do) |
| `TextHigh`, `TextMid`, `TextLow` | on-surface, on-surface-variant, outline |
| `Accent` | brand fill (indigo) |
| `AccentText` | primary for text and small controls (lighter indigo in dark) |
| `AccentWash`, `OnAccentWash` | primary-container and its content |
| `SecondaryWash`, `OnSecondaryWash` | secondary-container (selected chips) |
| `Teal`, `TealWash` | tertiary (translate, info) |
| `RecRed` | recording red, identical in both skins |
| `Alert`, `AlertWash`, `OnAlertWash` | error roles |
| `Good`, `GoodWash`, `OnGoodWash` | success (done) |

Red means recording or destructive, green means done. Nothing else.

- `Type.kt`: `GoogleSansFlex` (variable font bundled in `res/font/google_sans_flex.ttf`, SIL OFL, licence in this folder), `Typography` (display-s 36/42, headline 24 to 32), `EyebrowStyle`, `TimerStyle`.
- `Theme.kt`: `MyApplicationTheme` (unchanged entry point), `RmblrShapes` (8, 12, 16, 28, 32).

## Components (`ui/m3`)

| Component | File | Use |
|---|---|---|
| `ExpressiveShapes` (Cookie9, Cookie7, Clover4, SoftBurst, Sunny, Flower, Puffy, Gem, Circle, Squircle), `morph(a, b, t)`, `RadialShape(radial)` | Shapes.kt | any `Modifier.background(color, RadialShape(...))` or `.clip(...)`; avatars, empty states, shape morphing |
| `groupedCorners(index, count)` | Shapes.kt | 28 outer, 8 inner corners |
| `Eyebrow(text)` | Components.kt | tracked caps label |
| `TonalCard(container, shape, padding, onClick)` | Components.kt | the card: tonal fill, 28dp, no outline |
| `GroupedRow(index, count, onClick)` | Components.kt | row of a grouped list; put `Arrangement.spacedBy(4.dp)` between rows |
| `TonalTextField(value, onValueChange, placeholder, leadingIcon, trailing, minLines)` | Components.kt | the one text field (search, names, prompts) |
| `M3Chip(label, selected, onClick, leadingIcon)` | Components.kt | filter or choice chip (12dp, selected 18dp with check) |
| `AssistChip`, `ToneTag`, `StatusChip(label, Status.RECORDING/DONE/WORKING/ERROR/INFO)` | Components.kt | actions, tone tags, state chips |
| `TonalIconButton(icon, contentDescription, onClick)` | Components.kt | round 48dp tonal icon button |
| `RecordButton(recording, onClick, level, size, animate)` | Components.kt | cookie that morphs to a red stop button, halos swell with `level` |
| `WaveformBars(levels)`, `WaveformBuffer`, `amplitudeToLevel`, `sampleWaveform` | Components.kt | live bars, newest on the right |
| `LiveTranscript(splitLive(text))` | Components.kt | settled words in ink, guess lighter, soft caret |
| `formatElapsed(seconds)` | Components.kt | `0:14` |
| `RmblrSnackbarHost(state)` | Components.kt | inverse-surface snackbar with action, 12dp |
| `FloatingNav(items, selected, onSelect)`, `FloatingNavItem` | Navigation.kt | 4-tab floating bar, selected tab expands |
| `ConfirmSheet(title, body, confirmLabel, destructive, ...)`, `SheetButton` | ConfirmSheet.kt | confirmations, always a slide-up sheet |

Existing components restyled in place (same signatures): `RmblrSheet` (ModalBottomSheet, 28dp top, slim handle), `SheetOption` (selected row morphs to a 24dp pill with a check), `Panel` (tonal, no outline), plus `ScreenHeader`, `SectionLabel`, `PrimaryAction`, `QuietAction`, `SettingRow` which pick up the new tokens and type.

Phone rule: every choice, form and confirmation is a slide-up sheet (`RmblrSheet`, `ConfirmSheet`). Never `AlertDialog` or `Dialog`.

## Screenshot tests

`app/src/test/.../ui/m3/ShotTest.kt`: extend it and call `shot("name") { Screen() }`. It draws the content at 412 x 915 dp in light then dark and writes `app/build/m3-proof/<name>-light.png` and `-dark.png`. Run on the server: `~/bin/build-slot ./gradlew :app:recordRoborazziDebug --tests '*YourShotTest'` then `./gradlew --stop`. Copy results to `docs/m3-build/proof/`.

## Build rules

Gradle only on the impetix server, only through `~/bin/build-slot ./gradlew ...` (needs `JAVA_HOME=/home/impetix/jdks/jdk-21.0.12.1+1` and a `local.properties` with `sdk.dir=/home/impetix/android-sdk`, not committed). Run `./gradlew --stop` after every job.

The old Home hub (permissions, orb setup, gestures) now lives in `ui/screens/OrbSetupScreen.kt` as `OrbSetupScreen`, unchanged. Helper B relocates it into Settings.

FOUNDATION READY

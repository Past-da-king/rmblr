# RMBLR: Material 3 Expressive design system (mocks)

Phone stills, 390 x 844 dp, rendered at 2x (780 x 1688 px), light and dark from one source. Sources: `_kit/tokens.css` (colour, shape, elevation), `_kit/components.css` + `_kit/rmblr.css` (components), `_kit/kit.js` (theme switch `?theme=dark`, shape library), `_kit/kit2.js` (system bars, nav, record button, orb, waveform, sample data). Fonts: Google Sans Flex and Material Symbols Rounded, bundled in `_kit/fonts`.

## Principles
1. **One accent, used with intent.** Indigo is the brand: seed `#5A46F0` (light) / `#7C6CFF` (dark). Primary and its containers carry selection and navigation. Red means recording, green means done and nothing else. Teal is a quiet tertiary for translate and info.
2. **Near-black ink, tonal surfaces.** Light base `#F7F8FA`, dark base `#0A0B0D`. Containers step up in tone (low, container, high, highest); no flat greys, no outlines where a tone will do.
3. **Shape is the brand.** The record button is a cookie that morphs to a rounded square when it records. App icons, avatars and empty states use the shape library (cookie, clover, puffy, gem, sunny, soft burst).
4. **Big, confident type.** Google Sans Flex, emphasised styles for titles (display-s 36/42, headline 24 to 32).
5. **Tonal fields everywhere** (search, tone name, prompts): filled tonal container, 28dp radius, focus ring in primary.
6. **Phone rule: no centred dialogs.** Every choice, form and confirmation is a slide-up sheet with a drag handle (language, tone, delete).
7. **4dp grid**, 16dp margins, 48dp touch targets, status bar 40dp, gesture bar drawn.

## Tokens (main roles)
| role | light | dark |
|---|---|---|
| brand / primary | `#5A46F0` | `#7C6CFF` brand, `#A79CFF` primary text and controls |
| primary-container | `#E3DEFF` | `#3A2FB4` |
| secondary-container | `#E3E1F7` | `#3F3E5C` |
| tertiary (teal) | `#0F6A78` / `#C3EAF1` | `#8FD3E0` / `#004E5A` |
| error / recording | `#D6362F` | `#FF8A80` |
| success / done | `#1D7A4B` / `#C4F0D5` | `#7FD9A1` / `#0B5230` |
| surface | `#F7F8FA` | `#0A0B0D` |
| containers | `#F1F2F8 #ECEEF6 #E6E8F1 #E0E2EC` | `#111217 #16171D #1F2027 #292A32` |
| on-surface | `#0A0B0D` | `#E6E6EE` |

Shape scale: 4, 8, 12, 16, 20, 28, 32, 48, full. Cards 28, grouped list rows 28 outer / 8 inner, chips 12 (selected 18), buttons full, sheets 28 top.

## Components
Floating nav (4 tabs: Home, Profiles, History, Settings; selected expands with its label), expressive record button (cookie to rounded-square stop, halos), floating orb, tone pill arc, waveform bars, tonal search field, connected button group, chips (assist, filter, tone tag), grouped list rows, slide-up sheets, snackbar, switch with check.

## Live text
Settled words are solid ink (`--on-surface`); the in-progress guess is `--on-surface-variant` at 62% with a soft indigo caret.

## Motion (notes only, not drawn on screens)
Spatial springs (damping .8, stiffness 380) for shape morph, press and sheets; effects springs for colour and opacity. Record button morphs cookie to rounded square on press; waveform bars ease with the audio level; settled text commits with a short fade from guess to ink.

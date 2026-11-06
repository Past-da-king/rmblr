# RMBLR: Material 3 Expressive mocks (design only, no app code)

Phone stills at 390 x 844 dp, rendered at 2x (780 x 1688 px), light and dark from one source.
RMBLR keeps its own identity: indigo seed `#5A46F0` light / `#7C6CFF` dark, near-black ink, tonal indigo containers, red for recording, green for done.
Language is a generic setting (Auto-detect by default) and all sample text is ordinary dictation.

## Files
- `index.html?screen=01-home&theme=dark`: the single app; loads `screens/<screen>.js`.
- `screens/*.js`: one file per screen (01 home, 02 recording, 03 result, 04 history, 05 history detail, 06 tone sheet, 07a/07b/07c orb overlay idle, arc, recording, 08 language sheet, 09 delete confirm sheet).
- `_kit/`: `tokens.css`, `components.css` (base), `rmblr.css`, `kit.js` (theme, shape library), `kit2.js` (bars, nav, record button, orb, waveform, fake chat), `fonts/` (Google Sans Flex, Material Symbols), `design-system.md`.
- `NOTES.md`: motion and behaviour notes per screen.
- `render.mjs`: renders every screen in light and dark to `png/` (`node render.mjs` or `node render.mjs 02 07`).
- `png/`: `01-home-light.png`, `01-home-dark.png` and so on.

## Render
```bash
npm i playwright && npx playwright install chromium   # once
node render.mjs                                        # needs Node 20+
```
Dark is `?theme=dark` (adds `class="dark"` on html).

## Rules followed
Every choice or confirmation is a slide-up sheet, never a centred dialog. Motion is described in `NOTES.md`, not drawn on the screens.

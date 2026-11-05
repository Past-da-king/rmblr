---
version: alpha
name: Blue Professional — Frame (video / frame layer)
description: >
  Video-first companion to Blue Professional's design.md. The unit is the frame (1920×1080). Atoms
  are identical and sacred — the warm cream canvas, a single saturated cobalt (#1e2bfa) as the only
  accent, the three-step gray text ladder, Roboto (display/numerals/chrome) + Roboto (body),
  soft cobalt-tinted cards (4% fill / 20% border / 10–14px radius) with NO shadows, pill chrome, and
  the cobalt progress bar. Composition + frame scale rewritten. Motion out of scope.
unit: the frame — 1920×1080 primary; 9:16 and 1:1 documented
principle: atoms are sacred · composition is free · numbers come from the script

colors:
  bg: "#0A0B0D"
  surface: "#15171B"
  raised: "#1E2127"
  line: "#2B2F37"
  primary: "#7C6CFF"
  accent-wash: "#23204A"
  text: "#EDEFF3"
  text-muted: "#9AA1AD"
  text-light: "#666D7A"
  on-accent: "#FFFFFF"
  scrim: "#07080A"
  accent-light: "rgba(124, 108, 255, 0.08)"
  accent-medium: "rgba(124, 108, 255, 0.15)"
  border: "rgba(124, 108, 255, 0.22)"
  card-bg: "rgba(124, 108, 255, 0.05)"
  positive: "#4FBF87"
  negative: "#E05561"

radii:
  pill: "100px"
  card-lg: "14px"
  card-md: "12px"
  card-sm: "10px"
  bar: "6px"
  circle: "50%"

typography:
  # — reading ramp (Roboto body + Roboto chrome) —
  body:    { fontFamily: "Roboto", cqw: 0.85, weight: 400, lineHeight: 1.6, color: "text-muted" }
  h4-eyebrow:{ fontFamily: "Roboto", cqw: 0.8, weight: 600, tracking: "0.08em", upper: true, color: "primary" }
  tag:     { fontFamily: "Roboto", px: 12, weight: 500, color: "primary" }
  counter: { fontFamily: "Roboto", px: 13, weight: 500, tracking: "0.05em", color: "text-muted" }
  # — display / numerical ramp (Roboto, near-black headings / cobalt numerals) —
  h3:      { fontFamily: "Roboto", cqw: 1.25, weight: 500, lineHeight: 1.3, tracking: "-0.02em", color: "text" }
  stat-num:{ fontFamily: "Roboto", cqw: 1.9, weight: 700, lineHeight: 1.0, color: "primary" }
  blockquote:{ fontFamily: "Roboto", cqw: 2.4, weight: 500, lineHeight: 1.35, color: "text" }
  h2:      { fontFamily: "Roboto", cqw: 2.6, weight: 600, lineHeight: 1.1, tracking: "-0.02em", color: "text" }
  metric-value:{ fontFamily: "Roboto", cqw: 3.0, weight: 700, lineHeight: 1.0, color: "primary" }
  h1:      { fontFamily: "Roboto", cqw: 4.2, weight: 700, lineHeight: 1.08, tracking: "-0.02em", color: "text" }
  quote-mark:{ fontFamily: "Roboto", cqw: 8.0, weight: 700, lineHeight: 0.5, color: "primary", opacity: 0.15 }

spacing:
  pad-x: "5cqw"
  pad-y-top: "5cqw"
  gap-cards: "1.4cqw"
  accent-line: "60px × 4px"

components:
  card-tinted:
    backgroundColor: "{colors.card-bg}"
    border: "1.5px solid {colors.border}"
    rounded: "{radii.card-lg}"
    shadow: "none"
    description: "Universal content card. Never solid-colored, never opaque-bordered, NO shadow."
  metric-card:
    backgroundColor: "{colors.card-bg}"
    border: "1.5px solid {colors.border}"
    rounded: "{radii.card-lg}"
    typography: "{typography.metric-value} ({colors.primary}) + {typography.metric-label} + {typography.metric-desc}"
    description: "+ optional inline ↑/↓ change chip ({colors.positive}/{colors.negative} text, no fill)."
  tag-pill:
    backgroundColor: "{colors.accent-light}"
    textColor: "{colors.primary}"
    rounded: "{radii.pill}"
    typography: "{typography.tag}"
    description: "Top-right of the slide-header."
  cta-button:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-accent}"
    rounded: "{radii.pill}"
    typography: "Roboto 600"
    shadow: "soft cobalt on hover only — the system's only shadow"
    description: "The one solid element."
  accent-line:
    backgroundColor: "{colors.primary}"
    size: "60×4, 2px radius"
    description: "Above cover titles / eyebrow separators."
  bar-track:
    backgroundColor: "{colors.accent-light}"
    fill: "{colors.primary} (display:block so width resolves)"
    rounded: "{radii.bar}"
    description: "28px track; fill carries the value."
  step-circle:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-accent}"
    rounded: "50%"
    size: "56px"
    description: "Sequential steps fade opacity 1.0→0.85→0.7→0.55."
  split-highlight:
    backgroundColor: "{colors.accent-light}"
    borderLeft: "4px solid {colors.primary}"
    rounded: "{radii.card-md}"
    description: "Inline pull-quote callout."
  slide-header:
    typography: "{typography.h4-eyebrow} (cobalt) left, tag-pill right; {typography.h2} below"
    description: "Top band of every content frame."
  atmosphere:
    elements: "clipped diagonal cobalt-tint panel, 3×3 cobalt dot grid, concentric closing rings"
    description: "Cover/closing only. Never on content frames."
  progress-bar:
    backgroundColor: "{colors.primary}"
    size: "3px tall, bottom edge, width grows with index"
    description: "Persistent progress strip."
---

# Blue Professional — Frame (video / frame layer)

## Brand adaptation (READ FIRST — the frontmatter is the source of truth)

This is the **blue-professional** preset remixed onto the captured brand. The YAML frontmatter above (colors · typography · components) is **normative and already correct — use it verbatim.** The prose below is the ORIGINAL preset's intent; read it THROUGH the frontmatter:

- **Fonts** — already set to **Roboto** (display) / **Roboto** (body); ignore any preset font name lingering in prose.
- **Colors** — use the frontmatter hex; preset color NAMES in prose (e.g. "cobalt", "cream") mean the remapped brand values.


## Overview

Blue Professional at frame scale is a **consulting-grade system: restraint with one strong
commitment.** A warm cream canvas and a single saturated cobalt that carries every accent —
eyebrow, metric, CTA, chart fill, progress bar. No secondary brand color, no pastels, just cream,
cobalt, and a tight ladder of grays. The register is investment-research / McKinsey briefing:
measured, data-dense without crowding, executive-readable at distance.

The voice is two faces in fixed roles: **Roboto** (display, every numeral, all chrome —
eyebrows uppercase 0.08em) and **Roboto** (body, muted gray, line 1.6). Headlines are near-black;
cobalt is reserved for accent moments. Depth is **soft and tinted** — 4% cobalt card fills with 20%
cobalt borders and 10–14px radii — never shadowed. The lack of harsh shadows is the premium signal.

**Key characteristics at frame scale:**

- **Warm cream ground** on every frame; **single cobalt** as the only accent.
- **Roboto** (display/numerals/chrome) + **Roboto** (body) — near-black headlines, cobalt numerals.
- **Tinted cards** — cobalt 4% fill, cobalt 20% 1.5px border, 10–14px radius, **no shadow**.
- **Pill chrome** (100px) — tag pills + the one solid cobalt CTA; cobalt **progress bar**.
- **Soft rounded corners everywhere** (no square corners save the progress bar).
- **Atmosphere** (diagonal panel, dot grid, concentric rings) on cover/closing only.

## The Frame

### Frame Craft Bar

Three eyeball tests gate every frame before any structural check:

- **Squint** — one **near-black headline or cobalt numeral** dominates at 3–6× its neighbor.
- **Silence** — content frames read **balanced, not crowded**; the **dashboard is the one dense exception**.
- **Restraint** — a **single cobalt accent** carries everything; headlines stay near-black (never cobalt); no shadows (tinted cards do the lift); positive/negative inline-text only.
- **Reference** — aim at an **investment-research / McKinsey quarterly briefing**; failure looks like a **heavy-outlined, multi-color dashboard**.

- **Primary:** 1920×1080 (16:9). Display authored in **`cqw`** (`px ÷ 1920 × 100 = cqw`).
- **Vertical:** 1080×1920 (9:16). **Square:** 1080×1080 (1:1).
- **Safe area:** `pad-x` 5cqw; bottom reserves room for the counter + progress bar.

**The container law (load-bearing).** Every frame ground sets `container-type: size`; ALL
frame-relative units are `cqw`/`cqh` against it — never `vw`. Card radii stay px (10–14px); the
pill radius stays 100px; borders stay 1–1.5px.

## Colors

Tokens identical to the source. `{colors.bg}` cream is the universal ground; `{colors.primary}`
cobalt is the **only** accent — every eyebrow, numeral, CTA, chart fill, progress bar, and the 4px
highlight left-rule. Headlines are `{colors.text}` near-black (never cobalt); body is
`{colors.text-muted}`; tertiary is `{colors.text-light}`. Cards fill `{colors.card-bg}` (4%) with
`{colors.border}` (20%) borders. `{colors.positive}`/`{colors.negative}` appear **only inline** on
directional change chips — never as fills. **No second accent color.**

## Typography

Two ramps. The **reading ramp** (Roboto body 0.85cqw muted; Roboto eyebrow uppercase 0.08em
cobalt) carries copy + chrome; the **display/numerical ramp** (Roboto `h3` 1.25cqw → `h1`
4.2cqw near-black; numerals `stat-num`/`metric-value` in cobalt) carries headings and figures.

- **Legibility floor:** any load-bearing line ≥ **1.4cqw**; px chrome (tag/counter) is colophon only.
- **Fit-to-measure:** size the headline to its length. Cap the block at **≤ 78cqw**; ≤3 words → `h1`; 4–6 → `h2`; 7+ → `h3`. Cobalt numerals scale `metric-value`→`stat-num` by card size.
- **Headlines near-black, −0.02em**; **eyebrows cobalt, uppercase, 0.08em**; **numerals cobalt 600–700**; **body Roboto 400 muted, line 1.6**. No italic, no uppercase body, no cobalt headline.

## Depth & Surface

Soft and tinted — never offset. Depth from:

- **Tinted cards** — 4% cobalt fill + 20% cobalt 1.5px border + 10–14px radius reads as lifted without offset.
- **Border-left accent** — the 4px cobalt rule on split-highlight blocks pulls a callout forward.
- **Rounded corners** — the 10–14px radius is part of the softness; square corners break it.

**Ceiling:** zero box-shadow on content (the only shadow is a soft cobalt CTA _hover_); no opaque cobalt borders; no harsh outlines.

## Shapes

- **100px** — tag pills, CTA, nav buttons (pill chrome).
- **14/12/10px** — cards by size (large metric / standard stat / detail + mini).
- **6px** — bar tracks + fills. **50%** — step circles, nav circles, dots, closing rings.
- **0** — only the progress bar. No square-cornered content.

## Components

- **card-tinted / metric-card** — the universal soft-tint content cards (no shadow).
- **tag-pill / cta-button / accent-line** — the cobalt pill chrome + the one solid CTA + the 60×4 rule.
- **bar-track / step-circle / split-highlight** — cobalt data + sequence + callout patterns.
- **slide-header** (eyebrow + tag pill) — the structural rhythm; **atmosphere** (diagonal/dots/rings) on cover/closing only; **progress-bar** on every frame.

## Frame Treatments

> Recipe: ground · container · composes · focal · chrome · accent · silence · Fixed/Free · density.
> Atmosphere only on cover/closing; content frames carry the slide-header rhythm.

### 1 · Cover (identity · move: diagonal accent · left)

**Ground** cream + the clipped diagonal cobalt-tint panel (right ~36%) + a 3×3 cobalt dot grid.
**Composes** accent-line, meta, h1, body sub. **Focal** a 2-line Roboto `h1` near-black, left,
under a cobalt accent-line + meta. **Chrome** counter + progress bar. **Accent** the cobalt line +
diagonal panel. **Silence** the diagonal panel holds the right third. **Fixed** near-black h1, cobalt
accents, atmosphere here only. **Free** title, meta. **Density** low.

### 2 · Dashboard (data · move: 3-up metric grid · the dense frame)

**Ground** cream, `pad-x`. **Composes** slide-header (eyebrow + tag-pill), h2, 3× metric/tinted card.
**Focal** a row of tinted cards — cobalt `metric-value` + Roboto label + muted desc + optional
green/red change chip. **Chrome** eyebrow left, tag-pill right; progress bar. **Accent** the cobalt
numerals. **Silence** tight — the density exception. **Fixed** 4% tint cards, 20% borders, no shadow,
cobalt numerals. **Free** figures (from script), labels. **Density** dense-exception.

### 3 · Bar Ranking (data · move: cobalt bars · left)

**Ground** cream, `pad-x`. **Composes** eyebrow, h2, bar-track rows. **Focal** 3–5 labeled
cobalt-fill bars on cobalt-8% tracks with cobalt percentages. **Chrome** eyebrow; progress bar.
**Accent** the cobalt fills + figures. **Silence** moderate. **Fixed** 6px tracks, cobalt fills.
**Free** rows, values (from script). **Density** standard.

### 4 · Pull Quote (quote · move: concentric rings · centered)

**Ground** cream, centered, with faint concentric closing-rings behind. **Composes** quote-mark,
blockquote, cite. **Focal** a Roboto `blockquote` near-black under a 15%-opacity cobalt
quote-mark; an uppercase cobalt-muted cite beneath. **Accent** the faint rings + quote-mark. **Silence**
~55%. **Fixed** near-black quote, soft rings. **Free** quote, cite. **Density** low.

### 5 · Split + Highlight (content · move: asymmetric split · left)

**Ground** cream, two columns. **Composes** eyebrow, h2, body, split-highlight block. **Focal** an
Roboto body column beside a cobalt-8% highlight block (4px cobalt left rule) carrying an inline pull
quote. **Accent** the highlight's left rule. **Silence** generous gutter. **Fixed** tinted highlight,
4px cobalt rule. **Free** body, callout. **Density** standard.

### 6 · Closing / CTA (closer · move: centered rings + CTA)

**Ground** cream + concentric closing-rings. **Composes** accent-line, h1, body, cta-button. **Focal**
a Roboto `h1` near-black, centered, with the one solid cobalt `cta-button` pill below. **Accent**
the CTA + rings. **Silence** ~60%. **Fixed** one CTA, near-black h1, soft rings. **Free** sign-off, CTA
label. **Density** low.

## Composition Rules

### Do

- Start every frame on **warm cream**; let **cobalt carry every accent** (eyebrow, numeral, CTA, bar, progress).
- Set headlines **near-black, −0.02em**; eyebrows **cobalt uppercase 0.08em**; numerals **cobalt 600–700**.
- Use **tinted cards** (4% fill, 20% border, 10–14px radius, no shadow); body Roboto 400 muted, line 1.6.
- Keep all chrome **pill-shaped (100px)**; one solid cobalt CTA per closing frame.
- Reserve **atmosphere** (diagonal panel, dots, rings) for cover/closing; content frames keep the slide-header rhythm.
- Lean left on cover/dashboard/split, centered on quote/closer.

### Don't

- No second accent color; no cobalt headlines.
- No drop shadows on content (only the soft cobalt CTA hover); no opaque cobalt borders.
- No square corners (save the progress bar); no font substitutes; no uppercase body.
- Don't use the green/red change chips as general accents — directional comparisons only.
- Don't fill space with heavier borders — add substance; don't blow a headline edge-to-edge.

## Aspect-Ratio Behavior

| Treatment         | 16:9                       | 9:16                           | 1:1                      |
| ----------------- | -------------------------- | ------------------------------ | ------------------------ |
| Cover             | title left, diagonal right | title top, diagonal band below | title upper, dots corner |
| Dashboard         | 3 cards across             | 3 stacked                      | 2×2                      |
| Bar Ranking       | 3–5 bars                   | 3–5 bars (tighter)             | 3 bars                   |
| Pull Quote        | centered, rings behind     | centered, taller               | centered                 |
| Split + Highlight | side-by-side               | stacked                        | stacked                  |
| Closing / CTA     | centered + CTA             | centered + CTA                 | centered + CTA           |

`pad-x` holds on the short edge; re-step display above the 1.4cqw floor. The diagonal cover panel
becomes a top/bottom band on 9:16. Numerals stay Latin Arabic digits in CJK builds.

## Approved Entities

No real customers, logos, or vendors are defined in the source — render any such mark as a
placeholder. Figures, metrics, and quotes are content; the system supplies cream + cobalt + grays.

## Numerals & Claims (hard rule)

Never invent figures, financials, percentages, or dates at frame scale. Render slots as `— figure —`,
`{metric}`, `+NN%`, `↑ —`. Metric cards, bar values, and stat cells especially carry placeholders
until the script supplies them. Directional chips require a real comparison from the script.

## Pre-Render Self-Audit

- **Squint** — one near-black headline or cobalt numeral dominates per frame.
- **Silence** — content frames balanced, not crowded; only the dashboard runs dense.
- **Single accent** — cobalt only; headlines near-black; positive/negative inline only.
- **Type** — Roboto headings −0.02em near-black, cobalt eyebrows 0.08em + numerals; Roboto body muted line 1.6; ≥1.4cqw floor.
- **Depth** — tinted cards (no shadow), soft rounded corners, 20% cobalt borders; no square content corners.
- **Anchor** — left on cover/dashboard/split, centered on quote/closer; atmosphere on cover/closing only.
- **Fabrication** — every numeral traces to the script, else placeholder.

## Known Gaps

- **Motion intentionally out of scope.** frame.md specifies composition only; the source's 500ms translateX transitions + bar-fill animations are deck mechanics.
- **Roboto + Roboto via Google Fonts.** CJK pairing (Noto Sans SC 700 display / Noto Serif SC 400 body) carries over; the eyebrow's uppercase+tracking signal weakens in CJK — pair it with the accent-line.
- **9:16 / 1:1 are guidance**; verify the floor and that the diagonal panel reflows to a band.
- Diagonal panel (clip-path), dot grid, concentric rings, and bars are CSS-only; no external imagery is required.

---

# ⭐ RMBLR PRODUCT UI KIT — build the app, do not approximate it

This film's subject is a real Android app. **Every pixel of RMBLR that appears on screen is
rebuilt here from the app's own Kotlin** (`orb/OrbUi.kt`, `ui/theme/Color.kt`), not mocked
up and not composited from screenshots. The numbers below are the app's numbers. Copy this
kit into your frame verbatim and use its classes; do not re-derive it, do not "improve" the
proportions, and do not substitute a microphone icon for the wave mark.

## The unit: `--dp`

The app is measured in Android `dp`. Every value in this kit is `dp`-relative, so a frame
changes shot size by changing one number:

| Shot | `--dp` | What it gives you |
|---|---|---|
| Whole phone in frame | `1.15px` | screen 452 × 979 px, orb 60 px |
| Waist-up on the field + orb | `2.0px` | orb 104 px |
| Pushed in on the orb | `3.2px` | orb 166 px |
| Macro on the orb | `5.0px` | orb 260 px |

Animate `--dp` (or better, `scale` on a wrapper — cheaper and sub-pixel smooth) to push in.
**Never animate `--dp` and `scale` at the same time on the same element.**

## Copy-paste CSS

```css
:root{
  --dp: 2px;
  /* colour tokens — verbatim from ui/theme/Color.kt, dark skin */
  --ink:#0A0B0D; --surface:#15171B; --raised:#1E2127; --line:#2B2F37;
  --text-high:#EDEFF3; --text-mid:#9AA1AD; --text-low:#666D7A;
  --accent:#7C6CFF; --accent-wash:#23204A; --alert:#E05561; --good:#4FBF87;
  --on-accent:#FFFFFF;
  /* Scrim is NEVER derived from the theme. See the note below. */
  --scrim:#07080A;
}

/* ---------- the phone screen ---------- */
/* A real device viewport is 393 × 851 dp. Use .rm-screen as the ground that the
   app-behind (WhatsApp, Gmail, a browser) and the orb both live on. */
.rm-screen{
  position:relative; overflow:hidden;
  width:calc(393*var(--dp)); height:calc(851*var(--dp));
  background:var(--ink);
}

/* ---------- the orb ---------- */
/* ORB_SIZE = 52dp. The body circle is 0.84×. The glyph is 0.46×. */
.rm-orb{
  position:absolute;
  width:calc(52*var(--dp)); height:calc(52*var(--dp));
  display:grid; place-items:center;
  transform-origin:50% 50%;
}
.rm-orb-body{
  width:calc(52*0.84*var(--dp)); height:calc(52*0.84*var(--dp));
  border-radius:50%;
  display:grid; place-items:center;
  background:var(--surface);            /* IDLE */
}
.rm-orb[data-phase="recording"] .rm-orb-body{ background:var(--alert); }
.rm-orb[data-phase="working"]   .rm-orb-body{ background:var(--accent); }
.rm-orb[data-phase="done"]      .rm-orb-body{ background:var(--good);  }

/* The RECORDING halo: a circle at 0.22 alpha of Alert, radius scaling
   1.0 → 1.35 with mic amplitude. It sits BEHIND the body, at the full 52dp box. */
.rm-orb-halo{
  position:absolute; inset:0; border-radius:50%;
  background:rgba(224,85,97,0.22);
  transform:scale(1); opacity:0;
}
.rm-orb[data-phase="recording"] .rm-orb-halo{ opacity:1; }

/* ---------- the wave mark ---------- */
/* NOT a microphone. Five bars rising to the middle — the same shape as the launcher
   icon. A mic glyph says "recording"; this says which app you are looking at.
   Heights 0.34 / 0.66 / 1.0 / 0.72 / 0.42 of the glyph box; gap = w/(5×2.2);
   bar width = gap; round caps. Glyph box is 0.46 × ORB_SIZE = 23.92dp. */
.rm-wave{ width:calc(52*0.46*var(--dp)); height:calc(52*0.46*var(--dp)); display:block; }
```

```html
<!-- the wave mark, as SVG on a 100×100 user-space grid so it scales exactly.
     gap = 100/(5*2.2) = 9.0909; barWidth = gap; x_i = gap*0.6 + i*(barWidth+gap)
     → 5.4545, 23.6364, 41.8182, 60.0, 78.1818 ; then +barWidth/2 for the stroke centre
     → 10.0, 28.18, 46.36, 64.55, 82.73  -->
<svg class="rm-wave" viewBox="0 0 100 100" fill="none">
  <g stroke="currentColor" stroke-width="9.0909" stroke-linecap="round">
    <line x1="10.00" x2="10.00" y1="33.0" y2="67.0"/>   <!-- 0.34 -->
    <line x1="28.18" x2="28.18" y1="17.0" y2="83.0"/>   <!-- 0.66 -->
    <line x1="46.36" x2="46.36" y1="4.55" y2="95.45"/>  <!-- 1.00, capped by the box -->
    <line x1="64.55" x2="64.55" y1="14.0" y2="86.0"/>   <!-- 0.72 -->
    <line x1="82.73" x2="82.73" y1="29.0" y2="71.0"/>   <!-- 0.42 -->
  </g>
</svg>
```

The glyph takes its colour from `currentColor`: set `color:var(--text-high)` at rest,
`color:var(--on-accent)` while recording or done.

Bars **grow with the voice**. Amplitude scales each bar by `1 + level × 0.3`, except the
centre bar which uses `1 + level × 0.15`, and no bar ever exceeds the glyph box. Animate by
scaling each `<line>` about `y=50`.

## The working ring

The orb's WORKING state draws a turning arc, not a static one — a bare arc reads as frozen
at exactly the moment you need to know something is happening. **Rotation 360° per 900ms
linear; sweep 40° → 260° per 1100ms, reversing, ease-in-out.** Stroke 3dp, round caps, in
`--on-accent`, on a 0.46 × ORB_SIZE box.

## The arc (`OrbFan`)

```css
.rm-scrim{ position:absolute; inset:0; background:rgba(7,8,10,0.42); }  /* the arc's wash */
.rm-chip{
  position:absolute;
  width:calc(128*var(--dp)); height:calc(34*var(--dp));
  border-radius:calc(17*var(--dp));
  display:grid; place-items:center;
  background:var(--surface); color:var(--text-mid);
  font:400 calc(12*var(--dp))/1 Roboto, system-ui, sans-serif;
  white-space:nowrap;
}
.rm-chip[data-active="true"]{
  background:var(--accent); color:var(--on-accent); font-weight:600;
}
```

Geometry, exactly as the app computes it:

- Radius **116dp** from the orb's centre. Chips are **128 × 34dp**, corner radius **17dp**.
- Total spread **150°**. For item `i` of `n`: `angle = -75 + (150/(n-1))·i` when the fan
  opens right; `180 - that` when it opens left.
- **0° points right and grows clockwise** (screen coordinates, `y` down).
  `dx = 116·cos(angle)`, `dy = 116·sin(angle)`.
- The chip is centred on that point: `x = cx + dx - 64dp`, `y = cy + dy - 17dp`.
- **The fan always opens away from the edge the orb is parked on**, which is why it is a
  half-circle pointing into the screen rather than one chip per compass direction — a
  compass layout puts an option off-screen the moment the orb sits against an edge.
- **Selection is by angle, not by quadrant.** You slide *toward* the one you want, and the
  chip under your heading highlights. Animate the highlight as the thumb sweeps, not as a
  discrete jump between chips.

The shipped tone chips, in arc order, are: **Professional · Clean up · Casual · Bullets ·
Summary**. In translate mode the same arc carries **languages** instead of tones.

## The scrim rule — do not break this

`--scrim` is `#07080A` and is **never derived from a theme surface colour**. It was once
`Ink`, which is very nearly white in light mode, so the thing whose whole job is to dim the
screen threw a 55% white sheet over it — a flashbang in a dark room. It is also deliberately
gentle: **0.42 alpha behind the arc, 0.58 behind the translation bubble.** Enough to push
the page back, not enough to hide it. The app behind must stay legible through it; that
legibility is the point of the shot.

## The translation bubble

A card floating over the 0.58 scrim, not a toast: you are reading a paragraph in a language
you do not speak, so it has to sit still and let you take the text with you. Full width less
20dp of side padding, 18dp radius, `--surface` fill, 18dp of internal padding. Inside, in
order: a 12dp label reading **"In <language>"** in `--text-mid`; 10dp of space; the
translated text at 16dp in `--text-high`; 16dp of space; then **Copy** and **Close** in
`--accent` at 14dp, right-aligned.

## Idle behaviour worth animating

Ignored for long enough, the orb **scales to 0.72 and fades over 600ms**; touched, it comes
back in **140ms**. Slow out, quick back — fading away should be something you half-notice,
coming back has to feel like it answered you. It only ever scales what is *drawn*, so the
thing stays exactly as easy to hit while it is small.

## ⛔ Never on screen

- **Snippet shortcuts.** The shortcut field is displayed and never matched against anything.
- **Sound and keyboard-theme preferences.** Dead — nothing reads them.
- A **microphone glyph** in place of the wave mark.
- The **green** accent from `docs/screenshot-arc.png`. That is a Material You wallpaper
  colour on the operator's own phone, not RMBLR's. The brand accent is `#7C6CFF`.

Everything on screen must be true of the shipped APK.

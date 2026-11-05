"""v4 — the flat language.

Ayanda on v3: "I like the 3D-ness of it, but try doing something similar and
remove all the 3D. Flat flat version. But I like the phone. And it's still a bit
still — it lacks motion. You can do much, much better."

THE RULE THAT REPLACES THE LIGHTING: the orb is the only circle in a world of
rectangles. v3 made the orb the hero with specular highlights and bloom; flat
design has none of that, so the hero-making is done by SHAPE instead. Every other
element in the film — every panel, band, chip, phone, letter — is a hard-edged
rectangle. The one round thing is the product.

WHAT THAT BUYS: because a flat circle at 52px and at 420px are the same object
scaled, there is no hero/UI seam to hide any more. ONE orb travels the whole
film, growing when the mechanic needs to be legible and shrinking back onto the
phone to do its job. v3 needed two orbs and a match-cut; v4 needs neither.

AND THE MOTION: with no lighting or perspective to protect, the FRAME ITSELF can
reconfigure. Panels wipe, bands deal in from alternating sides, the layout
re-lays itself out on the beat. Nothing holds longer than a bar.

BANNED, absolutely: gradients, shadows, blur, bloom, perspective, rotate3d,
border-radius on anything that is not the orb or the phone. Hard edges only.
"""

FLAT_CSS = """
:root{
  --dp:1px;
  --ink:#0A0B0D; --slate:#15171B; --line:#2B2F37;
  --paper:#F2F4F8; --dim:#8D95A3;
  --indigo:#7C6CFF; --deep:#2A2394; --red:#E05561; --green:#4FBF87;
}
*{margin:0;padding:0;box-sizing:border-box}
body{background:#000}
#root{position:relative;width:1920px;height:1080px;overflow:hidden;background:#07080B;
  font-family:Roboto,system-ui,sans-serif;-webkit-font-smoothing:antialiased}
.lay{position:absolute;inset:0}
#ground{position:absolute;inset:0;z-index:0;
  background:
    radial-gradient(1400px 900px at 50% 46%, rgba(124,108,255,.075), rgba(124,108,255,0) 70%),
    radial-gradient(900px 700px at 50% 50%, rgba(255,255,255,.035), rgba(255,255,255,0) 72%),
    repeating-linear-gradient(0deg,rgba(255,255,255,.020) 0 1px,transparent 1px 64px),
    repeating-linear-gradient(90deg,rgba(255,255,255,.020) 0 1px,transparent 1px 64px);}
#vig{position:absolute;inset:0;z-index:1;pointer-events:none;
  background:radial-gradient(ellipse 82% 70% at 50% 48%,transparent 0%,rgba(0,0,0,.42) 78%,rgba(0,0,0,.72) 100%)}

/* flat colour fields — the frame's own ground, which changes on the beat */
.field{position:absolute;inset:0}
#f-indigo{background:var(--indigo)}
#f-paper{background:var(--paper)}
#f-deep{background:var(--deep)}

/* the phone: flat. A dark rectangle with a hairline, no bezel gradient, no
   shadow, no tilt. It is furniture, and the app inside it is the truth. */
#dev{position:absolute;width:419px;height:877px;background:#141519;
  border-radius:48px;padding:13px;
  box-shadow:inset 0 0 0 1px rgba(255,255,255,.11),
             inset 0 0 0 3px #0C0D10,
             0 2px 0 0 rgba(255,255,255,.05),
             0 40px 90px rgba(0,0,0,.75)}
.scr{position:relative;width:393px;height:851px;overflow:hidden;border-radius:36px;background:#0B141A}

/* THE ONE CIRCLE */
#orb{position:absolute;width:52px;height:52px;border-radius:50%;background:var(--slate);
  display:grid;place-items:center;z-index:40}
#orbring{position:absolute;inset:-5px;border-radius:50%;border:1.5px solid var(--red);opacity:0}
#wave{width:23.92px;height:23.92px;color:var(--paper);display:block}
#glyph{position:absolute;font-family:NotoJP,Roboto,sans-serif;font-size:26px;font-weight:500;
  color:var(--paper);opacity:0;line-height:1}

/* kinetic type — masked by moving blocks, never faded in softly */
.tw{position:absolute;overflow:hidden}
.tw > span{display:block}
#slam{position:absolute;left:0;right:0;text-align:center;color:var(--paper);
  font-size:210px;font-weight:900;letter-spacing:-.035em;line-height:1;opacity:0;z-index:26}
#head{position:absolute;color:var(--paper);font-size:78px;font-weight:900;letter-spacing:-.03em;
  line-height:1.02;white-space:pre-line}
#sub{position:absolute;color:var(--dim);font-size:30px;font-weight:500;letter-spacing:.01em;
  line-height:1.35;white-space:pre-line}

/* the five tone bands — the arc, re-imagined as a stack of flat rows */
#bands{position:absolute;inset:0;opacity:0}
.band{position:absolute;left:0;width:1920px;height:216px;display:flex;align-items:center;
  padding-left:330px;transform-origin:0 50%}
.band b{font-size:104px;font-weight:900;letter-spacing:-.02em}

/* the flat waveform: rectangles, not a glow */
#meter{position:absolute;display:flex;align-items:center;gap:14px;opacity:0;z-index:30}
.mbar{width:26px;background:var(--paper)}

/* the streak the flick leaves — hard rectangles, no motion blur */
.streak{position:absolute;top:514px;height:52px;border-radius:26px;background:var(--indigo);opacity:0}

/* the grid of places the orb could live */
#dots{position:absolute;opacity:0}
.dot{position:absolute;width:10px;height:10px;border-radius:50%;background:var(--line)}

.chip{position:absolute;height:56px;display:flex;align-items:center;padding:0 26px;
  font-size:26px;font-weight:700;letter-spacing:.01em;background:var(--slate);color:var(--dim);
  border:1px solid var(--line);opacity:0}
.chip.on{background:var(--indigo);color:#fff;border-color:var(--indigo)}

#tonelist{position:absolute;left:1010px;top:606px;width:800px}
.trow{overflow:hidden;height:58px}
.trow > span{display:block;font-size:44px;font-weight:900;letter-spacing:-.02em;color:var(--paper);
  line-height:58px}
.trow > span i{font-style:normal;color:var(--indigo)}
#pulse{position:absolute;inset:-5px;border-radius:50%;border:1.5px solid var(--indigo);opacity:0}
#zulu{color:#0A0B0D;font-size:72px;font-weight:900;letter-spacing:-.03em;
  line-height:1.08;white-space:pre-line}
/* ── THE SIGNATURE: the orb is the transition. Every scene change happens
      THROUGH it — it swells from the orb's exact position until it IS the frame,
      the next beat plays inside it, then it collapses back to 52dp and drops onto
      the phone. The circle is the wipe, which means the film is one object
      transforming rather than a run of cuts, and it reinforces the shape rule
      every single time it fires. ── */
#wipe{position:absolute;width:52px;height:52px;border-radius:50%;background:var(--indigo);
  z-index:20;transform:scale(0)}

/* shutters — the frame splitting into columns or rows that move independently */
.sh{position:absolute;top:0;height:1080px;background:var(--ink);z-index:22}
.shr{position:absolute;left:0;width:1920px;background:var(--ink);z-index:22}

/* the tone slot: the five names run past at speed and SLAM onto the one you took */
#slot{position:absolute;left:0;width:1920px;top:465px;height:150px;overflow:hidden;z-index:24;opacity:0}
#slotinner{position:absolute;left:0;width:1920px}
.slotrow{height:150px;display:flex;align-items:center;justify-content:center;
  font-size:118px;font-weight:900;letter-spacing:-.03em;color:var(--paper)}
#slotrule{position:absolute;left:660px;width:600px;height:5px;background:var(--ink);
  top:612px;z-index:25;opacity:0;transform-origin:50% 50%}

/* the numeral that indexes each gesture */
#num{position:absolute;left:118px;top:104px;font-size:150px;font-weight:900;color:var(--ink);
  opacity:0;letter-spacing:-.04em;z-index:26}
#label{position:absolute;left:120px;top:96px;font-size:26px;font-weight:900;letter-spacing:.22em;
  color:var(--dim);z-index:12}

/* the voice, full width — 21 flat columns, no glow */
#wide{position:absolute;left:0;width:1920px;top:760px;height:230px;display:flex;
  align-items:center;justify-content:center;gap:18px;opacity:0;z-index:12}
.wbar{width:38px;background:var(--paper)}
#prog{position:absolute;left:0;top:970px;width:1920px;height:8px;background:var(--indigo);
  transform-origin:0 50%;opacity:0;z-index:12}

/* ── the static text card. Zero motion by design: all three reference films hold
      their type dead still, and the stillness is what gives it authority. ── */
#card{position:absolute;inset:0;background:#07080B;z-index:30;opacity:0}
#card::before{content:"";position:absolute;inset:0;
  background:radial-gradient(1200px 800px at 50% 50%, rgba(124,108,255,.07), transparent 70%)}
#cardtext{position:absolute;left:0;right:0;top:50%;transform:translateY(-50%);text-align:center;
  color:var(--paper);font-size:150px;font-weight:900;letter-spacing:-.035em;line-height:1.02;
  white-space:pre-line}
#cardsub{position:absolute;left:0;right:0;top:50%;margin-top:130px;text-align:center;
  color:var(--dim);font-size:30px;font-weight:700;letter-spacing:.2em}

/* ── the flash-flood ending: the brand colour is held back for 27 seconds and
      then floods the frame at 100%. Arc's move, and the reason their accent lands. ── */
#flood{position:absolute;inset:0;background:var(--indigo);z-index:32;opacity:0}
#fmark{position:absolute;color:#fff}
#fword{position:absolute;color:#fff;font-size:132px;font-weight:900;letter-spacing:-.035em;
  line-height:1;opacity:0}
.ffact{position:absolute;left:0;width:1920px;text-align:center;color:rgba(255,255,255,.82);
  font-size:31px;font-weight:700;opacity:0}
#furl{position:absolute;left:0;width:1920px;text-align:center;color:#fff;font-size:31px;
  font-weight:900;opacity:0}
#tag{position:absolute;left:96px;bottom:78px;color:var(--dim);font-size:24px;font-weight:900;
  letter-spacing:.24em;z-index:28}

#lockmark{position:absolute;color:var(--paper)}
#lockword{position:absolute;color:var(--paper);font-size:132px;font-weight:900;
  letter-spacing:-.035em;line-height:1;opacity:0}
.fact{position:absolute;color:var(--dim);font-size:31px;font-weight:500;opacity:0}
#url{position:absolute;color:var(--indigo);font-size:31px;font-weight:700;opacity:0}
"""

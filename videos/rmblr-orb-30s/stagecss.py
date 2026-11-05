"""The look: a lit sphere, a real device, and a black void.

Everything here exists to serve one move — the orb LIFTS OFF the phone and
becomes monumental so the mechanic is legible, then drops back to 52dp and does
its job. That is Apple's oldest trick with a small UI element, and it is the only
honest way to show a gesture nobody has seen.

Two hard rules learned the hard way:
  · A CSS `filter` on a `preserve-3d` element FLATTENS the 3D world. Glow is done
    with box-shadow and stacked radial-gradients, never filter: blur.
  · Perspective lives on a static wrapper. The camera scales/translates a layer
    ABOVE it, so a push-in never fights the 3D projection.
"""

STAGE_CSS = """
/* ── the void: the hero stage ───────────────────────────────────────────── */
#void{position:absolute;inset:0;opacity:0}
.bloom{position:absolute;border-radius:50%;pointer-events:none}

/* ── the hero orb. Layered gradients do the lighting: a broad key from upper
      left, a tight specular, a cool bounce lower right, and an inset terminator
      that turns a flat circle into a sphere. ─────────────────────────────── */
.sphere{position:absolute;border-radius:50%;
  background:
    radial-gradient(circle at 33% 26%, rgba(255,255,255,.50) 0%, rgba(255,255,255,.12) 26%, rgba(255,255,255,0) 46%),
    radial-gradient(circle at 26% 20%, rgba(255,255,255,.85) 0%, rgba(255,255,255,0) 11%),
    radial-gradient(circle at 74% 82%, rgba(160,180,255,.22) 0%, rgba(160,180,255,0) 34%),
    radial-gradient(circle at 50% 46%, var(--sph1) 0%, var(--sph2) 62%, var(--sph3) 100%);
  box-shadow:
    inset 0 -0.11em 0.30em rgba(0,0,0,.60),
    inset 0 0.06em 0.16em rgba(255,255,255,.13),
    inset 0 0 0 1px rgba(255,255,255,.06);
}
/* the rim light — a hairline of the key colour catching the far edge */
.sphere::after{content:"";position:absolute;inset:0;border-radius:50%;
  background:radial-gradient(circle at 76% 80%, rgba(255,255,255,.30) 0%, rgba(255,255,255,0) 22%);}

/* ── the voice, at hero scale: five columns of light ──────────────────────── */
#beams{position:absolute;display:flex;align-items:center;justify-content:center;opacity:0}
.beam{border-radius:999px;background:linear-gradient(180deg,
  rgba(255,255,255,.35) 0%, #fff 22%, #fff 78%, rgba(255,255,255,.35) 100%);
  box-shadow:0 0 18px rgba(255,255,255,.55),0 0 46px rgba(255,255,255,.28)}

/* ── the arc, at hero scale: capsules on a ring, seen in perspective ─────── */
#ring{position:absolute;transform-style:preserve-3d;opacity:0}
.rchip{position:absolute;display:flex;align-items:center;justify-content:center;
  white-space:nowrap;border-radius:999px;
  background:rgba(24,26,32,.86);color:#C7CDD8;border:1px solid rgba(255,255,255,.11);
  box-shadow:0 10px 34px rgba(0,0,0,.55), inset 0 1px 0 rgba(255,255,255,.07);
  backface-visibility:hidden}
.rchip.on{background:linear-gradient(180deg,#8E80FF,#6A58F5);color:#fff;font-weight:600;
  border-color:rgba(255,255,255,.34);
  box-shadow:0 0 34px rgba(124,108,255,.85),0 0 90px rgba(124,108,255,.42),
             inset 0 1px 0 rgba(255,255,255,.35)}

/* ── reality: the device, a real object in space ─────────────────────────── */
#world{position:absolute;inset:0;perspective:2400px;opacity:0}
.dev{position:absolute;transform-style:preserve-3d;border-radius:var(--devr);
  background:linear-gradient(155deg,#3A3E46 0%,#14161A 26%,#0D0E11 62%,#25282F 100%);
  padding:var(--devp);
  box-shadow:0 60px 120px rgba(0,0,0,.75), 0 0 0 1px rgba(255,255,255,.07),
             0 0 90px rgba(124,108,255,.10)}
.dev::before{content:"";position:absolute;inset:0;border-radius:var(--devr);
  background:linear-gradient(120deg,rgba(255,255,255,.16) 0%,rgba(255,255,255,0) 26%,
    rgba(255,255,255,0) 72%,rgba(255,255,255,.09) 100%);pointer-events:none;z-index:3}
.scr{position:relative;width:100%;height:100%;overflow:hidden;
  border-radius:calc(var(--devr) - var(--devp));background:#0B141A}

/* ── full-bleed statement type, cut between the shots ────────────────────── */
/* one layout for the whole film: product left, words right. The hero orb and the
   phone sit at the same place, so the type column never has to move. */
#say{position:absolute;left:1010px;top:0;bottom:0;width:800px;display:flex;flex-direction:column;
  align-items:flex-start;justify-content:center;pointer-events:none}
#sayline{color:#F2F4F8;font-size:76px;font-weight:800;letter-spacing:-.034em;line-height:1.07;
  opacity:0}
#saysub{color:#A2AAB8;font-size:33px;font-weight:400;line-height:1.42;margin-top:24px;opacity:0}
#gest{position:absolute;color:#FFF;font-size:150px;font-weight:900;letter-spacing:.02em;
  opacity:0;text-shadow:0 0 60px rgba(124,108,255,.5)}
"""

#!/usr/bin/env python3
"""RMBLR — flat cut. 30.0s · 1920x1080 · 60fps. (v4)

Ayanda on v3: "I like the 3D-ness, but try the same thing with all the 3D
removed. Flat flat version. But I like the phone. And it's still a bit still —
you can do much, much better."

THE RULE THAT REPLACES THE LIGHTING — **the orb is the only circle in a world of
rectangles.** v3 made it the hero with speculars and bloom. Flat has none of
that, so the hero-making moves to SHAPE: every panel, band, chip, phone, letter
and bar in this film is a hard-edged rectangle, and the one round thing on screen
is the product.

WHAT FLAT BUYS BACK. A flat circle at 52px and at 416px are the same object
scaled — so there is no hero/UI seam left to hide. ONE orb travels the entire
film, swelling when the mechanic has to be legible and shrinking back onto the
phone to do its job. v3 needed two orbs and a match-cut; this needs neither.

AND THE MOTION. With no lighting or perspective to protect, the FRAME can
reconfigure. The phone slides between three stations, colour fields wipe on the
beat, the arc deals itself out as five full-width bands, the flick throws the orb
across the frame. Nothing on screen holds longer than a bar, and every move lands
on a 0.25s subdivision of the 120 BPM grid.

  A 0.0- 2.0  TITLE     · five bars punch in and slam into the orb
  B 2.0- 4.5  REVEAL    · the phone arrives, the orb takes its place
  C 4.5- 8.0  TAP       · the orb swells, listens, and the sentence lands
  D 8.0-12.0  HOLD      · the arc, dealt as five full-width bands
  E 12.0-15.0 FLICK     · thrown across the frame, straight to the tone
  F 15.0-19.0 APPS      · three phones, three different arcs
  G 19.0-22.0 DRAG      · every place it could live, then the one you pick
  H 22.0-26.0 TRANSLATE · the frame splits English | isiZulu
  I 26.0-30.0 LOCKUP    · the circle opens back into the mark

Exactly 30.000s so the approved score drops on unchanged.
"""
import math, os
from apps import whatsapp, gmail, slack, keyboard, APPS_CSS
from flat import FLAT_CSS

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "index.html")
W, H, FPS, DUR = 1920, 1080, 60, 30.0

DEV_W, DEV_H, PAD = 419, 877, 13
DEV_T = 101.5
DEV_L, DEV_MID, DEV_R = 140.0, 752.5, 1365.0      # the three stations
ORB_OX, ORB_OY = PAD + 300.0, PAD + 430.0          # the orb's spot on the screen
OX, OY = DEV_L + ORB_OX, DEV_T + ORB_OY            # 451, 544.5 — its home in frame
HERO = 8.0                                          # 52 -> 416px
HX, HY = 600.0, 540.0

BARS = [0.34, 0.66, 1.0, 0.72, 0.42]
GAP = 100.0 / (5 * 2.2)
BAR_X = [GAP * 0.6 + i * (GAP * 2) + GAP / 2 for i in range(5)]

TONES = {"wa": ["CASUAL", "CLEAN UP", "GRAMMAR", "BULLETS", "SUMMARY"],
         "gm": ["PROFESSIONAL", "SUMMARY", "CLEAN UP", "GRAMMAR", "BULLETS"],
         "sl": ["CLEAN UP", "CASUAL", "BULLETS", "PROFESSIONAL", "SUMMARY"]}
BAND_BG = ["#0A0B0D", "#7C6CFF", "#F2F4F8", "#2A2394", "#0A0B0D"]
BAND_FG = ["#F2F4F8", "#0A0B0D", "#0A0B0D", "#F2F4F8", "#F2F4F8"]

ORB_SX, ORB_SY = 300.0, 430.0          # the orb's spot in screen coordinates
# every place the orb could live, shown while you drag it
dots = "".join(f'<div class="dot" style="left:{26+c*68}px;top:{150+r*62}px"></div>'
               for r in range(5) for c in range(5))

def fan_angle(i, n=5):
    """OrbUi.fanAngleDegrees() — 0 points right, grows clockwise, opens left."""
    return 180.0 - (-75.0 + (150.0 / (n - 1)) * i)

def wave_svg(eid, cls=""):
    ls = "".join(f'<line class="wb" x1="{BAR_X[i]:.2f}" x2="{BAR_X[i]:.2f}" '
                 f'y1="{50-BARS[i]*50:.2f}" y2="{50+BARS[i]*50:.2f}"/>' for i in range(5))
    return (f'<svg id="{eid}" class="{cls}" viewBox="0 0 100 100" fill="none">'
            f'<g stroke="currentColor" stroke-width="{GAP:.4f}" stroke-linecap="round">{ls}</g></svg>')

def uchips():
    """The real fan, on the main phone — OrbUi's geometry at 1dp."""
    out = []
    for i in range(5):
        a = math.radians(fan_angle(i))
        out.append(f'<div class="uchip" id="uc{i}" data-layout-allow-overlap '
                   f'data-layout-allow-occlusion style="'
                   f'left:{ORB_SX + 116*math.cos(a) - 64:.1f}px;'
                   f'top:{ORB_SY + 116*math.sin(a) - 17:.1f}px"></div>')
    return "".join(out)

def dev(eid, app_fn, key):
    m = app_fn(1.0)
    if key != "main":
        for k in ("wa", "gm", "sl"):
            m = m.replace(f'id="{k}-txt"', f'data-field="{k}"')
    if eid == "dev":
        g, s = gmail(1.0), slack(1.0)
        return (f'<div id="{eid}" class="dev"><div class="scr">{m}{g}{s}{keyboard(1.0)}'
                f'{uchips()}<div id="dots">{dots}</div><div id="thumb"></div></div></div>')
    return (f'<div id="{eid}" class="dev"><div class="scr">{m}{keyboard(1.0)}</div></div>')

PHASES = [
    (0.00, 4.55, "idle"),
    (4.55, 6.60, "recording"), (6.60, 6.95, "working"), (6.95, 7.45, "done"),
    (7.45, 10.55, "idle"),
    (10.55, 11.15, "recording"), (11.15, 11.35, "working"), (11.35, 11.85, "done"),
    (11.85, 13.05, "idle"),
    (13.05, 13.35, "recording"), (13.35, 13.50, "working"), (13.50, 14.00, "done"),
    (14.00, 20.75, "idle"),
    (21.05, 21.50, "recording"),
    (21.50, 23.80, "idle"),
    (23.80, 24.40, "recording"), (24.40, 24.85, "working"), (24.85, 25.35, "done"),
    (25.35, 30.00, "idle"),
]
TRANSLATE_ON, TRANSLATE_OFF = 23.30, 23.80
FIELDTEXT = [
    (0.00,  "wa-txt", ""),
    (6.95,  "wa-txt", "I'll be there at about half seven."),
    (11.35, "wa-txt", "Yeah, half seven works — see you there."),
    (13.50, "wa-txt", "half seven · bring the deck · ash drives"),
    (15.50, "wa-txt", "Yeah, half seven works — see you there."),
    (22.85, "wa-txt", "Thank you, I will see you tomorrow."),
    (24.85, "wa-txt", "Ngiyabonga, ngizokubona kusasa."),
]
# Static cards. Word replacement happens IN PLACE on the beat — nothing moves.
CARD = [
    (1.25, 1.80, "52dp.", "THE WHOLE CONTROL"),
    (1.80, 2.25, "THAT'S THE\nWHOLE APP.", ""),
    (6.00, 6.50, "TAP", "01"),
    (8.00, 8.50, "HOLD", "02"),
    (12.25, 12.75, "FLICK", "03"),
    (18.50, 19.50, "THE TONES CHANGE\nWITH THE APP.", ""),
    (19.50, 20.05, "DRAG", "04"),
    # ⚡ the climax — the vocabulary fired back at you, ~3 cuts a second
    (20.60, 21.05, "TAP", "RECAP"),
    (21.50, 21.95, "HOLD", "RECAP"),
    (22.40, 22.85, "FLICK", "RECAP"),
    (23.30, 23.80, "DOUBLE TAP", "05"),
]
TAG = [(2.30, 4.25, "RMBLR / 52dp"), (4.25, 6.00, "LISTENING"),
       (6.50, 8.00, "INSERTED AT THE CURSOR"), (8.50, 11.00, "FIVE TONES"),
       (12.75, 15.50, "NO MENU"), (15.50, 18.50, "PER APP"),
       (24.60, 27.00, "ENGLISH IN / ISIZULU OUT")]
SHOTS = [
    (0.00, "orb",   0,   0,   9.0, "wa", 0),
    (1.25, "card",  0,   0,   1.0, "wa", 0),
    (2.25, "phone", 196, 425, 1.0, "wa", 0),
    (4.25, "phone", 300, 430, 3.4, "wa", 0),
    (4.75, "orb",   0,   0,   9.0, "wa", 0),
    (6.00, "card",  0,   0,   1.0, "wa", 0),
    (6.50, "phone", 196, 484, 3.6, "wa", 0),
    (8.00, "card",  0,   0,   1.0, "wa", 0),
    (8.50, "phone", 213, 430, 2.6, "wa", 1),
    (10.55, "phone", 213, 430, 2.6, "wa", 0),
    (11.00, "phone", 196, 484, 3.6, "wa", 0),
    (12.25, "card",  0,   0,  1.0, "wa", 0),
    (12.75, "phone", 196, 460, 3.0, "wa", 0),
    (14.00, "phone", 196, 425, 1.0, "wa", 0),
    (15.50, "phone", 213, 430, 2.6, "gm", 1),
    (17.00, "phone", 213, 430, 2.6, "sl", 1),
    (18.50, "card",  0,   0,  1.0, "wa", 0),
    (19.50, "card",  0,   0,  1.0, "wa", 0),
    (20.05, "phone", 240, 380, 2.2, "wa", 0),
    # the recap, at two cuts a second — three was flicker, not rhythm
    (20.60, "card",  0,   0,  1.0, "wa", 0),
    (21.05, "orb",   0,   0,  9.0, "wa", 0),
    (21.50, "card",  0,   0,  1.0, "wa", 0),
    (21.95, "phone", 213, 430, 2.6, "wa", 1),
    (22.40, "card",  0,   0,  1.0, "wa", 0),
    (22.85, "phone", 196, 484, 3.6, "wa", 0),
    (23.30, "card",  0,   0,  1.0, "wa", 0),
    (23.80, "orb",   0,   0,  9.0, "wa", 0),
    (24.50, "phone", 196, 425, 1.0, "wa", 0),   # the long static hold
    (27.00, "flood", 0,   0,  1.0, "wa", 0),
]

def js(rows):
    """Serialise to JS. Real newlines in the copy must survive as \\n escapes —
    a literal newline inside a JS string literal is a syntax error."""
    def cell(c):
        if isinstance(c, str):
            return '"' + c.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n") + '"'
        return f"{c}"
    return "[" + ",".join("[" + ",".join(cell(c) for c in r) + "]" for r in rows) + "]"

bands = "".join(
    f'<div class="band" id="bd{i}" style="top:{i*216}px;background:{BAND_BG[i]}">'
    f'<b id="bdt{i}" data-layout-allow-overlap data-layout-allow-occlusion style="color:{BAND_FG[i]}"></b></div>' for i in range(5))
streaks = "".join(f'<div class="streak" id="st{i}"></div>' for i in range(7))


HTML = f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>RMBLR</title><style>
@font-face{{font-family:Roboto;src:url(assets/fonts/roboto-latin-400-normal.woff2)format('woff2');font-weight:400;font-display:block}}
@font-face{{font-family:Roboto;src:url(assets/fonts/roboto-latin-500-normal.woff2)format('woff2');font-weight:500;font-display:block}}
@font-face{{font-family:Roboto;src:url(assets/fonts/roboto-latin-700-normal.woff2)format('woff2');font-weight:700;font-display:block}}
@font-face{{font-family:Roboto;src:url(assets/fonts/roboto-latin-900-normal.woff2)format('woff2');font-weight:900;font-display:block}}
@font-face{{font-family:NotoJP;src:url(assets/fonts/noto-sans-jp-japanese-500-normal.woff2)format('woff2');font-weight:500;font-display:block}}
{FLAT_CSS}
{APPS_CSS}
/* v6 is built from hard cuts and macro crops, so every piece of v5 scenery is
   dead weight — and its ink shutters were sitting at z-index 22 over the phone. */
#wipe,.sh,.shr,#slot,#slotrule,#bands,#split,#splitlab,#splitlab2,#zuluw,.streak,
#num,#label,#head,#sub,#rule,#slam,#tonelist,#fired,#trace,#meter,#wide,#prog,
#lockmark,#lockword,.fact,#url{{display:none!important}}
#world{{position:absolute;inset:0;transform-origin:0 0}}
/* the arc, at 1dp — OrbUi's own chip metrics */
.uchip{{position:absolute;width:128px;height:34px;border-radius:17px;background:rgba(21,23,27,.96);
  color:#9AA1AD;font-size:12px;font-weight:500;display:flex;align-items:center;justify-content:center;
  white-space:nowrap;border:1px solid #2B2F37;z-index:6;opacity:0}}
.uchip.on{{background:var(--indigo);color:#fff;font-weight:700;border-color:var(--indigo)}}
/* every place the orb could live */
#dots{{position:absolute;left:0;top:0;width:393px;height:851px;z-index:5;opacity:0}}
.dot{{position:absolute;width:5px;height:5px;border-radius:50%;background:#3A4048}}
#thumb{{position:absolute;left:0;top:0;width:62px;height:62px;border-radius:50%;
  margin-left:-31px;margin-top:-31px;background:rgba(237,239,243,.10);
  border:2px solid rgba(237,239,243,.5);z-index:7;opacity:0}}
#dev{{left:{DEV_L}px;top:{DEV_T}px}}
#devB_unused{{position:absolute;width:{DEV_W}px;height:{DEV_H}px;background:#000;
  border:1px solid var(--line);border-radius:26px;padding:{PAD}px;top:{DEV_T}px;opacity:0}}
#gm,#sl{{opacity:0}}
#orb{{left:{OX-26:.1f}px;top:{OY-26:.1f}px}}
#wipe{{left:{OX-26:.1f}px;top:{OY-26:.1f}px}}
#fmark{{left:579px;top:{540-101-40:.0f}px;width:202px;height:202px;opacity:0}}
#fword{{left:821px;top:{540-66-40:.0f}px}}
#meter{{left:{HX-95:.0f}px;top:{HY+270:.0f}px;height:190px}}
#head{{left:1010px;top:392px;width:840px}}
#sub{{left:1010px;top:592px;width:800px}}
#slam{{top:436px}}
#split{{position:absolute;inset:0;background:var(--indigo);opacity:0;transform-origin:100% 50%}}
#zuluw{{overflow:hidden}}
#splitlab{{position:absolute;right:96px;top:104px;color:#0A0B0D;font-size:64px;font-weight:900;
  letter-spacing:-.02em;opacity:0}}
#splitlab2{{position:absolute;left:620px;top:104px;color:var(--dim);font-size:64px;font-weight:900;
  letter-spacing:-.02em;opacity:0}}
#rule{{position:absolute;left:1010px;top:560px;width:640px;height:6px;background:var(--indigo);
  transform-origin:0 50%}}
#lockmark{{left:{960-330:.0f}px;top:{540-101:.0f}px;width:202px;height:202px;opacity:0}}
#lockword{{left:{960-90:.0f}px;top:{540-66:.0f}px}}
.fact{{left:0;width:1920px;text-align:center}}
#url{{left:0;width:1920px;text-align:center;top:872px}}
#trace{{position:absolute;height:6px;background:var(--indigo);transform-origin:0 50%;opacity:0}}
</style></head><body>
<div id="root" data-composition-id="rmblr" data-width="{W}" data-height="{H}"
     data-fps="{FPS}" data-duration="{DUR}">
  <div class="clip lay" id="film" data-start="0" data-duration="{DUR}" data-track-index="0">

    <div id="ground"></div>

    <!-- the camera: paint() writes one transform here and the phone and the orb
         move together, so a macro crop never has to be computed twice -->
    <div id="world">
      {dev("dev", whatsapp, "main")}
      <div id="orb"><div id="orbring"></div><div id="pulse"></div>{wave_svg("wave")}<div id="glyph">文A</div></div>
    </div>

    <div id="tag" data-layout-allow-overlap data-layout-allow-occlusion></div>
    <div id="card"><div id="cardtext" data-layout-allow-overlap data-layout-allow-occlusion></div>
      <div id="cardsub" data-layout-allow-overlap data-layout-allow-occlusion></div></div>

    <div id="vig"></div>

    <div id="flood">
      {wave_svg("fmark")}
      <div id="fword">RMBLR</div>
      <div class="ffact" id="ff1" style="top:706px">Your own API key. No account. No subscription.</div>
      <div class="ffact" id="ff2" style="top:760px">A dictionary of your own words. Open source, MIT.</div>
      <div id="furl" style="top:836px">github.com/Past-da-king/rmblr</div>
    </div>

  </div>
<script src="vendor/gsap.min.js"></script>
<script>
(function(){{
const $$=s=>document.getElementById(s);
const PHASES={js(PHASES)},FIELDTEXT={js(FIELDTEXT)},SHOTS={js(SHOTS)};
const CARD={js(CARD)},TAG={js(TAG)};
const TONES={{wa:{TONES['wa']},gm:{TONES['gm']},sl:{TONES['sl']}}};
const BARS={BARS};
const COL={{idle:"#15171B",recording:"#E05561",working:"#7C6CFF",done:"#4FBF87"}};
const orb=$$("orb"),orbring=$$("orbring"),wave=$$("wave"),glyph=$$("glyph"),
      wbars=[].slice.call(document.querySelectorAll("#wave .wb"));
const pick=(r,t)=>{{let v=null;for(const x of r) if(t>=x[0]&&t<x[1]) v=x; return v;}};
const last=(r,t)=>{{let v=null;for(const x of r) if(t>=x[0]) v=x; return v;}};
const level=t=>Math.max(0,Math.min(1,0.52+0.46*(
  0.60*Math.sin(2*Math.PI*3.10*t)+0.30*Math.sin(2*Math.PI*7.30*t+1.1)
 +0.25*Math.sin(2*Math.PI*1.70*t+2.3))));

function paint(t){{
  /* THE CUT IS STATE. Resolve which shot we are in from t, then place the
     camera and set visibility — no tweens, so every seek lands exactly right. */
  const sh=last(SHOTS,t)||SHOTS[0], mode=sh[1], k=sh[4];
  let tx,ty;
  if(mode==="orb"){{ tx=960-453*k; ty=540-544.5*k; }}
  else {{ tx=960-(153+sh[2])*k; ty=540-(114.5+sh[3])*k; }}
  $$("world").style.transform="translate("+tx.toFixed(2)+"px,"+ty.toFixed(2)+"px) scale("+k+")";
  $$("dev").style.opacity   = (mode==="phone")?1:0;
  $$("orb").style.opacity   = (mode==="phone"||mode==="orb")?1:0;
  $$("card").style.opacity  = (mode==="card")?1:0;
  $$("flood").style.opacity = (mode==="flood")?1:0;
  $$("wa").style.opacity=sh[5]==="wa"?1:0;
  $$("gm").style.opacity=sh[5]==="gm"?1:0;
  $$("sl").style.opacity=sh[5]==="sl"?1:0;
  for(let i=0;i<5;i++) $$("uc"+i).style.opacity=sh[6]?1:0;
  const ph=(pick(PHASES,t)||[0,0,"idle"])[2];
  const rec=ph==="recording", lv=rec?level(t):0;
  orb.style.background=(t<1.90)?"#F2F4F8":COL[ph];
  orbring.style.opacity=rec?1:0;
  orbring.style.transform="scale("+(1+lv*0.30).toFixed(4)+")";
  for(let i=0;i<5;i++){{
    const h=Math.min(BARS[i]*(1+lv*(i===2?0.15:0.30)),1.0);
    wbars[i].setAttribute("y1",(50-h*50).toFixed(2));
    wbars[i].setAttribute("y2",(50+h*50).toFixed(2));
  }}
  const g=(t>={TRANSLATE_ON}&&t<{TRANSLATE_OFF}&&ph==="idle");
  glyph.style.opacity=g?1:0; wave.style.opacity=g?0:1;

  ["wa-txt","gm-txt","sl-txt"].forEach(id=>{{
    const e=last(FIELDTEXT.filter(r=>r[1]===id),t),el=$$(id); if(el) el.textContent=e?e[2]:"";
  }});
  document.querySelectorAll("#dev .caret").forEach(c=>
    c.style.opacity=(t>2.1&&t<26.0&&Math.floor(t*2)%2===0)?1:0);

  const c=pick(CARD,t);
  $$("cardtext").textContent=c?c[2]:""; $$("cardsub").textContent=c?c[3]:"";
  const tg=pick(TAG,t); $$("tag").textContent=tg?tg[2]:"";
  const set=t<15.0?TONES.wa:(t<16.35?TONES.wa:(t<17.65?TONES.gm:TONES.sl));
  /* the three-app beat: whichever phone the frame is on owns the list */
  const tset = (t>=15.50&&t<17.00)?TONES.gm : ((t>=17.00&&t<18.50)?TONES.sl : TONES.wa);
  for(let i=0;i<5;i++) $$("uc"+i).textContent=tset[i];
}}

const tl=gsap.timeline({{paused:true}}), IR={{immediateRender:false}};
const OUT4="power4.out", INOUT="power3.inOut";
gsap.set("#orb",{{transformOrigin:"50% 50%"}});
gsap.set("#head span,#sub span,#zuluw span",{{yPercent:100}});
gsap.set("#rule",{{scaleX:0}});
gsap.set("#meter",{{transformOrigin:"50% 100%"}});
tl.to({{p:0}},{{p:1,duration:{DUR},ease:"none",onUpdate:function(){{paint(this.targets()[0].p*{DUR});}}}},0);

/* every reveal is a MASK, never a soft fade — flat design has no dissolve */
function say(el,at,dur){{ tl.fromTo(el+" span",{{yPercent:100}},
  {{yPercent:0,duration:dur||0.5,ease:OUT4,immediateRender:false}},at); }}
function unsay(el,at){{ tl.to(el+" span",{{yPercent:-110,duration:0.35,ease:"power3.in"}},at); }}
function slam(at){{ tl.fromTo("#slam",{{opacity:0,scaleX:1.25,scaleY:0.6}},
  {{opacity:1,scaleX:1,scaleY:1,duration:0.26,ease:OUT4,immediateRender:false}},at); }}
function unslam(at){{ tl.to("#slam",{{opacity:0,duration:0.14,ease:"none"}},at); }}
function devTo(at,x,dur){{ tl.to("#dev",{{x:x,duration:dur||0.55,ease:OUT4}},at); }}
function orbTo(at,dx,dy,sc,dur,ez){{ tl.to("#orb",{{x:dx,y:dy,scale:sc,duration:dur,ease:ez||OUT4}},at); }}

gsap.set("#orb",{{transformOrigin:"50% 50%"}});

/* Only genuine MOTION lives on the timeline now — the springs and the thumb.
   Everything that is a cut is resolved from t by paint(). */
tl.fromTo("#orb",{{scale:0.5}},{{scale:1,duration:0.34,ease:"back.out(2.4)",...IR}},2.60);
tl.fromTo("#thumb",{{opacity:0,x:300,y:900}},
  {{opacity:1,x:300,y:430,duration:0.24,ease:OUT4,...IR}},4.26);
tl.fromTo("#thumb",{{scale:1}},{{scale:0.8,duration:0.07,yoyo:true,repeat:1,ease:OUT4,...IR}},4.52);
tl.to("#thumb",{{opacity:0,duration:0.06}},4.66);
/* the arc springs out with an OS-style overshoot, then the thumb walks it */
for(let i=0;i<5;i++)
  tl.fromTo("#uc"+i,{{scale:0.5}},
    {{scale:1,duration:0.30,ease:"back.out(2.8)",immediateRender:false}},8.56+i*0.05);
tl.fromTo("#thumb",{{opacity:0,x:300,y:900}},
  {{opacity:1,x:300,y:430,duration:0.22,ease:OUT4,...IR}},8.58);
/* chip centres, straight off fan_angle(): 116dp from the orb at (300,430) */
tl.to("#thumb",{{x:270,y:542,duration:0.46,ease:"power2.inOut"}},8.92);   /* → SUMMARY */
tl.to("#thumb",{{x:184,y:430,duration:0.46,ease:"power2.inOut"}},9.46);   /* → GRAMMAR */
tl.to("#thumb",{{x:270,y:318,duration:0.46,ease:"power2.inOut"}},10.00);  /* → CASUAL  */
tl.to("#thumb",{{opacity:0,duration:0.08}},10.42);
[[9.24,4],[9.78,2],[10.30,0]].forEach(s=>tl.call(()=>{{
  for(let i=0;i<5;i++) $$("uc"+i).classList.toggle("on",i===s[1]);}},null,s[0]));
tl.call(()=>{{for(let i=0;i<5;i++) $$("uc"+i).classList.remove("on");}},null,10.52);
/* the two other apps get the same spring */
for(const at of [15.54,17.04])
  for(let i=0;i<5;i++)
    tl.fromTo("#uc"+i,{{scale:0.5}},
      {{scale:1,duration:0.26,ease:"back.out(2.8)",immediateRender:false}},at+i*0.045);
tl.call(()=>$$("uc0").classList.add("on"),null,16.30);
tl.call(()=>$$("uc0").classList.remove("on"),null,16.98);
tl.call(()=>$$("uc1").classList.add("on"),null,17.80);
tl.call(()=>$$("uc1").classList.remove("on"),null,18.46);
/* the drag */
tl.fromTo("#thumb",{{opacity:0,x:300,y:900}},
  {{opacity:1,x:300,y:430,duration:0.14,ease:OUT4,...IR}},20.08);
tl.fromTo("#dots",{{opacity:0}},{{opacity:1,duration:0.08,...IR}},20.14);
tl.to("#thumb",{{x:150,y:250,duration:0.30,ease:"power2.inOut"}},20.20);
tl.to(["#dots","#thumb"],{{opacity:0,duration:0.10}},20.50);
/* the climax's one moving beat, and the flood's type */
for(let i=0;i<5;i++)
  tl.fromTo("#uc"+i,{{scale:0.5}},
    {{scale:1,duration:0.18,ease:"back.out(3)",immediateRender:false}},21.96+i*0.02);
tl.call(()=>$$("uc0").classList.add("on"),null,22.02);
tl.call(()=>$$("uc0").classList.remove("on"),null,22.36);
tl.set("#fmark",{{opacity:1}},27.12);
tl.set("#fword",{{opacity:1}},27.44);
tl.set("#ff1",{{opacity:1}},27.92);
tl.set("#ff2",{{opacity:1}},28.28);
tl.set("#furl",{{opacity:1}},28.92);

paint(0);
window.__timelines=window.__timelines||{{}};
window.__timelines["rmblr"]=tl;
}})();
</script></body></html>
"""
open(OUT, "w", encoding="utf-8").write(HTML)
print(f"wrote index.html ({len(HTML)} bytes)  orb home ({OX},{OY})  hero x{HERO}")

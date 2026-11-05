#!/usr/bin/env python3
"""The three apps RMBLR floats over, rebuilt properly.

Ayanda's note on v1: "I want Gmail to look like Gmail. I want WhatsApp to look
like WhatsApp. I want Slack to look like Slack." So these are real recreations —
the actual chrome, the actual colours, the actual furniture — not neutral
stand-ins. Everything is measured in dp and scaled by --dp, so all three sit at
the same scale as the orb.
"""

# ── WhatsApp (dark) ─────────────────────────────────────────────────────────
WA = dict(bg="#0B141A", head="#1F2C34", inb="#202C33", outb="#005C4B",
          comp="#2A3942", send="#00A884", txt="#E9EDEF", dim="#A9B4BB",
          icon="#AEBAC1", tick="#7CCDF0", pill="#182229")

WA_THREAD = [
    ("in",  "Still on for tonight?",        "18:41"),
    ("in",  "What time works for you?",     "18:41"),
    ("out", "Half seven I think",           "18:43"),
    ("in",  "Cool — where are we meeting?", "18:44"),
]

def whatsapp(dp):
    d = lambda v: v * dp
    rows, y = [], 214
    for kind, txt, tm in WA_THREAD:
        w = 150 + len(txt) * 6.4
        w = min(w, 250)
        out = kind == "out"
        side = f'right:{d(10):.1f}px' if out else f'left:{d(10):.1f}px'
        tail = "border-bottom-right-radius:3px" if out else "border-bottom-left-radius:3px"
        ticks = (f'<span class="wa-tk">✓✓</span>' if out else '')
        rows.append(
            f'<div class="wa-b" style="{side};top:{d(y):.1f}px;max-width:{d(w):.1f}px;'
            f'background:{WA["outb"] if out else WA["inb"]};{tail}">'
            f'<span class="wa-t" data-layout-allow-overlap data-layout-allow-occlusion>{txt}</span>'
            f'<span class="wa-m" data-layout-allow-overlap data-layout-allow-occlusion>{tm}{ticks}</span></div>')
        y += 34 + (20 if len(txt) > 26 else 0)
    return f"""
<div class="app" id="wa">
  <div class="wa-head">
    <span class="wa-back" data-layout-allow-overlap data-layout-allow-occlusion>‹</span>
    <span class="wa-av" data-layout-allow-overlap data-layout-allow-occlusion></span>
    <span class="wa-name" data-layout-allow-overlap data-layout-allow-occlusion>Ash</span><span class="wa-on" data-layout-allow-overlap data-layout-allow-occlusion>online</span>
    <span class="wa-ic wa-ic1"></span><span class="wa-ic wa-ic2"></span><span class="wa-dots" data-layout-allow-overlap data-layout-allow-occlusion>⋮</span>
  </div>
  <div class="wa-day" data-layout-allow-overlap data-layout-allow-occlusion>TODAY</div>
  {''.join(rows)}
  <div class="wa-bar">
    <div class="wa-field"><span class="wa-emo" data-layout-allow-overlap data-layout-allow-occlusion>☺</span><span class="fieldtext" id="wa-txt" data-layout-allow-overlap data-layout-allow-occlusion></span><span class="caret"></span><span class="wa-clip" data-layout-allow-overlap data-layout-allow-occlusion>📎</span></div>
    <div class="wa-send" data-layout-allow-overlap data-layout-allow-occlusion>🎤</div>
  </div>
</div>"""

WA_CSS = f"""
#wa{{background:{WA['bg']}}}
#wa::before{{content:"";position:absolute;inset:0;opacity:.045;
  background-image:radial-gradient(circle at 12% 14%,#fff 1.6px,transparent 1.7px),
                   radial-gradient(circle at 62% 32%,#fff 1.2px,transparent 1.3px),
                   radial-gradient(circle at 34% 68%,#fff 1.8px,transparent 1.9px),
                   radial-gradient(circle at 82% 82%,#fff 1.3px,transparent 1.4px);
  background-size:calc(90*var(--dp)) calc(90*var(--dp))}}
.wa-head{{position:absolute;left:0;top:0;right:0;height:calc(56*var(--dp));background:{WA['head']};
  display:flex;align-items:center;padding:0 calc(10*var(--dp))}}
.wa-back{{color:{WA['icon']};font-size:calc(26*var(--dp));width:calc(22*var(--dp));line-height:1}}
.wa-av{{width:calc(34*var(--dp));height:calc(34*var(--dp));border-radius:50%;
  background:linear-gradient(150deg,#6A7B87,#3B4A54);margin:0 calc(10*var(--dp)) 0 calc(2*var(--dp))}}
.wa-name{{color:#E9EDEF;font-size:calc(16*var(--dp));font-weight:500;position:absolute;
  left:calc(60*var(--dp));top:calc(11*var(--dp))}}
.wa-on{{color:{WA['dim']};font-size:calc(11.5*var(--dp));position:absolute;
  left:calc(60*var(--dp));top:calc(31*var(--dp))}}
.wa-ic{{width:calc(19*var(--dp));height:calc(19*var(--dp));border:2px solid {WA['icon']};
  margin-left:auto;opacity:.9}}
.wa-ic1{{border-radius:3px}}
.wa-ic2{{border-radius:50%;margin-left:calc(18*var(--dp))}}
.wa-dots{{color:{WA['icon']};font-size:calc(19*var(--dp));margin-left:calc(14*var(--dp))}}
.wa-day{{position:absolute;left:50%;top:calc(72*var(--dp));transform:translateX(-50%);
  background:{WA['pill']};color:{WA['dim']};font-size:calc(11*var(--dp));font-weight:500;
  padding:calc(4*var(--dp)) calc(11*var(--dp));border-radius:calc(7*var(--dp));letter-spacing:.04em}}
.wa-b{{position:absolute;border-radius:calc(8*var(--dp));padding:calc(6*var(--dp)) calc(9*var(--dp));
  padding-right:calc(46*var(--dp))}}
.wa-t{{color:{WA['txt']};font-size:calc(14.5*var(--dp));line-height:1.32;display:block}}
.wa-m{{position:absolute;right:calc(8*var(--dp));bottom:calc(5*var(--dp));color:#D2DCE0;
  font-size:calc(10.5*var(--dp))}}
.wa-tk{{color:{WA['tick']};margin-left:calc(3*var(--dp));font-size:calc(10*var(--dp))}}
.wa-bar{{position:absolute;left:0;right:0;top:calc(452*var(--dp));height:calc(64*var(--dp));
  display:flex;align-items:center;padding:0 calc(8*var(--dp));gap:calc(7*var(--dp))}}
.wa-field{{flex:1;height:calc(48*var(--dp));background:{WA['comp']};border-radius:calc(24*var(--dp));
  display:flex;align-items:center;padding:0 calc(13*var(--dp));gap:calc(9*var(--dp));overflow:hidden}}
.wa-emo,.wa-clip{{color:{WA['dim']};font-size:calc(19*var(--dp))}}
.wa-clip{{margin-left:auto}}
.wa-send{{width:calc(48*var(--dp));height:calc(48*var(--dp));border-radius:50%;background:{WA['send']};
  display:grid;place-items:center;font-size:calc(21*var(--dp))}}
"""

# ── Gmail (dark, compose) ───────────────────────────────────────────────────
def gmail(dp):
    rows = [("From", "ayanda@gmail.com"), ("To", "ash@—"), ("Subject", "Tonight")]
    r = "".join(
        f'<div class="gm-row" style="top:{(60 + i*46)*dp:.1f}px">'
        f'<span class="gm-k" data-layout-allow-overlap data-layout-allow-occlusion>{k}</span><span class="gm-v" data-layout-allow-overlap data-layout-allow-occlusion>{v}</span></div>'
        for i, (k, v) in enumerate(rows))
    return f"""
<div class="app" id="gm">
  <div class="gm-head">
    <span class="gm-x" data-layout-allow-overlap data-layout-allow-occlusion>✕</span>
    <span class="gm-clip" data-layout-allow-overlap data-layout-allow-occlusion>📎</span><span class="gm-snd" data-layout-allow-overlap data-layout-allow-occlusion>➤</span><span class="gm-dots" data-layout-allow-overlap data-layout-allow-occlusion>⋮</span>
  </div>
  {r}
  <div class="gm-body"><span class="fieldtext" id="gm-txt" data-layout-allow-overlap data-layout-allow-occlusion></span><span class="caret"></span></div>
  <div class="gm-sig" data-layout-allow-overlap data-layout-allow-occlusion>Sent from Gmail</div>
</div>"""

GM_CSS = """
#gm{background:#1F1F1F}
.gm-head{position:absolute;left:0;top:0;right:0;height:calc(56*var(--dp));
  display:flex;align-items:center;padding:0 calc(14*var(--dp))}
.gm-x{color:#E8EAED;font-size:calc(19*var(--dp))}
.gm-clip{margin-left:auto;font-size:calc(18*var(--dp));opacity:.85}
.gm-snd{color:#8AB4F8;font-size:calc(20*var(--dp));margin-left:calc(20*var(--dp))}
.gm-dots{color:#E8EAED;font-size:calc(19*var(--dp));margin-left:calc(18*var(--dp))}
.gm-row{position:absolute;left:calc(16*var(--dp));right:calc(16*var(--dp));height:calc(46*var(--dp));
  border-bottom:1px solid #3C4043;display:flex;align-items:center;gap:calc(14*var(--dp))}
.gm-k{color:#9AA0A6;font-size:calc(14*var(--dp));width:calc(62*var(--dp))}
.gm-v{color:#E8EAED;font-size:calc(15*var(--dp))}
.gm-body{position:absolute;left:calc(16*var(--dp));right:calc(16*var(--dp));top:calc(212*var(--dp));
  min-height:calc(60*var(--dp));display:flex;align-items:flex-start;flex-wrap:wrap}
.gm-sig{position:absolute;left:calc(16*var(--dp));top:calc(470*var(--dp));color:#8A8F94;
  font-size:calc(13*var(--dp))}
"""

# ── Slack (dark) ────────────────────────────────────────────────────────────
SL_MSGS = [("A", "#2EB67D", "Ash",   "18:41", "can someone own the launch note?"),
           ("T", "#E01E5A", "Thabo", "18:42", "I can review it tonight")]

def slack(dp):
    out, y = [], 96
    for ini, col, name, tm, txt in SL_MSGS:
        out.append(
            f'<div class="sl-m" style="top:{y*dp:.1f}px">'
            f'<span class="sl-av" data-layout-allow-overlap data-layout-allow-occlusion style="background:{col}">{ini}</span>'
            f'<span class="sl-n" data-layout-allow-overlap data-layout-allow-occlusion>{name}</span><span class="sl-tm" data-layout-allow-overlap data-layout-allow-occlusion>{tm}</span>'
            f'<span class="sl-tx" data-layout-allow-overlap data-layout-allow-occlusion>{txt}</span></div>')
        y += 66
    return f"""
<div class="app" id="sl">
  <div class="sl-head"><span class="sl-hash" data-layout-allow-overlap data-layout-allow-occlusion>#</span><span class="sl-ch" data-layout-allow-overlap data-layout-allow-occlusion>launch</span>
    <span class="sl-mem" data-layout-allow-overlap data-layout-allow-occlusion>◍ 12</span></div>
  {''.join(out)}
  <div class="sl-box">
    <div class="sl-tools" data-layout-allow-overlap data-layout-allow-occlusion><b>B</b><i>I</i><s>S</s><span>🔗</span><span>≡</span></div>
    <div class="sl-in"><span class="fieldtext" id="sl-txt" data-layout-allow-overlap data-layout-allow-occlusion></span><span class="caret"></span></div>
    <div class="sl-send" data-layout-allow-overlap data-layout-allow-occlusion>➤</div>
  </div>
</div>"""

SL_CSS = """
#sl{background:#1A1D21}
.sl-head{position:absolute;left:0;top:0;right:0;height:calc(50*var(--dp));
  border-bottom:1px solid #35373B;display:flex;align-items:center;padding:0 calc(16*var(--dp));
  gap:calc(3*var(--dp))}
.sl-hash{color:#ABABAD;font-size:calc(19*var(--dp))}
.sl-ch{color:#D1D2D3;font-size:calc(17*var(--dp));font-weight:700}
.sl-mem{margin-left:auto;color:#ABABAD;font-size:calc(13*var(--dp))}
.sl-m{position:absolute;left:calc(16*var(--dp));right:calc(16*var(--dp))}
.sl-av{position:absolute;left:0;top:0;width:calc(36*var(--dp));height:calc(36*var(--dp));
  border-radius:calc(8*var(--dp));display:grid;place-items:center;color:#fff;font-weight:700;
  font-size:calc(16*var(--dp))}
.sl-n{position:absolute;left:calc(48*var(--dp));top:0;color:#D1D2D3;font-size:calc(15*var(--dp));
  font-weight:700}
.sl-tm{position:absolute;left:calc(48*var(--dp));top:0;color:#B6B6B8;font-size:calc(12*var(--dp));
  margin-left:calc(62*var(--dp))}
.sl-tx{position:absolute;left:calc(48*var(--dp));top:calc(21*var(--dp));color:#D1D2D3;
  font-size:calc(15*var(--dp));line-height:1.4}
.sl-box{position:absolute;left:calc(12*var(--dp));right:calc(12*var(--dp));top:calc(368*var(--dp));
  border:1px solid #565856;border-radius:calc(8*var(--dp));background:#222529;
  padding:calc(8*var(--dp)) calc(10*var(--dp))}
.sl-tools{color:#ABABAD;font-size:calc(13*var(--dp));display:flex;gap:calc(13*var(--dp));
  padding-bottom:calc(7*var(--dp));border-bottom:1px solid #35373B;margin-bottom:calc(8*var(--dp))}
.sl-in{min-height:calc(38*var(--dp));display:flex;align-items:center;flex-wrap:wrap}
.sl-send{position:absolute;right:calc(9*var(--dp));bottom:calc(8*var(--dp));
  width:calc(28*var(--dp));height:calc(28*var(--dp));border-radius:calc(5*var(--dp));
  background:#007A5A;color:#fff;display:grid;place-items:center;font-size:calc(13*var(--dp))}
"""

APPS_CSS = WA_CSS + GM_CSS + SL_CSS


KB_ROWS = ["qwertyuiop", "asdfghjkl", "zxcvbnm"]

def keyboard(dp):
    d = lambda v: v * dp
    out, top = [], 48        # relative to #kb, which is itself at 516dp
    for r, row in enumerate(KB_ROWS):
        kw, gap = 35.4, 3.0
        n = len(row)
        span = n * kw + (n - 1) * gap
        x0 = (393 - span) / 2 if r < 2 else (393 - (span + 2 * 48 + 2 * gap)) / 2 + 48 + gap
        if r == 2:
            out.append(f'<div class="kb-k kb-w" data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion style="left:{d((393-(span+2*48+2*gap))/2):.1f}px;'
                       f'top:{d(top):.1f}px">\u21e7</div>')
        for i, ch in enumerate(row):
            out.append(f'<div class="kb-k" data-layout-allow-overlap data-layout-allow-occlusion style="left:{d(x0+i*(kw+gap)):.1f}px;'
                       f'top:{d(top):.1f}px">{ch}</div>')
        if r == 2:
            out.append(f'<div class="kb-k kb-w" data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion style="left:{d(x0+span+gap):.1f}px;'
                       f'top:{d(top):.1f}px">\u232b</div>')
        top += 52
    out.append(f'<div class="kb-k kb-w" data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion style="left:{d(6):.1f}px;top:{d(top):.1f}px">?123</div>')
    out.append(f'<div class="kb-k kb-sp" data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion style="left:{d(60):.1f}px;top:{d(top):.1f}px"></div>')
    out.append(f'<div class="kb-k kb-w kb-go" data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion data-layout-allow-overlap data-layout-allow-occlusion style="left:{d(339):.1f}px;top:{d(top):.1f}px">\u21b5</div>')
    return ('<div id="kb"><div class="kb-sug" data-layout-allow-overlap data-layout-allow-occlusion><span data-layout-allow-overlap data-layout-allow-occlusion>Half</span>'
            '<span class="kb-s1" data-layout-allow-overlap data-layout-allow-occlusion>half</span><span data-layout-allow-overlap data-layout-allow-occlusion>Have</span></div>' + "".join(out) + '</div>')

KB_CSS = """
#kb{position:absolute;left:0;right:0;top:calc(516*var(--dp));height:calc(335*var(--dp));
  background:#1B1C1E;border-top:1px solid #2A2B2E}
.kb-sug{height:calc(40*var(--dp));display:flex;align-items:center;justify-content:space-around;
  color:#AAB0B6;font-size:calc(13*var(--dp));border-bottom:1px solid #2A2B2E}
.kb-s1{color:#E8EAED;font-weight:500}
.kb-k{position:absolute;width:calc(35.4*var(--dp));height:calc(44*var(--dp));
  background:#2C2D30;border-radius:calc(5*var(--dp));display:grid;place-items:center;
  color:#E8EAED;font-size:calc(16*var(--dp))}
.kb-w{width:calc(48*var(--dp));background:#2D2E31;color:#B4BAC0;font-size:calc(14*var(--dp))}
.kb-sp{width:calc(273*var(--dp));background:#2C2D30}
.kb-go{background:#007A5F;color:#fff}
"""
APPS_CSS = APPS_CSS + KB_CSS

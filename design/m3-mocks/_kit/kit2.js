/* RMBLR kit builders: system bars, nav, orb, record button, waveform, app icon placeholders, sample data.
   Load after kit.js. Every screen calls mount(html). Theme: ?theme=dark. */
const status = (t = '9:41') => `<div class="status"><span>${t}</span><span class="r">${ic('wifi', 'f')}${ic('signal_cellular_4_bar', 'f')}${ic('battery_full', 'f')}</span></div>`;
const gesture = () => '<div class="gesture"></div>';
function mount(html, o = {}) {
  document.body.innerHTML = `<div class="phone">${html}${o.noStatus ? '' : status(o.time)}${o.noGesture ? '' : gesture()}</div>`;
  installShapes();
}

/* four tabs, like the app today: Home, Profiles, History, Settings */
const NAV = { home: ['mic', 'Home'], profiles: ['layers', 'Profiles'], history: ['history', 'History'], settings: ['settings', 'Settings'] };
const nav = a => `<div class="fade"></div><div class="ftb"><div class="bar">${Object.entries(NAV).map(([k, [i, l]]) => `<div class="ti ${a === k ? 'on' : ''}">${ic(i, a === k ? 'f' : '')}${a === k ? l : ''}</div>`).join('')}</div></div>`;

/* generic app icon placeholders (no real brands): a shape + a glyph */
const APPS = {
  chat: ['chat_bubble', 'cookie9', 'var(--primary-container)', 'var(--on-primary-container)'],
  mail: ['mail', 'clover4', 'var(--tertiary-container)', 'var(--on-tertiary-container)'],
  notes: ['sticky_note_2', 'puffy', 'var(--success-container)', 'var(--on-success-container)'],
  docs: ['description', 'gem', 'var(--secondary-container)', 'var(--on-secondary-container)'],
  web: ['language', 'sunny', 'var(--surface-container-highest)', 'var(--on-surface)'],
};
const appIc = (k, s = 40) => { const [g, sh, bg, fg] = APPS[k]; return `<div class="shp" style="width:${s}px;height:${s}px;${clip(sh)}background:${bg};color:${fg};display:grid;place-items:center;flex:none">${ic(g, 'f s20')}</div>`; };

/* waveform: rounded bars, quiet on the left (older), full on the right (now). Returns svg. */
function wave(w, h, n = 40, o = {}) {
  const gap = o.gap || 4, bw = (w - gap * (n - 1)) / n; let s = '', seed = o.seed || 7;
  const rnd = () => (seed = (seed * 9301 + 49297) % 233280) / 233280;
  for (let i = 0; i < n; i++) {
    const t = i / (n - 1);
    const env = o.flat ? .35 : (.18 + .82 * Math.pow(Math.sin(Math.PI * Math.min(1, t * 1.08)), 1.4)) * (.35 + .65 * rnd());
    const bh = Math.max(bw, h * env), x = i * (bw + gap), y = (h - bh) / 2;
    const live = t > .78;
    const fill = o.color || 'currentColor';
    s += `<rect x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${bw.toFixed(1)}" height="${bh.toFixed(1)}" rx="${(bw / 2).toFixed(1)}" fill="${fill}" opacity="${o.fade === false ? 1 : (.28 + .72 * Math.pow(t, .8)).toFixed(2)}"/>`;
  }
  return `<svg width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" style="display:block;overflow:visible">${s}</svg>`;
}

/* big record button. state: idle | live. size = core diameter */
function recBtn(state = 'idle', size = 176) {
  const live = state === 'live', box = size + 112;
  const halos = live
    ? [1.3, 1.6, 1.9].map((k, i) => `<div class="halo" style="width:${size * k}px;height:${size * k}px;background:var(--error);opacity:${[.16, .10, .05][i]}"></div>`).join('')
    : `<div class="halo" style="width:${size * 1.34}px;height:${size * 1.34}px;background:var(--primary-container);opacity:.75;${clip('cookie9')}border-radius:0"></div><div class="halo" style="width:${size * 1.62}px;height:${size * 1.62}px;background:var(--primary-container);opacity:.35;${clip('softburst')}border-radius:0"></div>`;
  const shape = live ? `border-radius:${size * .34}px` : clip('cookie9');
  const glyph = live ? ic('stop', 'f') : ic('mic', 'f');
  return `<div class="rec ${live ? 'live' : ''}" style="width:${box}px;height:${box}px">${halos}<div class="core" style="width:${size}px;height:${size}px;${shape}"><span class="i f" style="font-size:${size * .38}px;width:${size * .38}px;height:${size * .38}px;font-variation-settings:'FILL' 1,'wght' 500,'opsz' 48">${live ? 'stop' : 'mic'}</span></div></div>`;
}

/* the floating orb (as on screen over other apps): waveform glyph on a brand circle */
function orb(state = 'idle', s = 56) {
  const live = state === 'live';
  const bg = live ? 'var(--error)' : 'var(--brand)';
  const fg = live ? '#fff' : (document.documentElement.classList.contains('dark') ? '#0a0b0d' : '#fff');
  const bars = [.35, .65, 1, .65, .35].map((k, i) => `<i style="display:block;width:${s * .055}px;height:${s * .42 * k}px;border-radius:9px;background:${fg}"></i>`).join('');
  return `<div style="width:${s}px;height:${s}px;border-radius:50%;background:${bg};box-shadow:${live ? '0 6px 20px rgba(214,54,47,.45)' : '0 6px 20px rgba(90,70,240,.40)'},0 0 0 ${s * .12}px ${live ? 'rgba(214,54,47,.18)' : 'rgba(90,70,240,.16)'};display:flex;align-items:center;justify-content:center;gap:${s * .05}px">${bars}</div>`;
}

/* sample data: neutral, ordinary dictation */
const TONES = { clean: ['Clean up', 'auto_fix_high'], formal: ['Professional', 'work'], casual: ['Casual', 'sentiment_satisfied'], bullets: ['Bullets', 'format_list_bulleted'], summary: ['Summary', 'short_text'], grammar: ['Grammar', 'spellcheck'] };
const toneTag = k => `<span class="tone">${ic(TONES[k][1], 'f')}${TONES[k][0]}</span>`;
const DICT = [
  { app: 'chat', t: '14:32', d: '0:08', tone: 'casual', text: 'Running ten minutes late, start without me and I will grab us a table.' },
  { app: 'mail', t: '11:05', d: '0:21', tone: 'formal', text: 'Hi Dana, thanks for your time today. As discussed, I will send the revised timeline by Thursday and loop in the design team.' },
  { app: 'notes', t: '09:18', d: '0:12', tone: 'bullets', text: 'Milk, oat flakes, tomatoes, lemons, dish soap, bin bags, and something for dinner on Friday.' },
  { app: 'notes', t: '08:40', d: '0:05', tone: 'clean', text: 'Remember to renew the car licence before the end of the month.' },
  { app: 'mail', t: '17:52', d: '0:07', tone: 'casual', text: 'Could you send over the invoice when you get a chance? Thanks so much.' },
  { app: 'chat', t: '16:10', d: '0:03', tone: 'clean', text: 'Sounds great, see you at seven.' },
  { app: 'docs', t: '15:26', d: '0:14', tone: 'bullets', text: 'Agenda for Monday: budget review, hiring update, next release date.' },
];
const wrap = (inner, extra = '') => `<div class="abs" style="${extra}">${inner}</div>`;

/* small app bar: back + title + actions (64dp tall, below the 40dp status bar) */
const appBar = (title, o = {}) => `<div class="abs" style="top:40px;left:0;right:0;height:64px;display:flex;align-items:center;padding:0 4px">${o.back === false ? '<div style="width:12px"></div>' : `<div class="ib">${ic(o.back || 'arrow_back')}</div>`}<div class="title-l" style="flex:1;margin-left:4px">${title}</div>${(o.actions || []).map(a => `<div class="ib">${ic(a)}</div>`).join('')}</div>`;
const scrim = () => '<div class="scrim"></div>';
/* a sheet: handle + content. use inside mount() after the dimmed screen */
const sheet = (inner, pad = 36) => `${scrim()}<div class="sheet" style="padding-bottom:${pad}px"><div class="grab"></div>${inner}</div>`;
const snack = (txt, act, bottom = 108) => `<div class="snack" style="bottom:${bottom}px">${txt}${act ? `<span class="act">${act}</span>` : ''}</div>`;

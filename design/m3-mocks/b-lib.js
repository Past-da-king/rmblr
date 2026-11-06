/* helper B builders (screens 10 to 19). Load after kit.js and kit2.js. */
document.head.insertAdjacentHTML('beforeend', '<link rel="stylesheet" href="kit-b.css">');
const BS = {};
const av = (shape, bg, fg, glyph, s = 40, fill = 'f') => `<div class="shp" style="width:${s}px;height:${s}px;${clip(shape)}background:${bg};color:${fg};display:grid;place-items:center;flex:none">${ic(glyph, fill + (s >= 56 ? ' s32' : ' s20'))}</div>`;
const letterAv = (ch, shape, bg, fg, s = 40) => `<div class="shp" style="width:${s}px;height:${s}px;${clip(shape)}background:${bg};color:${fg};display:grid;place-items:center;flex:none;font:700 ${Math.round(s * .42)}px/1 var(--font)">${ch}</div>`;
const PRI = ['cookie9', 'var(--primary-container)', 'var(--on-primary-container)'];
const TER = ['clover4', 'var(--tertiary-container)', 'var(--on-tertiary-container)'];
const SEC = ['gem', 'var(--secondary-container)', 'var(--on-secondary-container)'];
const SUC = ['puffy', 'var(--success-container)', 'var(--on-success-container)'];
const seg = (items, on, cls = '') => `<div class="seg ${cls}">${items.map((t, i) => `<div class="s ${i === on ? 'on' : ''}">${i === on ? ic('check', 'b') : ''}${t}</div>`).join('')}</div>`;
const slider = (p, dots = false) => `<div class="sl"><div class="a" style="width:calc(${p}% - 6px)"></div><div class="h" style="left:calc(${p}% - 2px)"></div><div class="b" style="left:calc(${p}% + 6px)"></div></div>`;
const sw = on => `<div class="switch ${on ? 'on' : ''}">${on ? ic('check') : ''}</div>`;
const stat = (k, txt, icon) => `<span class="st ${k}">${icon ? ic(icon, 'f') : ''}${txt}</span>`;
const sectionLbl = (title, desc, shape, glyph) => `<div class="lbl">${av(shape[0], shape[1], shape[2], glyph, 40)}<div><div class="t">${title}</div>${desc ? `<div class="d">${desc}</div>` : ''}</div></div>`;
const kr = (lead, t, s, trail = '') => `<div class="kr">${lead}<div class="tx"><div class="s1">${t}</div>${s ? `<div class="s2">${s}</div>` : ''}</div>${trail}</div>`;
const bigHead = (title, sub, shape, glyph) => `<div class="hd"><div><div class="display-s-em">${title}</div><div class="body-m sub">${sub}</div></div>${av(shape[0], shape[1], shape[2], glyph, 64)}</div>`;
const smallBar = (title, o = {}) => `<div class="abs" style="top:40px;left:0;right:0;height:64px;display:flex;align-items:center;padding:0 4px;background:var(--surface-container);z-index:30"><div class="ib">${ic(o.back || 'arrow_back')}</div><div class="title-l" style="flex:1;margin-left:4px">${title}</div></div>`;
const wallpaper = () => `<div class="wall ${DARK ? 'd' : 'l'}"><div class="bl" style="width:300px;height:300px;left:-60px;top:120px;${clip('cookie7')}"></div><div class="bl" style="width:240px;height:240px;right:-70px;top:430px;${clip('softburst')}"></div><div class="bl" style="width:200px;height:200px;left:90px;top:610px;${clip('clover4')}"></div></div>`;
const progBar = (n, at) => `<div class="prog">${Array.from({ length: n }, (_, i) => `<i class="${i < at ? 'd' : i === at ? 'on' : ''}"></i>`).join('')}</div>`;
const obTop = at => `<div class="ob-top">${progBar(5, at)}<div class="btn text" style="height:40px">Skip</div></div>`;
const obBot = (primary, back = true, icon = 'arrow_forward') => `<div class="ob-bot">${back ? `<div class="ib surf" style="--r:50%;width:56px;height:56px">${ic('arrow_back')}</div>` : ''}<div class="btn filled lg">${primary}${icon ? ic(icon, 'b') : ''}</div></div>`;
const APPS2 = { Messages: 'chat', Mail: 'mail', Notes: 'notes', Docs: 'docs', Browser: 'web' };
/* quiet generic provider avatar */
const PROV = {
  Gemini: ['G', 'cookie9', 'var(--primary-container)', 'var(--on-primary-container)'],
  Groq: ['Q', 'clover4', 'var(--tertiary-container)', 'var(--on-tertiary-container)'],
  Mistral: ['M', 'gem', 'var(--secondary-container)', 'var(--on-secondary-container)'],
  OpenRouter: ['R', 'puffy', 'var(--success-container)', 'var(--on-success-container)'],
  OpenAI: ['O', 'sunny', 'var(--surface-container-highest)', 'var(--on-surface)'],
  Custom: ['+', 'flower', 'var(--surface-container-highest)', 'var(--on-surface)'],
};
const pav = (k, s = 40) => letterAv(PROV[k][0], PROV[k][1], PROV[k][2], PROV[k][3], s);
const dim = inner => `${inner}`;

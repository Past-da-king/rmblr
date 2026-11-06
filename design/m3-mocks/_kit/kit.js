/* RMBLR kit: theme switch (?theme=dark), M3E shape library (clip paths), builders. */
const DARK = location.search.includes('dark');
document.documentElement.classList.toggle('dark', DARK);

/* ---------- shape library (objectBoundingBox clip paths) ---------- */
function pathFromR(fn, N = 360) {
  let d = '';
  for (let k = 0; k < N; k++) {
    const th = (2 * Math.PI * k) / N, r = fn(th);
    const x = 0.5 + 0.5 * r * Math.cos(th), y = 0.5 + 0.5 * r * Math.sin(th);
    d += (k ? 'L' : 'M') + x.toFixed(4) + ' ' + y.toFixed(4);
  }
  return d + 'Z';
}
function normMax(fn) { let m = 0; for (let k = 0; k < 360; k++) m = Math.max(m, fn(2 * Math.PI * k / 360)); return th => fn(th) / m; }
function polyR(m, rot, soft) { // rounded regular polygon
  return th => { const a = ((th - rot) % (2 * Math.PI / m) + 2 * Math.PI / m) % (2 * Math.PI / m) - Math.PI / m;
    const p = Math.cos(Math.PI / m) / Math.cos(a); return p * (1 - soft) + soft; };
}
const SHAPES = {
  cookie9: normMax(th => 1 + 0.075 * Math.cos(9 * th)),
  cookie7: normMax(th => 1 + 0.085 * Math.cos(7 * th)),
  clover4: normMax(th => 1 + 0.20 * Math.cos(4 * th - Math.PI / 4 * 0)),
  softburst: normMax(th => 1 + 0.07 * Math.cos(12 * th) + 0.015 * Math.cos(24 * th)),
  sunny: normMax(th => 1 + 0.11 * Math.pow(Math.abs(Math.cos(4 * th)), .6)),
  flower: normMax(th => 1 + 0.11 * Math.cos(8 * th) + 0.02 * Math.cos(16 * th)),
  puffy: normMax(th => 1 + 0.12 * Math.cos(5 * th - Math.PI / 2)),
  gem: normMax(polyR(6, Math.PI / 6, .55)),
  pent: normMax(polyR(5, -Math.PI / 2, .6)),
  diamond: normMax(polyR(4, Math.PI / 4, .55)),
  triangle: normMax(polyR(3, -Math.PI / 2, .62)),
};
function installShapes() {
  let s = '<svg width="0" height="0" style="position:absolute"><defs>';
  for (const k in SHAPES) s += `<clipPath id="s-${k}" clipPathUnits="objectBoundingBox"><path d="${pathFromR(SHAPES[k])}"/></clipPath>`;
  s += `<clipPath id="s-arch" clipPathUnits="objectBoundingBox"><path d="M0 .22C0 .08 .2 0 .5 0S1 .08 1 .22V.93Q1 1 .93 1H.07Q0 1 0 .93Z"/></clipPath>`;
  s += `<clipPath id="s-arch2" clipPathUnits="objectBoundingBox"><path d="M0 .5A.5 .5 0 0 1 1 .5V.9Q1 1 .9 1H.1Q0 1 0 .9Z"/></clipPath>`;
  s += '</defs></svg>';
  document.body.insertAdjacentHTML('afterbegin', s);
}
const clip = n => `clip-path:url(#s-${n});-webkit-clip-path:url(#s-${n});`;

/* ---------- helpers ---------- */
const ic = (n, cls = '') => `<span class="i ${cls}">${n}</span>`;

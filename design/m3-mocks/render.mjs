// node render.mjs [screen ...]   renders every screen (or the named ones) in light + dark to png/ at 390x844 @2x (780x1688)
// Run through ~/bin/build-slot on the server. Needs playwright (npm i playwright; npx playwright install chromium).
import { chromium } from 'playwright';
import { readdirSync, mkdirSync } from 'fs';
import { resolve } from 'path';
const dir = resolve('.');
mkdirSync('png', { recursive: true });
const all = readdirSync('screens').filter(f => f.endsWith('.js')).map(f => f.replace('.js', '')).sort();
const only = process.argv.slice(2);
const list = only.length ? all.filter(s => only.some(o => s.startsWith(o))) : all;
const b = await chromium.launch();
const p = await b.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 });
p.on('pageerror', e => console.log('PAGEERROR', e.message));
for (const s of list) for (const t of ['light', 'dark']) {
  await p.goto(`file://${dir}/index.html?screen=${s}&theme=${t}`);
  await p.evaluate(() => document.fonts.ready);
  await p.waitForTimeout(500);
  await p.screenshot({ path: `png/${s}-${t}.png` });
  console.log('ok', s, t);
}
await b.close();

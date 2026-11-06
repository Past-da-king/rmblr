// node b-overview.mjs  -> png/overview.png : contact sheet of every screen, light left, dark right
import { chromium } from 'playwright';
import { readdirSync } from 'fs';
import { resolve } from 'path';
const dir = resolve('.');
const names = [...new Set(readdirSync('png').filter(f => /-(light|dark)\.png$/.test(f)).map(f => f.replace(/-(light|dark)\.png$/, '')))].sort();
const b = await chromium.launch();
const p = await b.newPage({ viewport: { width: 1760, height: 900 }, deviceScaleFactor: 1 });
await p.goto(`file://${dir}/b-overview.html?list=${encodeURIComponent(JSON.stringify(names))}`);
await p.evaluate(() => document.fonts.ready);
await p.waitForFunction(() => [...document.images].every(i => i.complete));
await p.waitForTimeout(500);
await p.screenshot({ path: 'png/overview.png', fullPage: true });
console.log('ok overview', names.length);
await b.close();

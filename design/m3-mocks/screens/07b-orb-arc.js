const cx = 340, cy = 460, R = 150;
const tones = [['Professional', 115, 0], ['Clean up', 140, 0], ['Casual', 165, 1], ['Bullets', 190, 0], ['Summary', 215, 0]];
const pills = tones.map(([n, a, s]) => { const x = cx + R * Math.cos(a * Math.PI / 180), y = cy - R * Math.sin(a * Math.PI / 180);
  return `<div class="abs" style="left:${x}px;top:${y}px;transform:translate(-50%,-50%) ${s ? 'scale(1.1)' : ''};height:${s ? 44 : 40}px;padding:0 ${s ? 20 : 16}px;border-radius:22px;display:flex;align-items:center;font:600 14px/20px var(--font);white-space:nowrap;background:${s ? 'var(--brand)' : 'var(--surface-container-highest)'};color:${s ? '#fff' : 'var(--on-surface)'};box-shadow:var(--e3);z-index:45">${n}</div>`; }).join('');
mount(`
${fakeChat('', { inputTop: 506 })}
<div class="abs" style="inset:0;background:var(--scrim);opacity:.45;z-index:35"></div>
<svg class="abs" style="left:0;top:0;z-index:36" width="390" height="844"><path d="M ${cx + R * Math.cos(105 * Math.PI / 180)} ${cy - R * Math.sin(105 * Math.PI / 180)} A ${R} ${R} 0 0 0 ${cx + R * Math.cos(225 * Math.PI / 180)} ${cy - R * Math.sin(225 * Math.PI / 180)}" fill="none" stroke="var(--on-surface)" stroke-opacity=".22" stroke-width="2" stroke-dasharray="2 8" stroke-linecap="round"/></svg>
${pills}
<div class="abs" style="left:${cx - 28}px;top:${cy - 28}px;z-index:46;transform:scale(1.18)">${orb('idle', 56)}</div>
<div class="abs" style="left:0;right:0;top:272px;text-align:center;z-index:46"><span class="label-l" style="padding:6px 14px;border-radius:14px;background:var(--inverse-surface);color:var(--inverse-on-surface)">Flick to a tone, release to dictate</span></div>
`);

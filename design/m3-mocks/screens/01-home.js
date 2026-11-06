const rows = DICT.slice(0, 3).map(d => `<div class="row">${appIc(d.app)}<div class="tx"><div class="body-m clamp2" style="color:var(--on-surface)">${d.text}</div><div class="meta"><span>${d.t}</span><i class="dotsep"></i><span>${d.d}</span><span style="margin-left:auto">${toneTag(d.tone)}</span></div></div></div>`).join('');
mount(`
<div class="abs" style="top:52px;left:24px;right:12px;display:flex;align-items:flex-start;justify-content:space-between">
  <div><div class="eyebrow">Fri 6 Nov</div><div class="display-s-em" style="margin-top:2px">Ready</div></div>
  <div class="ib surf" style="margin-top:4px">${ic('help')}</div>
</div>
<div class="abs" style="top:112px;left:0;right:0;display:flex;justify-content:center">${recBtn('idle', 156)}</div>
<div class="abs" style="top:392px;left:0;right:0;text-align:center">
  <div class="title-l-em">Tap to dictate</div>
  <div class="body-m muted" style="margin-top:2px">Or hold the orb in any app for tones</div>
</div>
<div class="abs" style="top:462px;left:0;right:0;padding:0 16px;display:flex;gap:8px;overflow:hidden">
  <span class="chip sel">${ic('check')}Auto</span><span class="chip">Spanish</span><span class="chip">${ic("add")}More</span>
</div>
<div class="abs" style="top:512px;left:16px;right:16px;display:flex;gap:8px">
  <div class="card" style="flex:1;height:64px;border-radius:20px;padding:0 16px;display:flex;align-items:center;gap:12px;background:var(--primary-container);color:var(--on-primary-container)">${ic('auto_fix_high', 'f')}<div style="flex:1"><div class="label-s" style="opacity:.75">TONE</div><div class="title-m-em">Clean up</div></div>${ic('expand_more')}</div>
  <div class="card" style="width:112px;height:64px;border-radius:20px;padding:0 16px;display:flex;flex-direction:column;justify-content:center;background:var(--surface-container-high)"><div class="label-s muted">TODAY</div><div class="title-l-em" style="line-height:24px">7 <span class="body-s muted" style="font-weight:500">notes</span></div></div>
</div>
<div class="abs" style="top:596px;left:0;right:0"><div class="sec-h"><div class="title-m-em">Recent</div><div class="label-l" style="color:var(--primary)">See all</div></div></div>
<div class="abs grp" style="top:640px;left:0;right:0">${rows}</div>
${nav('home')}
`);

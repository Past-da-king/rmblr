const r = (d) => `<div class="row">${appIc(d.app)}<div class="tx"><div class="body-m clamp2" style="color:var(--on-surface)">${d.text}</div><div class="meta"><span>${d.t}</span><i class="dotsep"></i><span>${d.d}</span><span style="margin-left:auto">${toneTag(d.tone)}</span></div></div></div>`;
mount(`
<div class="abs" style="top:52px;left:24px;right:12px"><div class="display-s-em">History</div></div>
<div class="abs" style="top:116px;left:16px;right:16px"><div class="field">${ic('search')}<span class="ph">Search your dictations</span></div></div>
<div class="abs grp" style="top:196px;left:0;right:0">${r(DICT[0])}${r(DICT[1])}${r(DICT[2])}</div>
${sheet(`
<div style="display:flex;flex-direction:column;align-items:center;text-align:center;padding:4px 8px 0">
  <div class="shp" style="width:72px;height:72px;${clip('cookie9')}background:var(--error-container);color:var(--on-error-container);display:grid;place-items:center">${ic('delete', 'f s32')}</div>
  <div class="headline-s-em" style="margin-top:16px">Delete this dictation?</div>
  <div class="body-m muted" style="margin-top:6px;max-width:300px">It is removed from the history on this phone, along with the original transcript. This can not be undone.</div>
</div>
<div class="row" style="margin-top:16px;border-radius:20px;background:var(--surface-container-high)">${appIc('mail')}<div class="tx"><div class="body-m clamp1">Hi Dana, thanks for your time today. As discussed...</div><div class="meta" style="margin-top:2px"><span>Today, 11:05</span><i class="dotsep"></i><span>0:21</span></div></div></div>
<div style="display:flex;flex-direction:column;gap:8px;margin-top:20px"><div class="btn err lg" style="width:100%">${ic('delete', 'f')}Delete</div><div class="btn tonal lg" style="width:100%">Keep it</div></div>
`, 40)}
`);

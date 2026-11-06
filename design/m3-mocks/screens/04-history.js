const r = (d) => `<div class="row">${appIc(d.app)}<div class="tx"><div class="body-m clamp2" style="color:var(--on-surface)">${d.text}</div><div class="meta"><span>${d.t}</span><i class="dotsep"></i><span>${d.d}</span><span style="margin-left:auto">${toneTag(d.tone)}</span></div></div></div>`;
const swiped = d => `<div style="position:relative;overflow:hidden;border-radius:8px;background:var(--error-container);color:var(--on-error-container)"><div style="position:absolute;right:0;top:0;bottom:0;width:80px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px" class="label-s">${ic('delete', 'f')}Delete</div><div style="transform:translateX(-80px);width:100%">${r(d)}</div></div>`;
mount(`
<div class="abs" style="top:52px;left:24px;right:12px;display:flex;align-items:center;justify-content:space-between"><div class="display-s-em">History</div><div class="ib surf">${ic('delete_sweep')}</div></div>
<div class="abs" style="top:116px;left:16px;right:16px"><div class="field">${ic('search')}<span class="ph">Search your dictations</span></div></div>
<div class="abs" style="top:184px;left:16px;right:16px"><div class="fg" style="gap:2px">
  <div class="fi on">${ic('apps', 'f')}All</div><div class="fi">${ic('chat_bubble')}</div><div class="fi">${ic('mail')}</div><div class="fi">${ic('sticky_note_2')}</div><div class="fi">${ic('translate')}</div></div></div>
<div class="abs" style="top:240px;left:0;right:0"><div class="sec-h" style="height:36px"><div class="label-l muted">Today</div></div></div>
<div class="abs grp" style="top:276px;left:0;right:0">${r(DICT[0])}${swiped(DICT[1])}${r(DICT[2])}</div>
<div class="abs" style="top:600px;left:0;right:0"><div class="sec-h" style="height:36px"><div class="label-l muted">Yesterday</div></div></div>
<div class="abs grp" style="top:636px;left:0;right:0">${r(DICT[4])}${r(DICT[5])}</div>
${nav('history')}
`);

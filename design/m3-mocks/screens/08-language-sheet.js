const langs = ['Arabic', 'Bengali', 'Chinese', 'Dutch', 'English', 'French', 'German', 'Hindi', 'Indonesian', 'Italian', 'Japanese'];
const sel = ['English', 'Spanish'];
const row = (n, on) => `<div style="display:flex;align-items:center;gap:16px;height:48px;padding:0 12px;border-radius:12px;${on ? 'background:var(--primary-container);color:var(--on-primary-container)' : ''}"><div class="check sq ${on ? 'on' : ''}">${on ? ic('check', 'b s18') : ''}</div><div class="title-m" style="flex:1">${n}</div></div>`;
mount(`
${appBar('Settings', { back: false })}
<div class="abs" style="top:116px;left:16px;right:16px;height:64px;border-radius:20px;background:var(--surface-container)"></div>
${sheet(`
<div style="display:flex;align-items:center;justify-content:space-between"><div class="headline-s-em">Languages</div><div class="ib">${ic('close')}</div></div>
<div style="display:flex;align-items:center;gap:14px;margin-top:6px;padding:12px 16px;border-radius:20px;background:var(--surface-container-high)"><div style="width:40px;height:40px;border-radius:14px;display:grid;place-items:center;background:var(--primary);color:var(--on-primary)">${ic('travel_explore')}</div><div style="flex:1"><div class="title-m-em">Auto-detect</div><div class="body-s muted">Works out the language as you speak</div></div><div class="switch on">${ic('check')}</div></div>
<div style="display:flex;gap:10px;align-items:flex-start;margin:12px 4px 12px;color:var(--on-surface-variant)">${ic('info', 's20')}<div class="body-s">Mixing is supported. Pick every language you switch between and RMBLR will keep each one in its own script.</div></div>
<div class="field" style="height:48px;border-radius:24px">${ic('search')}<span class="ph" style="font-size:15px">Search 99 languages</span></div>
<div style="display:flex;gap:8px;margin:12px 0 4px"><span class="chip sel">${sel[0]}${ic('close')}</span><span class="chip sel">${sel[1]}${ic('close')}</span></div>
<div style="height:290px;overflow:hidden;position:relative"><div style="display:flex;flex-direction:column;gap:2px;margin-top:4px">${langs.map(l => row(l, sel.includes(l))).join('')}</div><div style="position:absolute;left:0;right:0;bottom:0;height:60px;background:linear-gradient(to bottom,transparent,var(--surface-container-low))"></div></div>
<div class="btn filled lg" style="width:100%;margin-top:8px">Done</div>
`, 40)}
`);

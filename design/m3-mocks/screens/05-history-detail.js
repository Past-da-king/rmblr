const kv = (i, k, v) => `<div style="flex:1;min-width:0;background:var(--surface-container);border-radius:8px;padding:12px 14px"><div class="label-s muted" style="display:flex;gap:6px;align-items:center">${ic(i, 's18')}${k}</div><div class="title-m-em" style="margin-top:4px">${v}</div></div>`;
mount(`
${appBar('Dictation', { actions: ['star', 'more_vert'] })}
<div class="abs" style="top:108px;left:24px;right:16px;display:flex;align-items:center;gap:12px">${appIc('mail', 44)}<div><div class="title-m-em">Mail</div><div class="body-s muted">Today, 11:05</div></div></div>
<div class="abs card" style="top:172px;left:16px;right:16px;background:var(--surface-container-high);padding:18px 20px">
  <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px"><span class="label-s muted" style="letter-spacing:1px">REWRITTEN</span>${toneTag('formal')}</div>
  <div style="font:500 18px/27px var(--font)">Hi Dana, thank you for your time today. As discussed, I will send the revised timeline by Thursday and loop in the design team.</div>
</div>
<div class="abs card" style="top:362px;left:16px;right:16px;background:var(--surface-container-low);padding:16px 20px;border:1px dashed var(--outline-variant)">
  <div class="label-s muted" style="letter-spacing:1px;margin-bottom:6px">AS SPOKEN</div>
  <div class="body-m muted">hi dana um thanks for your time today as we discussed i will send the the revised timeline by thursday and uh loop in the design team</div>
</div>
<div class="abs" style="top:508px;left:16px;right:16px;display:flex;flex-direction:column;gap:4px">
  <div style="display:flex;gap:4px">${kv('schedule', 'Length', '0:21')}${kv('notes', 'Words', '24')}</div>
  <div style="display:flex;gap:4px">${kv('translate', 'Language', 'Auto, English')}${kv('graphic_eq', 'Engine', 'Your provider')}</div>
</div>
<div class="abs" style="top:664px;left:16px;right:16px;display:flex;gap:8px">
  <div class="btn filled" style="flex:1;height:48px;border-radius:24px">${ic('content_copy')}Copy</div>
  <div class="btn tonal" style="flex:1;height:48px;border-radius:24px">${ic('ios_share')}Share</div>
  <div class="btn tonal" style="width:48px;padding:0;height:48px;border-radius:24px">${ic('auto_fix_high')}</div>
</div>
<div class="abs" style="top:728px;left:16px;right:16px;display:flex"><div class="btn" style="flex:1;height:48px;border-radius:24px;color:var(--error)">${ic('delete')}Delete dictation</div></div>
`);

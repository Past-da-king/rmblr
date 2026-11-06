const tones = [['Clean up', 'auto_fix_high', 0], ['Professional', 'work', 1], ['Casual', 'sentiment_satisfied', 0], ['Bullets', 'format_list_bulleted', 0], ['Summary', 'short_text', 0], ['Grammar', 'spellcheck', 0]]
  .map(([l, i, s]) => `<span class="chip ${s ? 'sel' : ''}">${s ? ic('check') : ic(i)}${l}</span>`).join('');
mount(`
${appBar('Result', { actions: ['more_vert'] })}
<div class="abs" style="top:112px;left:16px;right:16px;display:flex;gap:8px;align-items:center">
  <span class="badge" style="height:28px;border-radius:10px;background:var(--success-container);color:var(--on-success-container);font-size:12px">${ic('check_circle', 'f')}Done</span>
  <span class="chip tonal" style="height:28px;font-size:12px;padding:0 10px">${ic('mail')}Mail</span>
  <span class="chip tonal" style="height:28px;font-size:12px;padding:0 10px">${ic('schedule')}0:21</span>
  <span class="chip tonal" style="height:28px;font-size:12px;padding:0 10px">${ic('translate')}English</span>
</div>
<div class="abs card" style="top:156px;left:16px;right:16px;background:var(--surface-container-high);padding:20px 20px 12px">
  <div class="label-s muted" style="letter-spacing:1px;margin-bottom:8px">PROFESSIONAL</div>
  <div style="font:500 19px/28px var(--font);letter-spacing:-.1px">Hi Dana, thank you for your time today. As discussed, I will send the revised timeline by Thursday and loop in the design team.</div>
  <div style="display:flex;align-items:center;gap:8px;margin-top:14px;height:40px;border-top:1px solid var(--outline-variant);padding-top:8px;color:var(--primary)" class="label-l">${ic('undo')}<span>was: raw transcript</span><span style="margin-left:auto">${ic('expand_less')}</span></div>
</div>
<div class="abs" style="top:372px;left:16px;right:16px;border-radius:20px;background:var(--surface-container-low);padding:14px 16px;border:1px dashed var(--outline-variant)">
  <div class="body-m muted">hi dana um thanks for your time today as we discussed i will send the the revised timeline by thursday and uh loop in the design team</div>
</div>
<div class="abs" style="top:500px;left:16px;right:16px"><div class="label-m muted" style="margin-bottom:8px;letter-spacing:1px">WRITE IT AS</div>
<div style="display:flex;flex-wrap:wrap;gap:8px">${tones}<span class="chip">${ic('add')}New</span></div></div>
<div class="abs" style="top:664px;left:16px;right:16px"><div class="seg tonal">
  <div class="s">${ic('content_copy')}Copy</div><div class="s">${ic('ios_share')}Share</div><div class="s">${ic('translate')}Translate</div></div></div>
${snack('Copied to clipboard', 'Undo', 64)}
`);

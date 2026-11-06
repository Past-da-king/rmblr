const items = [['Clean up', 'Removes filler words and false starts', 'auto_fix_high', 1], ['Professional', 'Polite, concise, ready to send', 'work'], ['Casual', 'Relaxed, for chats and messages', 'sentiment_satisfied'], ['Bullets', 'Key points as a short list', 'format_list_bulleted'], ['Summary', 'One or two sentences', 'short_text'], ['Grammar', 'Spelling and punctuation only', 'spellcheck']];
const it = ([n, d, i, s]) => `<div class="sel-row ${s ? 'on' : ''}" style="min-height:58px;padding:6px 16px 6px 12px"><div style="width:40px;height:40px;display:grid;place-items:center;border-radius:14px;background:${s ? 'var(--primary)' : 'var(--surface-container-highest)'};color:${s ? 'var(--on-primary)' : 'var(--on-surface)'}">${ic(i, 'f')}</div><div style="flex:1"><div class="title-m-em">${n}</div><div class="body-s" style="opacity:.75">${d}</div></div>${s ? ic('check_circle', 'f') : ''}</div>`;
mount(`
<div class="abs" style="top:52px;left:24px;right:12px"><div class="eyebrow">Fri 6 Nov</div><div class="display-s-em">Ready</div></div>
<div class="abs" style="top:120px;left:0;right:0;display:flex;justify-content:center">${recBtn('idle', 140)}</div>
${sheet(`
<div style="display:flex;align-items:center;justify-content:space-between"><div class="headline-s-em">Pick a tone</div><div class="ib">${ic('tune')}</div></div>
<div class="body-m muted" style="margin:0 0 8px">How your words are written back</div>
<div style="display:flex;flex-direction:column;gap:2px">${items.map(it).join('')}
<div class="label-s muted" style="margin:8px 0 2px 12px;letter-spacing:1px">YOURS</div>
${it(['Meeting notes', 'Decisions and who does what', 'edit_note'])}
</div>
<div style="display:flex;align-items:center;gap:16px;height:56px;padding:0 12px;margin-top:6px;border-radius:12px;border:1.5px dashed var(--outline)" class="title-m-em"><div style="width:40px;height:40px;border-radius:14px;display:grid;place-items:center;background:var(--primary-container);color:var(--on-primary-container)">${ic('add', 'b')}</div>New tone</div>
`, 36)}
`);

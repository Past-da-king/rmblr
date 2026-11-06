mount(`
<div class="abs" style="top:40px;left:0;right:0;height:64px;display:flex;align-items:center;padding:0 4px 0 4px"><div class="ib">${ic('close')}</div><div class="title-l" style="flex:1;margin-left:4px">Listening</div><span class="chip pri" style="margin-right:12px">${ic('translate')}Auto</span></div>
<div class="abs" style="top:116px;left:0;right:0;text-align:center">
  <div style="display:inline-flex;align-items:center;gap:10px"><i style="width:10px;height:10px;border-radius:5px;background:var(--error);display:block"></i><span class="display-s-em" style="font-variant-numeric:tabular-nums">0:14</span></div>
  <div class="label-m muted" style="margin-top:2px;letter-spacing:1px">TONE: CLEAN UP</div>
</div>
<div class="abs" style="top:196px;left:24px;right:24px;height:104px;display:flex;align-items:center;color:var(--brand)">${wave(342, 104, 46, { gap: 4, seed: 11 })}</div>
<div class="abs card words" style="top:322px;left:16px;right:16px;height:216px;background:var(--surface-container);padding:20px 20px;overflow:hidden">
  <div class="label-s muted" style="margin-bottom:10px;letter-spacing:1px">LIVE TRANSCRIPT</div>
  <div style="font:500 20px/30px var(--font);letter-spacing:-.1px"><span class="done">Hi Dana, thanks for your time today. As discussed, I will send the revised timeline by</span> <span class="guess">Thursday and loop in the</span><span class="caret"></span></div>
</div>
<div class="abs" style="top:566px;left:0;right:0;height:230px;display:flex;align-items:center;justify-content:center;gap:20px">
  <div class="ib tonal" style="width:56px;height:56px;--bg:var(--secondary-container)">${ic('close')}</div>
  ${recBtn('live', 104)}
  <div class="ib tonal" style="width:56px;height:56px;--bg:var(--secondary-container)">${ic('auto_fix_high')}</div>
</div>
<div class="abs" style="top:786px;left:0;right:0;text-align:center"><span class="label-m muted">Tap to stop and write</span></div>
`);

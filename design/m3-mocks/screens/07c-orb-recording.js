mount(`
${fakeChat('', { inputTop: 506 })}
<div class="abs words" style="left:20px;top:372px;width:262px;padding:14px 16px;border-radius:24px 24px 8px 24px;background:var(--surface-container-highest);box-shadow:var(--e4);z-index:44">
  <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px"><i style="width:8px;height:8px;border-radius:4px;background:var(--error);display:block"></i><span class="label-m" style="font-variant-numeric:tabular-nums">0:06</span><span style="margin-left:auto;color:var(--error)">${wave(80, 18, 16, { gap: 3, seed: 5, color: 'currentColor' })}</span></div>
  <div style="font:500 16px/23px var(--font)"><span class="done">Sure, I will bring it. Should I grab some dessert</span> <span class="guess">on the way</span><span class="caret"></span></div>
</div>
<div class="abs" style="left:340px;top:416px;width:0;height:0;z-index:44"></div>
<div class="abs" style="left:312px;top:432px;z-index:44">${orb('live', 56)}</div>
`);

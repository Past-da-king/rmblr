/* screens 10 to 13 */
const settingsHead = () => bigHead('Settings', 'Set the first three once, then forget this page exists.', SEC, 'settings');

BS['10-settings'] = () => mount(`<div class="page">
${settingsHead()}
<div class="sect">${sectionLbl('Listening', 'Who turns your voice into words', PRI, 'mic')}
 <div class="tcard">
  <div class="title-s" style="margin-bottom:10px">Speech engine</div>
  ${seg(['Gemini', 'Groq', 'Mistral', 'More'], 0)}
  <div style="display:flex;align-items:center;gap:12px;margin-top:14px;padding:12px 14px;border-radius:20px;background:var(--surface-container-high)">
   <div class="tx" style="flex:1"><div class="s1" style="font:600 15px/20px var(--font)">Gemini Transcribe Live</div><div class="s2" style="font:400 13px/18px var(--font);color:var(--on-surface-variant)">Words appear while you speak</div></div>
   ${stat('pri', 'Streams', 'bolt')}
  </div>
 </div>
</div>
<div class="sect">${sectionLbl('Rewriting', 'Who applies your tones and translates', TER, 'auto_fix_high')}
 <div class="tcard">
  <div class="title-s" style="margin-bottom:10px">Text provider</div>
  <div style="display:flex;flex-wrap:wrap;gap:8px">
   <span class="chip sel">${ic('check')}Gemini</span><span class="chip">Mistral</span><span class="chip">OpenRouter</span><span class="chip">Groq</span><span class="chip">OpenAI</span><span class="chip">Custom</span>
  </div>
  <div style="display:flex;align-items:center;justify-content:space-between;margin-top:14px;padding:0 4px">
   <div><div class="label-s" style="font:500 12px/16px var(--font);color:var(--on-surface-variant)">Model</div><div class="title-s">gemini-3.5-flash-lite</div></div>${ic('edit', 's20')}
  </div>
 </div>
</div>
<div class="sect">${sectionLbl('API keys', 'Stored on this phone only', SUC, 'key')}
 <div class="stack">${kr(pav('Gemini'), 'Gemini', 'AIza••••••••3f9a', stat('ok', 'Connected', 'check_circle'))}</div>
</div>
</div>${nav('settings')}`);

BS['10-settings-2'] = () => mount(`<div class="page" style="padding-top:104px">
${smallBar('Settings')}
<div class="sect" style="margin-top:6px">${sectionLbl('API keys', 'Stored on this phone only. One key per provider.', SUC, 'key')}
 <div class="stack">
  ${kr(pav('Gemini'), 'Gemini', 'AIza••••••••3f9a', `${stat('ok', 'Connected', 'check_circle')}`)}
  ${kr(pav('Groq'), 'Groq', 'gsk_••••••••••••k2Qz', `<span class="sbtn">${ic('network_check')}Test</span>`)}
  ${kr(pav('Mistral'), 'Mistral', 'Not set', `<span class="sbtn pri">${ic('add')}Add</span>`)}
  ${kr(pav('OpenRouter'), 'OpenRouter', 'Not set', `<span class="sbtn pri">${ic('add')}Add</span>`)}
  ${kr(pav('Custom'), 'Anything OpenAI-compatible', 'Your own base URL and model', `${ic('chevron_right')}`)}
 </div>
</div>
<div class="sect">${sectionLbl('Languages', 'What you speak', TER, 'translate')}
 <div class="tcard">
  <div style="display:flex;align-items:center;gap:12px"><div style="flex:1"><div class="s1" style="font:600 15px/20px var(--font)">Auto-detect</div><div class="s2" style="font:400 13px/18px var(--font);color:var(--on-surface-variant)">Works out the language, or a mix of them, as you speak</div></div>${sw(true)}</div>
  <div style="height:1px;background:var(--outline-variant);margin:14px 0;opacity:.6"></div>
  <div class="title-s" style="margin-bottom:10px">Also expect</div>
  <div style="display:flex;flex-wrap:wrap;gap:8px"><span class="chip sel">French${ic('close')}</span><span class="chip sel">Spanish${ic('close')}</span><span class="chip">${ic('add')}Add language</span></div>
 </div>
</div>
</div>${nav('settings')}`);

BS['10-settings-3'] = () => mount(`<div class="page" style="padding-top:104px">
${smallBar('Settings')}
<div class="sect" style="margin-top:6px">${sectionLbl('Orb behaviour', 'How the floating orb acts', PRI, 'bubble_chart')}
 <div class="stack">
  ${kr('', 'Show when a text field is focused', 'Hidden everywhere else', sw(true))}
  <div class="kr" style="flex-direction:column;align-items:stretch;gap:2px"><div style="display:flex;align-items:center;justify-content:space-between"><div class="s1">Size</div><div style="display:flex;align-items:center;justify-content:center;width:56px;height:56px">${orb('idle', 40)}</div></div>${slider(46)}<div class="mini"><span>Small</span><span>Large</span></div></div>
  <div class="kr" style="flex-direction:column;align-items:stretch;gap:10px"><div class="s1">Side of the screen</div>${seg(['Left', 'Right'], 1)}</div>
  <div class="kr" style="flex-direction:column;align-items:stretch;gap:2px"><div style="display:flex;justify-content:space-between"><div class="s1">Fade when idle</div><div class="s2">After 3 s</div></div>${slider(30)}<div class="mini"><span>Never</span><span>Quickly</span></div></div>
 </div>
</div>
<div class="sect">${sectionLbl('Appearance', 'Colour and brightness', SEC, 'palette')}
 <div class="tcard">
  ${seg(['Light', 'Dark', 'Black'], DARK ? 1 : 0)}
  <div style="display:flex;align-items:center;gap:12px;margin-top:16px"><div style="flex:1"><div class="s1" style="font:600 15px/20px var(--font)">Wallpaper colours</div><div class="s2" style="font:400 13px/18px var(--font);color:var(--on-surface-variant)">Take the palette from your wallpaper</div></div>${sw(false)}</div>
  <div style="display:flex;gap:10px;margin-top:14px"><span class="sw" style="background:var(--brand);width:28px;height:28px;box-shadow:0 0 0 3px var(--surface-container),0 0 0 5px var(--primary)"></span><span class="sw" style="background:#0f6a78;width:28px;height:28px"></span><span class="sw" style="background:#1d7a4b;width:28px;height:28px"></span><span class="sw" style="background:#b3541e;width:28px;height:28px"></span><span class="sw" style="background:#a8326f;width:28px;height:28px"></span></div>
 </div>
</div>
</div>${nav('settings')}`);

BS['10-settings-4'] = () => mount(`<div class="page" style="padding-top:104px">
${smallBar('Settings')}
<div class="sect" style="margin-top:6px">
 <div style="border-radius:28px;background:var(--tertiary-container);color:var(--on-tertiary-container);padding:20px;position:relative;overflow:hidden">
  <div class="shp" style="position:absolute;right:-30px;top:-34px;width:130px;height:130px;${clip('softburst')}background:var(--tertiary);opacity:.18"></div>
  <div style="display:flex;align-items:center;gap:12px;margin-bottom:10px">${av('gem', 'var(--tertiary)', 'var(--on-tertiary)', 'shield_lock', 40)}<div class="title-l-em">Private by design</div></div>
  <div class="body-m" style="position:relative">Your keys stay on this phone. Audio goes only to the provider you pick, straight from the app. No account, nothing sent to us.</div>
 </div>
</div>
<div class="sect" style="margin-top:14px">${sectionLbl('Typing', 'Small touches', SEC, 'keyboard')}
 <div class="stack">${kr('', 'Vibrate on tap', '', sw(true))}${kr('', 'Capitalise sentences', '', sw(true))}</div>
</div>
<div class="sect">${sectionLbl('About', '', PRI, 'info')}
 <div class="tcard" style="display:flex;align-items:center;gap:16px;margin-bottom:4px;border-radius:28px 28px 8px 8px">
  <div style="width:64px;height:64px;${clip('cookie9')}background:var(--brand);display:grid;place-items:center">${ic('mic', 'f s32').replace('<span', '<span style="color:' + (DARK ? '#0a0b0d' : '#fff') + '"')}</div>
  <div style="flex:1"><div class="title-l-em">RMBLR</div><div class="body-m" style="color:var(--on-surface-variant)">Version 2.6.2 · MIT licence</div></div>${stat('ok', 'Up to date', 'check')}
 </div>
 <div class="stack">${kr(ic('system_update', 's20'), 'Check for updates', '', ic('chevron_right'))}${kr(ic('auto_awesome', 's20'), 'What is new', '', ic('chevron_right'))}${kr(ic('replay', 's20'), 'Show the walkthrough again', '', ic('chevron_right'))}${kr(ic('forum', 's20'), 'Feedback', '', ic('chevron_right'))}</div>
</div>
</div>${nav('settings')}`);

/* 11: key sheet over settings */
BS['11-settings-keys'] = () => mount(`<div class="page" style="opacity:.55">${settingsHead()}
<div class="sect">${sectionLbl('API keys', '', SUC, 'key')}<div class="stack">${kr(pav('Gemini'), 'Gemini', 'AIza••••••••3f9a', stat('ok', 'Connected', 'check_circle'))}${kr(pav('Groq'), 'Groq', 'Not set', '')}</div></div></div>
${sheet(`
<div style="display:flex;align-items:center;gap:14px;padding:0 8px 16px">${pav('Groq', 48)}<div style="flex:1"><div class="title-l-em">Groq API key</div><div class="body-s" style="color:var(--on-surface-variant);font:400 13px/18px var(--font)">Used for dictation and rewriting</div></div></div>
<div class="keyfield focus"><span class="fl">API key</span><div class="v">gsk_••••••••••••k2Qz</div><div class="ib">${ic('visibility')}</div></div>
<div style="display:flex;gap:8px;margin:12px 0 0">
 <div class="sbtn" style="height:40px;padding:0 16px">${ic('content_paste')}Paste</div><div class="sbtn" style="height:40px;padding:0 16px">${ic('open_in_new')}Get a key</div>
</div>
<div style="display:flex;align-items:center;gap:12px;margin:16px 0;padding:14px 16px;border-radius:20px;background:var(--success-container);color:var(--on-success-container)">
 ${av('cookie9', 'var(--success)', '#fff', 'check', 36)}
 <div style="flex:1"><div style="font:650 15px/20px var(--font)">Key works</div><div style="font:400 13px/18px var(--font);opacity:.85">Checked just now. whisper-large-v3-turbo answered.</div></div>
</div>
<div class="body-s" style="font:400 13px/18px var(--font);color:var(--on-surface-variant);padding:0 8px 16px">Free from console.groq.com. The key is saved on this phone and sent only to Groq.</div>
<div style="display:flex;gap:8px"><div class="btn tonal lg" style="flex:1">${ic('network_check')}Test again</div><div class="btn filled lg" style="flex:1">Save</div></div>
`, 40)}`);

/* 12: profiles */
const profCard = (name, shape, glyph, apps, tap, extra = '', fallback = false) => `<div class="pcard">
 <div class="top">${av(shape[0], shape[1], shape[2], glyph, 48)}<div style="flex:1;min-width:0"><div class="title-m-em">${name}</div><div class="body-s" style="font:400 13px/18px var(--font);color:var(--on-surface-variant)">${fallback ? 'Every app not listed' : apps.length + ' apps'}</div></div>
 ${fallback ? stat('idle', 'Fallback') : `<div class="appstack">${apps.map(a => appIc(a, 32)).join('')}</div>`}</div>
 <div style="display:flex;gap:8px;flex-wrap:wrap">${toneTag(tap)}<span class="chip tag" style="height:28px">${ic('swipe', 's18')}${extra}</span></div>
</div>`;
BS['12-profiles'] = () => mount(`<div class="page">
${bigHead('Profiles', 'A different tone for each kind of app.', PRI, 'layers')}
<div class="sect stack" style="display:flex;flex-direction:column;gap:10px">
 ${profCard('Chat', PRI, 'chat_bubble', ['chat', 'notes', 'web'], 'casual', 'Hold for 4 tones')}
 ${profCard('Email', TER, 'mail', ['mail', 'docs'], 'formal', 'Hold for 4 tones')}
 ${profCard('Work', SEC, 'work', ['docs', 'notes', 'chat'], 'clean', 'Hold for 4 tones')}
 ${profCard('Anywhere else', SUC, 'public', [], 'clean', 'Hold for 4 tones', true)}
</div>
<div class="xfab">${ic('add')}Add profile</div>
</div>${nav('profiles')}`);

/* 13: choose apps sheet */
const appPick = (name, k, on) => `<div class="sel-row ${on ? 'on' : ''}" style="min-height:60px;padding:6px 16px 6px 12px">${appIc(k, 40)}<div style="flex:1"><div class="title-m">${name}</div></div><div class="check ${on ? 'on' : ''}">${on ? ic('check', 'b s18') : ''}</div></div>`;
BS['13-profile-apps-sheet'] = () => mount(`<div class="page" style="opacity:.55">${bigHead('Profiles', 'A different tone for each kind of app.', PRI, 'layers')}<div class="sect">${profCard('Chat', PRI, 'chat_bubble', ['chat', 'notes'], 'casual', 'Hold for 4 tones')}</div></div>
${sheet(`
<div style="display:flex;align-items:center;gap:12px;padding:0 8px 12px"><div style="flex:1"><div class="title-l-em">Choose apps</div><div class="body-s" style="font:400 13px/18px var(--font);color:var(--on-surface-variant)">Chat tones apply in these apps</div></div>${stat('pri', '3 selected')}</div>
<div class="field" style="margin-bottom:8px">${ic('search')}<span class="ph">Search installed apps</span></div>
<div style="display:flex;flex-direction:column;gap:2px;margin:0 -4px">
 ${appPick('Messages', 'chat', true)}${appPick('Notes', 'notes', true)}${appPick('Browser', 'web', true)}${appPick('Mail', 'mail', false)}${appPick('Docs', 'docs', false)}
</div>
<div style="display:flex;gap:8px;margin-top:14px"><div class="btn tonal lg" style="flex:1">Clear</div><div class="btn filled lg" style="flex:2">Done</div></div>
`, 40)}`);
/* screens 14 to 17 */
const obCopy = (top, h, p) => `<div class="abs ob-copy" style="top:${top}px;left:0;right:0"><div class="display-s-em">${h}</div><div class="body-l">${p}</div></div>`;
const brandFg = () => (DARK ? '#0a0b0d' : '#fff');
const shapeBox = (shape, w, bg, style = '', inner = '') => `<div class="shp abs" style="width:${w}px;height:${w}px;${clip(shape)}background:${bg};${style}">${inner}</div>`;

BS['14-onboarding-1'] = () => mount(`<div class="page">${obTop(0)}
<div class="abs" style="top:70px;left:0;width:390px;height:330px">
 ${shapeBox('softburst', 300, 'var(--primary-container)', 'left:45px;top:10px;opacity:.55')}
 ${shapeBox('cookie9', 240, 'var(--primary-container)', 'left:75px;top:40px')}
 ${shapeBox('cookie7', 128, 'var(--brand)', 'left:131px;top:96px;display:grid;place-items:center;color:' + brandFg(), ic('mic', 'f s40').replace('<span', '<span style="font-size:56px;width:56px;height:56px"'))}
 <div class="abs" style="left:60px;top:288px;color:var(--primary)">${wave(270, 40, 34, { seed: 11, color: 'currentColor' })}</div>
 ${shapeBox('sunny', 52, 'var(--tertiary-container)', 'right:42px;top:26px')}
 ${shapeBox('clover4', 40, 'var(--success-container)', 'left:34px;top:70px')}
 <div class="abs" style="right:16px;top:196px;width:226px;padding:14px 16px;border-radius:20px 20px 6px 20px;background:var(--surface-container-high);box-shadow:var(--e3)"><div class="body-m words"><span class="done">Running ten minutes late, </span><span class="guess">start without me</span><span class="caret"></span></div></div>
</div>
${obCopy(430, 'Speak. It writes.', 'Say it out loud and the words land in whatever app you are typing in, cleaned up and ready to send.')}
${obBot('Next', false)}
</div>`);

const kbd = () => [10, 9].map((n, r) => `<div style="display:flex;gap:5px;justify-content:center;margin-top:6px">${Array.from({ length: n }, () => '<i style="display:block;width:' + (r === 2 ? 34 : 29) + 'px;height:38px;border-radius:9px;background:var(--surface-container-high)"></i>').join('')}</div>`).join('');
BS['14-onboarding-2'] = () => mount(`<div class="page">${obTop(1)}
<div class="abs" style="top:70px;left:0;width:390px;height:340px">
 ${shapeBox('clover4', 90, 'var(--tertiary-container)', 'right:20px;top:4px;opacity:.7')}
 <div class="abs" style="left:30px;top:14px;width:300px;height:156px;border-radius:28px;background:var(--surface-container);padding:16px">
  <div style="display:flex;align-items:center;gap:10px">${appIc('chat', 36)}<div><i style="display:block;width:84px;height:8px;border-radius:4px;background:var(--on-surface);opacity:.75"></i><i style="display:block;width:50px;height:6px;border-radius:3px;background:var(--outline);opacity:.5;margin-top:6px"></i></div></div>
  <div style="margin-top:14px;display:flex;flex-direction:column;gap:8px"><i style="display:block;width:190px;height:30px;border-radius:16px 16px 16px 4px;background:var(--surface-container-highest)"></i><i style="display:block;width:150px;height:30px;border-radius:16px 16px 4px 16px;background:var(--primary-container);align-self:flex-end"></i></div>
 </div>
 <div class="abs" style="left:30px;top:182px;width:300px;height:56px;border-radius:28px;background:var(--surface-container-high);box-shadow:inset 0 0 0 2px var(--primary);display:flex;align-items:center;padding:0 16px"><span class="body-m" style="color:var(--on-surface-variant)">Message</span><span class="caret" style="margin-left:2px"></span></div>
 <div class="abs" style="left:262px;top:164px">${orb('idle', 60)}</div>
 <div class="abs" style="left:14px;top:246px;width:362px">${kbd()}</div>
</div>
${obCopy(420, 'It shows up when you type', 'Tap into any text field and a small orb appears beside it. When you are not typing, it is not there.')}
${obBot('Next')}
</div>`);

const arcPill = (txt, ang, sel, cx = 195, cy = 250, r = 132) => { const a = ang * Math.PI / 180, x = cx + r * Math.cos(a), y = cy + r * Math.sin(a); return `<div class="pill ${sel ? 'sel' : ''}" style="position:absolute;left:${x}px;top:${y}px;transform:translate(-50%,-50%)">${txt}</div>`; };
const legend = (g, t, s) => `<div style="flex:1;border-radius:20px;background:var(--surface-container);padding:12px 10px 12px 12px"><div style="display:flex;align-items:center;gap:6px;color:var(--primary)">${ic(g, 'f s20')}<span style="font:650 14px/18px var(--font);color:var(--on-surface)">${t}</span></div><div style="font:400 12px/16px var(--font);color:var(--on-surface-variant);margin-top:4px">${s}</div></div>`;
BS['14-onboarding-3'] = () => mount(`<div class="page">${obTop(2)}
<div class="abs" style="top:70px;left:0;width:390px;height:330px">
 <svg class="abs" style="left:0;top:0" width="390" height="330" viewBox="0 0 390 330"><path d="M63 250 A132 132 0 0 1 327 250" fill="none" stroke="var(--primary)" stroke-opacity=".28" stroke-width="2" stroke-dasharray="3 8" stroke-linecap="round"/><path d="M195 250 C 210 190, 250 160, 280 135" fill="none" stroke="var(--primary)" stroke-width="5" stroke-linecap="round" opacity=".55"/></svg>
 <div class="abs" style="left:250px;top:120px;width:52px;height:52px;border-radius:50%;background:var(--primary);opacity:.22"></div><div class="abs" style="left:262px;top:132px;width:28px;height:28px;border-radius:50%;background:var(--primary);opacity:.5"></div>
 ${arcPill('Casual', -165, false)}${arcPill('Clean up', -128, false)}${arcPill('Summary', -90, false)}${arcPill('Professional', -52, true)}${arcPill('Bullets', -15, false)}
 <div class="abs" style="left:159px;top:214px">${orb('idle', 72)}</div>
 ${shapeBox('sunny', 36, 'var(--tertiary-container)', 'left:20px;top:20px')}${shapeBox('puffy', 30, 'var(--success-container)', 'right:26px;top:22px')}
</div>
${obCopy(420, 'Tap, hold, flick', 'Tap to dictate. Hold to fan out your tones, then flick toward one. Double tap to translate.')}
<div class="abs" style="top:552px;left:16px;right:16px;display:flex;gap:8px">${legend('touch_app', 'Tap', 'Dictate')}${legend('pan_tool', 'Hold', 'Fan the tones')}${legend('swipe_up', 'Flick', 'Fire one')}</div>
${obBot('Next')}
</div>`);

BS['14-onboarding-4'] = () => mount(`<div class="page">${obTop(3)}
<div class="abs" style="top:64px;left:0;width:390px;height:210px">
 ${shapeBox('flower', 190, 'var(--tertiary-container)', 'left:100px;top:10px')}
 ${shapeBox('gem', 118, 'var(--brand)', 'left:136px;top:46px;display:grid;place-items:center;color:' + brandFg(), ic('key', 'f').replace('<span', '<span style="font-size:56px;width:56px;height:56px"'))}
 ${shapeBox('cookie7', 46, 'var(--primary-container)', 'left:62px;top:30px')}${shapeBox('clover4', 40, 'var(--success-container)', 'right:62px;top:150px')}
</div>
${obCopy(288, 'Bring your own key', 'Paste a key from the provider you like. No account with us, ever.')}
<div class="abs" style="top:414px;left:16px;right:16px"><div class="eyebrow" style="margin:0 8px 10px">Provider</div><div style="display:flex;flex-wrap:wrap;gap:8px"><span class="chip sel">${ic('check')}Gemini</span><span class="chip">Groq</span><span class="chip">Mistral</span><span class="chip">OpenRouter</span><span class="chip">OpenAI</span><span class="chip">Custom</span></div></div>
<div class="abs" style="top:546px;left:16px;right:16px"><div class="keyfield focus"><span class="fl">Gemini API key</span><div class="v" style="letter-spacing:0;color:var(--on-surface-variant);font-weight:400">AIzaSy...<span class="caret"></span></div><div class="sbtn" style="height:40px">${ic('content_paste')}Paste</div></div>
 <div style="display:flex;gap:10px;align-items:flex-start;margin:16px 8px 0;color:var(--on-surface-variant)">${ic('lock', 's20')}<div class="body-s" style="font:400 13px/18px var(--font)">Free from Google AI Studio. The key lives on this phone and calls the provider directly.</div></div></div>
${obBot('Continue')}
</div>`);

/* 15 permissions */
const perm = (shape, glyph, t, s, ok, btn) => `<div style="border-radius:24px;background:var(--surface-container);padding:14px 14px 14px 14px;display:flex;gap:12px;align-items:center">${av(shape[0], shape[1], shape[2], glyph, 48)}<div style="flex:1;min-width:0"><div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap"><span class="title-m-em">${t}</span>${ok ? stat('ok', 'Granted', 'check') : stat('need', 'Needed')}</div><div style="font:400 13px/18px var(--font);color:var(--on-surface-variant);margin-top:3px">${s}</div></div>${ok ? '' : `<span class="sbtn pri" style="align-self:center">${btn || 'Grant'}</span>`}</div>`;
BS['15-permissions'] = () => mount(`<div class="page">${obTop(4)}
<div class="ob-copy" style="margin-top:14px"><div class="display-s-em">Four permissions</div><div class="body-m" style="color:var(--on-surface-variant);margin-top:10px">Android asks you to switch these on yourself. Each one is for a single job.</div></div>
<div style="padding:18px 16px 0;display:flex;flex-direction:column;gap:8px">
 ${perm(PRI, 'mic', 'Microphone', 'To hear you. Audio goes only to the provider you chose.', true)}
 ${perm(TER, 'layers', 'Draw over other apps', 'To float the orb above the app you are typing in.', true)}
 ${perm(SEC, 'accessibility_new', 'Accessibility', 'Service named RMBLR orb. Notices a focused text field and writes your words back into it.', false, 'Open')}
 ${perm(SUC, 'notifications', 'Notifications', 'To show a Stop button while it is listening.', false)}
</div>
<div class="ob-bot">${`<div class="ib surf" style="--r:50%;width:56px;height:56px">${ic('arrow_back')}</div>`}<div class="btn lg" style="background:var(--surface-container-high);color:var(--outline);flex:1">Finish setup</div></div>
</div>`);

/* 16 update sheet */
const logGroup = (title, color, items) => `<div><div class="g" style="color:${color};margin-bottom:8px">${title}</div><ul class="clog" style="gap:8px;padding:0">${items.map(i => `<li>${i}</li>`).join('')}</ul></div>`;
BS['16-update-sheet'] = () => mount(`<div class="page" style="opacity:.55">${smallBar('Settings')}<div class="sect" style="margin-top:80px"><div class="tcard" style="display:flex;gap:14px;align-items:center"><div style="width:56px;height:56px;${clip('cookie9')}background:var(--brand);display:grid;place-items:center">${ic('system_update', 'f').replace('<span', '<span style="color:' + brandFg() + '"')}</div><div><div class="title-m-em">Update available</div><div class="body-s" style="font:400 13px/18px var(--font);color:var(--on-surface-variant)">2.7.0 · you have 2.6.2</div></div></div></div></div>
${sheet(`
<div style="display:flex;align-items:center;gap:14px;padding:0 8px 14px"><div style="width:52px;height:52px;${clip('cookie9')}background:var(--brand);display:grid;place-items:center">${ic('auto_awesome', 'f s32').replace('<span', '<span style="color:' + brandFg() + '"')}</div><div style="flex:1"><div class="title-l-em">RMBLR 2.7.0 is out</div><div style="display:flex;gap:6px;margin-top:4px">${stat('pri', 'Update')}<span class="st idle">14 MB · APK</span></div></div></div>
<div style="display:flex;flex-direction:column;gap:16px;padding:6px 8px 0;max-height:300px;overflow:hidden">
 ${logGroup('New', 'var(--primary)', ['Pick a tone from the quick settings tile', 'Dictation history is searchable'])}
 ${logGroup('Better', 'var(--tertiary)', ['The orb remembers its place per app', 'Faster start on streaming engines'])}
 ${logGroup('Fixed', 'var(--success)', ['Text sometimes landed twice in one app', 'Key test reported success on a blank key'])}
</div>
<div class="body-s" style="font:400 12px/16px var(--font);color:var(--on-surface-variant);margin:14px 8px">RMBLR is not on an app store, so updates arrive as an APK that you install yourself.</div>
<div style="display:flex;gap:8px"><div class="btn text lg" style="flex:1">Skip this version</div><div class="btn filled lg" style="flex:1.2">${ic('download', 'b')}Download</div></div>
`, 40)}`);

/* 17 feedback */
const kindCard = (g, t, on) => `<div style="flex:1;height:92px;border-radius:24px;background:${on ? 'var(--primary-container)' : 'var(--surface-container)'};color:${on ? 'var(--on-primary-container)' : 'var(--on-surface)'};padding:12px;display:flex;flex-direction:column;justify-content:space-between;${on ? 'box-shadow:inset 0 0 0 2px var(--primary)' : ''}">${ic(g, on ? 'f s32' : 's32')}<div style="font:650 13px/16px var(--font)">${t}</div></div>`;
BS['17-feedback'] = () => mount(`<div class="page" style="padding-top:104px">
${smallBar('Feedback')}
<div class="sect" style="margin-top:8px"><div class="eyebrow" style="padding:0 8px 10px">What is it about</div>
 <div style="display:flex;gap:8px">${kindCard('bug_report', 'Something is broken', true)}${kindCard('lightbulb', 'An idea', false)}${kindCard('help', 'A question', false)}</div></div>
<div class="sect"><div class="field ta" style="flex-direction:column;gap:8px"><div class="body-m" style="color:var(--on-surface)">The orb does not appear in my browser's address bar. It works in every other text field I tried.<span class="caret"></span></div><div style="margin-top:auto;font:500 12px/16px var(--font);color:var(--outline)">Steps, what you saw, what you expected</div></div></div>
<div class="sect"><div class="stack">${kr('', 'Attach version and phone', 'RMBLR 2.6.2 · Android 15', sw(true))}</div></div>
<div class="sect" style="display:flex;flex-direction:column;gap:8px"><div class="btn filled lg" style="width:100%">${ic('open_in_new', 'b')}Open a GitHub issue</div><div class="btn tonal lg" style="width:100%">${ic('content_copy', 'b')}Copy it instead</div></div>
<div class="sect"><div class="stack">${kr(ic('forum', 's20'), 'Read what others have said', 'Open issues on GitHub', ic('chevron_right'))}${kr(ic('code', 's20'), 'The code itself', 'github.com/Past-da-king/rmblr', ic('chevron_right'))}</div></div>
<div class="body-s" style="font:400 12px/16px var(--font);color:var(--on-surface-variant);padding:0 28px">Nothing is sent anywhere until you press one of the two buttons. A GitHub issue needs a free GitHub account.</div>
</div>`);
/* screens 18 and 19 */
const snip = (app, txt, t) => `<div style="display:flex;align-items:center;gap:10px;height:34px">${appIc(app, 26)}<div class="clamp1" style="flex:1;font:400 13px/18px var(--font)">${txt}</div><div style="font:500 11px/14px var(--font);color:var(--on-surface-variant)">${t}</div></div>`;
const wg1 = (x, y, state = 'idle') => `<div class="wg" style="left:${x}px;top:${y}px;width:84px;height:84px;border-radius:28px;display:grid;place-items:center;background:${state === 'live' ? 'var(--error-container)' : 'var(--primary-container)'}">${orb(state, 46)}</div>`;
const wg2 = (x, y) => `<div class="wg" style="left:${x}px;top:${y}px;width:176px;height:84px;border-radius:28px;padding:0 14px;display:flex;align-items:center;gap:12px">${orb('idle', 44)}<div style="min-width:0;flex:1"><div class="title-m-em">Dictate</div><div class="clamp1" style="font:400 12px/16px var(--font);color:var(--on-surface-variant)">Sounds great, see you at seven.</div></div></div>`;
const wg4rec = (x, y) => `<div class="wg" style="left:${x}px;top:${y}px;width:358px;height:176px;border-radius:32px;padding:14px 16px">
 <div style="display:flex;align-items:center;gap:12px">${orb('live', 44)}<div style="flex:1"><div class="title-m-em" style="color:var(--error)">Recording</div><div style="font:500 12px/16px var(--font);color:var(--on-surface-variant)">0:07 · Casual</div></div><div style="width:44px;height:44px;border-radius:16px;background:var(--error);color:var(--on-error);display:grid;place-items:center">${ic('stop', 'f')}</div></div>
 <div style="margin:8px 0 4px;color:var(--error);display:flex;justify-content:center">${wave(326, 32, 44, { seed: 5, color: 'currentColor' })}</div>
 ${snip('chat', 'Sounds great, see you at seven.', '14:32')}${snip('mail', 'Could you send over the invoice when you get a chance?', '11:05')}</div>`;
const wg4idle = (x, y) => `<div class="wg" style="left:${x}px;top:${y}px;width:358px;height:166px;border-radius:32px;padding:14px 16px">
 <div style="display:flex;align-items:center;gap:12px">${orb('idle', 44)}<div style="flex:1"><div class="title-m-em">Tap to dictate</div><div style="font:500 12px/16px var(--font);color:var(--on-surface-variant)">Writes into the app you are in</div></div><span class="chip tag">${ic('auto_fix_high', 'f')}Clean up</span></div>
 <div style="height:1px;background:var(--outline-variant);margin:10px 0 4px;opacity:.6"></div>
 ${snip('notes', 'Remember to renew the car licence before the end of the month.', '09:18')}${snip('docs', 'Agenda for Monday: budget review, hiring update, next release date.', '15:26')}</div>`;
const homeIcon = (g, shape, bg) => `<div style="width:56px;height:56px;${clip(shape)}background:${bg};display:grid;place-items:center;color:#fff">${ic(g, 'f s32')}</div>`;

BS['18-widget'] = () => mount(`${wallpaper()}
<div class="abs" style="top:66px;left:24px;color:var(--on-surface)"><div style="font:300 64px/66px var(--font);letter-spacing:-2px">9:41</div><div class="title-m" style="opacity:.8">Friday, 6 November</div></div>
${wg4rec(16, 168)}${wg4idle(16, 356)}
${wg2(16, 538)}${wg1(204, 538)}${wg1(298, 538, 'live')}
<div class="abs" style="left:16px;right:16px;top:640px;display:flex;justify-content:space-between;padding:0 4px">${homeIcon('photo_camera', 'cookie9', '#6a5af0')}${homeIcon('call', 'clover4', '#0f6a78')}${homeIcon('chat', 'puffy', '#1d7a4b')}${homeIcon('language', 'sunny', '#b3541e')}</div>
<div class="dock"><div class="glass" style="flex:1;height:56px;border-radius:28px;display:flex;align-items:center;padding:0 20px;gap:12px;font:500 15px/20px var(--font)">${ic('search')}<span style="opacity:.7">Search</span></div></div>`, {});

const tq = (cls, g, t, s) => `<div class="tile-q ${cls}"><div class="ci">${ic(g, 'f')}</div><div style="min-width:0"><div class="tt">${t}</div><div class="ts">${s}</div></div></div>`;
BS['18-tiles'] = () => mount(`${wallpaper()}<div class="abs" style="inset:0;background:${DARK ? 'rgba(6,6,12,.62)' : 'rgba(255,255,255,.55)'};"></div>
<div class="abs" style="top:56px;left:24px;right:24px;display:flex;justify-content:space-between;align-items:flex-end"><div><div style="font:400 44px/48px var(--font);letter-spacing:-1px">9:41</div><div class="body-m" style="opacity:.8">Fri, 6 Nov</div></div><div style="display:flex;gap:4px"><div class="ib surf">${ic('edit')}</div><div class="ib surf">${ic('settings')}</div></div></div>
<div class="abs" style="top:150px;left:16px;right:16px;display:flex;flex-direction:column;gap:8px">
 <div style="display:flex;gap:8px">${tq('on', 'wifi', 'Internet', 'Home')}${tq('', 'bluetooth', 'Bluetooth', 'Off')}</div>
 <div style="display:flex;gap:8px">${tq('', 'mic', 'RMBLR', 'Off')}${tq('on', 'mic', 'RMBLR', 'Orb on')}</div>
 <div style="display:flex;gap:8px">${tq('rec', 'graphic_eq', 'RMBLR', 'Recording 0:07')}${tq('', 'flashlight_on', 'Flashlight', 'Off')}</div>
 <div style="margin-top:12px;padding:6px 4px;border-radius:28px;background:var(--surface-container-high)"><div style="display:flex;align-items:center;gap:8px;padding:0 14px"><div style="flex:1">${slider(62)}</div>${ic('brightness_6', 's20')}</div></div>
</div>
<div class="abs" style="top:464px;left:0;right:0">${recNote()}</div>
<div class="abs" style="bottom:44px;left:0;right:0;display:flex;justify-content:center"><div style="width:36px;height:5px;border-radius:3px;background:var(--on-surface);opacity:.4"></div></div>`);

const ntf = (inner, ex = '') => `<div class="nt glass" style="${ex}">${inner}</div>`;
const nhdr = (t, glyph = 'graphic_eq') => `<div class="hdr"><div style="width:20px;height:20px;border-radius:50%;background:var(--brand);color:${brandFg()};display:grid;place-items:center">${ic(glyph, 'f').replace('<span class="i f"', '<span class="i f" style="font-size:13px;width:13px;height:13px"')}</div><span>RMBLR</span><span>·</span><span>${t}</span></div>`;
const recNote = () => ntf(`${nhdr('now')}<div style="display:flex;align-items:center;gap:12px"><div style="flex:1"><div class="ttl">Recording, 0:07</div><div class="txt">Writing into Messages</div></div><div style="width:48px;height:48px;border-radius:50%;background:var(--error);color:var(--on-error);display:grid;place-items:center">${ic('mic', 'f')}</div></div><div style="margin-top:10px;color:var(--error)">${wave(318, 28, 42, { seed: 9, color: 'currentColor' })}</div><div class="acts"><span class="ab stop">${ic('stop', 'f')}Stop</span><span class="ab">${ic('close')}Cancel</span></div>`);
const readyNote = () => ntf(`${nhdr('Orb is on')}<div class="ttl">Ready when you type</div><div class="txt">The orb shows up when a text field has focus.</div><div class="acts"><span class="ab">${ic('pause')}Pause orb</span><span class="ab">${ic('settings')}Settings</span></div>`);
const doneNote = () => ntf(`${nhdr('2 min')}<div style="display:flex;gap:12px;align-items:center"><div style="flex:1"><div class="ttl">Written into Messages</div><div class="txt">Sounds great, see you at seven.</div></div><div style="width:40px;height:40px;${clip('cookie9')}background:var(--success-container);color:var(--on-success-container);display:grid;place-items:center">${ic('check', 'b')}</div></div><div class="acts"><span class="ab">${ic('content_copy')}Copy</span><span class="ab">${ic('undo')}Undo</span></div>`);

BS['19-notification'] = () => mount(`${wallpaper()}<div class="abs" style="inset:0;background:${DARK ? 'rgba(6,6,12,.72)' : 'rgba(255,255,255,.5)'}"></div>
<div class="abs" style="top:52px;left:24px;right:24px;display:flex;justify-content:space-between;align-items:center"><div class="title-l">Notifications</div><div class="chip tonal">Clear all</div></div>
<div class="abs" style="top:116px;left:0;right:0">${recNote()}${readyNote()}${doneNote()}</div>`);

BS['19-lockscreen'] = () => mount(`${wallpaper()}<div class="abs" style="inset:0;background:${DARK ? 'rgba(6,6,12,.35)' : 'rgba(255,255,255,.15)'}"></div>
<div class="abs" style="top:64px;left:0;right:0;text-align:center"><div style="font:300 96px/92px var(--font);letter-spacing:-4px">9:41</div><div class="title-m" style="opacity:.85;margin-top:6px">Friday, 6 November</div></div>
<div class="abs" style="top:290px;left:0;right:0">${recNote()}${ntf(`${nhdr('Orb is on')}<div style="display:flex;align-items:center;gap:12px"><div style="flex:1"><div class="ttl">Ready when you type</div></div><span class="ab" style="height:34px;padding:0 14px;border-radius:17px;background:rgba(127,127,127,.18);font:600 13px/34px var(--font)">Pause</span></div>`)}</div>
<div class="abs" style="bottom:56px;left:40px;right:40px;display:flex;justify-content:space-between"><div class="ib surf" style="width:56px;height:56px">${ic('phone')}</div><div class="ib surf" style="width:56px;height:56px">${ic('photo_camera')}</div></div>`);

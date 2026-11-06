# Motion and behaviour notes (kept off the screens)

| screen | note |
|---|---|
| 01-home | Record button idles with a slow breathing scale (1.0 to 1.03, 3 s) and the cookie halos rotate a few degrees. Press morphs the cookie toward the rounded-square stop shape (spatial spring, damping .8, stiffness 380). Nav selection expands the label with a spring. |
| 02-recording | Waveform bars follow the live input level, newest on the right, older bars scroll left and fade. Red halos pulse outward on each syllable peak. Settled words commit from the lighter guess tone to solid ink with a 120 ms fade; the caret blinks softly. Timer ticks with tabular digits. |
| 03-result | Cleaned text fades in over the raw transcript. Choosing a tone chip shows a wavy progress line on the card while it rewrites, then cross-fades the text. The snackbar slides up and dismisses after 4 s. |
| 04-history | Swiping a row left reveals the red delete action; passing 60% of the width commits and the row collapses with the list closing the gap. Filter selection morphs a small-radius button into a full pill. |
| 05-history-detail | Original and rewritten cards share the same enter transition from the list item (container transform). |
| 06-tone-sheet | Sheet slides up with an effects spring and a drag handle; the selected row morphs from a rounded square to a full pill. |
| 07a/b/c orb | Idle: orb sits at the screen edge and fades to low opacity after a few seconds. Press: orb grows 1.18x, scrim fades in, tone pills fan out along the arc one after the other (30 ms stagger); the pill under the finger fills with the brand colour and the orb gives a short haptic tick. Recording: orb turns red and the caption bubble grows from the orb; guess words stay light until settled. |
| 08-language-sheet | Turning on Auto-detect keeps the list usable as hints; selected languages become removable chips that animate in at the top of the list. |
| 09-delete-confirm-sheet | Delete is the only red element; pressing it closes the sheet and the row leaves the list, with an Undo snackbar for a few seconds. |

cd ~/repos/rmblr-mocks-b
git add design/m3-mocks && git commit -q -m "Design mocks: RMBLR screens 10 to 19 (settings, keys, profiles, onboarding, permissions, update, feedback, widget, tiles, notifications)" && git pull -q --rebase origin design/m3-mocks && git push -q origin design/m3-mocks; git log --oneline | head -4; git config user.name; ls design/m3-mocks design/m3-mocks/png | head -90

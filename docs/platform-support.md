# Platform support

One row per feature. "Verified" means run and observed on that platform, not just compiled.

| Feature | Android | iOS |
|---|---|---|
| Bubble: draggable, swipe-to-edge dismiss | Verified | Not implemented |
| Shake-to-recall | Verified | Not implemented — no accelerometer listener yet (`IosShakeListener` is a stub) |
| Panel shell (tab navigation) | Verified (empty tab bodies) | Not implemented — no host set up yet |
| Overlay lives above native screens/sheets | Not decided — see `docs/architecture.md` decision 1 | Not decided — same |
| Network inspector | Not implemented | Not implemented |
| Database inspector | Not implemented | Not implemented |
| Storage (DataStore/prefs) viewer | Not implemented | Not implemented |
| Logs viewer | Not implemented | Not implemented |
| Crash capture | Not implemented | Not implemented |
| Feature-flag overrides | Not implemented | Not implemented |
| No-op / release-safe artifact | Not implemented | Not implemented |

# Changelog

All notable, consumer-observable changes to Heimdall are recorded here. See
`.github/instructions/docs.instructions.md` for what belongs here vs. in the design docs.

## Unreleased

### Added
- `heimdall-core`: `ShakeDetector` (shared threshold/debounce logic) and `AndroidShakeListener`
  (Android accelerometer wiring). iOS listener is a stub — see `docs/TODO.md`.
- `heimdall-core`: `OverlayState` — visibility (visible / dismissed-to-edge) and drag offset.
- `heimdall-ui`: `HeimdallOverlay` composable — wrap a screen's root content once to get the
  draggable bubble and, on tap, the panel. `HeimdallOverlayController.recall()` reopens a
  swiped-away bubble.
- `heimdall-ui`: `HeimdallPanel` — tab shell for Network / Database / Storage / Logs / Flags.
  All tab bodies are placeholders; no collector is implemented yet.
- `sample/androidApp`: reference integration wiring `HeimdallOverlay` + `AndroidShakeListener`
  into an Activity.

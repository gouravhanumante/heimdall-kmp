# Overlay

Implemented in `heimdall-ui` (`HeimdallOverlay`, `HeimdallBubbleLayer`, `HeimdallPanel`). Compiles;
**not yet run on a device** (see `docs/platform-support.md`).

## Bubble

- Starts at the right edge, 70% down the screen. Drag it anywhere; it is kept inside the screen.
- **Tap** opens the panel. The bubble is hidden while the panel is open.
- **Drag to hide**: while dragging, a "Drop to hide · shake to bring back" target appears at the
  bottom. Dropping the bubble with its centre inside the bottom 140dp (`HideZoneHeight`) hides it.
- The bubble remembers where it was **before** that drag, so a recall brings it back there, not
  into the hide zone. It can be hidden and recalled any number of times.
- Touches anywhere outside the bubble reach the app underneath.

## Shake-to-recall

- `ShakeDetector` (in `heimdall-core`) takes per-axis G-force samples and calls `onShake()` when
  the magnitude exceeds `DEFAULT_THRESHOLD_G = 2.7g` above resting gravity, at most once per
  `DEFAULT_MIN_INTERVAL_MS = 1000ms`.
- `AndroidShakeListener` feeds it from `Sensor.TYPE_ACCELEROMETER`. iOS has no listener yet.
- The host calls `HeimdallOverlayController.recall()` from the shake callback.

## Panel

- Full screen, with a "Heimdall" header and a **Close** button. It blocks touches, so nothing
  underneath is pressed by mistake.
- Tabs: Network / Database / Storage / Logs / Flags — placeholder bodies until the UI chunk.
- The system back button does not close it yet (see `docs/TODO.md`).

# Overlay

Everything in this doc is implemented in `heimdall-ui`'s `HeimdallBubble`/`HeimdallPanel`/
`HeimdallOverlay`. Implemented for Android but not yet run on a device (see `docs/platform-support.md`).

## Bubble

- Circular, draggable anywhere on screen via `HeimdallBubble`'s `pointerInput` drag handling.
- Dragging it within `dismissEdgeThresholdDp` (default 24dp) of the left or top edge on release
  dismisses it — `onDismissedToEdge()` fires and `HeimdallOverlay` stops drawing the bubble.
  Only the left/top edges are wired today; right/bottom are not (see `docs/TODO.md`).
- Tapping it (when not dismissed) opens `HeimdallPanel` full-screen.

## Shake-to-recall

- `ShakeDetector` (in `heimdall-core`) takes raw per-axis G-force samples and calls `onShake()`
  when the combined magnitude exceeds `DEFAULT_THRESHOLD_G = 2.7g` above resting gravity, at most
  once per `DEFAULT_MIN_INTERVAL_MS = 1000ms`.
- `AndroidShakeListener` feeds it from `Sensor.TYPE_ACCELEROMETER` at
  `SensorManager.SENSOR_DELAY_NORMAL`.
- The Activity calls `HeimdallOverlayController.recall()` from the shake callback, which resets
  the bubble's dismissed state — it reappears at the position it was dismissed from (position is
  not currently reset or persisted, see `docs/TODO.md`).

## Panel

- Tab shell only: Network / Database / Storage / Logs / Flags tabs, each currently a
  placeholder body. No collector reports data into any of them yet.

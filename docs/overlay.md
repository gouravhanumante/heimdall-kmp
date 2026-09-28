# Overlay

Implemented in `heimdall-ui` (`HeimdallOverlay`, `HeimdallBubbleLayer`, `HeimdallPanel`). Compiles;
**not yet run on a device** (see `docs/platform-support.md`).

## Bubble

- Starts docked to the right edge, 70% down the screen. It follows the finger while dragged and,
  on release, springs to the nearest left/right edge, inset 8dp (`BubbleEdgeInset`).
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

- A centered, bounded floating inspector window: 92% of the screen width (max 420dp) and 72% of
  the screen height (max 620dp), with rounded corners, elevation, and a **Close** button. The app remains
  visible around it; touches inside the inspector are consumed.
- An icon-only navigation rail selects Overview / Timeline / Network / Database / Storage / Logs /
  Flags. The selected section name appears in the header, and each icon has an accessibility label.
- The header shows the current screen when the app provides one, otherwise `Global`.
- Overview shows current-session counts and slow measured operations; Timeline shows app-reported
  events, performance measurements, and optional screen attribution.
  Network, storage, database metadata, logs, crashes, and registered flags render published live
  data; empty sections show an empty state.
- Android hosts can call `HeimdallOverlayController.handleBack()` from their back dispatcher; it
  returns `true` when it closes the inspector and `false` when the app should handle navigation.

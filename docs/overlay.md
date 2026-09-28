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
- Its resting position (after a successful drag to an edge) is saved as a fraction of the screen
  through `Heimdall.bubblePosition`, and restored on the next `Heimdall.install(...)` — so it
  survives an app restart, not just hide/show within one run. A bubble that's never been dragged
  still uses the proportional default so it adapts to rotation/screen size; once restored from a
  past run, it's fixed in pixels for that composition, same as any other in-run drag.
- Touches anywhere outside the bubble reach the app underneath.

## Shake-to-recall

- `ShakeDetector` (in `heimdall-core`) takes per-axis G-force samples and calls `onShake()` when
  the magnitude exceeds `DEFAULT_THRESHOLD_G = 2.7g` above resting gravity, at most once per
  `DEFAULT_MIN_INTERVAL_MS = 1000ms`.
- `AndroidShakeListener` feeds it from `Sensor.TYPE_ACCELEROMETER`. `IosShakeListener` is fed by
  the host view controller's `motionEnded` (see `ShakeHostViewController` in the iOS sample).
- The host calls `HeimdallOverlayController.recall()` from the shake callback.

## Where the overlay lives

- Default, both platforms: wrap the app's root composable in `HeimdallOverlay { ... }`.
- Limitation: anything that opens its own window — Compose `Dialog`, `ModalBottomSheet`, `Popup`,
  or native screens outside the wrapped composable — draws over the bubble.
- iOS opt-in: the sample's `MainViewControllerWithNativeOverlayWindow()` puts the overlay in a
  second `UIWindow` above alerts (`UIWindowLevelAlert + 1`) and passes touches outside the
  bubble/panel through (`HeimdallOverlayController.acceptsOverlayTouch`). Sample-only, not run.

## Panel

- A centered, bounded floating inspector window: 94% of the available width and 90% of the
  available height, capped at 560dp by 760dp on larger screens. It has rounded corners, elevation,
  and a **Close** button. The app remains
  visible around it; touches inside the inspector are consumed.
- An icon-only navigation rail selects Overview / Network / Database / Storage / Logs / Flags. The
  selected section name appears in the header, and each icon has an accessibility label.
- The header shows the current screen when the app provides one, otherwise `Global`.
- The floating bubble and inspector header use the supplied Heimdall horn artwork rasterized as a
  PNG Compose resource. Android runtime uses the PNG because Compose Android does not decode SVG
  resources directly.
- The inspector shell follows the device light/dark setting through `HeimdallDesign.palette()`;
  code/data surfaces keep their structured developer-tool contrast in either mode.
- Overview shows current-session counts, recent activity, and slow measured operations. Timeline
  events remain available as activity data rather than a primary navigation destination.
- Overview also shows a Health table containing only currently measured crashes, network errors,
  and explicit slow operations. Compose recomposition, frame-jank, and component-health metrics are
  not shown until their instrumentation exists.
- Flags support search, boolean toggles, text/number override editors, global reset, and per-flag
  reset.
- On Android, the sample also reports live frame intervals from `Choreographer`; slow-frame counts
  and the worst measured interval appear in Overview and Health. No frame history is persisted.
- Android crash capture and frame monitoring are installed by `Heimdall.install(...)`; the sample
  only supplies the platform shake listener needed to recall the UI bubble.
- Network details render request and response headers/bodies in selectable monospace code blocks,
  with a copy-cURL action.
- Database first shows a table list, then drills into one selected table with column headers,
  horizontally scrollable cells, and per-table search. Storage renders a key/value table; Logs and
  Crashes render contained severity records. Collection views provide local search where the data
  set can grow large.
- Sessions lists current and previous runs. Selecting an older session switches Network, Logs,
  Crashes, and Timeline to historical data; Database, Storage, and Flags remain live-only.
- Shared inspector tokens live in `HeimdallDesign`: panel/rail/surface colors, status colors,
  corner radii, code typography, and label sizes are reused across these views.
  Network, storage, database metadata, logs, crashes, and registered flags render published live
  data; empty sections show an empty state.
- Android hosts can call `HeimdallOverlayController.handleBack()` from their back dispatcher; it
  returns `true` when it closes the inspector and `false` when the app should handle navigation.

# TODO / known gaps

Deliberately deferred work, per `.github/instructions/docs.instructions.md`.

- **iOS shake + overlay entirely unimplemented.** `IosShakeListener` is a stub. Blocked on
  deciding where the overlay lives (`docs/architecture.md` decision 1) before wiring
  `motionEnded`/`UIWindow`, so the two land together instead of half-wiring shake with nowhere
  for it to bring the bubble back to.
- **Bubble only dismisses off the left/top edges.** Right/bottom edge detection isn't wired —
  low cost to add, just not done in milestone 1.
- **Dismissed bubble position is not persisted or reset.** Recall brings it back wherever it was
  when dismissed; there's no "reset to default corner" behaviour yet.
- **No no-op / release-safety mechanism at all.** See `docs/release-builds.md`. This must exist
  before any consumer is told it's safe to ship with Heimdall in the dependency graph.
- **Storage, database, flags-UI, logs/crash-capture collectors unbuilt.** `FlagStore`'s
  override-vs-delegate logic exists in `heimdall-core`, but nothing calls it from a real flag
  provider yet, there's no persistence for overrides, and there's no panel screen for any of
  these five areas. Each needs its own design-before-implementing pass per `docs.instructions.md`.
- **Overlay-window prototype (decision 2 in `docs/architecture.md`) not attempted.** Currently
  running on "wrap the root composable", which has the known limitation that native
  sheets/screens outside that composable hide the bubble.
- **`Heimdall.install()` must be called before anything records, and nothing enforces this at
  the call site.** `HeimdallDatabase.requireConnection()` throws at first use if it wasn't, which
  only surfaces the mistake at runtime, not at compile time.

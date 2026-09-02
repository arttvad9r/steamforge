# Steamforge frame timing benchmark

This module measures the production `BoardView` animation path with AndroidX Macrobenchmark.

The benchmark-only `BenchmarkBoardActivity` starts from a deterministic 4x4 board containing two
mergeable pairs per row. One right swipe therefore exercises eight merge events, movement ghosts,
merge pop animations, and a spawn in one burst.

## Run

Use a physical Android 12+ device:

```bash
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

The target `benchmark` app variant is minified and shrunk from the release configuration, signed
with the debug key for local/CI installation, and is profileable only through the benchmark manifest.

`FrameTimingMetric` reports `frameDurationCpuMs` and `frameOverrunMs` percentiles. Positive
`frameOverrunMs` values indicate frames that missed their deadline.

Hosted emulator timings are intentionally not used as a pass/fail performance SLA because emulator
results depend on host load and are not representative of end-user devices. CI only verifies that
the release-like benchmark target and Macrobenchmark test APK compile.

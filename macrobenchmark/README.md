# Steamforge frame timing benchmark

This module measures the production `BoardView` animation path with AndroidX Macrobenchmark.

The benchmark-only `BenchmarkBoardActivity` starts from a deterministic 4x4 board containing two
mergeable pairs per row. One right swipe therefore exercises eight merge events, movement ghosts,
merge pop animations, and a spawn in one burst.

## Physical-device acceptance run

Use a physical Android 12+ device:

```bash
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

The target `benchmark` app variant is minified and shrunk from the release configuration, signed
with the debug key for local/CI installation, and is profileable only through the benchmark manifest.

`FrameTimingMetric` reports `frameDurationCpuMs` and `frameOverrunMs` percentiles. Positive
`frameOverrunMs` values indicate frames that missed their deadline.

## CI emulator diagnostic

`Frame Timing Diagnostic Smoke` also executes the same release-like Macrobenchmark on an Android 16
hosted emulator. The workflow suppresses only the Benchmark library's `EMULATOR` environment error
and uploads the benchmark result files so the production benchmark path itself is continuously
exercised.

Hosted emulator timings are intentionally **not** a pass/fail performance SLA because results depend
on host load and are not representative of end-user devices. A green emulator diagnostic means the
benchmark installed, launched the benchmark-only host, executed the dense merge burst and emitted
Macrobenchmark output; it does not close the physical-device Gate A performance requirement.

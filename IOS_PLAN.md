# iOS Support — Future Implementation Plan

> **Status: DEFERRED.** This is a planning document. No iOS code has been written yet.
> The library is Android-only as of v1.2.0. This document captures the design decision
> and the full implementation plan so the work can be picked up later.

## The Decision: Option A (in-app capture)

Port the library to iOS using **ReplayKit `RPScreenRecorder.startCapture`**.

- **Scope:** captures the content of **your own app only** (foreground-only).
- **Why:** iOS has no public API to programmatically capture other apps, and no
  background screen capture. The only system-wide mechanism is a ReplayKit
  Broadcast Upload Extension, started manually by the user from Control Center —
  a huge UX and maintenance cost, deferred to "Option C" (not planned).

### Platform feature matrix (target state)

| Feature | Android | iOS |
|---|---|---|
| Interval capture | ✅ | ✅ |
| Change-detection capture | ✅ | ✅ |
| Image format / quality | ✅ | ✅ |
| Resolution scaling (`scaleResolution`) | ✅ | ✅ |
| Custom capture region | ✅ | ✅ |
| Pause / resume / stop / status | ✅ | ✅ |
| Text overlays | ✅ | ✅ (CGContext) |
| Image overlays | ✅ | ✅ |
| Frame events + all event parity | ✅ | ✅ |
| Storage `location: 'private'` | ✅ | ✅ (app sandbox) |
| Storage warning threshold | ✅ | ✅ |
| `autoStopTimeout` | ✅ | ✅ (`DispatchWorkItem`) |
| **Background capture** | ✅ (foreground service) | ❌ |
| **Capture other apps / whole system** | ✅ | ❌ |
| **Persistent notification controls** | ✅ | ❌ |
| Storage `location: 'public'` | ✅ | ❌ |
| `excludeStatusBar` | ✅ | ❌ (ReplayKit excludes it itself when using in-app window capture) |
| `requestPermission()`/`checkPermission() == 'granted'` | ✅ | ❌ (ReplayKit prompts on **every** capture start; no persistent grant) |

Android-only options must **reject with `NOT_SUPPORTED`/`INVALID_OPTIONS`** on iOS
rather than silently ignoring them. Add a Platform translation table to the README
and `docs/` as part of this work.

### `ios/` module layout

```
ios/
  FrameCapture.mm          # TurboModule glue, already exists (scaffolded by create-react-native-library)
  CaptureManager.swift     # RPScreenRecorder.startCapture + interval throttle + state machine
  BitmapProcessor.swift    # CMSampleBuffer -> CGImage -> region crop -> scale -> JPEG/PNG encode
  OverlayRenderer.swift    # CGContext text + image drawing (mirrors Android OverlayRenderer)
  StorageManager.swift     # sandbox dir, temp vs persistent, storage warnings, cleanup
  FrameCaptureError.swift  # error codes mapping to JS CaptureErrorCode enum
```

Key iOS facts verified:

- `RPScreenRecorder.shared().startCapture { sampleBuffer, type, error in ... }` delivers
  ~60 fps `CVPixelBuffer`. Throttle to your `capture.interval` inside the handler.
- Use a single Metal-backed `CIContext` for pixel→CGImage conversion.
- `CGImageDestination` handles `format`/`quality` (`kCGImageDestinationLossyCompressionQuality`).
- `RPScreenRecorder.shared().isRecording`/`currentCaptureStartedTime` map to `CaptureStatus`.
- Auto-stop: `DispatchWorkItem` scheduled for `autoStopTimeout` ms after start, cancelled on stop.
- Minimum deployment target iOS 12, raise to 13 if we need newer APIs.

### Release plan when implemented

- `CHANGELOG.md`, README Platform Support table, new `docs/ios.md`
- Bump from **1.2.0 → 1.3.0** (minor — new platform, backward-compatible for Android)
- Example app: add `example/ios` Podfile integration, run on simulator + one physical device
- Simulator ReplayKit works for sanity checks, but verify timing/state on a real device

### Explicit non-goals (documented so it's clear)

- **Option B:** stay Android-only (rejected — in-app capture is valuable).
- **Option C:** Broadcast Upload Extension for system-wide capture (deferred; do only if
  users request it — it's a separate extension target + App Group IPC + Requires Data Share).
- **macOS** (`react-native-macos`): ScreenCaptureKit would be the right approach, but the
  user has explicitly scoped this out.

---

*This plan captures the sneering research and decision of 2026-08-04. Revisit when there's
an active iOS user request before implementing.*

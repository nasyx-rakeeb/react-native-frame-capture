# Usage Examples

Practical examples for common use cases. All examples assume:

```typescript
import {
  requestPermission,
  startCapture,
  stopCapture,
  addListener,
  CaptureEventType,
} from 'react-native-frame-capture';
```

> **Always call `await requestPermission()` before `startCapture`**, otherwise start rejects with `PERMISSION_DENIED`.

## Basic Capture

Capture one frame per second and stop it manually.

```typescript
import { useEffect, useState } from 'react';

export default function ScreenRecorder() {
  const [frames, setFrames] = useState<string[]>([]);

  useEffect(() => {
    const sub = addListener(
      CaptureEventType.FRAME_CAPTURED,
      (event) => {
        setFrames((prev) => [...prev, event.filePath]);
      }
    );
    return () => sub.remove(); // always remove listeners on unmount
  }, []);

  const start = async () => {
    await requestPermission();

    await startCapture({
      capture: { interval: 1000 },
      image: { quality: 80, format: 'jpeg' },
      storage: { saveFrames: true, location: 'private' },
    });
  };

  const stop = async () => {
    await stopCapture();
    console.log(`Saved ${frames.length} frames`);
  };

  // ... UI buttons calling start() / stop()
}
```

## Auto-Stop After a Duration

Capture for exactly 30 seconds and let native code stop it — works even when the app is in the background (JS timers don't). Use the `reason` field to distinguish a timeout stop from a manual one.

```typescript
useEffect(() => {
  const stopSub = addListener(
    CaptureEventType.CAPTURE_STOP,
    (event) => {
      if (event.reason === 'auto_stop_timeout') {
        console.log('Capture auto-stopped');
      } else {
        console.log('Capture stopped manually');
      }
    }
  );
  return () => stopSub.remove();
}, []);

const start = async () => {
  await requestPermission();

  await startCapture({
    capture: {
      interval: 1000,
      autoStopTimeout: 30_000, // stop 30s after capture starts
    },
    image: { quality: 80, format: 'jpeg' },
    storage: { saveFrames: true, location: 'private' },
  });

  // optional: release the app before the timeout fires; the native
  // foreground service keeps capturing and stops itself on schedule
};
```

## Change Detection Capture

Capture only when screen content changes (with the same 30s auto-stop).

```typescript
useEffect(() => {
  // Optional: monitor change detection for debugging
  const changeSub = addListener(
    CaptureEventType.CHANGE_DETECTED,
    (event) => {
      console.log(
        `Change: ${event.changePercent.toFixed(1)}%, captured: ${event.captured}`
      );
    }
  );
  return () => changeSub.remove();
}, []);

await startCapture({
  capture: {
    mode: 'change-detection',
    changeDetection: {
      threshold: 15, // Capture when 15% of screen changes
      minInterval: 500, // Poll every 500ms
      maxInterval: 5000, // Force capture at least every 5s
    },
    autoStopTimeout: 30_000,
  },
  image: { quality: 80, format: 'jpeg' },
  storage: { saveFrames: true, location: 'private' },
});
```

## Capture with Text Overlay

```typescript
await startCapture({
  capture: { interval: 1000 },
  image: { quality: 80, format: 'jpeg' },
  overlays: [
    {
      type: 'text',
      content: 'Frame {frameNumber} • {timestamp}',
      position: 'bottom-right',
      style: {
        fontSize: 14,
        color: '#FFFFFF',
        backgroundColor: '#00000099',
        padding: 8,
        fontWeight: 'bold',
      },
    },
  ],
});
```

## Capture with Image Watermark

```typescript
await startCapture({
  capture: { interval: 1000 },
  image: { quality: 80, format: 'jpeg' },
  overlays: [
    {
      type: 'image',
      source: 'logo', // Drawable resource name
      position: 'top-right',
      size: { width: 50, height: 50 },
      opacity: 0.8,
    },
  ],
});
```

## Capture Custom Region

```typescript
await startCapture({
  capture: { interval: 1000 },
  image: {
    quality: 80,
    format: 'jpeg',
    region: {
      x: 0.1, // 10% from left
      y: 0.1, // 10% from top
      width: 0.8, // 80% width
      height: 0.8, // 80% height
      unit: 'percentage',
    },
  },
});
```

## Capture with Resolution Scaling

```typescript
await startCapture({
  capture: { interval: 1000 },
  image: {
    quality: 80,
    format: 'jpeg',
    scaleResolution: 0.5, // 50% resolution (75% smaller files)
  },
});
```

## Custom Notification

```typescript
await startCapture({
  capture: { interval: 1000 },
  image: { quality: 80, format: 'jpeg' },
  notification: {
    title: 'Recording Screen',
    description: 'Captured {frameCount} frames',
    color: '#FF5722',
    showFrameCount: true,
    updateInterval: 10,
    showPauseAction: true,
    showStopAction: true,
  },
});
```

## Handle Errors

```typescript
useEffect(() => {
  const errorSub = addListener(
    CaptureEventType.CAPTURE_ERROR,
    (event) => {
      console.error(`Capture error [${event.code}]: ${event.message}`);
    }
  );

  const stopSub = addListener(
    CaptureEventType.CAPTURE_STOP,
    (event) => {
      console.log(
        `Stopped after ${event.totalFrames} frames (${event.reason ?? 'manual'})`
      );
    }
  );

  return () => {
    errorSub.remove();
    stopSub.remove();
  };
}, []);
```

## Cleanup Temporary Frames

Frames captured with `saveFrames: false` are stored temporarily. Clean them up when you're done:

```typescript
import { cleanupTempFrames } from 'react-native-frame-capture';

await cleanupTempFrames();
```

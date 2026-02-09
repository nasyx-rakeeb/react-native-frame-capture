# Usage Examples

Practical examples for common use cases.

## Basic Capture

```typescript
await FrameCapture.startCapture({
  capture: { interval: 1000 },
  image: { quality: 80, format: 'jpeg' },
  storage: { saveFrames: true },
});
```

## Change Detection Capture

Capture frames only when screen content changes:

```typescript
await FrameCapture.startCapture({
  capture: {
    mode: 'change-detection',
    changeDetection: {
      threshold: 15, // Capture when 15% of screen changes
      minInterval: 500, // Poll every 500ms
      maxInterval: 5000, // Force capture at least every 5s
    },
  },
  image: { quality: 80, format: 'jpeg' },
  storage: { saveFrames: true },
});

// Optional: Monitor change detection for debugging
FrameCapture.addListener(
  FrameCapture.CaptureEventType.CHANGE_DETECTED,
  (event) => {
    console.log(
      `Change: ${event.changePercent.toFixed(1)}%, captured: ${event.captured}`
    );
  }
);
```

## Capture with Text Overlay

```typescript
await FrameCapture.startCapture({
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
await FrameCapture.startCapture({
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
await FrameCapture.startCapture({
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
await FrameCapture.startCapture({
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
await FrameCapture.startCapture({
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

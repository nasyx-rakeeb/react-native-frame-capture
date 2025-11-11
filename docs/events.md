# Events

Complete guide to handling events in React Native Frame Capture.

## Event Types

```typescript
enum CaptureEventType {
  FRAME_CAPTURED = 'onFrameCaptured',
  CAPTURE_ERROR = 'onCaptureError',
  CAPTURE_STOP = 'onCaptureStop',
  CAPTURE_START = 'onCaptureStart',
  STORAGE_WARNING = 'onStorageWarning',
  CAPTURE_PAUSE = 'onCapturePause',
  CAPTURE_RESUME = 'onCaptureResume',
  OVERLAY_ERROR = 'onOverlayError',
}
```

## Event Payloads

### FrameCapturedEvent

```typescript
interface FrameCapturedEvent {
  filePath: string; // Path to captured frame
  timestamp: number; // Capture timestamp
  frameNumber: number; // Frame number in session
  fileSize: number; // File size in bytes
}
```

### CaptureErrorEvent

```typescript
interface CaptureErrorEvent {
  code: string; // Error code
  message: string; // Error message
  details?: Object; // Additional error details
}
```

### CaptureStopEvent

```typescript
interface CaptureStopEvent {
  sessionId: string; // Session ID
  totalFrames: number; // Total frames captured
  duration: number; // Session duration in milliseconds
}
```

### StorageWarningEvent

```typescript
interface StorageWarningEvent {
  availableSpace: number; // Available storage in bytes
  threshold: number; // Warning threshold in bytes
}
```

## Error Codes

```typescript
enum CaptureErrorCode {
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  ALREADY_CAPTURING = 'ALREADY_CAPTURING',
  INVALID_OPTIONS = 'INVALID_OPTIONS',
  STORAGE_ERROR = 'STORAGE_ERROR',
  SYSTEM_ERROR = 'SYSTEM_ERROR',
  NOT_SUPPORTED = 'NOT_SUPPORTED',
}
```

## Event Handling Example

```typescript
// Listen for all events
const subscriptions = [
  FrameCapture.addListener(
    FrameCapture.CaptureEventType.FRAME_CAPTURED,
    (event) => {
      console.log(`Frame ${event.frameNumber}: ${event.filePath}`);
    }
  ),
  FrameCapture.addListener(
    FrameCapture.CaptureEventType.CAPTURE_ERROR,
    (event) => {
      console.error(`Error: ${event.message}`);
    }
  ),
  FrameCapture.addListener(
    FrameCapture.CaptureEventType.CAPTURE_STOP,
    (event) => {
      console.log(
        `Stopped: ${event.totalFrames} frames in ${event.duration}ms`
      );
    }
  ),
  FrameCapture.addListener(
    FrameCapture.CaptureEventType.STORAGE_WARNING,
    (event) => {
      console.warn(`Low storage: ${event.availableSpace} bytes available`);
    }
  ),
];

// Cleanup
subscriptions.forEach((sub) => sub.remove());
```

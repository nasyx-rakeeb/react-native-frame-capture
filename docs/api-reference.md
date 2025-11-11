# API Reference

## Permission Management

### `requestPermission()`

Requests **MediaProjection permission** (screen capture/screen sharing) from the user. This opens the Android system dialog asking the user to allow screen recording.

```typescript
const status = await FrameCapture.requestPermission();
```

**Permission:** `android.permission.SYSTEM_ALERT_WINDOW` (MediaProjection API)

**Returns:** `Promise<PermissionStatus>`

**Note:** This is a runtime permission that must be granted before starting capture. The permission dialog is shown by the Android system, not your app.

### `checkPermission()`

Checks if MediaProjection permission (screen capture) has been previously granted without showing the permission dialog.

```typescript
const status = await FrameCapture.checkPermission();
```

**Returns:** `Promise<PermissionStatus>`

**Note:** Returns `NOT_DETERMINED` if permission was never requested, `GRANTED` if previously granted. MediaProjection permission cannot be checked programmatically on Android, so this only verifies if permission data exists from a previous grant.

### `checkNotificationPermission()`

Checks if notification permission is granted (Android 13+).

```typescript
const status = await FrameCapture.checkNotificationPermission();
```

**Returns:** `Promise<PermissionStatus>`

## Capture Control

### `startCapture(options)`

Starts screen capture with the specified options.

```typescript
const session = await FrameCapture.startCapture({
  capture: {
    interval: 1000,
  },
  image: {
    quality: 80,
    format: 'jpeg',
  },
});
```

**Parameters:**

- `options` (optional): `Partial<CaptureOptions>` - Capture configuration

**Returns:** `Promise<CaptureSession>`

**Throws:** `CaptureError` if capture cannot be started

### `stopCapture()`

Stops the active capture session.

```typescript
await FrameCapture.stopCapture();
```

**Returns:** `Promise<void>`

### `pauseCapture()`

Pauses the active capture session.

```typescript
await FrameCapture.pauseCapture();
```

**Returns:** `Promise<void>`

### `resumeCapture()`

Resumes a paused capture session.

```typescript
await FrameCapture.resumeCapture();
```

**Returns:** `Promise<void>`

### `getCaptureStatus()`

Gets the current capture status.

```typescript
const status = await FrameCapture.getCaptureStatus();
console.log(status.state); // 'idle' | 'capturing' | 'paused'
```

**Returns:** `Promise<CaptureStatus>`

## Utility Functions

### `cleanupTempFrames()`

Manually cleans up all temporary frame files stored in the app's cache directory.

```typescript
await FrameCapture.cleanupTempFrames();
```

**Returns:** `Promise<void>`

**What it does:**

- Deletes all frames from the cache directory (`/data/data/[package]/cache/captured_frames/`)
- Only affects temporary frames (when `saveFrames: false`)
- Automatically called on app startup
- Useful for freeing up storage space after processing frames

**When to use:**

- After uploading frames to a server
- After processing frames in your app
- When you want to free up cache storage manually

### `addListener(eventType, callback)`

Adds an event listener for capture events.

```typescript
const subscription = FrameCapture.addListener(
  FrameCapture.CaptureEventType.FRAME_CAPTURED,
  (event) => {
    console.log('Frame:', event.filePath);
  }
);

// Remove listener when done
subscription.remove();
```

**Parameters:**

- `eventType`: `CaptureEventType` - Event type to listen for
- `callback`: `CaptureEventCallback` - Function to call when event is emitted

**Returns:** `EventSubscription`

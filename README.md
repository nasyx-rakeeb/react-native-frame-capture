# React Native Frame Capture

[![npm version](https://img.shields.io/npm/v/react-native-frame-capture.svg)](https://www.npmjs.com/package/react-native-frame-capture)
[![npm downloads](https://img.shields.io/npm/dm/react-native-frame-capture.svg)](https://www.npmjs.com/package/react-native-frame-capture)
[![license](https://img.shields.io/npm/l/react-native-frame-capture.svg)](https://github.com/nasyx-rakeeb/react-native-frame-capture/blob/main/LICENSE)
[![platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)

Reliable screen capture for React Native Android. Capture frames at intervals with customizable overlays and storage options.

## ✨ Features

- 📸 **Interval-based capture** - Capture frames at configurable intervals (100ms - 60s)
- 🎨 **Customizable overlays** - Add text and image overlays with template variables
- 💾 **Flexible storage** - Save to app-specific, public, or custom directories
- 🔄 **Background capture** - Continues capturing when app is minimized (foreground service)
- ⚡ **High performance** - Built with Kotlin and TurboModule architecture
- 🎯 **Precise control** - Pause, resume, and stop capture on demand
- 📊 **Real-time events** - Get notified for every captured frame
- 🔧 **Highly configurable** - Image quality, format, resolution scaling, and more
- 📱 **Expo compatible** - Works with Expo through config plugin
- 🎭 **Custom regions** - Capture specific screen areas
- 🚫 **Status bar exclusion** - Optionally exclude status bar from captures

## � How Ite Works

React Native Frame Capture uses Android's **MediaProjection API** to capture screen content at regular intervals. Here's the flow:

1. **Request Permission** - User grants screen capture permission via system dialog
2. **Start Foreground Service** - Ensures capture continues in background
3. **Create Virtual Display** - Mirrors screen content to an ImageReader
4. **Capture Frames** - Grabs frames at your specified interval (e.g., every 1 second)
5. **Process & Save** - Converts to bitmap, applies overlays, saves to storage
6. **Emit Events** - Notifies your app with frame info (path, size, timestamp)

**Key Components:**

- **MediaProjection** - Android API for screen capture (no root required)
- **Foreground Service** - Keeps capture running when app is minimized
- **ImageReader** - Efficiently captures screen pixels
- **TurboModule** - Fast native-to-JS communication

**Why Foreground Service?** Android kills background processes aggressively. The foreground service (with notification) ensures reliable capture even when your app isn't visible.

## 📋 Requirements

- React Native >= 0.74
- Android minSdkVersion >= 21 (Android 5.0)
- Android compileSdkVersion >= 34

## 📦 Installation

```bash
npm install react-native-frame-capture
```

or

```bash
yarn add react-native-frame-capture
```

### Expo

Add the config plugin to your `app.json` or `app.config.js`:

```json
{
  "expo": {
    "plugins": ["react-native-frame-capture"]
  }
}
```

Then rebuild your app:

```bash
npx expo prebuild
npx expo run:android
```

## 🚀 Quick Start

```typescript
import * as FrameCapture from 'react-native-frame-capture';
import { Platform, PermissionsAndroid } from 'react-native';

// 1. Request notification permission (Android 13+)
if (Platform.OS === 'android' && Platform.Version >= 33) {
  await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
  );
}

// 2. Request screen capture permission
const permissionStatus = await FrameCapture.requestPermission();

if (permissionStatus === FrameCapture.PermissionStatus.GRANTED) {
  // 3. Start capturing
  await FrameCapture.startCapture({
    capture: {
      interval: 1000, // Capture every second
    },
    image: {
      quality: 80,
      format: 'jpeg',
    },
    storage: {
      saveFrames: true,
      location: 'private',
    },
  });

  // 4. Listen for captured frames
  const subscription = FrameCapture.addListener(
    FrameCapture.CaptureEventType.FRAME_CAPTURED,
    (event) => {
      console.log('Frame captured:', event.filePath);
    }
  );

  // 5. Stop capturing when done
  await FrameCapture.stopCapture();
  subscription.remove();
}
```

## 📖 API Reference

### Methods

#### `requestPermission()`

Requests **MediaProjection permission** (screen capture/screen sharing) from the user. This opens the Android system dialog asking the user to allow screen recording.

```typescript
const status = await FrameCapture.requestPermission();
```

**Permission:** `android.permission.SYSTEM_ALERT_WINDOW` (MediaProjection API)

**Returns:** `Promise<PermissionStatus>`

**Note:** This is a runtime permission that must be granted before starting capture. The permission dialog is shown by the Android system, not your app.

---

#### `checkPermission()`

Checks if MediaProjection permission (screen capture) has been previously granted without showing the permission dialog.

```typescript
const status = await FrameCapture.checkPermission();
```

**Returns:** `Promise<PermissionStatus>`

**Note:** Returns `NOT_DETERMINED` if permission was never requested, `GRANTED` if previously granted. MediaProjection permission cannot be checked programmatically on Android, so this only verifies if permission data exists from a previous grant.

---

#### `startCapture(options)`

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

---

#### `stopCapture()`

Stops the active capture session.

```typescript
await FrameCapture.stopCapture();
```

**Returns:** `Promise<void>`

---

#### `pauseCapture()`

Pauses the active capture session.

```typescript
await FrameCapture.pauseCapture();
```

**Returns:** `Promise<void>`

---

#### `resumeCapture()`

Resumes a paused capture session.

```typescript
await FrameCapture.resumeCapture();
```

**Returns:** `Promise<void>`

---

#### `getCaptureStatus()`

Gets the current capture status.

```typescript
const status = await FrameCapture.getCaptureStatus();
console.log(status.state); // 'idle' | 'capturing' | 'paused'
```

**Returns:** `Promise<CaptureStatus>`

---

#### `checkNotificationPermission()`

Checks if notification permission is granted (Android 13+).

```typescript
const status = await FrameCapture.checkNotificationPermission();
```

**Returns:** `Promise<PermissionStatus>`

---

#### `cleanupTempFrames()`

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

---

#### `addListener(eventType, callback)`

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

## ⚙️ Configuration Options

### CaptureOptions

```typescript
interface CaptureOptions {
  capture: CaptureConfig;
  image: ImageConfig;
  storage?: StorageOptions;
  performance?: PerformanceOptions;
  notification?: NotificationOptions;
  overlays?: OverlayConfig[];
}
```

### CaptureConfig

```typescript
interface CaptureConfig {
  interval: number; // Milliseconds between captures (100-60000)
}
```

### ImageConfig

```typescript
interface ImageConfig {
  quality: number; // Image quality 0-100
  format: 'png' | 'jpeg'; // Output format
  scaleResolution?: number; // Resolution scale factor 0.1-1.0
  region?: CaptureRegion; // Custom capture region
  excludeStatusBar?: boolean; // Exclude status bar from capture
}
```

### StorageOptions

```typescript
interface StorageOptions {
  saveFrames?: boolean; // Whether to save frames to storage (default: false)
  location?: 'private' | 'public'; // Storage location (default: 'private')
  outputDirectory?: string; // Custom output directory (must be public path)
  warningThreshold?: number; // Storage warning threshold in bytes (default: 100MB)
  fileNaming?: FileNamingConfig; // File naming configuration
}
```

**Detailed Explanation:**

#### `saveFrames` (default: `false`)

- **`false`**: Frames are stored **temporarily** in the app's cache directory
  - Path: `/data/data/[package]/cache/captured_frames/[sessionId]/`
  - Automatically cleaned on app restart
  - Use for: Upload to server, display in app, temporary processing
  - Call `cleanupTempFrames()` to manually clean up

- **`true`**: Frames are saved **permanently** to device storage
  - Location determined by `location` or `outputDirectory`
  - Persists across app restarts
  - Use for: User wants to keep frames, gallery visibility

#### `location` (default: `'private'`)

Only applies when `saveFrames: true`

- **`'private'`**: App-specific directory (no permissions needed)
  - Path: `/Android/data/[package]/files/Pictures/[sessionId]/`
  - Not visible in gallery
  - Deleted on app uninstall

- **`'public'`**: Public Pictures directory (Android 10+)
  - Path: `/storage/emulated/0/Pictures/[sessionId]/`
  - Visible in gallery
  - Persists after uninstall
  - Falls back to private on Android 9 and below

#### `outputDirectory`

Custom output directory path. **Must be a public storage path** (e.g., `/storage/emulated/0/...`).

- Overrides `location` setting if specified
- Only applies when `saveFrames: true`
- Examples:
  - `/storage/emulated/0/DCIM/MyApp`
  - `/storage/emulated/0/Pictures/CustomFolder`
  - `/storage/emulated/0/Documents/Captures`

**Note:** Cannot use app-specific paths. Use `location: 'private'` instead for app-specific storage.

#### `warningThreshold` (default: `100MB`)

Storage space threshold in bytes. Emits `STORAGE_WARNING` event when available space falls below this value.

- Set to `0` to disable warnings
- Example: `50 * 1024 * 1024` (50MB)

### NotificationOptions

```typescript
interface NotificationOptions {
  title?: string; // Notification title (default: "Screen Capture Active")
  description?: string; // Notification description (supports {frameCount} variable)
  icon?: string; // Large icon drawable resource name
  smallIcon?: string; // Small icon drawable resource name
  color?: string; // Notification color (hex, e.g., "#FF5722")
  channelName?: string; // Notification channel name
  channelDescription?: string; // Channel description
  priority?: 'low' | 'default' | 'high'; // Notification priority
  showFrameCount?: boolean; // Show frame count in notification
  updateInterval?: number; // Frames between notification updates
  pausedTitle?: string; // Title when paused
  pausedDescription?: string; // Description when paused (supports {frameCount})
  showStopAction?: boolean; // Show stop button
  showPauseAction?: boolean; // Show pause button
  showResumeAction?: boolean; // Show resume button
}
```

**Icon Resources:**

Icons must be placed in your Android drawable folder:

1. **Location:** `android/app/src/main/res/drawable/`
2. **Format:** PNG or XML vector drawable
3. **Naming:** Use lowercase with underscores (e.g., `ic_notification.png`, `app_logo.png`)
4. **Usage:** Pass just the filename without extension

```typescript
notification: {
  icon: 'app_logo',        // Uses drawable/app_logo.png
  smallIcon: 'ic_notification', // Uses drawable/ic_notification.png
}
```

**Template Variables:**

- `{frameCount}` - Current frame count (e.g., "Captured 42 frames")

```typescript
notification: {
  description: 'Captured {frameCount} frames',
  pausedDescription: '{frameCount} frames captured so far',
}
```

### OverlayConfig

#### Text Overlay

```typescript
interface TextOverlay {
  type: 'text';
  content: string; // Text content with template variables
  position: OverlayPosition; // Position on screen
  style?: TextStyle; // Text styling
}

interface TextStyle {
  fontSize?: number; // Font size in pixels (default: 14)
  color?: string; // Text color (hex, default: "#FFFFFF")
  backgroundColor?: string; // Background color with alpha (default: "#00000080")
  padding?: number; // Padding in pixels (default: 8)
  fontWeight?: 'normal' | 'bold'; // Font weight (default: "normal")
  textAlign?: 'left' | 'center' | 'right'; // Text alignment (default: "left")
}
```

**Template Variables:**

Text overlays support dynamic template variables that are replaced at capture time:

- **`{frameNumber}`** - Current frame number (zero-based)
  - Example: `0`, `1`, `2`, `42`, `100`
  - Use case: Frame identification, debugging

- **`{timestamp}`** - Current timestamp in ISO 8601 format (UTC)
  - Example: `2024-01-15T10:30:45.123Z`
  - Use case: Time tracking, synchronization

- **`{sessionId}`** - Current capture session ID (UUID)
  - Example: `abc123de-f456-7890-abcd-ef1234567890`
  - Use case: Session identification, grouping frames

**Example:**

```typescript
overlays: [
  {
    type: 'text',
    content: 'Frame {frameNumber} • {timestamp}',
    position: 'bottom-right',
  },
];
// Output: "Frame 42 • 2024-01-15T10:30:45.123Z"
```

**Color Format:**

Colors support hex format with optional alpha channel:

- `#RGB` - 3 digits (e.g., `#F00` = red)
- `#RRGGBB` - 6 digits (e.g., `#FF0000` = red)
- `#AARRGGBB` - 8 digits with alpha (e.g., `#80FF0000` = 50% transparent red)

```typescript
style: {
  color: '#FFFFFF',           // White text
  backgroundColor: '#00000099', // Black background, 60% opacity
}
```

#### Image Overlay

```typescript
interface ImageOverlay {
  type: 'image';
  source: string; // Drawable resource name or file URI
  position: OverlayPosition; // Position on screen
  size?: ImageSize; // Image dimensions
  opacity?: number; // Opacity 0.0-1.0 (default: 1.0)
}
```

**Image Sources:**

Images must be placed in your Android drawable folder:

1. **Location:** `android/app/src/main/res/drawable/`
2. **Format:** PNG, JPG, or XML vector drawable
3. **Naming:** Use lowercase with underscores (e.g., `logo.png`, `watermark.png`)
4. **Usage:** Pass just the filename without extension

```typescript
overlays: [
  {
    type: 'image',
    source: 'logo', // Uses drawable/logo.png
    position: 'top-right',
    size: { width: 50, height: 50 },
  },
];
```

**Alternative Sources:**

You can also use file URIs or content URIs:

- `file:///storage/emulated/0/Pictures/logo.png`
- `content://media/external/images/media/123`

### OverlayPosition

```typescript
type OverlayPosition =
  | 'top-left'
  | 'top-right'
  | 'bottom-left'
  | 'bottom-right'
  | 'center'
  | { x: number; y: number; unit?: 'pixels' | 'percentage' };
```

## 📡 Events

### CaptureEventType

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

### Event Payloads

#### FrameCapturedEvent

```typescript
interface FrameCapturedEvent {
  filePath: string; // Path to captured frame
  timestamp: number; // Capture timestamp
  frameNumber: number; // Frame number in session
  fileSize: number; // File size in bytes
}
```

#### CaptureErrorEvent

```typescript
interface CaptureErrorEvent {
  code: string; // Error code
  message: string; // Error message
  details?: Object; // Additional error details
}
```

#### CaptureStopEvent

```typescript
interface CaptureStopEvent {
  sessionId: string; // Session ID
  totalFrames: number; // Total frames captured
  duration: number; // Session duration in milliseconds
}
```

#### StorageWarningEvent

```typescript
interface StorageWarningEvent {
  availableSpace: number; // Available storage in bytes
  threshold: number; // Warning threshold in bytes
}
```

## ❌ Error Codes

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

## 💡 Usage Examples

### Basic Capture

```typescript
await FrameCapture.startCapture({
  capture: { interval: 1000 },
  image: { quality: 80, format: 'jpeg' },
  storage: { saveFrames: true },
});
```

### Capture with Text Overlay

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

### Capture with Image Watermark

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

### Capture Custom Region

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

### Capture with Resolution Scaling

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

### Custom Notification

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

### Event Handling

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

## 🗂️ Storage Behavior

### Overview

The library provides flexible storage options depending on your use case:

| Scenario                | `saveFrames` | `location`                  | Storage Path           | Cleaned On       |
| ----------------------- | ------------ | --------------------------- | ---------------------- | ---------------- |
| **Temporary (default)** | `false`      | N/A                         | Cache directory        | App restart      |
| **Private permanent**   | `true`       | `'private'`                 | App-specific directory | App uninstall    |
| **Public permanent**    | `true`       | `'public'`                  | Public Pictures        | Never (persists) |
| **Custom permanent**    | `true`       | N/A (use `outputDirectory`) | Custom path            | Never (persists) |

### Temporary Storage (Default)

When `saveFrames: false` (default):

```typescript
await FrameCapture.startCapture({
  storage: {
    saveFrames: false, // or omit this field
  },
});
```

- **Path:** `/data/data/[package]/cache/captured_frames/[sessionId]/`
- **Visibility:** Not accessible to user or other apps
- **Persistence:** Automatically deleted on app restart
- **Use case:** Upload to server, display in app, temporary processing
- **Cleanup:** Call `cleanupTempFrames()` to manually clean up

### Private Storage

When `saveFrames: true` and `location: 'private'` (default):

```typescript
await FrameCapture.startCapture({
  storage: {
    saveFrames: true,
    location: 'private', // default
  },
});
```

- **Path:** `/Android/data/[package]/files/Pictures/[sessionId]/`
- **Visibility:** Not visible in gallery
- **Persistence:** Deleted on app uninstall
- **Permissions:** None required
- **Use case:** App-specific storage, user wants to keep frames

### Public Storage

When `saveFrames: true` and `location: 'public'`:

```typescript
await FrameCapture.startCapture({
  storage: {
    saveFrames: true,
    location: 'public',
  },
});
```

- **Path:** `/storage/emulated/0/Pictures/[sessionId]/`
- **Visibility:** Visible in gallery
- **Persistence:** Persists after uninstall
- **Permissions:** None required (Android 10+)
- **Fallback:** Uses private storage on Android 9 and below
- **Use case:** User wants frames in gallery, sharing with other apps

### Custom Directory

When `saveFrames: true` and `outputDirectory` is specified:

```typescript
await FrameCapture.startCapture({
  storage: {
    saveFrames: true,
    outputDirectory: '/storage/emulated/0/DCIM/MyApp',
  },
});
```

- **Path:** Your specified path
- **Visibility:** Depends on path (DCIM is visible in gallery)
- **Persistence:** Persists after uninstall
- **Permissions:** None required for public paths
- **Restrictions:** Must be a public storage path (e.g., `/storage/emulated/0/...`)
- **Use case:** Custom organization, specific folder requirements

**Valid Custom Paths:**

- `/storage/emulated/0/DCIM/MyApp` - Visible in gallery (Camera folder)
- `/storage/emulated/0/Pictures/MyApp` - Visible in gallery (Pictures folder)
- `/storage/emulated/0/Documents/Captures` - Documents folder
- `/storage/emulated/0/Download/Frames` - Downloads folder

**Invalid Paths:**

- `/data/data/[package]/...` - Use `location: 'private'` instead
- `/Android/data/[package]/...` - Use `location: 'private'` instead
- Relative paths - Must be absolute paths

## 🔒 Permissions

### Required Permissions

The library automatically adds these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

### Runtime Permissions

#### MediaProjection Permission

Required for screen capture. Request using:

```typescript
const status = await FrameCapture.requestPermission();
```

#### Notification Permission (Android 13+)

Required for foreground service notification visibility:

```typescript
import { PermissionsAndroid } from 'react-native';

if (Platform.Version >= 33) {
  await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
  );
}
```

## 🎯 Platform Support

| Platform | Supported | Version |
| -------- | --------- | ------- |
| Android  | ✅ Yes    | 5.0+    |
| iOS      | ❌ No     | N/A     |

## 🏗️ Architecture

- **TurboModule:** New Architecture compatible
- **Foreground Service:** Reliable background capture
- **Kotlin:** Native Android implementation
- **TypeScript:** Type-safe JavaScript API

## 🤝 Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## 📄 License

MIT © [Nasyx Rakeeb](https://github.com/nasyx-rakeeb)

---

Made with ❤️ using [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

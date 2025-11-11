# Configuration

Complete guide to configuring React Native Frame Capture.

## CaptureOptions

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

## CaptureConfig

```typescript
interface CaptureConfig {
  interval: number; // Milliseconds between captures (100-60000)
}
```

## ImageConfig

```typescript
interface ImageConfig {
  quality: number; // Image quality 0-100
  format: 'png' | 'jpeg'; // Output format
  scaleResolution?: number; // Resolution scale factor 0.1-1.0
  region?: CaptureRegion; // Custom capture region
  excludeStatusBar?: boolean; // Exclude status bar from capture
}
```

## StorageOptions

```typescript
interface StorageOptions {
  saveFrames?: boolean; // Whether to save frames to storage (default: false)
  location?: 'private' | 'public'; // Storage location (default: 'private')
  outputDirectory?: string; // Custom output directory (must be public path)
  warningThreshold?: number; // Storage warning threshold in bytes (default: 100MB)
  fileNaming?: FileNamingConfig; // File naming configuration
}
```

### Detailed Explanation

**`saveFrames` (default: `false`)**

- **`false`**: Frames are stored **temporarily** in the app's cache directory
  - Path: `/data/data/[package]/cache/captured_frames/[sessionId]/`
  - Automatically cleaned on app restart
  - Use for: Upload to server, display in app, temporary processing
  - Call `cleanupTempFrames()` to manually clean up

- **`true`**: Frames are saved **permanently** to device storage
  - Location determined by `location` or `outputDirectory`
  - Persists across app restarts
  - Use for: User wants to keep frames, gallery visibility

**`location` (default: `'private'`)**

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

**`outputDirectory`**

Custom output directory path. **Must be a public storage path** (e.g., `/storage/emulated/0/...`).

- Overrides `location` setting if specified
- Only applies when `saveFrames: true`
- Examples:
  - `/storage/emulated/0/DCIM/MyApp`
  - `/storage/emulated/0/Pictures/CustomFolder`
  - `/storage/emulated/0/Documents/Captures`

**Note:** Cannot use app-specific paths. Use `location: 'private'` instead for app-specific storage.

**`warningThreshold` (default: `100MB`)**

Storage space threshold in bytes. Emits `STORAGE_WARNING` event when available space falls below this value.

- Set to `0` to disable warnings
- Example: `50 * 1024 * 1024` (50MB)

## NotificationOptions

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

### Icon Resources

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

### Template Variables

- `{frameCount}` - Current frame count (e.g., "Captured 42 frames")

```typescript
notification: {
  description: 'Captured {frameCount} frames',
  pausedDescription: '{frameCount} frames captured so far',
}
```

## OverlayConfig

### Text Overlay

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

### Template Variables

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

### Color Format

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

### Image Overlay

```typescript
interface ImageOverlay {
  type: 'image';
  source: string; // Drawable resource name or file URI
  position: OverlayPosition; // Position on screen
  size?: ImageSize; // Image dimensions
  opacity?: number; // Opacity 0.0-1.0 (default: 1.0)
}
```

### Image Sources

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

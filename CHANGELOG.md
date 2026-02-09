# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.1] - 2025-02-09

### Fixed

- Added missing event listener registration for `CHANGE_DETECTED` event in the TypeScript layer.

---

## [1.1.0] - 2025-02-09

### Added

#### Change Detection Capture Mode

- New capture mode that captures frames only when screen content changes
- Configurable change detection options:
  - `threshold` - Percentage of pixels that must change to trigger capture (1-100%)
  - `minInterval` - Minimum milliseconds between captures (100-60000ms)
  - `maxInterval` - Maximum milliseconds before forced capture (0 = disabled)
  - `sampleRate` - Pixel sampling rate for performance optimization (1-100)
  - `detectionRegion` - Optional region to monitor for changes
- New `CHANGE_DETECTED` event for debugging and monitoring
- Pixel sampling algorithm for efficient frame comparison

### Example

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
});
```

---

## [1.0.0] - 2025-11-11

### Added

#### Core Capture Features

- Interval-based screen capture with configurable intervals (100ms - 60s)
- MediaProjection API integration for reliable screen capture
- Foreground service for background capture (continues when app is minimized)
- Pause and resume capture functionality
- Real-time capture status monitoring
- Automatic cleanup of temporary frames on app startup

#### Image Processing

- Configurable image quality (0-100)
- Multiple output formats (PNG, JPEG)
- Resolution scaling (0.1x - 1.0x) for reduced file sizes
- Custom region capture (capture specific screen areas)
- Status bar exclusion option
- Efficient bitmap processing with memory management

#### Storage Options

- Flexible storage modes:
  - Temporary cache storage (default, auto-cleaned)
  - Private app-specific storage
  - Public Pictures directory (gallery visible)
  - Custom directory support
- Configurable file naming (prefix, date format, frame padding)
- Storage warning system with configurable threshold (default: 100MB)
- Session-based folder organization

#### Overlay System

- Text overlays with template variables:
  - `{frameNumber}` - Current frame number
  - `{timestamp}` - ISO 8601 timestamp
  - `{sessionId}` - Session identifier
- Text styling options:
  - Font size, color, background color
  - Padding, font weight, text alignment
  - Hex color support with alpha channel
- Image overlays (watermarks/logos):
  - Drawable resource support
  - File URI and content URI support
  - Configurable size and opacity
  - Position presets and custom coordinates
- Overlay image caching for performance (10MB default)

#### Notification System

- Customizable foreground service notification
- Real-time frame count display
- Configurable update intervals
- Custom icons, colors, and text
- Action buttons (pause, resume, stop)
- Template variable support (`{frameCount}`)
- Multiple priority levels (low, default, high)
- Custom notification channels

#### Event System

- Comprehensive event emission:
  - `FRAME_CAPTURED` - Emitted for each captured frame
  - `CAPTURE_START` - Capture session started
  - `CAPTURE_STOP` - Capture session stopped with statistics
  - `CAPTURE_PAUSE` - Capture paused
  - `CAPTURE_RESUME` - Capture resumed
  - `CAPTURE_ERROR` - Error occurred during capture
  - `STORAGE_WARNING` - Low storage space detected
  - `OVERLAY_ERROR` - Overlay rendering failed
- Detailed event payloads with frame info, timestamps, and metadata

#### Error Handling

- Comprehensive error classification:
  - `PERMISSION_DENIED` - MediaProjection permission not granted
  - `ALREADY_CAPTURING` - Capture session already active
  - `INVALID_OPTIONS` - Invalid configuration provided
  - `STORAGE_ERROR` - Failed to save frame
  - `SYSTEM_ERROR` - Unexpected system error
  - `NOT_SUPPORTED` - Feature not supported
- Detailed error messages and debugging information
- Graceful error recovery and cleanup

#### Performance Optimizations

- TurboModule architecture for optimal performance
- Configurable ImageReader buffer count (1-10 buffers)
- Executor thread management with configurable timeouts
- Overlay image caching to reduce I/O
- Efficient bitmap recycling to prevent memory leaks
- Background processing for image conversion and storage

#### Developer Experience

- Full TypeScript support with comprehensive type definitions
- Detailed JSDoc documentation
- Input validation with helpful error messages
- Expo compatibility via config plugin
- React Native New Architecture support
- Kotlin-based native implementation

#### Platform Support

- Android 5.0+ (API 21+) support
- Android 13+ notification permission handling
- Android 10+ MediaStore integration for public storage
- Automatic fallback for older Android versions

### Technical Details

#### Architecture

- **TurboModule**: New Architecture compatible
- **Foreground Service**: Reliable background operation
- **Kotlin**: Modern, type-safe native implementation
- **TypeScript**: Type-safe JavaScript API

#### Dependencies

- React Native >= 0.74
- Android minSdkVersion 21
- Android compileSdkVersion 34

#### Permissions

- `FOREGROUND_SERVICE` - Required for background capture
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - Required for MediaProjection
- `POST_NOTIFICATIONS` - Required for Android 13+ notification visibility (runtime)

### Notes

This is the initial stable release of React Native Frame Capture. The library provides production-ready screen capture functionality with a focus on reliability, performance, and developer experience.

[1.1.1]: https://github.com/nasyx-rakeeb/react-native-frame-capture/releases/tag/v1.1.1
[1.1.0]: https://github.com/nasyx-rakeeb/react-native-frame-capture/releases/tag/v1.1.0
[1.0.0]: https://github.com/nasyx-rakeeb/react-native-frame-capture/releases/tag/v1.0.0

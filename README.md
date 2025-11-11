# React Native Frame Capture

[![npm version](https://img.shields.io/npm/v/react-native-frame-capture.svg)](https://www.npmjs.com/package/react-native-frame-capture)
[![npm downloads](https://img.shields.io/npm/dm/react-native-frame-capture.svg)](https://www.npmjs.com/package/react-native-frame-capture)
[![license](https://img.shields.io/npm/l/react-native-frame-capture.svg)](https://github.com/nasyx-rakeeb/react-native-frame-capture/blob/main/LICENSE)
[![platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)

Reliable screen capture for React Native Android. Capture frames at intervals with customizable overlays and storage options.

## Features

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

## How It Works

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

## Requirements

- React Native >= 0.74
- Android minSdkVersion >= 21 (Android 5.0)
- Android compileSdkVersion >= 34

## Installation

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

## Quick Start

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

## Documentation

- **[API Reference](docs/api-reference.md)** - Complete API documentation
- **[Configuration](docs/configuration.md)** - Configuration options and interfaces
- **[Events](docs/events.md)** - Event types and handling
- **[Usage Examples](docs/usage-examples.md)** - Practical examples for common use cases
- **[Storage](docs/storage.md)** - Storage behavior and options
- **[Permissions](docs/permissions.md)** - Permission requirements and setup

## Platform Support

| Platform | Supported | Version |
| -------- | --------- | ------- |
| Android  | ✅ Yes    | 5.0+    |
| iOS      | ❌ No     | N/A     |

## Architecture

- **TurboModule:** New Architecture compatible
- **Foreground Service:** Reliable background capture
- **Kotlin:** Native Android implementation
- **TypeScript:** Type-safe JavaScript API

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT © [Nasyx Rakeeb](https://github.com/nasyx-rakeeb)

Made with ❤️ using [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

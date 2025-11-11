# React Native Frame Capture

Reliable screen capture for React Native Android. Capture frames at intervals with customizable overlays and storage options.

## 📦 Installation

```bash
npm install react-native-frame-capture
```

## 📚 Documentation

- [API Reference](api-reference.md) - Complete API documentation
- [Configuration](configuration.md) - Configuration options and interfaces
- [Events](events.md) - Event types and handling
- [Usage Examples](usage-examples.md) - Practical examples
- [Storage](storage.md) - Storage behavior and options
- [Permissions](permissions.md) - Permission requirements

## 🚀 Quick Start

```typescript
import * as FrameCapture from 'react-native-frame-capture';

// Request permission
const status = await FrameCapture.requestPermission();

if (status === FrameCapture.PermissionStatus.GRANTED) {
  // Start capturing
  await FrameCapture.startCapture({
    capture: { interval: 1000 },
    image: { quality: 80, format: 'jpeg' },
  });
}
```

## ✨ Features

- 📸 Interval-based capture (100ms - 60s)
- 🎨 Customizable overlays
- 💾 Flexible storage options
- 🔄 Background capture
- ⚡ High performance
- 📱 Expo compatible

## 🔗 Links

- [GitHub Repository](https://github.com/nasyx-rakeeb/react-native-frame-capture)
- [npm Package](https://www.npmjs.com/package/react-native-frame-capture)
- [Example App](https://github.com/nasyx-rakeeb/react-native-frame-capture/tree/main/example)

## 📄 License

MIT © [Nasyx Rakeeb](https://github.com/nasyx-rakeeb)

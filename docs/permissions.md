# Permissions

Complete guide to permissions required by React Native Frame Capture.

## Required Permissions

The library automatically adds these permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

## Runtime Permissions

### MediaProjection Permission

Required for screen capture. Request using:

```typescript
const status = await FrameCapture.requestPermission();
```

This opens the Android system dialog asking the user to allow screen recording.

### Notification Permission (Android 13+)

Required for foreground service notification visibility:

```typescript
import { Platform, PermissionsAndroid } from 'react-native';

if (Platform.OS === 'android' && Platform.Version >= 33) {
  await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
  );
}
```

## Permission Flow

1. **Check notification permission** (Android 13+)
2. **Request MediaProjection permission** using `requestPermission()`
3. **Start capture** once permission is granted

## Example

```typescript
import * as FrameCapture from 'react-native-frame-capture';
import { Platform, PermissionsAndroid } from 'react-native';

async function startCapturing() {
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
      capture: { interval: 1000 },
      image: { quality: 80, format: 'jpeg' },
    });
  }
}
```

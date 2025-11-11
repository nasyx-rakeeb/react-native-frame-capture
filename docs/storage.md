# Storage Behavior

Complete guide to storage options and behavior.

## Overview

The library provides flexible storage options depending on your use case:

| Scenario                | `saveFrames` | `location`                  | Storage Path           | Cleaned On       |
| ----------------------- | ------------ | --------------------------- | ---------------------- | ---------------- |
| **Temporary (default)** | `false`      | N/A                         | Cache directory        | App restart      |
| **Private permanent**   | `true`       | `'private'`                 | App-specific directory | App uninstall    |
| **Public permanent**    | `true`       | `'public'`                  | Public Pictures        | Never (persists) |
| **Custom permanent**    | `true`       | N/A (use `outputDirectory`) | Custom path            | Never (persists) |

## Temporary Storage (Default)

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

## Private Storage

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

## Public Storage

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

## Custom Directory

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

### Valid Custom Paths

- `/storage/emulated/0/DCIM/MyApp` - Visible in gallery (Camera folder)
- `/storage/emulated/0/Pictures/MyApp` - Visible in gallery (Pictures folder)
- `/storage/emulated/0/Documents/Captures` - Documents folder
- `/storage/emulated/0/Download/Frames` - Downloads folder

### Invalid Paths

- `/data/data/[package]/...` - Use `location: 'private'` instead
- `/Android/data/[package]/...` - Use `location: 'private'` instead
- Relative paths - Must be absolute paths

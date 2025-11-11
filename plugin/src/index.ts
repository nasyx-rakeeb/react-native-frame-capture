import type { ConfigPlugin } from '@expo/config-plugins';
import { createRunOncePlugin } from '@expo/config-plugins';

/**
 * Expo Config Plugin for react-native-frame-capture
 *
 * This plugin ensures the library is properly configured for Expo projects.
 * The native Android configuration (permissions, service) is already defined
 * in the library's AndroidManifest.xml and will be merged automatically
 * during the Expo prebuild process.
 *
 * This plugin serves as:
 * 1. A marker that the library supports Expo
 * 2. A placeholder for future configuration options
 * 3. Documentation for Expo users
 */
const withFrameCapture: ConfigPlugin = (config) => {
  // The library's AndroidManifest.xml already contains:
  // - FOREGROUND_SERVICE permission
  // - FOREGROUND_SERVICE_MEDIA_PROJECTION permission (API 34+)
  // - POST_NOTIFICATIONS permission (API 33+)
  // - ScreenCaptureService with proper foregroundServiceType
  //
  // These will be automatically merged by Expo's manifest merger.
  // No additional modifications needed at this time.

  return config;
};

// Read package.json to get the actual version
let pkg: { name: string; version: string } = {
  name: 'react-native-frame-capture',
  version: '1.0.0',
};

try {
  pkg = require('react-native-frame-capture/package.json');
} catch {
  // Fallback to hardcoded values if package.json can't be found
  // This shouldn't happen in normal usage
}

export default createRunOncePlugin(withFrameCapture, pkg.name, pkg.version);

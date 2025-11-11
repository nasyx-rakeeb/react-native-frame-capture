/**
 * React Native Frame Capture
 * Production-grade screen capture library for React Native Android
 *
 * @packageDocumentation
 */

// Export types
export * from './types';

// Export constants
export * from './constants';

// Export errors
export { CaptureError } from './errors';

// Export API functions
export {
  requestPermission,
  checkPermission,
  startCapture,
  stopCapture,
  pauseCapture,
  resumeCapture,
  getCaptureStatus,
  checkNotificationPermission,
  cleanupTempFrames,
} from './api';

// Export event functions
export { addListener, removeAllListeners } from './events';

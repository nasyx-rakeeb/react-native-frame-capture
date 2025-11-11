/**
 * Event handling for React Native Frame Capture
 * @module events
 */

import type { EventSubscription } from 'react-native';
import NativeFrameCapture from './NativeFrameCapture';
import { CaptureEventType, type CaptureEventCallback } from './types';

/**
 * Adds an event listener for capture events
 *
 * @param event - Event type to listen for
 * @param callback - Function to call when event is emitted
 * @returns Subscription object that can be used to remove the listener
 *
 * @example
 * ```typescript
 * const subscription = addListener(
 *   CaptureEventType.FRAME_CAPTURED,
 *   (event) => console.log('Frame captured:', event.filePath)
 * );
 *
 * // Later, remove the listener
 * subscription.remove();
 * ```
 */
export function addListener(
  event: CaptureEventType,
  callback: CaptureEventCallback
): EventSubscription {
  switch (event) {
    case CaptureEventType.FRAME_CAPTURED:
      return NativeFrameCapture.onFrameCaptured(callback as any);
    case CaptureEventType.CAPTURE_ERROR:
      return NativeFrameCapture.onCaptureError(callback as any);
    case CaptureEventType.CAPTURE_STOP:
      return NativeFrameCapture.onCaptureStop(callback as any);
    case CaptureEventType.CAPTURE_START:
      return NativeFrameCapture.onCaptureStart(callback as any);
    case CaptureEventType.STORAGE_WARNING:
      return NativeFrameCapture.onStorageWarning(callback as any);
    case CaptureEventType.CAPTURE_PAUSE:
      return NativeFrameCapture.onCapturePause(callback as any);
    case CaptureEventType.CAPTURE_RESUME:
      return NativeFrameCapture.onCaptureResume(callback as any);
    case CaptureEventType.OVERLAY_ERROR:
      return NativeFrameCapture.onOverlayError(callback as any);
    default:
      throw new Error(`Unknown event type: ${event}`);
  }
}

/**
 * @deprecated Use subscription.remove() instead
 */
export function removeAllListeners(_event: CaptureEventType): void {
  console.warn(
    'removeAllListeners is deprecated in New Architecture. Use subscription.remove() instead.'
  );
}

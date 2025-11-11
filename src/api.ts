/**
 * Core API functions for React Native Frame Capture
 * @module api
 */

import NativeFrameCapture from './NativeFrameCapture';
import { CaptureError } from './errors';
import { validateOptions, mergeOptions } from './validation';
import { normalizeOptions } from './normalize';
import {
  CaptureErrorCode,
  PermissionStatus,
  type CaptureOptions,
  type CaptureSession,
  type CaptureStatus,
} from './types';

/**
 * Requests MediaProjection permission from the user
 *
 * @returns Promise resolving to permission status
 * @throws {CaptureError} if permission request fails
 */
export async function requestPermission(): Promise<PermissionStatus> {
  try {
    const result = await NativeFrameCapture.requestPermission();
    return result as PermissionStatus;
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to request permission',
      error.details
    );
  }
}

/**
 * Checks current permission status without requesting
 *
 * @returns Promise resolving to current permission status
 */
export async function checkPermission(): Promise<PermissionStatus> {
  try {
    const result = await NativeFrameCapture.checkPermission();
    return result as PermissionStatus;
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to check permission',
      error.details
    );
  }
}

/**
 * Starts screen capture with the given options
 *
 * @param options - Capture configuration options (optional, uses defaults if not provided)
 * @returns Promise resolving to capture session information
 * @throws {CaptureError} if capture cannot be started
 */
export async function startCapture(
  options?: Partial<CaptureOptions>
): Promise<CaptureSession> {
  if (options) {
    validateOptions(options);
  }

  const finalOptions = mergeOptions(options);
  const nativeOptions = normalizeOptions(finalOptions);

  try {
    const result = await NativeFrameCapture.startCapture(nativeOptions);
    return result as CaptureSession;
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to start capture',
      error.details
    );
  }
}

/**
 * Stops the active capture session
 *
 * @returns Promise that resolves when capture is stopped
 * @throws {CaptureError} if stop operation fails
 */
export async function stopCapture(): Promise<void> {
  try {
    await NativeFrameCapture.stopCapture();
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to stop capture',
      error.details
    );
  }
}

/**
 * Pauses the active capture session
 *
 * @returns Promise that resolves when capture is paused
 * @throws {CaptureError} if pause operation fails
 */
export async function pauseCapture(): Promise<void> {
  try {
    await NativeFrameCapture.pauseCapture();
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to pause capture',
      error.details
    );
  }
}

/**
 * Resumes a paused capture session
 *
 * @returns Promise that resolves when capture is resumed
 * @throws {CaptureError} if resume operation fails
 */
export async function resumeCapture(): Promise<void> {
  try {
    await NativeFrameCapture.resumeCapture();
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to resume capture',
      error.details
    );
  }
}

/**
 * Gets the current capture status
 *
 * @returns Promise resolving to current capture status
 */
export async function getCaptureStatus(): Promise<CaptureStatus> {
  try {
    const result = await NativeFrameCapture.getCaptureStatus();
    return result as CaptureStatus;
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to get capture status',
      error.details
    );
  }
}

/**
 * Checks if notification permission is granted (Android 13+)
 *
 * @returns Promise resolving to permission status
 */
export async function checkNotificationPermission(): Promise<PermissionStatus> {
  try {
    const result = await NativeFrameCapture.checkNotificationPermission();
    return result as PermissionStatus;
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to check notification permission',
      error.details
    );
  }
}

/**
 * Manually cleans up all temporary frame files
 *
 * @returns Promise that resolves when cleanup is complete
 */
export async function cleanupTempFrames(): Promise<void> {
  try {
    await NativeFrameCapture.cleanupTempFrames();
  } catch (error: any) {
    throw new CaptureError(
      error.code || CaptureErrorCode.SYSTEM_ERROR,
      error.message || 'Failed to cleanup temp frames',
      error.details
    );
  }
}

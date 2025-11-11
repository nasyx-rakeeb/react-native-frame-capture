/**
 * Native TurboModule bridge for React Native Frame Capture
 * @module NativeFrameCapture
 */

import { TurboModuleRegistry, type TurboModule } from 'react-native';

/**
 * EventEmitter type for TurboModule events
 */
type EventEmitter<T> = (callback: (event: T) => void) => { remove: () => void };

/**
 * Event payload types (defined inline for codegen compatibility)
 */
export type FrameCapturedEvent = {
  filePath: string;
  timestamp: number;
  frameNumber: number;
  fileSize: number;
};

export type CaptureErrorEvent = {
  code: string;
  message: string;
  details?: Object;
};

export type CaptureStopEvent = {
  sessionId: string;
  totalFrames: number;
  duration: number;
};

export type CaptureStartEvent = {
  sessionId: string;
  options: Object;
};

export type StorageWarningEvent = {
  availableSpace: number;
  threshold: number;
};

export type CapturePauseEvent = {
  sessionId: string;
};

export type CaptureResumeEvent = {
  sessionId: string;
};

export type OverlayErrorEvent = {
  overlayIndex: number;
  overlayType: string;
  message: string;
};

/**
 * TurboModule specification for FrameCapture
 * Defines the native methods implemented in Kotlin
 */
export interface Spec extends TurboModule {
  // Event Emitters
  readonly onFrameCaptured: EventEmitter<FrameCapturedEvent>;
  readonly onCaptureError: EventEmitter<CaptureErrorEvent>;
  readonly onCaptureStop: EventEmitter<CaptureStopEvent>;
  readonly onCaptureStart: EventEmitter<CaptureStartEvent>;
  readonly onStorageWarning: EventEmitter<StorageWarningEvent>;
  readonly onCapturePause: EventEmitter<CapturePauseEvent>;
  readonly onCaptureResume: EventEmitter<CaptureResumeEvent>;
  readonly onOverlayError: EventEmitter<OverlayErrorEvent>;

  // Methods
  requestPermission(): Promise<string>;
  checkPermission(): Promise<string>;
  startCapture(options: Object): Promise<Object>;
  stopCapture(): Promise<void>;
  pauseCapture(): Promise<void>;
  resumeCapture(): Promise<void>;
  getCaptureStatus(): Promise<Object>;
  checkNotificationPermission(): Promise<string>;
  cleanupTempFrames(): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('FrameCapture');

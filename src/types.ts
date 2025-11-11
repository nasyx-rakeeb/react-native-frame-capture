/**
 * Type definitions for React Native Frame Capture
 * @module types
 */

// ============================================================================
// Enums
// ============================================================================

/**
 * Capture state values
 */
export enum CaptureState {
  IDLE = 'idle',
  CAPTURING = 'capturing',
  PAUSED = 'paused',
  STOPPING = 'stopping',
}

/**
 * Permission status values
 */
export enum PermissionStatus {
  GRANTED = 'granted',
  DENIED = 'denied',
  NOT_DETERMINED = 'not_determined',
}

/**
 * Error codes for capture operations
 */
export enum CaptureErrorCode {
  PERMISSION_DENIED = 'PERMISSION_DENIED',
  ALREADY_CAPTURING = 'ALREADY_CAPTURING',
  INVALID_OPTIONS = 'INVALID_OPTIONS',
  STORAGE_ERROR = 'STORAGE_ERROR',
  SYSTEM_ERROR = 'SYSTEM_ERROR',
  NOT_SUPPORTED = 'NOT_SUPPORTED',
}

/**
 * Event types emitted by the capture system
 */
export enum CaptureEventType {
  FRAME_CAPTURED = 'onFrameCaptured',
  CAPTURE_ERROR = 'onCaptureError',
  CAPTURE_STOP = 'onCaptureStop',
  CAPTURE_START = 'onCaptureStart',
  STORAGE_WARNING = 'onStorageWarning',
  CAPTURE_PAUSE = 'onCapturePause',
  CAPTURE_RESUME = 'onCaptureResume',
  OVERLAY_ERROR = 'onOverlayError',
}

// ============================================================================
// Basic Types
// ============================================================================

/**
 * Unit type for capture region coordinates
 */
export type CaptureRegionUnit = 'pixels' | 'percentage';

/**
 * Position preset for overlay placement
 */
export type PositionPreset =
  | 'top-left'
  | 'top-right'
  | 'bottom-left'
  | 'bottom-right'
  | 'center';

// ============================================================================
// Capture Configuration
// ============================================================================

/**
 * Custom capture region specification
 */
export interface CaptureRegion {
  /** Left position (pixels or 0-1 for percentage) */
  x: number;
  /** Top position (pixels or 0-1 for percentage) */
  y: number;
  /** Width (pixels or 0-1 for percentage) */
  width: number;
  /** Height (pixels or 0-1 for percentage) */
  height: number;
  /** Unit type for coordinates (default: 'percentage') */
  unit?: CaptureRegionUnit;
}

/**
 * File naming configuration
 */
export interface FileNamingConfig {
  /** Filename prefix (default: "capture_") */
  prefix?: string;
  /** Date format pattern (default: "yyyyMMdd_HHmmss_SSS") */
  dateFormat?: string;
  /** Frame number padding digits (default: 5) */
  framePadding?: number;
}

/**
 * Notification customization options for the foreground service
 */
export interface NotificationOptions {
  title?: string;
  description?: string;
  icon?: string;
  smallIcon?: string;
  color?: string;
  channelName?: string;
  channelDescription?: string;
  priority?: 'low' | 'default' | 'high';
  showFrameCount?: boolean;
  updateInterval?: number;
  pausedTitle?: string;
  pausedDescription?: string;
  showStopAction?: boolean;
  showPauseAction?: boolean;
  showResumeAction?: boolean;
}

/**
 * Capture behavior configuration
 */
export interface CaptureConfig {
  /** Milliseconds between captures (100-60000) */
  interval: number;
}

/**
 * Image processing configuration
 */
export interface ImageConfig {
  /** Image quality 0-100 */
  quality: number;
  /** Output format */
  format: 'png' | 'jpeg';
  /** Resolution scale factor 0.1-1.0 */
  scaleResolution?: number;
  /** Custom capture region */
  region?: CaptureRegion;
  /** Exclude status bar from capture */
  excludeStatusBar?: boolean;
}

/**
 * Storage configuration
 */
export interface StorageOptions {
  /** Whether to save captured frames to device storage */
  saveFrames?: boolean;
  /** Storage location for captured frames */
  location?: 'private' | 'public';
  /** Custom output directory */
  outputDirectory?: string;
  /** Storage warning threshold in bytes (default: 100MB, set to 0 to disable) */
  warningThreshold?: number;
  /** File naming configuration */
  fileNaming?: FileNamingConfig;
}

/**
 * Performance tuning configuration
 */
export interface PerformanceOptions {
  /** Overlay image cache size in bytes (default: 10MB) */
  overlayCacheSize?: number;
  /** ImageReader buffer count (default: 2) */
  imageReaderBuffers?: number;
  /** Executor shutdown timeout in milliseconds (default: 5000ms) */
  executorShutdownTimeout?: number;
  /** Forced executor shutdown timeout in milliseconds (default: 1000ms) */
  executorForcedShutdownTimeout?: number;
}

/**
 * Configuration options for screen capture
 */
export interface CaptureOptions {
  /** Capture behavior configuration */
  capture: CaptureConfig;
  /** Image processing configuration */
  image: ImageConfig;
  /** Storage configuration */
  storage?: StorageOptions;
  /** Performance tuning configuration */
  performance?: PerformanceOptions;
  /** Notification customization options */
  notification?: NotificationOptions;
  /** Array of overlays to render on captured frames */
  overlays?: OverlayConfig[];
}

// ============================================================================
// Overlay Configuration
// ============================================================================

/**
 * Custom position coordinates for overlay placement
 */
export interface PositionCoordinates {
  x: number;
  y: number;
  unit?: 'pixels' | 'percentage';
}

/**
 * Overlay position - either a preset string or custom coordinates
 */
export type OverlayPosition = PositionPreset | PositionCoordinates;

/**
 * Text overlay style configuration
 */
export interface TextStyle {
  fontSize?: number;
  color?: string;
  backgroundColor?: string;
  padding?: number;
  fontWeight?: 'normal' | 'bold';
  textAlign?: 'left' | 'center' | 'right';
}

/**
 * Text overlay configuration
 */
export interface TextOverlay {
  type: 'text';
  content: string;
  position: OverlayPosition;
  style?: TextStyle;
}

/**
 * Image size configuration
 */
export interface ImageSize {
  width: number;
  height: number;
}

/**
 * Image overlay configuration
 */
export interface ImageOverlay {
  type: 'image';
  source: string;
  position: OverlayPosition;
  size?: ImageSize;
  opacity?: number;
}

/**
 * Union type for all overlay configurations
 */
export type OverlayConfig = TextOverlay | ImageOverlay;

// ============================================================================
// Session and Status
// ============================================================================

/**
 * Information about an active capture session
 */
export interface CaptureSession {
  id: string;
  startTime: number;
  frameCount: number;
  options: CaptureOptions;
}

/**
 * Current capture status
 */
export interface CaptureStatus {
  state: CaptureState;
  session: CaptureSession | null;
  isPaused: boolean;
}

// ============================================================================
// Event Interfaces
// ============================================================================

/**
 * Re-export event types from NativeFrameCapture for consistency
 * These are defined in NativeFrameCapture.ts for codegen compatibility
 */
import type {
  FrameCapturedEvent,
  CaptureErrorEvent,
  CaptureStopEvent,
  CaptureStartEvent,
  StorageWarningEvent,
  CapturePauseEvent,
  CaptureResumeEvent,
  OverlayErrorEvent,
} from './NativeFrameCapture';

export type {
  FrameCapturedEvent,
  CaptureErrorEvent,
  CaptureStopEvent,
  CaptureStartEvent,
  StorageWarningEvent,
  CapturePauseEvent,
  CaptureResumeEvent,
  OverlayErrorEvent,
};

/**
 * Union type for all event callbacks
 */
export type CaptureEventCallback =
  | ((event: FrameCapturedEvent) => void)
  | ((event: CaptureErrorEvent) => void)
  | ((event: CaptureStopEvent) => void)
  | ((event: CaptureStartEvent) => void)
  | ((event: StorageWarningEvent) => void)
  | ((event: CapturePauseEvent) => void)
  | ((event: CaptureResumeEvent) => void)
  | ((event: OverlayErrorEvent) => void);

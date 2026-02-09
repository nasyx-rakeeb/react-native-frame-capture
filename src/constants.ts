/**
 * Constants for React Native Frame Capture
 * @module constants
 */

import type {
  CaptureOptions,
  FileNamingConfig,
  PerformanceOptions,
} from './types';

/**
 * Default configuration options
 */
export const DEFAULT_OPTIONS: CaptureOptions = {
  capture: {
    interval: 1000,
  },
  image: {
    quality: 80,
    format: 'jpeg',
  },
};

/**
 * Default advanced configuration (for validation fallbacks)
 */
export const DEFAULT_ADVANCED_CONFIG = {
  storage: {
    warningThreshold: 100 * 1024 * 1024, // 100MB
  },
  fileNaming: {
    prefix: 'capture_',
    dateFormat: 'yyyyMMdd_HHmmss_SSS',
    framePadding: 5,
  } as Required<FileNamingConfig>,
  performance: {
    overlayCacheSize: 10 * 1024 * 1024, // 10MB
    imageReaderBuffers: 2,
    executorShutdownTimeout: 5000,
    executorForcedShutdownTimeout: 1000,
  } as Required<PerformanceOptions>,
};

/**
 * Minimum interval between captures (milliseconds)
 */
export const MIN_INTERVAL = 100;

/**
 * Maximum interval between captures (milliseconds)
 */
export const MAX_INTERVAL = 60000;

/**
 * Minimum image quality value
 */
export const MIN_QUALITY = 0;

/**
 * Maximum image quality value
 */
export const MAX_QUALITY = 100;

/**
 * Storage warning threshold (bytes)
 * @deprecated Use advanced.storage.warningThreshold in CaptureOptions instead
 */
export const STORAGE_WARNING_THRESHOLD = 100 * 1024 * 1024; // 100MB

// =============================================================================
// Change Detection Mode Constants
// =============================================================================

/**
 * Default change detection threshold (percentage of pixels changed)
 */
export const DEFAULT_CHANGE_THRESHOLD = 5;

/**
 * Minimum change detection threshold
 */
export const MIN_CHANGE_THRESHOLD = 0;

/**
 * Maximum change detection threshold
 */
export const MAX_CHANGE_THRESHOLD = 100;

/**
 * Default minimum interval between captures in change detection mode (ms)
 */
export const DEFAULT_CHANGE_MIN_INTERVAL = 500;

/**
 * Default maximum interval between captures in change detection mode (ms)
 * 0 means disabled - no forced captures when screen is static
 */
export const DEFAULT_CHANGE_MAX_INTERVAL = 0;

/**
 * Default sample rate for change detection (every Nth pixel)
 */
export const DEFAULT_CHANGE_SAMPLE_RATE = 16;

/**
 * Minimum sample rate (1 = every pixel)
 */
export const MIN_CHANGE_SAMPLE_RATE = 1;

/**
 * Maximum sample rate
 */
export const MAX_CHANGE_SAMPLE_RATE = 64;

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

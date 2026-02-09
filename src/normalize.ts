/**
 * Normalization utilities for converting organized CaptureOptions
 * to the flat structure expected by native code
 * @module normalize
 */

import type { CaptureOptions, CaptureMode } from './types';

/**
 * Flat options structure expected by native code
 */
export interface NativeCaptureOptions {
  // Capture mode
  captureMode: CaptureMode;
  interval?: number;
  // Change detection options (flat)
  changeThreshold?: number;
  changeMinInterval?: number;
  changeMaxInterval?: number;
  changeSampleRate?: number;
  changeDetectionRegion?: any;
  // Image options
  quality: number;
  format: 'png' | 'jpeg';
  saveFrames?: boolean;
  storageLocation?: 'private' | 'public';
  outputDirectory?: string;
  scaleResolution?: number;
  captureRegion?: any;
  excludeStatusBar?: boolean;
  notification?: any;
  overlays?: any[];
  advanced?: {
    storage?: {
      warningThreshold?: number;
    };
    fileNaming?: {
      prefix?: string;
      dateFormat?: string;
      framePadding?: number;
    };
    performance?: {
      overlayCacheSize?: number;
      imageReaderBuffers?: number;
      executorShutdownTimeout?: number;
      executorForcedShutdownTimeout?: number;
    };
  };
}

/**
 * Normalizes organized CaptureOptions to flat structure for native code
 */
export function normalizeOptions(
  options: CaptureOptions
): NativeCaptureOptions {
  const { capture, image, storage, performance, notification, overlays } =
    options;

  const captureMode = capture.mode || 'interval';
  const changeDetection = capture.changeDetection;

  return {
    // Capture mode
    captureMode,
    interval: capture.interval,

    // Change detection config (flattened)
    changeThreshold: changeDetection?.threshold,
    changeMinInterval: changeDetection?.minInterval,
    changeMaxInterval: changeDetection?.maxInterval,
    changeSampleRate: changeDetection?.sampleRate,
    changeDetectionRegion: changeDetection?.detectionRegion,

    // Image config
    quality: image.quality,
    format: image.format,
    scaleResolution: image.scaleResolution,
    captureRegion: image.region,
    excludeStatusBar: image.excludeStatusBar,

    // Storage config
    saveFrames: storage?.saveFrames,
    storageLocation: storage?.location,
    outputDirectory: storage?.outputDirectory,

    // Pass through as-is
    notification,
    overlays,

    // Advanced config (nested structure for native)
    advanced: {
      storage:
        storage?.warningThreshold !== undefined
          ? { warningThreshold: storage.warningThreshold }
          : undefined,
      fileNaming: storage?.fileNaming,
      performance,
    },
  };
}

/**
 * Input validation utilities
 * @module validation
 */

import { CaptureErrorCode } from './types';
import { CaptureError } from './errors';
import {
  MIN_INTERVAL,
  MAX_INTERVAL,
  MIN_QUALITY,
  MAX_QUALITY,
} from './constants';
import type { CaptureOptions } from './types';

/**
 * Validates capture options
 * @throws {CaptureError} if validation fails
 */
export function validateOptions(options: Partial<CaptureOptions>): void {
  // Validate capture config
  if (options.capture) {
    const { interval } = options.capture;

    if (interval !== undefined) {
      if (
        typeof interval !== 'number' ||
        interval < MIN_INTERVAL ||
        interval > MAX_INTERVAL
      ) {
        throw new CaptureError(
          CaptureErrorCode.INVALID_OPTIONS,
          `capture.interval must be a number between ${MIN_INTERVAL} and ${MAX_INTERVAL} milliseconds`
        );
      }
    }
  }

  // Validate image config
  if (options.image) {
    const { quality, format, scaleResolution, region, excludeStatusBar } =
      options.image;

    if (quality !== undefined) {
      if (
        typeof quality !== 'number' ||
        quality < MIN_QUALITY ||
        quality > MAX_QUALITY
      ) {
        throw new CaptureError(
          CaptureErrorCode.INVALID_OPTIONS,
          `image.quality must be a number between ${MIN_QUALITY} and ${MAX_QUALITY}`
        );
      }
    }

    if (format !== undefined) {
      if (format !== 'png' && format !== 'jpeg') {
        throw new CaptureError(
          CaptureErrorCode.INVALID_OPTIONS,
          `image.format must be either 'png' or 'jpeg'`
        );
      }
    }

    if (scaleResolution !== undefined) {
      if (
        typeof scaleResolution !== 'number' ||
        scaleResolution < 0.1 ||
        scaleResolution > 1.0
      ) {
        throw new CaptureError(
          CaptureErrorCode.INVALID_OPTIONS,
          `image.scaleResolution must be a number between 0.1 and 1.0`
        );
      }
    }

    if (region !== undefined) {
      validateCaptureRegion(region);
    }

    if (
      excludeStatusBar !== undefined &&
      typeof excludeStatusBar !== 'boolean'
    ) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `image.excludeStatusBar must be a boolean`
      );
    }
  }

  // Validate storage config
  if (options.storage) {
    const {
      saveFrames,
      location,
      outputDirectory,
      warningThreshold,
      fileNaming,
    } = options.storage;

    if (saveFrames !== undefined && typeof saveFrames !== 'boolean') {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `storage.saveFrames must be a boolean`
      );
    }

    if (location !== undefined) {
      if (location !== 'private' && location !== 'public') {
        throw new CaptureError(
          CaptureErrorCode.INVALID_OPTIONS,
          `storage.location must be either 'private' or 'public'`
        );
      }
    }

    if (outputDirectory !== undefined) {
      if (
        typeof outputDirectory !== 'string' ||
        outputDirectory.trim() === ''
      ) {
        throw new CaptureError(
          CaptureErrorCode.INVALID_OPTIONS,
          `storage.outputDirectory must be a non-empty string`
        );
      }
    }

    if (warningThreshold !== undefined) {
      if (typeof warningThreshold !== 'number' || warningThreshold < 0) {
        throw new CaptureError(
          CaptureErrorCode.INVALID_OPTIONS,
          `storage.warningThreshold must be a non-negative number (bytes)`
        );
      }
    }

    if (fileNaming) {
      validateFileNaming(fileNaming);
    }
  }

  // Validate performance config
  if (options.performance) {
    validatePerformance(options.performance);
  }
}

/**
 * Validates file naming configuration
 */
function validateFileNaming(fn: any): void {
  if (fn.prefix !== undefined) {
    if (typeof fn.prefix !== 'string' || fn.prefix.trim() === '') {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `storage.fileNaming.prefix must be a non-empty string`
      );
    }
    if (/[<>:"/\\|?*]/.test(fn.prefix)) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `storage.fileNaming.prefix contains invalid filename characters`
      );
    }
  }

  if (fn.dateFormat !== undefined) {
    if (typeof fn.dateFormat !== 'string' || fn.dateFormat.trim() === '') {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `storage.fileNaming.dateFormat must be a non-empty string`
      );
    }
  }

  if (fn.framePadding !== undefined) {
    if (
      typeof fn.framePadding !== 'number' ||
      fn.framePadding < 1 ||
      fn.framePadding > 10
    ) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `storage.fileNaming.framePadding must be between 1 and 10`
      );
    }
  }
}

/**
 * Validates performance configuration
 */
function validatePerformance(perf: any): void {
  if (perf.overlayCacheSize !== undefined) {
    if (
      typeof perf.overlayCacheSize !== 'number' ||
      perf.overlayCacheSize < 0
    ) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `performance.overlayCacheSize must be a non-negative number (bytes)`
      );
    }
  }

  if (perf.imageReaderBuffers !== undefined) {
    if (
      typeof perf.imageReaderBuffers !== 'number' ||
      perf.imageReaderBuffers < 1 ||
      perf.imageReaderBuffers > 10
    ) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `performance.imageReaderBuffers must be between 1 and 10`
      );
    }
  }

  if (perf.executorShutdownTimeout !== undefined) {
    if (
      typeof perf.executorShutdownTimeout !== 'number' ||
      perf.executorShutdownTimeout < 100 ||
      perf.executorShutdownTimeout > 30000
    ) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `performance.executorShutdownTimeout must be between 100 and 30000ms`
      );
    }
  }

  if (perf.executorForcedShutdownTimeout !== undefined) {
    if (
      typeof perf.executorForcedShutdownTimeout !== 'number' ||
      perf.executorForcedShutdownTimeout < 100 ||
      perf.executorForcedShutdownTimeout > 10000
    ) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `performance.executorForcedShutdownTimeout must be between 100 and 10000ms`
      );
    }
  }
}

/**
 * Validates capture region configuration
 */
function validateCaptureRegion(region: any): void {
  const unit = region.unit || 'percentage';

  if (
    typeof region.x !== 'number' ||
    typeof region.y !== 'number' ||
    typeof region.width !== 'number' ||
    typeof region.height !== 'number'
  ) {
    throw new CaptureError(
      CaptureErrorCode.INVALID_OPTIONS,
      `image.region coordinates must be numbers`
    );
  }

  if (unit === 'percentage') {
    if (
      region.x < 0 ||
      region.x > 1 ||
      region.y < 0 ||
      region.y > 1 ||
      region.width < 0 ||
      region.width > 1 ||
      region.height < 0 ||
      region.height > 1
    ) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `image.region percentage values must be between 0 and 1`
      );
    }
    if (region.x + region.width > 1 || region.y + region.height > 1) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `image.region extends beyond screen bounds (x + width or y + height > 1)`
      );
    }
  } else if (unit === 'pixels') {
    if (region.width <= 0 || region.height <= 0) {
      throw new CaptureError(
        CaptureErrorCode.INVALID_OPTIONS,
        `image.region width and height must be positive`
      );
    }
  }
}

/**
 * Merges user options with defaults
 */
export function mergeOptions(
  options?: Partial<CaptureOptions>
): CaptureOptions {
  return {
    capture: {
      interval: 1000,
      ...options?.capture,
    },
    image: {
      quality: 80,
      format: 'jpeg',
      ...options?.image,
    },
    storage: options?.storage,
    performance: options?.performance,
    notification: options?.notification,
    overlays: options?.overlays,
  };
}

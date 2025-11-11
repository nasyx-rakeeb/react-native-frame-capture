/**
 * Error handling for React Native Frame Capture
 * @module errors
 */

import { CaptureErrorCode } from './types';

/**
 * Custom error class for capture operations
 */
export class CaptureError extends Error {
  code: CaptureErrorCode;
  details?: any;

  constructor(code: CaptureErrorCode, message: string, details?: any) {
    super(message);
    this.name = 'CaptureError';
    this.code = code;
    this.details = details;
  }
}

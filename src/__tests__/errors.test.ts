import { CaptureError } from '../errors';
import { CaptureErrorCode } from '../types';

describe('CaptureError', () => {
  it('should create error with code and message', () => {
    const error = new CaptureError(
      CaptureErrorCode.PERMISSION_DENIED,
      'Permission denied'
    );

    expect(error).toBeInstanceOf(Error);
    expect(error).toBeInstanceOf(CaptureError);
    expect(error.name).toBe('CaptureError');
    expect(error.code).toBe(CaptureErrorCode.PERMISSION_DENIED);
    expect(error.message).toBe('Permission denied');
  });

  it('should create error with details', () => {
    const details = { permission: 'MEDIA_PROJECTION' };
    const error = new CaptureError(
      CaptureErrorCode.PERMISSION_DENIED,
      'Permission denied',
      details
    );

    expect(error.details).toEqual(details);
  });

  it('should create error without details', () => {
    const error = new CaptureError(
      CaptureErrorCode.SYSTEM_ERROR,
      'System error'
    );

    expect(error.details).toBeUndefined();
  });

  it('should support all error codes', () => {
    const codes = [
      CaptureErrorCode.PERMISSION_DENIED,
      CaptureErrorCode.ALREADY_CAPTURING,
      CaptureErrorCode.INVALID_OPTIONS,
      CaptureErrorCode.STORAGE_ERROR,
      CaptureErrorCode.SYSTEM_ERROR,
      CaptureErrorCode.NOT_SUPPORTED,
    ];

    codes.forEach((code) => {
      const error = new CaptureError(code, 'Test error');
      expect(error.code).toBe(code);
    });
  });
});
